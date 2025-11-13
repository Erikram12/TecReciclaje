package com.example.tecreciclaje

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import com.example.tecreciclaje.utils.AppLogger
import android.util.Patterns
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tecreciclaje.Model.NfcBottomSheetDialogFragment
import com.example.tecreciclaje.utils.FCMTokenManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

class RegistroActivity : AppCompatActivity(), NfcBottomSheetDialogFragment.OnRegistrationCancelledListener {

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "RegistroActivity"
        private const val DEBUG = false // Cambiar a true solo en desarrollo
    }

    private lateinit var nombreEditText: EditText
    private lateinit var apellidoEditText: EditText
    private lateinit var numControlEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var carreraEditText: AutoCompleteTextView
    private lateinit var btnSiguiente: RelativeLayout
    private lateinit var btnGoogle: RelativeLayout
    private lateinit var loginRedirectText: TextView

    // Firebase Auth
    private lateinit var mAuth: FirebaseAuth

    // Google Sign In
    private lateinit var mGoogleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        initializeViews()
        setupFirebaseAuth()
        setupGoogleSignIn()
        setupClickListeners()
    }

    private fun initializeViews() {
        nombreEditText = findViewById(R.id.nombreEditText)
        apellidoEditText = findViewById(R.id.apellidoEditText)
        numControlEditText = findViewById(R.id.numControlEditText)
        carreraEditText = findViewById(R.id.carreraEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        btnSiguiente = findViewById(R.id.btnSiguiente)
        btnGoogle = findViewById(R.id.btnGoogle)
        loginRedirectText = findViewById(R.id.loginRedirectText)

        val googleIcon = findViewById<ImageView>(R.id.googleIcon)
        googleIcon.setImageResource(R.drawable.ic_google)

        setupCarreraDropdown()
    }

    private fun setupFirebaseAuth() {
        mAuth = FirebaseAuth.getInstance()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupCarreraDropdown() {
        val carreras = arrayOf(
            "Ingeniería en Desarrollo Comunitario",
            "Ingeniería Forestal",
            "Ingeniería en Tecnologías de la Información y Comunicaciones",
            "Ingeniería en Innovación Agrícola Sustentable",
            "Ingeniería Administracion"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, carreras)
        carreraEditText.setAdapter(adapter)
        carreraEditText.setOnClickListener { carreraEditText.showDropDown() }
    }

    private fun setupClickListeners() {
        btnSiguiente.setOnClickListener { registerWithEmail() }
        btnGoogle.setOnClickListener { signInWithGoogle() }
        loginRedirectText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        passwordEditText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (passwordEditText.right - passwordEditText.compoundDrawables[2].bounds.width())) {
                    togglePasswordVisibility()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun registerWithEmail() {
        val nombre = nombreEditText.text.toString().trim()
        val apellido = apellidoEditText.text.toString().trim()
        val numControl = numControlEditText.text.toString().trim()
        val carrera = carreraEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (!validarCampos(nombre, apellido, numControl, carrera, email, password)) return

        val role = determineUserRole(email)
        val perfil = "https://firebasestorage.googleapis.com/v0/b/resiclaje-39011.firebasestorage.app/o/user.png?alt=media&token=745bbda5-2229-4d42-af3f-16dd6ec8db23"

        val roleMessage = if (role == "admin") "Registro como Administrador (@tecnm.mx)" else "Registro como Usuario"
        logInfo(roleMessage)

        if (role == "admin") {
            // Admin: verificar si existe en BD
            checkIfUserExistsInDatabase(email) { exists ->
                if (exists) {
                    mostrarDialogoError(
                        "Usuario Ya Registrado",
                        "Este correo electrónico ya está registrado en el sistema.\n\n¿Ya tienes una cuenta? Intenta iniciar sesión."
                    )
                } else {
                    registerAdminDirectly(nombre, apellido, numControl, carrera, email, password, perfil)
                }
            }
        } else {
            // Usuario: verificar si existe en BD ANTES de crear en Auth
            checkIfUserExistsInDatabase(email) { exists ->
                if (exists) {
                    mostrarDialogoError(
                        "Usuario Ya Registrado",
                        "Este correo electrónico ya está registrado en el sistema.\n\n¿Ya tienes una cuenta? Intenta iniciar sesión."
                    )
                } else {
                    createUserAndShowNFC(nombre, apellido, numControl, carrera, email, password, perfil)
                }
            }
        }
    }

    private fun checkIfUserExistsInDatabase(email: String, callback: (Boolean) -> Unit) {
        val db = FirebaseDatabase.getInstance().reference
        db.child("usuarios").orderByChild("usuario_email").equalTo(email)
            .get()
            .addOnSuccessListener { snapshot ->
                callback(snapshot.exists())
            }
            .addOnFailureListener { e ->
                logError("Error al verificar usuario en BD: ${e.message}")
                // En caso de error, permitir continuar (mejor experiencia)
                callback(false)
            }
    }

    private fun createUserAndShowNFC(
        nombre: String,
        apellido: String,
        numControl: String,
        carrera: String,
        email: String,
        password: String,
        perfil: String
    ) {
        showLoading(true)
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                showLoading(false)
                FCMTokenManager.checkAndUsePendingToken(this)
                val sheet = NfcBottomSheetDialogFragment.newInstance(
                    nombre, apellido, numControl, carrera, email, password, "user", perfil, false
                )
                sheet.setOnRegistrationCancelledListener(this)
                sheet.show(supportFragmentManager, "NFC_SHEET")
            }
            .addOnFailureListener { e ->
                showLoading(false)
                if (e is FirebaseAuthUserCollisionException) {
                    mostrarDialogoError(
                        "Correo Ya Registrado",
                        "Este correo electrónico ya está en uso.\n\n¿Ya tienes una cuenta? Intenta iniciar sesión."
                    )
                } else {
                    mostrarDialogoError(
                        "Error de Registro",
                        "No se pudo completar el registro. Por favor, intenta nuevamente."
                    )
                    logError("Error al crear usuario: ${e.message}")
                }
            }
    }

    private fun validarCampos(
        nombre: String,
        apellido: String,
        numControl: String,
        carrera: String,
        email: String,
        password: String
    ): Boolean {
        when {
            nombre.isEmpty() -> { mostrarDialogoError("Campo Requerido","Por favor, ingresa tu nombre."); return false }
            apellido.isEmpty() -> { mostrarDialogoError("Campo Requerido","Por favor, ingresa tu apellido."); return false }
            numControl.isEmpty() -> { mostrarDialogoError("Campo Requerido","Por favor, ingresa tu número de control."); return false }
            carrera.isEmpty() -> { mostrarDialogoError("Campo Requerido","Por favor, selecciona tu carrera."); return false }
            email.isEmpty() -> { mostrarDialogoError("Campo Requerido","Por favor, ingresa tu correo electrónico."); return false }
            password.isEmpty() -> { mostrarDialogoError("Campo Requerido","Por favor, ingresa una contraseña."); return false }
        }
        if (!nombre.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$"))) {
            mostrarDialogoError("Nombre Inválido","El nombre solo puede contener letras."); return false
        }
        if (!apellido.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$"))) {
            mostrarDialogoError("Apellido Inválido","El apellido solo puede contener letras."); return false
        }
        if (!numControl.matches(Regex("^[0-9]+$"))) {
            mostrarDialogoError("Número de Control Inválido","El número de control solo puede contener números."); return false
        }
        if (numControl.length < 8) {
            mostrarDialogoError("Número de Control Inválido","El número de control debe tener al menos 8 dígitos."); return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mostrarDialogoError("Correo Inválido","Por favor, ingresa un correo electrónico válido.\n\nEjemplo: usuario@ejemplo.com"); return false
        }
        if (password.length < 8) {
            mostrarDialogoError("Contraseña Débil","La contraseña debe tener al menos 8 caracteres."); return false
        }
        val tieneNumero = password.any { it.isDigit() }
        val tieneLetra = password.any { it.isLetter() }
        if (!tieneNumero || !tieneLetra) {
            mostrarDialogoError("Contraseña Débil","La contraseña debe contener al menos:\n• Una letra\n• Un número"); return false
        }
        return true
    }

    private fun mostrarDialogoError(titulo: String, mensaje: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_error_campo, null)
        val builder = android.app.AlertDialog.Builder(this).setView(dialogView).setCancelable(false)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.findViewById<TextView>(R.id.txtTituloError).text = titulo
        dialogView.findViewById<TextView>(R.id.txtMensajeError).text = mensaje
        dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.btnEntendido).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun signInWithGoogle() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun handleGoogleSignInSuccess(user: FirebaseUser?) {
        if (user != null) {
            val email = user.email
            val displayName = user.displayName
            val photoUrl = user.photoUrl?.toString()
                ?: "https://firebasestorage.googleapis.com/v0/b/resiclaje-39011.firebasestorage.app/o/user.png?alt=media&token=745bbda5-2229-4d42-af3f-16dd6ec8db23"

            val nameParts = displayName?.split(" ", limit = 2) ?: listOf("", "")
            val nombre = if (nameParts.isNotEmpty()) nameParts[0] else ""
            val apellido = if (nameParts.size > 1) nameParts[1] else ""
            val role = determineUserRole(email)

            val roleMessage = if (role == "admin") "Intento de Registro como Administrador (@tecnm.mx)" else "Intento de Registro como Usuario"
            logInfo(roleMessage)

            FCMTokenManager.checkAndUsePendingToken(this)

            // Verificar si ya existe en BD ANTES de proceder
            if (email != null) {
                checkIfUserExistsInDatabase(email) { exists ->
                    if (exists) {
                        // Usuario ya existe, cerrar sesión de Google y mostrar error
                        mGoogleSignInClient.signOut().addOnCompleteListener {
                            mAuth.signOut()
                            mostrarDialogoError(
                                "Usuario Ya Registrado",
                                "Este correo electrónico ya está registrado en el sistema.\n\n¿Ya tienes una cuenta? Intenta iniciar sesión."
                            )
                        }
                    } else {
                        // Usuario no existe, proceder con el registro
                        if (role == "admin") {
                            registerAdminDirectly(nombre, apellido, "", "", email, "", photoUrl)
                        } else {
                            val sheet = NfcBottomSheetDialogFragment.newInstance(
                                nombre, apellido, "", "", email, "", role, photoUrl, true
                            )
                            sheet.setOnRegistrationCancelledListener(this)
                            sheet.show(supportFragmentManager, "NFC_SHEET")
                        }
                    }
                }
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    logInfo("Autenticación con Google exitosa")
                    handleGoogleSignInSuccess(mAuth.currentUser)
                } else {
                    logError("Error en autenticación con Google: ${task.exception?.message}")
                    mostrarDialogoError(
                        "Error de Autenticación",
                        "No se pudo autenticar con Google. Por favor, intenta nuevamente."
                    )
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                logInfo("Google Sign In exitoso")
                firebaseAuthWithGoogle(account?.idToken)
            } catch (e: ApiException) {
                logError("Error en Google Sign In: ${e.message}")
                if (e.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                    mGoogleSignInClient.signOut().addOnCompleteListener {
                        logInfo("Registro cancelado")
                    }
                } else {
                    mostrarDialogoError(
                        "Error de Google Sign-In",
                        "No se pudo completar el registro con Google.\n\nPor favor, intenta nuevamente."
                    )
                }
            }
        }
    }

    private fun togglePasswordVisibility() {
        val currentInputType = passwordEditText.inputType
        val isPasswordVisible = currentInputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        if (isPasswordVisible) {
            passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            passwordEditText.setCompoundDrawablesWithIntrinsicBounds(
                getDrawable(R.drawable.baseline_lock_24), null, getDrawable(R.drawable.ic_visibility), null
            )
        } else {
            passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            passwordEditText.setCompoundDrawablesWithIntrinsicBounds(
                getDrawable(R.drawable.baseline_lock_24), null, getDrawable(R.drawable.ic_visibility_off), null
            )
        }
        passwordEditText.setSelection(passwordEditText.text.length)
    }

    private fun determineUserRole(email: String?): String {
        return if (email != null && email.lowercase().endsWith("@tecnm.mx")) "admin" else "user"
    }

    private fun registerAdminDirectly(
        nombre: String, apellido: String, numControl: String, carrera: String,
        email: String, password: String, perfil: String
    ) {
        showLoading(true)
        if (mAuth.currentUser != null) {
            val authUid = mAuth.currentUser!!.uid
            saveAdminToDatabase(nombre, apellido, numControl, carrera, email, "admin", perfil, authUid)
        } else {
            if (password.isEmpty()) {
                mostrarDialogoError("Error","Se requiere una contraseña para el registro con email.")
                showLoading(false)
                return
            }
            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val authUid = mAuth.currentUser!!.uid
                        saveAdminToDatabase(nombre, apellido, numControl, carrera, email, "admin", perfil, authUid)
                    } else {
                        showLoading(false)
                        if (task.exception is FirebaseAuthUserCollisionException) {
                            mostrarDialogoError("Correo Ya Registrado","Este correo electrónico ya está en uso.\n\n¿Ya tienes una cuenta? Intenta iniciar sesión.")
                        } else {
                            mostrarDialogoError("Error de Registro", "No se pudo completar el registro. Por favor, intenta nuevamente.")
                            logError("Error al crear usuario admin: ${task.exception?.message}")
                        }
                    }
                }
        }
    }

    private fun saveAdminToDatabase(
        nombre: String, apellido: String, numControl: String, carrera: String,
        email: String, role: String, perfil: String, authUid: String
    ) {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { tokenTask ->
                if (!tokenTask.isSuccessful) {
                    showLoading(false)
                    mostrarDialogoError("Error de Configuración","No se pudo obtener el token de notificaciones.")
                    logError("Error al obtener token FCM")
                    return@addOnCompleteListener
                }
                val token = tokenTask.result
                val db = FirebaseDatabase.getInstance().reference
                val adminMap = hashMapOf<String, Any>(
                    "usuario_nombre" to nombre,
                    "usuario_apellido" to apellido,
                    "usuario_numControl" to numControl,
                    "usuario_carrera" to carrera,
                    "usuario_email" to email,
                    "usuario_role" to role,
                    "usuario_perfil" to perfil,
                    "usuario_tokenFCM" to token,
                    "usuario_provider" to "email"
                )
                db.child("usuarios").child(authUid).setValue(adminMap)
                    .addOnSuccessListener {
                        showLoading(false)
                        logInfo("Administrador registrado exitosamente")
                        val intent = Intent(this, AdminPanel::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                    .addOnFailureListener { e ->
                        showLoading(false)
                        mostrarDialogoError("Error al Guardar","No se pudieron guardar los datos. Por favor, intenta nuevamente.")
                        logError("Error al guardar admin en BD: ${e.message}")
                    }
            }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            logInfo("Registrando usuario...")
        }
    }

    override fun onRegistrationCancelled() {
        mGoogleSignInClient.signOut().addOnCompleteListener {
            val currentUser = mAuth.currentUser
            if (currentUser != null) {
                currentUser.delete().addOnCompleteListener { deleteTask ->
                    if (deleteTask.isSuccessful) {
                        logInfo("Cuenta eliminada exitosamente")
                    } else {
                        logError("Error al eliminar cuenta: ${deleteTask.exception?.message}")
                    }
                    mAuth.signOut()
                    logInfo("Sesión cerrada. Usuario puede intentar con otra cuenta.")
                }
            } else {
                mAuth.signOut()
                logInfo("Sesión cerrada. Usuario puede intentar con otra cuenta.")
            }
        }
    }

    private fun logInfo(message: String) {
        if (DEBUG) {
            Log.i(TAG, message)
        }
    }

    private fun logError(message: String) {
        if (DEBUG) {
            Log.e(TAG, message)
        }
    }
}