package com.example.tecreciclaje

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tecreciclaje.Model.NfcBottomSheetDialogFragment
import com.example.tecreciclaje.utils.FCMTokenManager
import com.example.tecreciclaje.utils.LocaleHelper
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
import java.util.Locale

class MainLoginActivity : AppCompatActivity(), NfcBottomSheetDialogFragment.OnRegistrationCancelledListener {

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "MainLoginActivity"
    }

    private lateinit var btnGoogle: RelativeLayout
    private lateinit var btnEmail: RelativeLayout
    private lateinit var cancelButton: TextView
    private lateinit var btnCambiarIdioma: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var userRef: DatabaseReference
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private var currentGoogleUser: GoogleSignInAccount? = null
    
    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "TecReciclajePrefs"
    private val KEY_IDIOMA = "idioma_seleccionado"

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.attachBaseContext(newBase))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Aplicar idioma guardado
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        aplicarIdioma()
        
        setContentView(R.layout.activity_main_login)

        initViews()
        initFirebase()
        setupGoogleSignIn()
        setListeners()
        actualizarTextoIdioma()
    }

    private fun initViews() {
        btnGoogle = findViewById(R.id.btnGoogle)
        btnEmail = findViewById(R.id.btnEmail)
        cancelButton = findViewById(R.id.cancelButton)
        btnCambiarIdioma = findViewById(R.id.btnCambiarIdioma)
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
        btnCambiarIdioma.setOnClickListener { cambiarIdioma() }
    }
    
    private fun cambiarIdioma() {
        val idiomaActual = LocaleHelper.getCurrentLanguage(this)
        val nuevoIdioma = if (idiomaActual == "es") "zap" else "es"
        
        LocaleHelper.saveLanguage(this, nuevoIdioma)
        
        // Recargar la actividad para aplicar el nuevo idioma
        recreate()
    }
    
    private fun aplicarIdioma() {
        // El idioma ya se aplicó en attachBaseContext
        // Esta función se mantiene por compatibilidad pero ya no es necesaria
    }
    
    private fun actualizarTextoIdioma() {
        val idiomaActual = sharedPreferences.getString(KEY_IDIOMA, "es") ?: "es"
        if (idiomaActual == "zap") {
            btnCambiarIdioma.text = "Zapoteco / Español"
        } else {
            btnCambiarIdioma.text = "Español / Zapoteco"
        }
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

        android.util.Log.d(TAG, "onActivityResult llamado: requestCode=$requestCode, resultCode=$resultCode")
        
        if (requestCode == RC_SIGN_IN) {
            android.util.Log.d(TAG, "Procesando resultado de Google Sign-In")
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                android.util.Log.d(TAG, "Cuenta de Google obtenida: ${account?.email}, idToken: ${if (account?.idToken != null) "presente" else "null"}")
                if (account?.idToken != null) {
                    firebaseAuthWithGoogle(account.idToken)
                } else {
                    android.util.Log.e(TAG, "Error: No se pudo obtener el token de Google")
                    Toast.makeText(this, "Error: No se pudo obtener el token de Google", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                android.util.Log.e(TAG, "Error en Google Sign-In: statusCode=${e.statusCode}, message=${e.message}")
                // Manejar diferentes tipos de errores
                val errorMessage = when (e.statusCode) {
                    com.google.android.gms.common.api.CommonStatusCodes.NETWORK_ERROR -> 
                        "Error de conexión. Verifica tu internet e intenta nuevamente."
                    com.google.android.gms.common.api.CommonStatusCodes.SIGN_IN_REQUIRED -> 
                        "Se requiere iniciar sesión. Intenta nuevamente."
                    com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> 
                        "Inicio de sesión cancelado."
                    com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_FAILED -> 
                        "Error al iniciar sesión con Google. Intenta nuevamente."
                    else -> "Error desconocido: ${e.message}"
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error inesperado en onActivityResult: ${e.message}", e)
                Toast.makeText(this, "Error inesperado: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            android.util.Log.d(TAG, "Request code no coincide: $requestCode != $RC_SIGN_IN")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String?) {
        if (idToken == null) {
            android.util.Log.e(TAG, "Error: Token de Google inválido")
            Toast.makeText(this, "Error: Token de Google inválido", Toast.LENGTH_LONG).show()
            return
        }
        
        android.util.Log.d(TAG, "Autenticando con Firebase usando token de Google")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    android.util.Log.d(TAG, "Autenticación con Firebase exitosa")
                    val user = auth.currentUser
                    if (user != null) {
                        android.util.Log.d(TAG, "Usuario de Firebase obtenido: ${user.uid}, email: ${user.email}")
                        // Almacenar la información del usuario de Google
                        currentGoogleUser = GoogleSignIn.getLastSignedInAccount(this)
                        android.util.Log.d(TAG, "Usuario de Google almacenado: ${currentGoogleUser?.email}")
                        checkUserRole(user.uid)
                    } else {
                        android.util.Log.e(TAG, "Error: No se pudo obtener el usuario de Firebase")
                        Toast.makeText(this, "Error: No se pudo obtener el usuario", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Error desconocido en la autenticación"
                    android.util.Log.e(TAG, "Error en autenticación con Firebase: $errorMessage", task.exception)
                    Toast.makeText(this, "Error en la autenticación: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun checkUserRole(uid: String) {
        android.util.Log.d(TAG, "Verificando rol del usuario: $uid")
        userRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                android.util.Log.d(TAG, "Datos recibidos del usuario. Existe: ${snapshot.exists()}")
                if (snapshot.exists()) {
                    // Usuario existe, proceder con el login normal
                    FCMTokenManager.updateTokenForCurrentUser()
                    FCMTokenManager.checkAndUsePendingToken(this@MainLoginActivity)
                    
                    val role = snapshot.child("usuario_role").getValue(String::class.java)
                    android.util.Log.d(TAG, "Rol del usuario: $role")
                    when (role) {
                        "admin" -> {
                            android.util.Log.d(TAG, "Navegando a AdminPanel")
                            goTo(AdminPanel::class.java)
                        }
                        else -> {
                            android.util.Log.d(TAG, "Navegando a UserPanelDynamic")
                            goTo(UserPanelDynamic::class.java)
                        }
                    }
                } else {
                    android.util.Log.d(TAG, "Usuario no encontrado en la base de datos")
                    // Usuario no encontrado en la base de datos
                    // Si es un usuario de Google, mostrar NFC dialog para registro
                    if (currentGoogleUser != null) {
                        android.util.Log.d(TAG, "Mostrando diálogo de registro NFC")
                        showNfcRegistrationDialog()
                    } else {
                        Toast.makeText(this@MainLoginActivity, "Usuario no registrado", Toast.LENGTH_SHORT).show()
                        // Cerrar sesión si no es Google
                        auth.signOut()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e(TAG, "Error al verificar usuario: ${error.message}")
                Toast.makeText(this@MainLoginActivity, "Error al verificar usuario: ${error.message}", Toast.LENGTH_LONG).show()
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
            
            // Verificar NFC antes de mostrar el bottom sheet
            if (!verificarNfcHabilitado()) {
                return
            }
            
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
        try {
            android.util.Log.d(TAG, "Intentando navegar a: ${activityClass.simpleName}")
            val intent = Intent(this, activityClass)
            startActivity(intent)
            android.util.Log.d(TAG, "Navegación exitosa a: ${activityClass.simpleName}")
            finish()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error al navegar a ${activityClass.simpleName}: ${e.message}", e)
            e.printStackTrace()
            Toast.makeText(this, "Error al iniciar sesión: ${e.message}", Toast.LENGTH_LONG).show()
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
            // Cerrar sesión si se cancela
            mGoogleSignInClient.signOut().addOnCompleteListener {
                auth.signOut()
                currentGoogleUser = null
            }
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
        layoutParams.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
        btnCancelar.layoutParams = layoutParams
        btnCancelar.setOnClickListener {
            dialog.dismiss()
            // Cerrar sesión si se cancela
            mGoogleSignInClient.signOut().addOnCompleteListener {
                auth.signOut()
                currentGoogleUser = null
            }
        }

        dialog.show()
    }
}
