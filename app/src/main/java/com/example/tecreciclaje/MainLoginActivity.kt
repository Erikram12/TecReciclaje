package com.example.tecreciclaje

import android.content.Intent
import android.os.Bundle
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tecreciclaje.Model.NfcBottomSheetDialogFragment
import com.example.tecreciclaje.utils.FCMTokenManager
import com.example.tecreciclaje.UserPanelDynamic
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

class MainLoginActivity : AppCompatActivity(), NfcBottomSheetDialogFragment.OnRegistrationCancelledListener {

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "MainLoginActivity"
    }

    private lateinit var btnGoogle: RelativeLayout
    private lateinit var btnEmail: RelativeLayout
    private lateinit var cancelButton: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var userRef: DatabaseReference
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private var currentGoogleUser: GoogleSignInAccount? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_login)

        initViews()
        initFirebase()
        setupGoogleSignIn()
        setListeners()
    }

    private fun initViews() {
        btnGoogle = findViewById(R.id.btnGoogle)
        btnEmail = findViewById(R.id.btnEmail)
        cancelButton = findViewById(R.id.cancelButton)
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
        btnGoogle.setOnClickListener { signInWithGoogle() }
        btnEmail.setOnClickListener { openEmailLogin() }
        cancelButton.setOnClickListener { finish() }
    }

    private fun signInWithGoogle() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun openEmailLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account?.idToken)
            } catch (e: ApiException) {
                // Error en el login de Google
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Almacenar la información del usuario de Google
                        currentGoogleUser = GoogleSignIn.getLastSignedInAccount(this)
                        checkUserRole(user.uid)
                    }
                } else {
                    Toast.makeText(this, "Error en la autenticación con Google", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkUserRole(uid: String) {
        userRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Usuario existe, proceder con el login normal
                    FCMTokenManager.updateTokenForCurrentUser()
                    FCMTokenManager.checkAndUsePendingToken(this@MainLoginActivity)
                    
                    val role = snapshot.child("usuario_role").getValue(String::class.java)
                    when (role) {
                        "admin" -> goTo(AdminPanel::class.java)
                        else -> goTo(UserPanelDynamic::class.java)
                    }
                } else {
                    // Usuario no encontrado en la base de datos
                    // Si es un usuario de Google, mostrar NFC dialog para registro
                    if (currentGoogleUser != null) {
                        showNfcRegistrationDialog()
                    } else {
                        Toast.makeText(this@MainLoginActivity, "Usuario no registrado", Toast.LENGTH_SHORT).show()
                        // Cerrar sesión si no es Google
                        auth.signOut()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainLoginActivity, "Error al verificar usuario", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showNfcRegistrationDialog() {
        val googleUser = currentGoogleUser
        if (googleUser != null) {
            // Extraer información del usuario de Google
            val email = googleUser.email ?: ""
            val displayName = googleUser.displayName ?: ""
            val nameParts = displayName.split(" ")
            val nombre = nameParts.getOrNull(0) ?: ""
            val apellido = nameParts.drop(1).joinToString(" ")
            
            // Generar número de control basado en el email o usar un valor por defecto
            val numControl = generateNumControlFromEmail(email)
            
            // Determinar carrera basada en el email
            val carrera = determineCarreraFromEmail(email)
            
            // Determinar rol basado en el email
            val role = determineUserRole(email)
            
            // URL de perfil por defecto
            val perfil = "https://firebasestorage.googleapis.com/v0/b/resiclaje-39011.firebasestorage.app/o/user.png?alt=media&token=745bbda5-2229-4d42-af3f-16dd6ec8db23"
            
            // Mostrar mensaje informativo
            val roleMessage = if (role == "admin") 
                "Registro como Administrador (@tecnm.mx)" 
            else 
                "Registro como Usuario"
            Toast.makeText(this, roleMessage, Toast.LENGTH_SHORT).show()
            
            // Mostrar NFC BottomSheet para completar el registro
            val nfcDialog = NfcBottomSheetDialogFragment.newInstance(
                nombre, apellido, numControl, carrera, email, "", role, perfil, true // true = es Google Sign-In
            )
            // Establecer el listener para manejar la cancelación
            nfcDialog.setOnRegistrationCancelledListener(this)
            nfcDialog.show(supportFragmentManager, "NFC_REGISTRATION_SHEET")
        } else {
            Toast.makeText(this, "Error: No se pudo obtener información de Google", Toast.LENGTH_SHORT).show()
            auth.signOut()
        }
    }
    
    private fun generateNumControlFromEmail(email: String): String {
        // Si el email contiene un número, usarlo como base
        val numbers = email.filter { it.isDigit() }
        return if (numbers.isNotEmpty()) {
            "TEC${numbers.take(8)}"
        } else {
            // Generar un número de control basado en el hash del email
            "TEC${Math.abs(email.hashCode()).toString().take(8)}"
        }
    }
    
    private fun determineCarreraFromEmail(email: String): String {
        return when {
            email.contains("@tecnm.mx") -> "Tecnológico Nacional de México"
            email.contains("ingenieria") || email.contains("engineering") -> "Ingeniería"
            email.contains("sistemas") || email.contains("systems") -> "Sistemas Computacionales"
            email.contains("informatica") || email.contains("informatics") -> "Informática"
            else -> "" // Dejar vacío en lugar de "Carrera no especificada"
        }
    }
    
    private fun determineUserRole(email: String): String {
        return if (email.endsWith("@tecnm.mx")) "admin" else "usuario"
    }

    // Implementación del listener para manejar la cancelación del registro
    override fun onRegistrationCancelled() {
        // Limpiar la sesión de Google y Firebase
        mGoogleSignInClient.signOut().addOnCompleteListener {
            auth.signOut()
            currentGoogleUser = null
            Toast.makeText(this, "Sesión cerrada. Puedes intentar con otra cuenta.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun goTo(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
        finish()
    }
}
