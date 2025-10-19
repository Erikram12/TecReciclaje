package com.example.tecreciclaje

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        auth = FirebaseAuth.getInstance()

        // Retraso simulado (puedes ajustarlo o eliminarlo)
        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // Usuario autenticado, redirigir a MainActivity
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                // Usuario no autenticado, redirigir a MainLoginActivity
                startActivity(Intent(this, MainLoginActivity::class.java))
            }
            finish() // Finaliza SplashActivity
        }, 2000) // Duración del splash en milisegundos
    }
}
