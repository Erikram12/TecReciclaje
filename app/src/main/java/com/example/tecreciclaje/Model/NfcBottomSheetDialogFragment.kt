package com.example.tecreciclaje.Model

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.annotation.Nullable
import com.example.tecreciclaje.MainActivity
import com.example.tecreciclaje.R
import com.example.tecreciclaje.domain.model.Usuario
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging

class NfcBottomSheetDialogFragment : BottomSheetDialogFragment() {

    // Interface para comunicar con la actividad
    interface OnRegistrationCancelledListener {
        fun onRegistrationCancelled()
    }

    private var cancellationListener: OnRegistrationCancelledListener? = null

    companion object {
        private const val TAG = "NfcBottomSheet"
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
    private var isProcessing = false // Flag para evitar múltiples procesamiento

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
        registroCancelado = false
        isProcessing = false
    }

    private fun onTagDiscovered(tag: Tag) {
        // Verificar si el registro fue cancelado o ya se está procesando
        if (registroCancelado || isProcessing) {
            Log.d(TAG, "Registro cancelado o ya procesando, ignorando tarjeta NFC")
            return
        }

        isProcessing = true
        val nfcUid = bytesToHex(tag.id)

        Log.d(TAG, "NFC detectado: $nfcUid")

        requireActivity().runOnUiThread {
            Toast.makeText(context, "Tarjeta NFC detectada: $nfcUid", Toast.LENGTH_SHORT).show()
            // PRIMERO: Verificar si el UID de NFC ya está registrado
            verificarNfcDisponible(nfcUid)
        }
    }

    /**
     * Verifica si el UID de NFC ya está registrado en la base de datos
     */
    private fun verificarNfcDisponible(nfcUid: String) {
        Log.d(TAG, "Verificando disponibilidad de NFC: $nfcUid")

        val db = FirebaseDatabase.getInstance().reference

        // Consultar en nfc_index si el UID ya existe
        db.child("nfc_index").child(nfcUid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "Resultado verificación - Existe: ${snapshot.exists()}")

                if (snapshot.exists()) {
                    // El UID de NFC ya está registrado
                    val usuarioIdExistente = snapshot.getValue(String::class.java)
                    Log.e(TAG, "NFC ya registrado para usuario: $usuarioIdExistente")

                    isProcessing = false

                    // Mostrar diálogo de alerta personalizado
                    mostrarDialogoNfcYaRegistrado()
                } else {
                    // El UID de NFC está disponible, proceder con el registro
                    Log.d(TAG, "NFC disponible, procediendo con registro")

                    if (isGoogleSignIn) {
                        registrarUsuarioGoogle(nombre, apellido, numControl, carrera, email, role, perfil, nfcUid)
                    } else {
                        registrarUsuario(nombre, apellido, numControl, carrera, email, password, role, perfil, nfcUid)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error verificando NFC: ${error.message}")
                isProcessing = false

                context?.let { ctx ->
                    Toast.makeText(
                        ctx,
                        "Error al verificar NFC: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    private fun registrarUsuarioGoogle(
        nombre: String, apellido: String, numControl: String, carrera: String,
        email: String, role: String, perfil: String, nfcUid: String
    ) {
        // Verificar si el registro fue cancelado
        if (registroCancelado) return

        Log.d(TAG, "Iniciando registro Google con NFC: $nfcUid")

        // Verificar que el usuario esté autenticado
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e(TAG, "Usuario no autenticado")
            isProcessing = false
            context?.let { ctx ->
                Toast.makeText(ctx, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val authUid = currentUser.uid
        Log.d(TAG, "Usuario autenticado con UID: $authUid")

        // Obtener token FCM y guardar datos
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { tokenTask ->
                // Verificar si el registro fue cancelado
                if (registroCancelado) return@addOnCompleteListener

                if (!tokenTask.isSuccessful) {
                    Log.e(TAG, "Error obteniendo token FCM")
                    isProcessing = false
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Error obteniendo token FCM", Toast.LENGTH_SHORT).show()
                    }
                    return@addOnCompleteListener
                }

                val token = tokenTask.result
                Log.d(TAG, "Token FCM obtenido")

                // Guardar usuario en la base de datos
                val db = FirebaseDatabase.getInstance().reference

                // Crear objeto Usuario
                val usuario = Usuario(authUid, nombre, apellido, numControl, carrera, email, role, perfil, nfcUid)
                usuario.usuario_tokenFCM = token
                usuario.usuario_provider = "google"

                // Guardar usuario
                db.child("usuarios").child(authUid).setValue(usuario)
                    .addOnSuccessListener {
                        Log.d(TAG, "Usuario guardado en BD")
                        // Guardar índice NFC
                        db.child("nfc_index").child(nfcUid).setValue(authUid)
                            .addOnSuccessListener {
                                Log.d(TAG, "Índice NFC guardado")
                                // Crear usuario_puntos para activar Cloud Function
                                db.child("usuarios").child(authUid).child("usuario_puntos").setValue(0)
                                    .addOnSuccessListener {
                                        Log.d(TAG, "Registro completado exitosamente")
                                        isProcessing = false
                                        context?.let { ctx ->
                                            Toast.makeText(ctx, "✅ Registro completo con NFC", Toast.LENGTH_SHORT).show()
                                        }
                                        // Redirigir a MainActivity
                                        val intent = Intent(requireContext(), MainActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                        dismiss()
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Error guardando puntos: ${e.message}")
                                        isProcessing = false
                                        context?.let { ctx ->
                                            Toast.makeText(ctx, "Error guardando puntos: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error guardando índice NFC: ${e.message}")
                                isProcessing = false
                                context?.let { ctx ->
                                    Toast.makeText(ctx, "Error guardando índice NFC: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error guardando datos: ${e.message}")
                        isProcessing = false
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

        Log.d(TAG, "Iniciando registro Email con NFC: $nfcUid")

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val authUid = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnCompleteListener
                    Log.d(TAG, "Cuenta creada con UID: $authUid")

                    // Crear usuario
                    val usuario = Usuario(authUid, nombre, apellido, numControl, carrera, email, role, perfil, nfcUid)
                    usuario.usuario_puntos = 0
                    usuario.usuario_provider = "email"

                    val db = FirebaseDatabase.getInstance().reference
                    db.child("usuarios").child(authUid).setValue(usuario)
                        .addOnSuccessListener {
                            Log.d(TAG, "Usuario guardado en BD")
                            // Guardar índice NFC
                            db.child("nfc_index").child(nfcUid).setValue(authUid)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Índice NFC guardado")
                                    // Crear usuario_puntos para activar Cloud Function
                                    db.child("usuarios").child(authUid).child("usuario_puntos").setValue(0)
                                        .addOnSuccessListener {
                                            Log.d(TAG, "Registro completado exitosamente")
                                            isProcessing = false
                                            context?.let { ctx ->
                                                Toast.makeText(ctx, "✅ Registro completo con NFC", Toast.LENGTH_SHORT).show()
                                            }
                                            // Redirigir a MainActivity
                                            val intent = Intent(requireContext(), MainActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                            dismiss()
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e(TAG, "Error guardando puntos: ${e.message}")
                                            isProcessing = false
                                            context?.let { ctx ->
                                                Toast.makeText(ctx, "Error guardando puntos: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error guardando índice NFC: ${e.message}")
                                    isProcessing = false
                                    context?.let { ctx ->
                                        Toast.makeText(ctx, "Error guardando índice NFC: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error guardando datos: ${e.message}")
                            isProcessing = false
                            context?.let { ctx ->
                                Toast.makeText(ctx, "Error guardando datos: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Log.e(TAG, "Error creando cuenta: ${task.exception?.message}")
                    isProcessing = false
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }

    /**
     * Muestra un diálogo personalizado cuando el NFC ya está registrado
     */
    private fun mostrarDialogoNfcYaRegistrado() {
        context?.let { ctx ->
            // Inflar el layout personalizado
            val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_nfc_ya_registrado, null)

            // Crear el diálogo
            val builder = android.app.AlertDialog.Builder(ctx)
            builder.setView(dialogView)
            builder.setCancelable(false)

            val dialog = builder.create()

            // Hacer el fondo transparente para que se vea el CardView redondeado
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            // Configurar botón Reintentar
            val btnReintentar = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.btnReintentarDialog)
            btnReintentar.setOnClickListener {
                dialog.dismiss()

                // Resetear el flag de procesamiento para permitir nueva lectura
                isProcessing = false
                registroCancelado = false

                Toast.makeText(ctx, "✅ Acerque otra tarjeta NFC", Toast.LENGTH_SHORT).show()

                // El NFC reader mode ya está activo, solo esperará otra tarjeta
            }

            // Configurar botón Cancelar
            val btnCancelar = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.btnCancelarDialog)
            btnCancelar.setOnClickListener {
                dialog.dismiss()
                registroCancelado = true

                // Si es registro con Google, necesitamos eliminar la cuenta creada y limpiar sesión
                if (isGoogleSignIn) {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    currentUser?.delete()?.addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            Log.d(TAG, "Cuenta de Google eliminada exitosamente")
                        }
                        // Notificar a la actividad para limpiar sesión
                        cancellationListener?.onRegistrationCancelled()
                    }
                }

                Toast.makeText(ctx, "❌ Registro cancelado", Toast.LENGTH_SHORT).show()
                dismiss()
            }

            dialog.show()
        }
    }
}