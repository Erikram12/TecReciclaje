package com.example.tecreciclaje

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
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
import com.example.tecreciclaje.domain.model.Usuario
import com.example.tecreciclaje.utils.FCMTokenManager
import com.example.tecreciclaje.utils.LocaleHelper
import com.example.tecreciclaje.utils.NipGenerator
import com.example.tecreciclaje.utils.SessionManager
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

    private var loadingDialog: android.app.AlertDialog? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.attachBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        // Mostrar pantalla de carga mientras se limpia todo
        mostrarPantallaCarga()
        
        // Limpiar todo de forma agresiva en un hilo separado
        limpiarTodoYContinuar()
    }

    private fun mostrarPantallaCarga() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_loading_registro, null)
        val tvMensaje = dialogView.findViewById<TextView>(R.id.tvLoadingMessage)
        tvMensaje.text = "Por favor espere...\nLimpiando sesiones y caché"
        
        loadingDialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loadingDialog?.show()
    }

    private fun ocultarPantallaCarga() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    private fun limpiarTodoYContinuar() {
        // Ejecutar limpieza en un hilo separado para no bloquear la UI
        Thread {
            try {
                // 1. Limpiar sesión completa usando SessionManager
                SessionManager.clearCompleteSession(this@RegistroActivity)
                
                // 2. Esperar un momento para que se complete
                Thread.sleep(800)
                
                // 3. Limpieza adicional agresiva
                runOnUiThread {
                    limpiarSesionesAdicionales()
                }
                
                Thread.sleep(500)
                
                // 4. Inicializar la actividad
                runOnUiThread {
                    inicializarActividad()
                }
                
            } catch (e: Exception) {
                logError("Error durante la limpieza: ${e.message}")
                runOnUiThread {
                    inicializarActividad()
                }
            }
        }.start()
    }

    private fun limpiarSesionesAdicionales() {
        logInfo("Iniciando limpieza adicional de sesiones")
        
        // Limpiar Firebase Auth de forma más agresiva
        try {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            if (currentUser != null) {
                logInfo("Forzando cierre de sesión para: ${currentUser.email}")
                // Intentar eliminar el usuario si es necesario (solo para casos extremos)
                auth.signOut()
            }
            // Forzar signOut múltiples veces
            auth.signOut()
        } catch (e: Exception) {
            logError("Error en limpieza adicional de Firebase Auth: ${e.message}")
        }
        
        // Limpiar Google Sign-In
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(this, gso)
            googleSignInClient.signOut()
            googleSignInClient.revokeAccess()
        } catch (e: Exception) {
            logError("Error en limpieza adicional de Google: ${e.message}")
        }
        
        // Limpiar SharedPreferences de forma más agresiva
        try {
            val prefs = getSharedPreferences("TecReciclajePrefs", MODE_PRIVATE)
            prefs.edit().clear().commit()
            
            val fcmPrefs = getSharedPreferences("FCM_PREFS", MODE_PRIVATE)
            fcmPrefs.edit().clear().commit()
            
            val defaultPrefs = getSharedPreferences("default", MODE_PRIVATE)
            defaultPrefs.edit().clear().commit()
        } catch (e: Exception) {
            logError("Error limpiando SharedPreferences: ${e.message}")
        }
    }

    private fun inicializarActividad() {
        ocultarPantallaCarga()
        
        initializeViews()
        setupFirebaseAuth()
        setupGoogleSignIn()
        setupClickListeners()
        
        logInfo("Actividad de registro inicializada correctamente")
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

    /**
     * Cierra cualquier sesión previa de Firebase Auth y Google Sign-In
     * Esto asegura que no haya conflictos al intentar registrar un nuevo usuario
     */
    private fun cerrarSesionesPrevias() {
        logInfo("Iniciando limpieza de sesiones previas")
        
        // Cerrar sesión de Firebase Auth de forma forzada
        try {
            val currentUser = mAuth.currentUser
            if (currentUser != null) {
                logInfo("Cerrando sesión previa de Firebase Auth para usuario: ${currentUser.email}")
                mAuth.signOut()
                // Esperar un momento para asegurar que se complete
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    logInfo("Sesión de Firebase Auth cerrada")
                }, 300)
            }
        } catch (e: Exception) {
            logError("Error cerrando sesión de Firebase Auth: ${e.message}")
        }
        
        // Cerrar sesión de Google Sign-In
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(this, gso)
            googleSignInClient.signOut().addOnCompleteListener {
                logInfo("Sesión de Google Sign-In cerrada")
            }
            // También revocar acceso para asegurar limpieza completa
            googleSignInClient.revokeAccess().addOnCompleteListener {
                logInfo("Acceso de Google revocado")
            }
        } catch (e: Exception) {
            logError("Error cerrando sesión de Google: ${e.message}")
        }
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
            // Admin: verificar si existe en Firebase Auth Y en BD
            verificarUsuarioCompleto(email) { exists ->
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
            // Usuario: verificar si existe en Firebase Auth Y en BD ANTES de crear
            verificarUsuarioCompleto(email) { existe ->
                if (existe) {
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

    /**
     * Verifica si el usuario existe tanto en Firebase Auth como en la base de datos
     * Si existe en Auth pero no en BD, intenta limpiarlo
     */
    private fun verificarUsuarioCompleto(email: String, callback: (Boolean) -> Unit) {
        // Primero verificar en Firebase Auth
        mAuth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods
                    val existeEnAuth = signInMethods != null && signInMethods.isNotEmpty()
                    
                    if (existeEnAuth) {
                        logInfo("Usuario encontrado en Firebase Auth, verificando en BD...")
                        // Verificar si también existe en la BD
                        checkIfUserExistsInDatabase(email) { existeEnBD ->
                            if (existeEnBD) {
                                // Existe en ambos, usuario completamente registrado
                                logInfo("Usuario encontrado en Auth y BD - registro completo")
                                callback(true)
                            } else {
                                // Existe en Auth pero NO en BD - registro incompleto
                                logInfo("Usuario existe en Auth pero NO en BD - registro incompleto, intentando limpiar...")
                                intentarLimpiarUsuarioIncompleto(email) { limpiado ->
                                    if (limpiado) {
                                        logInfo("Usuario incompleto limpiado exitosamente")
                                        callback(false) // Permitir registro
                                    } else {
                                        logInfo("No se pudo limpiar usuario incompleto")
                                        callback(true) // Bloquear registro
                                    }
                                }
                            }
                        }
                    } else {
                        // Si no existe en Auth, verificar en la base de datos
                        logInfo("Usuario NO encontrado en Firebase Auth, verificando en BD...")
                        checkIfUserExistsInDatabase(email) { existeEnBD ->
                            callback(existeEnBD)
                        }
                    }
                } else {
                    logError("Error al verificar en Firebase Auth: ${task.exception?.message}")
                    // En caso de error, verificar en BD como respaldo
                    checkIfUserExistsInDatabase(email) { existeEnBD ->
                        callback(existeEnBD)
                    }
                }
            }
    }

    /**
     * Intenta limpiar un usuario que existe en Firebase Auth pero no en la BD
     * Esto puede pasar si el registro no se completó
     * En este caso, permitimos que intente crear el usuario de todas formas
     * y manejamos el error de forma más inteligente en crearUsuarioConEmail
     */
    private fun intentarLimpiarUsuarioIncompleto(email: String, callback: (Boolean) -> Unit) {
        logInfo("Usuario incompleto detectado - permitiendo intento de registro")
        // Permitir que intente crear el usuario
        // Si falla, lo manejaremos en crearUsuarioConEmail con un mensaje más claro
        callback(false) // false = permitir registro
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
        // Verificar NFC antes de crear el usuario
        if (!verificarNfcHabilitado()) {
            return
        }
        
        // Asegurarse de que no hay sesión activa antes de crear un nuevo usuario
        if (mAuth.currentUser != null) {
            logInfo("Cerrando sesión previa antes de crear nuevo usuario")
            mAuth.signOut()
            // Esperar un momento para que se complete el signOut
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                crearUsuarioConEmail(nombre, apellido, numControl, carrera, email, password, perfil)
            }, 500)
        } else {
            crearUsuarioConEmail(nombre, apellido, numControl, carrera, email, password, perfil)
        }
    }

    private fun crearUsuarioConEmail(
        nombre: String,
        apellido: String,
        numControl: String,
        carrera: String,
        email: String,
        password: String,
        perfil: String
    ) {
        // Asegurarse de que no hay sesión activa - forzar múltiples veces
        if (mAuth.currentUser != null) {
            logInfo("Forzando cierre de sesión antes de crear usuario")
            mAuth.signOut()
            // Esperar un momento para que se complete
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                // Verificar nuevamente después de esperar
                if (mAuth.currentUser != null) {
                    logInfo("Aún hay sesión activa, forzando nuevamente...")
                    mAuth.signOut()
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        crearUsuarioConEmailForzado(nombre, apellido, numControl, carrera, email, password, perfil)
                    }, 500)
                } else {
                    crearUsuarioConEmailForzado(nombre, apellido, numControl, carrera, email, password, perfil)
                }
            }, 500)
        } else {
            crearUsuarioConEmailForzado(nombre, apellido, numControl, carrera, email, password, perfil)
        }
    }

    private fun crearUsuarioConEmailForzado(
        nombre: String,
        apellido: String,
        numControl: String,
        carrera: String,
        email: String,
        password: String,
        perfil: String
    ) {
        showLoading(true)
        
        // Verificar una última vez antes de crear
        mAuth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { verifyTask ->
                if (verifyTask.isSuccessful) {
                    val signInMethods = verifyTask.result?.signInMethods
                    val existeEnAuth = signInMethods != null && signInMethods.isNotEmpty()
                    
                    if (existeEnAuth) {
                        // Existe en Auth, verificar si existe en BD
                        checkIfUserExistsInDatabase(email) { existeEnBD ->
                            if (existeEnBD) {
                                showLoading(false)
                                mostrarDialogoError(
                                    "Usuario Ya Registrado",
                                    "Este correo electrónico ya está completamente registrado en el sistema.\n\n¿Ya tienes una cuenta? Intenta iniciar sesión."
                                )
                            } else {
                                // Existe en Auth pero NO en BD - registro incompleto
                                showLoading(false)
                                mostrarDialogoError(
                                    "Registro Incompleto",
                                    "Este correo electrónico tiene un registro incompleto.\n\n" +
                                    "Por favor, intenta iniciar sesión con tu contraseña o usa la opción '¿Olvidaste tu contraseña?' para recuperarla.\n\n" +
                                    "Si no recuerdas tu contraseña, contacta al administrador."
                                )
                            }
                        }
                    } else {
                        // No existe en Auth, proceder con el registro
                        procederConRegistro(nombre, apellido, numControl, carrera, email, password, perfil)
                    }
                } else {
                    // Error al verificar, proceder de todas formas
                    logError("Error al verificar correo antes de crear: ${verifyTask.exception?.message}")
                    procederConRegistro(nombre, apellido, numControl, carrera, email, password, perfil)
                }
            }
    }

    private fun procederConRegistro(
        nombre: String,
        apellido: String,
        numControl: String,
        carrera: String,
        email: String,
        password: String,
        perfil: String
    ) {
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
                // Cerrar sesión si hay alguna activa
                if (mAuth.currentUser != null) {
                    mAuth.signOut()
                }
                
                if (e is FirebaseAuthUserCollisionException) {
                    logError("Correo ya registrado en Firebase Auth: ${e.message}")
                    // Verificar si existe en BD para dar un mensaje más preciso
                    checkIfUserExistsInDatabase(email) { existeEnBD ->
                        if (existeEnBD) {
                            mostrarDialogoError(
                                "Usuario Ya Registrado",
                                "Este correo electrónico ya está completamente registrado.\n\n¿Ya tienes una cuenta? Intenta iniciar sesión."
                            )
                        } else {
                            mostrarDialogoError(
                                "Registro Incompleto",
                                "Este correo electrónico tiene un registro incompleto.\n\n" +
                                "Por favor, intenta iniciar sesión o recupera tu contraseña usando la opción '¿Olvidaste tu contraseña?'"
                            )
                        }
                    }
                } else {
                    logError("Error al crear usuario: ${e.message}")
                    mostrarDialogoError(
                        "Error de Registro",
                        "No se pudo completar el registro. Por favor, intenta nuevamente.\n\nError: ${e.message}"
                    )
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
        // Asegurarse de que no hay sesión activa antes de iniciar el registro con Google
        if (mAuth.currentUser != null) {
            logInfo("Cerrando sesión previa antes de registro con Google")
            mAuth.signOut()
        }
        
        // Cerrar sesión de Google Sign-In también
        mGoogleSignInClient.signOut().addOnCompleteListener {
            logInfo("Sesión de Google Sign-In cerrada antes de nuevo registro")
            val signInIntent = mGoogleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
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

            // Verificar si ya existe en Firebase Auth Y en BD ANTES de proceder
            if (email != null) {
                verificarUsuarioCompleto(email) { exists ->
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
                            // Verificar NFC antes de mostrar el bottom sheet
                            val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
                            if (nfcAdapter == null) {
                                // No hay NFC, mostrar diálogo para continuar sin llavero
                                mostrarDialogoNfcNoDisponibleGoogle(nombre, apellido, email, photoUrl)
                                return@verificarUsuarioCompleto
                            }
                            if (!nfcAdapter.isEnabled) {
                                mostrarDialogoNfcNoHabilitado()
                                return@verificarUsuarioCompleto
                            }
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
        // Asegurarse de que no hay sesión activa
        if (mAuth.currentUser != null) {
            logInfo("Cerrando sesión activa antes de registrar admin")
            mAuth.signOut()
        }
        
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
                        // Cerrar sesión si hay alguna activa
                        if (mAuth.currentUser != null) {
                            mAuth.signOut()
                        }
                        
                        if (task.exception is FirebaseAuthUserCollisionException) {
                            logError("Correo ya registrado en Firebase Auth para admin: ${task.exception?.message}")
                            mostrarDialogoError("Correo Ya Registrado","Este correo electrónico ya está registrado en el sistema.\n\n¿Ya tienes una cuenta? Intenta iniciar sesión.")
                        } else {
                            logError("Error al crear usuario admin: ${task.exception?.message}")
                            mostrarDialogoError("Error de Registro", "No se pudo completar el registro. Por favor, intenta nuevamente.\n\nError: ${task.exception?.message}")
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

    private fun verificarNfcHabilitado(): Boolean {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        if (nfcAdapter == null) {
            mostrarDialogoNfcNoDisponible()
            return false
        }
        
        if (!nfcAdapter.isEnabled) {
            mostrarDialogoNfcNoHabilitado()
            return false
        }
        
        return true
    }

    private fun mostrarDialogoNfcNoHabilitado() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_nfc_no_habilitado, null)
        val builder = android.app.AlertDialog.Builder(this).setView(dialogView).setCancelable(false)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnConfiguracion = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.btnConfiguracionNfc)
        btnConfiguracion.setOnClickListener {
            dialog.dismiss()
            // Abrir configuración de NFC
            val intent = Intent(Settings.ACTION_NFC_SETTINGS)
            startActivity(intent)
        }

        val btnCancelar = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.btnCancelarNfc)
        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun mostrarDialogoNfcNoDisponible() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_nfc_no_habilitado, null)
        val builder = android.app.AlertDialog.Builder(this).setView(dialogView).setCancelable(false)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Cambiar el mensaje para indicar que el dispositivo no soporta NFC
        val txtMensaje = dialogView.findViewById<TextView>(R.id.txtMensajeNfc)
        txtMensaje.text = "Este dispositivo no soporta NFC"

        val btnConfiguracion = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.btnConfiguracionNfc)
        btnConfiguracion.visibility = android.view.View.GONE

        val btnCancelar = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.btnCancelarNfc)
        val layoutParams = btnCancelar.layoutParams
        layoutParams.width = 0
        btnCancelar.layoutParams = layoutParams

        val btnContinuarSinLlavero = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.btnContinuarSinLlavero)
        btnContinuarSinLlavero.visibility = android.view.View.VISIBLE
        val layoutParamsContinuar = btnContinuarSinLlavero.layoutParams
        layoutParamsContinuar.width = 0
        btnContinuarSinLlavero.layoutParams = layoutParamsContinuar

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        btnContinuarSinLlavero.setOnClickListener {
            dialog.dismiss()
            // Continuar con el registro sin NFC
            // Los datos del usuario ya están en los EditText, proceder con el registro
            val nombre = nombreEditText.text.toString().trim()
            val apellido = apellidoEditText.text.toString().trim()
            val numControl = numControlEditText.text.toString().trim()
            val carrera = carreraEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val perfil = "https://firebasestorage.googleapis.com/v0/b/resiclaje-39011.firebasestorage.app/o/user.png?alt=media&token=745bbda5-2229-4d42-af3f-16dd6ec8db23"
            
            // Proceder con el registro sin NFC
            crearUsuarioSinNfc(nombre, apellido, numControl, carrera, email, password, perfil)
        }

        dialog.show()
    }

    private fun crearUsuarioSinNfc(
        nombre: String,
        apellido: String,
        numControl: String,
        carrera: String,
        email: String,
        password: String,
        perfil: String
    ) {
        showLoading(true)
        
        // Asegurarse de que no hay sesión activa antes de crear un nuevo usuario
        if (mAuth.currentUser != null) {
            logInfo("Cerrando sesión previa antes de crear nuevo usuario sin NFC")
            mAuth.signOut()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                procederConRegistroSinNfc(nombre, apellido, numControl, carrera, email, password, perfil)
            }, 500)
        } else {
            procederConRegistroSinNfc(nombre, apellido, numControl, carrera, email, password, perfil)
        }
    }

    private fun procederConRegistroSinNfc(
        nombre: String,
        apellido: String,
        numControl: String,
        carrera: String,
        email: String,
        password: String,
        perfil: String
    ) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                showLoading(false)
                FCMTokenManager.checkAndUsePendingToken(this)
                
                val nip = NipGenerator.generateNip()
                logInfo("NIP generado para usuario sin NFC: $nip")
                
                val currentUser = mAuth.currentUser
                if (currentUser != null) {
                    FirebaseMessaging.getInstance().token
                        .addOnCompleteListener { tokenTask ->
                            if (!tokenTask.isSuccessful) {
                                logError("Error obteniendo token FCM")
                                Toast.makeText(this, "Error obteniendo token FCM", Toast.LENGTH_SHORT).show()
                                return@addOnCompleteListener
                            }
                            
                            val token = tokenTask.result
                            val db = FirebaseDatabase.getInstance().reference
                            val usuario = Usuario(
                                currentUser.uid,
                                nombre,
                                apellido,
                                numControl,
                                carrera,
                                email,
                                "user",
                                perfil,
                                "" // NFC vacío
                            ).apply {
                                usuario_puntos = 0
                                usuario_tokenFCM = token
                                usuario_provider = "email"
                                usuario_nip = nip
                            }
                            
                            db.child("usuarios").child(currentUser.uid).setValue(usuario)
                                .addOnSuccessListener {
                                    db.child("usuarios").child(currentUser.uid).child("usuario_puntos").setValue(0)
                                        .addOnSuccessListener {
                                            logInfo("Registro completado sin NFC con NIP: $nip")
                                            Toast.makeText(
                                                this,
                                                "Registro completo. Tu NIP es: $nip",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            
                                            val intent = Intent(this, MainActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                            finish()
                                        }
                                        .addOnFailureListener { e ->
                                            logError("Error guardando puntos: ${e.message}")
                                            Toast.makeText(this, "Error guardando puntos: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                                .addOnFailureListener { e ->
                                    logError("Error guardando datos: ${e.message}")
                                    Toast.makeText(this, "Error guardando datos: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                } else {
                    showLoading(false)
                    logError("Usuario autenticado es null después de crear cuenta")
                    Toast.makeText(this, "Error: No se pudo obtener información del usuario", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                if (mAuth.currentUser != null) {
                    mAuth.signOut()
                }
                
                if (e is FirebaseAuthUserCollisionException) {
                    logError("Correo ya registrado en Firebase Auth: ${e.message}")
                    checkIfUserExistsInDatabase(email) { existeEnBD ->
                        if (existeEnBD) {
                            mostrarDialogoError(
                                "Usuario Ya Registrado",
                                "Este correo electrónico ya está completamente registrado.\n\n¿Ya tienes una cuenta? Intenta iniciar sesión."
                            )
                        } else {
                            mostrarDialogoError(
                                "Registro Incompleto",
                                "Este correo electrónico tiene un registro incompleto.\n\n" +
                                "Por favor, intenta iniciar sesión o recupera tu contraseña usando la opción '¿Olvidaste tu contraseña?'"
                            )
                        }
                    }
                } else {
                    logError("Error al crear usuario: ${e.message}")
                    mostrarDialogoError(
                        "Error de Registro",
                        "No se pudo completar el registro. Por favor, intenta nuevamente.\n\nError: ${e.message}"
                    )
                }
            }
    }

    private fun mostrarDialogoNfcNoDisponibleGoogle(
        nombre: String,
        apellido: String,
        email: String,
        photoUrl: String
    ) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_nfc_no_habilitado, null)
        val builder = android.app.AlertDialog.Builder(this).setView(dialogView).setCancelable(false)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Cambiar el mensaje para indicar que el dispositivo no soporta NFC
        val txtMensaje = dialogView.findViewById<TextView>(R.id.txtMensajeNfc)
        txtMensaje.text = "Este dispositivo no soporta NFC"

        val btnConfiguracion = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.btnConfiguracionNfc)
        btnConfiguracion.visibility = android.view.View.GONE

        val btnCancelar = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.btnCancelarNfc)
        val layoutParams = btnCancelar.layoutParams
        layoutParams.width = 0
        btnCancelar.layoutParams = layoutParams

        val btnContinuarSinLlavero = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.btnContinuarSinLlavero)
        btnContinuarSinLlavero.visibility = android.view.View.VISIBLE
        val layoutParamsContinuar = btnContinuarSinLlavero.layoutParams
        layoutParamsContinuar.width = 0
        btnContinuarSinLlavero.layoutParams = layoutParamsContinuar

        btnCancelar.setOnClickListener {
            dialog.dismiss()
            // Cerrar sesión de Google si se cancela
            mGoogleSignInClient.signOut().addOnCompleteListener {
                mAuth.signOut()
            }
        }

        btnContinuarSinLlavero.setOnClickListener {
            dialog.dismiss()
            // Continuar con el registro sin NFC para Google
            registrarUsuarioGoogleSinNfc(nombre, apellido, email, photoUrl)
        }

        dialog.show()
    }

    private fun registrarUsuarioGoogleSinNfc(
        nombre: String,
        apellido: String,
        email: String,
        photoUrl: String
    ) {
        val nip = NipGenerator.generateNip()
        logInfo("NIP generado para usuario Google sin NFC: $nip")

        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { tokenTask ->
                    if (!tokenTask.isSuccessful) {
                        logError("Error obteniendo token FCM")
                        Toast.makeText(this, "Error obteniendo token FCM", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    val token = tokenTask.result
                    val db = FirebaseDatabase.getInstance().reference
                    val usuario = Usuario(
                        currentUser.uid,
                        nombre,
                        apellido,
                        "",
                        "",
                        email,
                        "user",
                        photoUrl,
                        "" // NFC vacío
                    ).apply {
                        usuario_puntos = 0
                        usuario_tokenFCM = token
                        usuario_provider = "google"
                        usuario_nip = nip
                    }

                    db.child("usuarios").child(currentUser.uid).setValue(usuario)
                        .addOnSuccessListener {
                            db.child("usuarios").child(currentUser.uid).child("usuario_puntos").setValue(0)
                                .addOnSuccessListener {
                                    logInfo("Registro Google completado sin NFC con NIP: $nip")
                                    Toast.makeText(
                                        this,
                                        "Registro completo. Tu NIP es: $nip",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    val intent = Intent(this, MainActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    logError("Error guardando puntos: ${e.message}")
                                    Toast.makeText(this, "Error guardando puntos: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            logError("Error guardando datos: ${e.message}")
                            Toast.makeText(this, "Error guardando datos: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
        } else {
            logError("Usuario autenticado es null después de Google Sign In")
            Toast.makeText(this, "Error: No se pudo obtener información del usuario", Toast.LENGTH_SHORT).show()
        }
    }
}