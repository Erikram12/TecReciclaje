package com.example.tecreciclaje

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
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
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import java.util.*

class RegistroActivity : AppCompatActivity(), NfcBottomSheetDialogFragment.OnRegistrationCancelledListener {

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "RegistroActivity"
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

        // Cambiar el ícono del botón de Google al ícono correcto
        val googleIcon = findViewById<ImageView>(R.id.googleIcon)
        googleIcon.setImageResource(R.drawable.ic_google)
        
        // Configurar dropdown de carreras
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
        
        // Configurar para que muestre el dropdown al hacer clic
        carreraEditText.setOnClickListener { carreraEditText.showDropDown() }
    }

    private fun setupClickListeners() {
        btnSiguiente.setOnClickListener { registerWithEmail() }

        btnGoogle.setOnClickListener { signInWithGoogle() }

        loginRedirectText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Configurar listener para mostrar/ocultar contraseña
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
        val role = determineUserRole(email)
        val perfil = "https://firebasestorage.googleapis.com/v0/b/resiclaje-39011.firebasestorage.app/o/user.png?alt=media&token=745bbda5-2229-4d42-af3f-16dd6ec8db23"

        if (nombre.isEmpty() || apellido.isEmpty() || numControl.isEmpty() ||
                carrera.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar mensaje informativo sobre el rol asignado
        val roleMessage = if (role == "admin") 
            "Registro como Administrador (@tecnm.mx)" 
        else 
            "Registro como Usuario"
        Toast.makeText(this, roleMessage, Toast.LENGTH_SHORT).show()

        // Si es administrador, registrar directamente sin NFC
        if (role == "admin") {
            registerAdminDirectly(nombre, apellido, numControl, carrera, email, password, perfil)
        } else {
            // Si es usuario, mostrar NFC BottomSheet
            val sheet = NfcBottomSheetDialogFragment.newInstance(
                nombre, apellido, numControl, carrera, email, password, role, perfil, false // false = no es Google Sign-In
            )
            // Establecer el listener para manejar la cancelación
            sheet.setOnRegistrationCancelledListener(this)
            sheet.show(supportFragmentManager, "NFC_SHEET")
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun handleGoogleSignInSuccess(user: FirebaseUser?) {
        if (user != null) {
            val email = user.email
            val displayName = user.displayName
            val photoUrl = user.photoUrl?.toString() ?: 
                "https://firebasestorage.googleapis.com/v0/b/resiclaje-39011.firebasestorage.app/o/user.png?alt=media&token=745bbda5-2229-4d42-af3f-16dd6ec8db23"

            // Dividir el nombre completo en nombre y apellido
            val nameParts = displayName?.split(" ", limit = 2) ?: listOf("", "")
            val nombre = if (nameParts.isNotEmpty()) nameParts[0] else ""
            val apellido = if (nameParts.size > 1) nameParts[1] else ""
            val role = determineUserRole(email)

            // Mostrar mensaje informativo sobre el rol asignado
            val roleMessage = if (role == "admin") 
                "Registro como Administrador (@tecnm.mx)" 
            else 
                "Registro como Usuario"
            Toast.makeText(this, roleMessage, Toast.LENGTH_SHORT).show()

            // Usar token FCM pendiente si existe
            FCMTokenManager.checkAndUsePendingToken(this)

            // Si es administrador, registrar directamente sin NFC
            if (role == "admin") {
                registerAdminDirectly(nombre, apellido, "", "", email ?: "", "", photoUrl ?: "")
            } else {
                // Si es usuario, mostrar NFC BottomSheet
                val sheet = NfcBottomSheetDialogFragment.newInstance(
                    nombre, apellido, "", "", email ?: "", "", role, photoUrl ?: "", true // true = es Google Sign-In
                )
                // Establecer el listener para manejar la cancelación
                sheet.setOnRegistrationCancelledListener(this)
                sheet.show(supportFragmentManager, "NFC_SHEET")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val user = mAuth.currentUser
                    handleGoogleSignInSuccess(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Falló la autenticación con Google.",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "firebaseAuthWithGoogle:${account?.id}")
                firebaseAuthWithGoogle(account?.idToken)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
                
                // Limpiar la sesión de Google si el usuario canceló
                if (e.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                    mGoogleSignInClient.signOut().addOnCompleteListener {
                        Toast.makeText(this, "Registro cancelado", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Falló el registro con Google: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun togglePasswordVisibility() {
        val currentInputType = passwordEditText.inputType
        val isPasswordVisible = currentInputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        
        if (isPasswordVisible) {
            // Mostrar contraseña
            passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            passwordEditText.setCompoundDrawablesWithIntrinsicBounds(
                getDrawable(R.drawable.baseline_lock_24),
                null,
                getDrawable(R.drawable.ic_visibility),
                null
            )
        } else {
            // Ocultar contraseña
            passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            passwordEditText.setCompoundDrawablesWithIntrinsicBounds(
                getDrawable(R.drawable.baseline_lock_24),
                null,
                getDrawable(R.drawable.ic_visibility_off),
                null
            )
        }
        // Mover el cursor al final del texto
        passwordEditText.setSelection(passwordEditText.text.length)
    }

    /**
     * Determina el rol del usuario basado en el dominio del correo electrónico
     * @param email El correo electrónico del usuario
     * @return "admin" si el correo termina con @tecnm.mx, "user" en caso contrario
     */
    private fun determineUserRole(email: String?): String {
        return if (email != null && email.lowercase().endsWith("@tecnm.mx")) {
            "admin"
        } else {
            "user"
        }
    }

    /**
     * Registra directamente un administrador sin necesidad de NFC
     */
    private fun registerAdminDirectly(nombre: String, apellido: String, numControl: String, carrera: String, 
                                     email: String, password: String, perfil: String) {
        
        // Mostrar loading
        showLoading(true)

        // Si el usuario ya está autenticado (Google), usar ese UID
        if (mAuth.currentUser != null) {
            val authUid = mAuth.currentUser!!.uid
            saveAdminToDatabase(nombre, apellido, numControl, carrera, email, "admin", perfil, authUid)
        } else {
            // Crear nuevo usuario con email y password
            if (password.isEmpty()) {
                Toast.makeText(this, "Error: Password requerido para registro con email", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this,
                            "Error registrando administrador: ${task.exception?.message ?: ""}",
                            Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    /**
     * Guarda los datos del administrador en la base de datos
     */
    private fun saveAdminToDatabase(nombre: String, apellido: String, numControl: String, carrera: String,
                                   email: String, role: String, perfil: String, authUid: String) {
        
        // Obtener token FCM
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { tokenTask ->
                if (!tokenTask.isSuccessful) {
                    Toast.makeText(this, "Error obteniendo token FCM", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                    return@addOnCompleteListener
                }

                val token = tokenTask.result

                // Guardar administrador en la base de datos
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
                    "usuario_provider" to "email" // Administradores se registran siempre con email
                )

                db.child("usuarios").child(authUid).setValue(adminMap)
                    .addOnSuccessListener {
                        showLoading(false)
                        Toast.makeText(this, "Administrador registrado exitosamente", Toast.LENGTH_SHORT).show()
                        
                        // Ir directamente al AdminPanel
                        val intent = Intent(this, AdminPanel::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                    .addOnFailureListener { e ->
                        showLoading(false)
                        Toast.makeText(this, "Error guardando administrador: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
    }

    /**
     * Muestra u oculta el loading
     */
    private fun showLoading(isLoading: Boolean) {
        // Aquí puedes implementar la lógica para mostrar/ocultar un loading
        // Por ahora solo mostraremos un Toast
        if (isLoading) {
            Toast.makeText(this, "Registrando administrador...", Toast.LENGTH_SHORT).show()
        }
    }

    // Implementación del listener para manejar la cancelación del registro
    override fun onRegistrationCancelled() {
        // Limpiar la sesión de Google y Firebase
        mGoogleSignInClient.signOut().addOnCompleteListener {
            mAuth.signOut()
            Toast.makeText(this, "Sesión cerrada. Puedes intentar con otra cuenta.", Toast.LENGTH_SHORT).show()
        }
    }
}
