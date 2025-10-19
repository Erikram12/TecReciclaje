package com.example.tecreciclaje.Model

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.example.tecreciclaje.MainActivity
import com.example.tecreciclaje.R
import com.example.tecreciclaje.domain.model.Usuario
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

class NfcBottomSheetDialogFragment : BottomSheetDialogFragment() {

    // Interface para comunicar con la actividad
    interface OnRegistrationCancelledListener {
        fun onRegistrationCancelled()
    }

    private var cancellationListener: OnRegistrationCancelledListener? = null

    companion object {
        private const val ARG_NOMBRE = "nombre"
        private const val ARG_APELLIDO = "apellido"
        private const val ARG_NUM_CONTROL = "numControl"
        private const val ARG_CARRERA = "carrera"
        private const val ARG_EMAIL = "email"
        private const val ARG_PASSWORD = "password"
        private const val ARG_ROLE = "role"
        private const val ARG_PERFIL = "perfil"
        private const val ARG_IS_GOOGLE = "isGoogle"

        fun newInstance(
            nombre: String, apellido: String, numControl: String, carrera: String,
            email: String, password: String, role: String, perfil: String, isGoogleSignIn: Boolean
        ): NfcBottomSheetDialogFragment {
            val f = NfcBottomSheetDialogFragment()
            val b = Bundle()
            b.putString(ARG_NOMBRE, nombre)
            b.putString(ARG_APELLIDO, apellido)
            b.putString(ARG_NUM_CONTROL, numControl)
            b.putString(ARG_CARRERA, carrera)
            b.putString(ARG_EMAIL, email)
            b.putString(ARG_PASSWORD, password)
            b.putString(ARG_ROLE, role)
            b.putString(ARG_PERFIL, perfil)
            b.putBoolean(ARG_IS_GOOGLE, isGoogleSignIn)
            f.arguments = b
            return f
        }

        // Método de compatibilidad para llamadas existentes sin isGoogle
        fun newInstance(
            nombre: String, apellido: String, numControl: String, carrera: String,
            email: String, password: String, role: String, perfil: String
        ): NfcBottomSheetDialogFragment {
            return newInstance(nombre, apellido, numControl, carrera, email, password, role, perfil, false)
        }
    }

    private lateinit var nombre: String
    private lateinit var apellido: String
    private lateinit var numControl: String
    private lateinit var carrera: String
    private lateinit var email: String
    private lateinit var password: String
    private lateinit var role: String
    private lateinit var perfil: String
    private var isGoogleSignIn: Boolean = false
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var btnCancelar: Button
    private var registroCancelado = false

    // Método para establecer el listener
    fun setOnRegistrationCancelledListener(listener: OnRegistrationCancelledListener?) {
        this.cancellationListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        if (args != null) {
            nombre = args.getString(ARG_NOMBRE) ?: ""
            apellido = args.getString(ARG_APELLIDO) ?: ""
            numControl = args.getString(ARG_NUM_CONTROL) ?: ""
            carrera = args.getString(ARG_CARRERA) ?: ""
            email = args.getString(ARG_EMAIL) ?: ""
            password = args.getString(ARG_PASSWORD) ?: ""
            role = args.getString(ARG_ROLE) ?: ""
            perfil = args.getString(ARG_PERFIL) ?: ""
            isGoogleSignIn = args.getBoolean(ARG_IS_GOOGLE, false)
        }
        isCancelable = true
    }

    @Nullable
    override fun onCreateView(
        // A esto (sin la anotación):
        inflater: LayoutInflater,
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_nfc, container, false)
        
        // Configurar botón cancelar
        btnCancelar = view.findViewById(R.id.btnCancelar)
        btnCancelar.setOnClickListener {
            registroCancelado = true
            context?.let { ctx ->
                Toast.makeText(ctx, "Registro cancelado", Toast.LENGTH_SHORT).show()
            }
            // Notificar a la actividad que se canceló el registro
            cancellationListener?.onRegistrationCancelled()
            dismiss()
        }
        
        return view
    }

    override fun onResume() {
        super.onResume()
        val act = requireActivity()
        nfcAdapter = NfcAdapter.getDefaultAdapter(act)
        nfcAdapter?.enableReaderMode(
            act, 
            this::onTagDiscovered,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(requireActivity())
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpiar el estado cuando se destruye el fragment
        registroCancelado = false
    }

    private fun onTagDiscovered(tag: Tag) {
        // Verificar si el registro fue cancelado
        if (registroCancelado) return
        
        val nfcUid = bytesToHex(tag.id)
        requireActivity().runOnUiThread {
            if (isGoogleSignIn) {
                // Para registro con Google, no crear nueva cuenta
                registrarUsuarioGoogle(nombre, apellido, numControl, carrera, email, role, perfil, nfcUid)
            } else {
                // Para registro normal, crear cuenta con email y password
                registrarUsuario(nombre, apellido, numControl, carrera, email, password, role, perfil, nfcUid)
            }
        }
    }

    private fun registrarUsuarioGoogle(
        nombre: String, apellido: String, numControl: String, carrera: String,
        email: String, role: String, perfil: String, nfcUid: String
    ) {
        // Verificar si el registro fue cancelado
        if (registroCancelado) return

        // Verificar que el usuario esté autenticado
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            context?.let { ctx ->
                Toast.makeText(ctx, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val authUid = currentUser.uid

        context?.let { ctx ->
            Toast.makeText(ctx, "Usuario autenticado con UID: $authUid", Toast.LENGTH_SHORT).show()
        }

        // Obtener token FCM y guardar datos
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { tokenTask ->
                // Verificar si el registro fue cancelado
                if (registroCancelado) return@addOnCompleteListener
                
                if (!tokenTask.isSuccessful()) {
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Error obteniendo token FCM", Toast.LENGTH_SHORT).show()
                    }
                    return@addOnCompleteListener
                }

                val token = tokenTask.result

                // Guardar usuario en la base de datos
                val db = FirebaseDatabase.getInstance().reference

                // Crear objeto Usuario
                val usuario = Usuario(authUid, nombre, apellido, numControl, carrera, email, role, perfil, nfcUid)
                usuario.usuario_tokenFCM = token
                usuario.usuario_provider = "google" // Establecer provider para Google

                // Guardar usuario
                db.child("usuarios").child(authUid).setValue(usuario)
                    .addOnSuccessListener {
                        // Guardar índice NFC
                        db.child("nfc_index").child(nfcUid).setValue(authUid)
                            .addOnSuccessListener {
                                // Crear usuario_puntos para activar Cloud Function
                                db.child("usuarios").child(authUid).child("usuario_puntos").setValue(0)
                                    .addOnSuccessListener {
                                        context?.let { ctx ->
                                            Toast.makeText(ctx, "Registro completo con NFC", Toast.LENGTH_SHORT).show()
                                        }
                                        // Redirigir a MainActivity
                                        val intent = Intent(requireContext(), MainActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                        dismiss()
                                    }
                                    .addOnFailureListener { e ->
                                        context?.let { ctx ->
                                            Toast.makeText(ctx, "Error guardando puntos: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                            .addOnFailureListener { e ->
                                context?.let { ctx ->
                                    Toast.makeText(ctx, "Error guardando índice NFC: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                    .addOnFailureListener { e ->
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Error guardando datos: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
    }

    private fun registrarUsuario(
        nombre: String, apellido: String, numControl: String, carrera: String,
        email: String, password: String, role: String, perfil: String, nfcUid: String
    ) {
        // Verificar si el registro fue cancelado
        if (registroCancelado) return

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful()) {
                    val authUid = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnCompleteListener

                    // Crear usuario
                    val usuario = Usuario(authUid, nombre, apellido, numControl, carrera, email, role, perfil, nfcUid)
                    usuario.usuario_puntos = 0
                    usuario.usuario_provider = "email" // Establecer provider para email

                    val db = FirebaseDatabase.getInstance().reference
                    db.child("usuarios").child(authUid).setValue(usuario)
                        .addOnSuccessListener {
                            // Guardar índice NFC
                            db.child("nfc_index").child(nfcUid).setValue(authUid)
                                .addOnSuccessListener {
                                    // Crear usuario_puntos para activar Cloud Function
                                    db.child("usuarios").child(authUid).child("usuario_puntos").setValue(0)
                                        .addOnSuccessListener {
                                            context?.let { ctx ->
                                                Toast.makeText(ctx, "Registro completo con NFC", Toast.LENGTH_SHORT).show()
                                            }
                                            // Redirigir a MainActivity
                                            val intent = Intent(requireContext(), MainActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                            dismiss()
                                        }
                                        .addOnFailureListener { e ->
                                            context?.let { ctx ->
                                                Toast.makeText(ctx, "Error guardando puntos: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                }
                                .addOnFailureListener { e ->
                                    context?.let { ctx ->
                                        Toast.makeText(ctx, "Error guardando índice NFC: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                        .addOnFailureListener { e ->
                            context?.let { ctx ->
                                Toast.makeText(ctx, "Error guardando datos: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }
}
