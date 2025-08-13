package com.example.tecreciclaje;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        auth = FirebaseAuth.getInstance();

        // Retraso simulado (puedes ajustarlo o eliminarlo)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                FirebaseUser currentUser = auth.getCurrentUser();
                if (currentUser != null) {
                    // Usuario autenticado, redirigir a MainActivity
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                } else {
                    // Usuario no autenticado, redirigir a LoginActivity
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                }
                finish(); // Finaliza SplashActivity
            }
        }, 2000); // Duración del splash en milisegundos
    }
}
