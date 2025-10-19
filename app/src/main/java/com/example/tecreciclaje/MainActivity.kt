package com.example.tecreciclaje

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.example.tecreciclaje.data.service.MyFirebaseMessagingService
import com.example.tecreciclaje.utils.FCMTokenManager
import com.example.tecreciclaje.UserPanelDynamic
import com.example.tecreciclaje.utils.TutorialManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private lateinit var loginAnimation: LottieAnimationView

    // Código de solicitud para permisos
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance().getReference("usuarios")

        // Inicializar animación de carga
        loginAnimation = findViewById(R.id.login_animation)
        loginAnimation.visibility = View.VISIBLE
        loginAnimation.playAnimation()

        // Verificar permisos de notificación primero
        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        // Para Android 13 (API 33) y superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

                // Solicitar permiso
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            } else {
                // Permiso ya concedido, continuar con la verificación del usuario
                proceedWithUserVerification()
            }
        } else {
            // Para versiones anteriores a Android 13, no se necesita solicitar permiso
            proceedWithUserVerification()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permisos de notificación concedidos", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "Las notificaciones fueron deshabilitadas. Puedes habilitarlas en configuración.",
                    Toast.LENGTH_LONG
                ).show()
            }
            // Continuar con la verificación del usuario independientemente del resultado
            proceedWithUserVerification()
        }
    }

    private fun proceedWithUserVerification() {
        // Verificar usuario autenticado
        val user = auth.currentUser
        if (user != null) {
            // Actualizar token FCM del usuario
            FCMTokenManager.updateTokenForCurrentUser()
            getUserRole(user.uid)
        } else {
            redirectToLogin()
        }
    }

    private fun getUserRole(firebaseUid: String) {
        // Buscar directamente el UID sin recorrer todos los usuarios
        databaseRef.child(firebaseUid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                loginAnimation.cancelAnimation()
                loginAnimation.visibility = View.GONE

                if (snapshot.exists()) {
                    val role = snapshot.child("usuario_role").getValue(String::class.java)

                    when (role) {
                        "admin" -> startActivity(Intent(this@MainActivity, AdminPanel::class.java))
                        else -> startActivity(Intent(this@MainActivity, UserPanelDynamic::class.java))
                    }

                    finish() // Finaliza esta actividad
                } else {
                    redirectToLogin()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                loginAnimation.cancelAnimation()
                loginAnimation.visibility = View.GONE
                redirectToLogin()
            }
        })
    }

    private fun redirectToLogin() {
        loginAnimation.cancelAnimation()
        loginAnimation.visibility = View.GONE
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
