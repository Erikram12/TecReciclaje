package com.example.tecreciclaje;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.tecreciclaje.Model.MyFirebaseMessagingService;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private DatabaseReference databaseRef;
    private LottieAnimationView loginAnimation;

    // Código de solicitud para permisos
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("usuarios");

        // Inicializar animación de carga
        loginAnimation = findViewById(R.id.login_animation);
        loginAnimation.setVisibility(View.VISIBLE);
        loginAnimation.playAnimation();

        // Verificar permisos de notificación primero
        checkNotificationPermission();
    }

    private void checkNotificationPermission() {
        // Para Android 13 (API 33) y superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                // Solicitar permiso
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );
            } else {
                // Permiso ya concedido, continuar con la verificación del usuario
                proceedWithUserVerification();
            }
        } else {
            // Para versiones anteriores a Android 13, no se necesita solicitar permiso
            proceedWithUserVerification();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permisos de notificación concedidos", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Las notificaciones fueron deshabilitadas. Puedes habilitarlas en configuración.",
                        Toast.LENGTH_LONG).show();
            }
            // Continuar con la verificación del usuario independientemente del resultado
            proceedWithUserVerification();
        }
    }

    private void proceedWithUserVerification() {
        // Verificar usuario autenticado
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // Actualizar token FCM del usuario
            MyFirebaseMessagingService.updateTokenForCurrentUser();
            getUserRole(user.getUid());
        } else {
            redirectToLogin();
        }
    }

    private void getUserRole(String firebaseUid) {
        // Buscar directamente el UID sin recorrer todos los usuarios
        databaseRef.child(firebaseUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                loginAnimation.cancelAnimation();
                loginAnimation.setVisibility(View.GONE);

                if (snapshot.exists()) {
                    String role = snapshot.child("role").getValue(String.class);

                    if ("admin".equals(role)) {
                        startActivity(new Intent(MainActivity.this, AdminPanel.class));
                    } else {
                        startActivity(new Intent(MainActivity.this, UserPanel.class));
                    }

                    finish(); // Finaliza esta actividad
                } else {
                    redirectToLogin();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loginAnimation.cancelAnimation();
                loginAnimation.setVisibility(View.GONE);
                redirectToLogin();
            }
        });
    }

    private void redirectToLogin() {
        loginAnimation.cancelAnimation();
        loginAnimation.setVisibility(View.GONE);
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }
}