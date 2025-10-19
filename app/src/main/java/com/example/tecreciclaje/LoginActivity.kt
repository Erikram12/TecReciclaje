package com.example.tecreciclaje

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.example.tecreciclaje.utils.CustomAlertDialog
import com.example.tecreciclaje.utils.FCMTokenManager
import com.example.tecreciclaje.UserPanelDynamic
import com.example.tecreciclaje.utils.SessionManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "LoginActivity"
    }

    private lateinit var loginEmail: EditText
    private lateinit var loginPassword: EditText
    private lateinit var loginButton: RelativeLayout
    private lateinit var btnBack: RelativeLayout
    private lateinit var signupRedirectText: TextView
    private lateinit var loginAnimation: LottieAnimationView

    private lateinit var auth: FirebaseAuth
    private lateinit var userRef: DatabaseReference

    // Google Sign In
    private lateinit var mGoogleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        initFirebase()
        setupGoogleSignIn()
        setListeners()
    }

    private fun initViews() {
        loginEmail = findViewById(R.id.login_email)
        loginPassword = findViewById(R.id.login_password)
        loginButton = findViewById(R.id.button_layout)
        btnBack = findViewById(R.id.btnBack)
        signupRedirectText = findViewById(R.id.signUpRedirectText)
        loginAnimation = findViewById(R.id.login_animation)
    }

    private fun initFirebase() {
        auth = FirebaseAuth.getInstance()
        userRef = FirebaseDatabase.getInstance().getReference("usuarios")
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setListeners() {
        loginButton.setOnClickListener { loginUser() }

        btnBack.setOnClickListener {
            startActivity(Intent(this, MainLoginActivity::class.java))
            finish()
        }

        signupRedirectText.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }

        // Configurar listener para mostrar/ocultar contraseña
        loginPassword.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (loginPassword.right - loginPassword.compoundDrawables[2].bounds.width())) {
                    togglePasswordVisibility()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun loginUser() {
        val email = loginEmail.text.toString().trim()
        val password = loginPassword.text.toString().trim()

        if (!validateInput(email, password)) return

        showLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    checkUserRole(currentUser.uid)
                } else {
                    showError("No se pudo obtener el usuario actual.")
                    showLoading(false)
                }
            }
            .addOnFailureListener { e ->
                showError("Error de inicio de sesión: ${e.message}")
                showLoading(false)
            }
    }

    private fun signInWithGoogle() {
        showLoading(true)
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "firebaseAuthWithGoogle:${account?.id}")
                firebaseAuthWithGoogle(account?.idToken)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                showError("Falló el inicio de sesión con Google: ${e.message}")
                showLoading(false)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    if (user != null) {
                        checkIfUserExistsInDatabase(user)
                    }
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    showError("Falló la autenticación con Google.")
                    showLoading(false)
                }
            }
    }

    private fun checkIfUserExistsInDatabase(user: FirebaseUser) {
        userRef.child(user.uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Usuario existe, proceder con el login normal
                    checkUserRole(user.uid)
                } else {
                    // Usuario no existe, mostrar alerta y cerrar sesión
                    showUserNotRegisteredAlert()
                    signOutGoogleUser()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showError("Error al verificar usuario en base de datos.")
                signOutGoogleUser()
            }
        })
    }

    private fun showUserNotRegisteredAlert() {
        showLoading(false)

        CustomAlertDialog.createConfirmationDialog(
            this,
            "Usuario no registrado",
            "Usuario no registrado, regístrate e intenta nuevamente",
            "Ir al Registro",
            "Cancelar",
            {
                // Redirigir a la actividad de registro
                startActivity(Intent(this, RegistroActivity::class.java))
            },
            null
        ).setCancelable(false).show()
    }

    private fun signOutGoogleUser() {
        // LIMPIEZA COMPLETA DE SESIÓN
        SessionManager.clearCompleteSession(this)
        showLoading(false)
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            loginEmail.error = "No se permiten campos vacíos"
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            loginEmail.error = "Por favor ingresa un correo válido"
            return false
        }

        if (password.isEmpty()) {
            loginPassword.error = "No se permiten campos vacíos"
            return false
        }

        return true
    }

    private fun checkUserRole(uid: String) {
        userRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                showLoading(false)

                if (snapshot.exists()) {
                    // ACTUALIZAR TOKEN FCM DESPUÉS DE LOGIN EXITOSO
                    FCMTokenManager.updateTokenForCurrentUser()
                    
                    // Usar token FCM pendiente si existe
                    FCMTokenManager.checkAndUsePendingToken(this@LoginActivity)
                    
                    val role = snapshot.child("usuario_role").getValue(String::class.java)
                    when (role) {
                        "admin" -> goTo(AdminPanel::class.java)
                        else -> goTo(UserPanelDynamic::class.java)
                    }
                } else {
                    showError("Datos de usuario no encontrados en la base de datos.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showError("Error al obtener datos del usuario.")
                showLoading(false)
            }
        })
    }

    private fun goTo(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
        finish()
    }

    private fun showLoading(isLoading: Boolean) {
        loginAnimation.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            loginAnimation.playAnimation()
        } else {
            loginAnimation.cancelAnimation()
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun togglePasswordVisibility() {
        val currentInputType = loginPassword.inputType
        val isPasswordVisible = currentInputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        
        if (isPasswordVisible) {
            // Mostrar contraseña
            loginPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            loginPassword.setCompoundDrawablesWithIntrinsicBounds(
                getDrawable(R.drawable.baseline_lock_24),
                null,
                getDrawable(R.drawable.ic_visibility),
                null
            )
        } else {
            // Ocultar contraseña
            loginPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            loginPassword.setCompoundDrawablesWithIntrinsicBounds(
                getDrawable(R.drawable.baseline_lock_24),
                null,
                getDrawable(R.drawable.ic_visibility_off),
                null
            )
        }
        // Mover el cursor al final del texto
        loginPassword.setSelection(loginPassword.text.length)
    }
}
