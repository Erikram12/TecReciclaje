package com.example.tecreciclaje.Model

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.provider.Settings
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
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging

class NfcBottomSheetDialogFragment : BottomSheetDialogFragment() {

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
    private var isProcessing = false

    // ★ Helper: asegura auth != null antes de tocar DB
    private fun withAuthUser(onReady: (FirebaseUser) -> Unit, onError: (Exception) -> Unit = { }) {
        val auth = FirebaseAuth.getInstance()
        val current = auth.currentUser
        if (current != null) {
            onReady(current)
            return
        }
        // Fallback: sesión anónima (solo por seguridad; normalmente ya habrá usuario válido)
        auth.signInAnonymously()
            .addOnSuccessListener { onReady(auth.currentUser!!) }
            .addOnFailureListener { e ->
                Log.e(TAG, "Fallo signInAnonymously: ${e.message}")
                onError(e)
            }
    }

    fun setOnRegistrationCancelledListener(listener: OnRegistrationCancelledListener?) {
        this.cancellationListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
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
        btnCancelar = view.findViewById(R.id.btnCancelar)
        btnCancelar.setOnClickListener {
            registroCancelado = true
            nfcAdapter?.disableReaderMode(requireActivity())
            context?.let { ctx -> Toast.makeText(ctx, "Registro cancelado", Toast.LENGTH_SHORT).show() }
            cancellationListener?.onRegistrationCancelled()
            dismiss()
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        val act = requireActivity()
        nfcAdapter = NfcAdapter.getDefaultAdapter(act)
        
        // Verificar si el NFC está habilitado
        if (nfcAdapter == null) {
            mostrarDialogoNfcNoDisponible()
            return
        }
        
        if (!nfcAdapter!!.isEnabled) {
            mostrarDialogoNfcNoHabilitado()
            return
        }
        
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
        if (registroCancelado || isProcessing) {
            Log.d(TAG, "Registro cancelado o ya procesando, ignorando tarjeta NFC")
            return
        }
        isProcessing = true
        val nfcUid = bytesToHex(tag.id)
        Log.d(TAG, "NFC detectado: $nfcUid")

        requireActivity().runOnUiThread {
            Toast.makeText(context, "Tarjeta NFC detectada: $nfcUid", Toast.LENGTH_SHORT).show()
            // ★ Espera/garantiza auth antes de leer nfc_index
            withAuthUser(
                onReady = { verificarNfcDisponible(nfcUid) },
                onError = { e ->
                    isProcessing = false
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Error de autenticación: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    private fun verificarNfcDisponible(nfcUid: String) {
        Log.d(TAG, "Verificando disponibilidad de NFC: $nfcUid")
        val db = FirebaseDatabase.getInstance().reference
        db.child("nfc_index").child(nfcUid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "Resultado verificación - Existe: ${snapshot.exists()}")
                if (snapshot.exists()) {
                    val usuarioIdExistente = snapshot.getValue(String::class.java)
                    Log.e(TAG, "NFC ya registrado para usuario: $usuarioIdExistente")
                    isProcessing = false
                    mostrarDialogoNfcYaRegistrado()
                } else {
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
                    Toast.makeText(ctx, "Error al verificar NFC: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun registrarUsuarioGoogle(
        nombre: String, apellido: String, numControl: String, carrera: String,
        email: String, role: String, perfil: String, nfcUid: String
    ) {
        if (registroCancelado) return
        Log.d(TAG, "Iniciando registro Google con NFC: $nfcUid")

        withAuthUser(onReady = { currentUser ->
            val authUid = currentUser.uid
            Log.d(TAG, "Usuario autenticado con UID: $authUid")

            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { tokenTask ->
                    if (registroCancelado) return@addOnCompleteListener
                    if (!tokenTask.isSuccessful) {
                        Log.e(TAG, "Error obteniendo token FCM")
                        isProcessing = false
                        context?.let { ctx -> Toast.makeText(ctx, "Error obteniendo token FCM", Toast.LENGTH_SHORT).show() }
                        return@addOnCompleteListener
                    }
                    val token = tokenTask.result
                    val db = FirebaseDatabase.getInstance().reference
                    val usuario = Usuario(authUid, nombre, apellido, numControl, carrera, email, role, perfil, nfcUid).apply {
                        usuario_tokenFCM = token
                        usuario_provider = "google"
                    }

                    db.child("usuarios").child(authUid).setValue(usuario)
                        .addOnSuccessListener {
                            db.child("nfc_index").child(nfcUid).setValue(authUid)
                                .addOnSuccessListener {
                                    db.child("usuarios").child(authUid).child("usuario_puntos").setValue(0)
                                        .addOnSuccessListener {
                                            Log.d(TAG, "Registro completado exitosamente")
                                            isProcessing = false
                                            context?.let { ctx ->
                                                Toast.makeText(ctx, "Registro completo con NFC", Toast.LENGTH_SHORT).show()
                                            }
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
        }, onError = { e ->
            isProcessing = false
            context?.let { ctx -> Toast.makeText(ctx, "Error de autenticación: ${e.message}", Toast.LENGTH_SHORT).show() }
        })
    }

    private fun registrarUsuario(
        nombre: String, apellido: String, numControl: String, carrera: String,
        email: String, password: String, role: String, perfil: String, nfcUid: String
    ) {
        if (registroCancelado) return
        Log.d(TAG, "Iniciando registro Email con NFC: $nfcUid")

        // ★ En este punto, RegistroActivity ya creó la cuenta; aún así, garantizamos auth.
        withAuthUser(onReady = { currentUser ->
            val authUid = currentUser.uid
            Log.d(TAG, "Usuario autenticado con UID: $authUid")

            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { tokenTask ->
                    if (registroCancelado) return@addOnCompleteListener
                    if (!tokenTask.isSuccessful) {
                        Log.e(TAG, "Error obteniendo token FCM")
                        isProcessing = false
                        context?.let { ctx -> Toast.makeText(ctx, "Error obteniendo token FCM", Toast.LENGTH_SHORT).show() }
                        return@addOnCompleteListener
                    }
                    val token = tokenTask.result
                    val usuario = Usuario(authUid, nombre, apellido, numControl, carrera, email, role, perfil, nfcUid).apply {
                        usuario_puntos = 0
                        usuario_tokenFCM = token
                        usuario_provider = "email"
                    }

                    val db = FirebaseDatabase.getInstance().reference
                    db.child("usuarios").child(authUid).setValue(usuario)
                        .addOnSuccessListener {
                            db.child("nfc_index").child(nfcUid).setValue(authUid)
                                .addOnSuccessListener {
                                    db.child("usuarios").child(authUid).child("usuario_puntos").setValue(0)
                                        .addOnSuccessListener {
                                            Log.d(TAG, "Registro completado exitosamente")
                                            isProcessing = false
                                            context?.let { ctx ->
                                                Toast.makeText(ctx, "Registro completo con NFC", Toast.LENGTH_SHORT).show()
                                            }
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
        }, onError = { e ->
            isProcessing = false
            context?.let { ctx -> Toast.makeText(ctx, "Error de autenticación: ${e.message}", Toast.LENGTH_SHORT).show() }
        })
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }

    private fun mostrarDialogoNfcYaRegistrado() {
        context?.let { ctx ->
            val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_nfc_ya_registrado, null)
            val builder = android.app.AlertDialog.Builder(ctx).setView(dialogView).setCancelable(false)
            val dialog = builder.create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val btnReintentar = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.btnReintentarDialog)
            btnReintentar.setOnClickListener {
                dialog.dismiss()
                isProcessing = false
                registroCancelado = false
                Toast.makeText(ctx, "Acerque otra tarjeta NFC", Toast.LENGTH_SHORT).show()
            }

            val btnCancelar = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.btnCancelarDialog)
            btnCancelar.setOnClickListener {
                dialog.dismiss()
                registroCancelado = true
                if (isGoogleSignIn) {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    currentUser?.delete()?.addOnCompleteListener {
                        cancellationListener?.onRegistrationCancelled()
                    }
                }
                Toast.makeText(ctx, "❌ Registro cancelado", Toast.LENGTH_SHORT).show()
                dismiss()
            }

            dialog.show()
        }
    }

    private fun mostrarDialogoNfcNoHabilitado() {
        context?.let { ctx ->
            val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_nfc_no_habilitado, null)
            val builder = android.app.AlertDialog.Builder(ctx).setView(dialogView).setCancelable(false)
            val dialog = builder.create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val btnConfiguracion = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.btnConfiguracionNfc)
            btnConfiguracion.setOnClickListener {
                dialog.dismiss()
                // Abrir configuración de NFC
                val intent = Intent(Settings.ACTION_NFC_SETTINGS)
                startActivity(intent)
                dismiss()
            }

            val btnCancelar = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.btnCancelarNfc)
            btnCancelar.setOnClickListener {
                dialog.dismiss()
                registroCancelado = true
                if (isGoogleSignIn) {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    currentUser?.delete()?.addOnCompleteListener {
                        cancellationListener?.onRegistrationCancelled()
                    }
                }
                dismiss()
            }

            dialog.show()
        }
    }

    private fun mostrarDialogoNfcNoDisponible() {
        context?.let { ctx ->
            val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_nfc_no_habilitado, null)
            val builder = android.app.AlertDialog.Builder(ctx).setView(dialogView).setCancelable(false)
            val dialog = builder.create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            // Cambiar el mensaje para indicar que el dispositivo no soporta NFC
            val txtMensaje = dialogView.findViewById<android.widget.TextView>(R.id.txtMensajeNfc)
            txtMensaje.text = "Este dispositivo no soporta NFC"

            val btnConfiguracion = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.btnConfiguracionNfc)
            btnConfiguracion.visibility = android.view.View.GONE

            val btnCancelar = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.btnCancelarNfc)
            btnCancelar.layoutParams.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
            btnCancelar.setOnClickListener {
                dialog.dismiss()
                registroCancelado = true
                if (isGoogleSignIn) {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    currentUser?.delete()?.addOnCompleteListener {
                        cancellationListener?.onRegistrationCancelled()
                    }
                }
                dismiss()
            }

            dialog.show()
        }
    }
}
