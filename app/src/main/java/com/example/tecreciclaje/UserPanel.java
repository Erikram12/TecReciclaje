package com.example.tecreciclaje;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tecreciclaje.Model.CircleTransform;
import com.example.tecreciclaje.userpanel.CafeActivity;
import com.example.tecreciclaje.userpanel.ComidaActivity;
import com.example.tecreciclaje.userpanel.DesayunoActivity;
import com.example.tecreciclaje.userpanel.HistorialActivity;
import com.example.tecreciclaje.userpanel.MisValesActivity;
import com.example.tecreciclaje.userpanel.PerfilActivity;
import com.example.tecreciclaje.userpanel.RefrescoActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.example.tecreciclaje.Model.MyFirebaseMessagingService;
import com.squareup.picasso.Picasso;

public class UserPanel extends AppCompatActivity {

    private CardView clothingCard, clothingCard2, clothingCard3, clothingCard4;
    private FirebaseAuth auth;
    private Button logoutButton;
    private TextView textViewName, textViewSaldo;
    private ImageView imageViewProfile;

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_panel);

        auth = FirebaseAuth.getInstance();
        initializeViews();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(UserPanel.this, LoginActivity.class));
            finish();
            return;
        }

        // Verificar permisos de notificación al entrar al panel de usuario
        checkNotificationPermission();

        // Actualizar token FCM para el usuario actual
        updateFCMToken();

        getUserData(currentUser.getUid());
        setupNavigation();
        setupCardListeners();
        setupLogoutButton();
    }

    private void checkNotificationPermission() {
        // Para Android 13 (API 33) y superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                // Mostrar diálogo explicativo antes de solicitar permiso
                new AlertDialog.Builder(this)
                        .setTitle("Notificaciones")
                        .setMessage("¿Te gustaría recibir notificaciones sobre nuevos productos y ofertas especiales?")
                        .setPositiveButton("Sí, habilitar", (dialog, which) -> {
                            // Solicitar permiso
                            ActivityCompat.requestPermissions(
                                    this,
                                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                    NOTIFICATION_PERMISSION_REQUEST_CODE
                            );
                        })
                        .setNegativeButton("Ahora no", (dialog, which) -> {
                            Toast.makeText(this, "Puedes habilitar las notificaciones después en tu perfil",
                                    Toast.LENGTH_LONG).show();
                        })
                        .setCancelable(false)
                        .show();
            }
        }
    }

    private void generateNewFCMToken(String uid) {
        // Eliminar token anterior forzando regeneración
        FirebaseMessaging.getInstance().deleteToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FCM", "Token anterior eliminado");

                        // Generar nuevo token
                        FirebaseMessaging.getInstance().getToken()
                                .addOnCompleteListener(tokenTask -> {
                                    if (!tokenTask.isSuccessful()) {
                                        Log.w("FCM", "Error al obtener nuevo token", tokenTask.getException());
                                        return;
                                    }

                                    // Obtener y guardar nuevo token
                                    String newToken = tokenTask.getResult();
                                    Log.d("FCM", "Nuevo token generado: " + newToken);

                                    // Guardar en base de datos
                                    FirebaseDatabase.getInstance()
                                            .getReference("usuarios")
                                            .child(uid)
                                            .child("fcm_token")
                                            .setValue(newToken)
                                            .addOnSuccessListener(aVoid ->
                                                    Log.d("FCM", "Token guardado exitosamente"))
                                            .addOnFailureListener(e ->
                                                    Log.e("FCM", "Error al guardar token: " + e.getMessage()));
                                });
                    } else {
                        Log.e("FCM", "Error al eliminar token anterior", task.getException());
                    }
                });
    }

    private void updateFCMToken() {
        // Forzar actualización del token FCM cada vez que el usuario inicia sesión
        MyFirebaseMessagingService.updateTokenForCurrentUser();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "¡Perfecto! Recibirás notificaciones sobre ofertas y productos",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Sin problema. Puedes activar las notificaciones después en configuración",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeViews() {
        clothingCard = findViewById(R.id.clothingCard);
        clothingCard2 = findViewById(R.id.clothingCard2);
        clothingCard3 = findViewById(R.id.clothingCard3);
        clothingCard4 = findViewById(R.id.clothingCard4);
        logoutButton = findViewById(R.id.logout_button);
        textViewName = findViewById(R.id.textViewName);
        textViewSaldo = findViewById(R.id.textViewSaldo);
        imageViewProfile = findViewById(R.id.imageViewProfile);
    }

    private void setupNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Selecciona el ítem actual (cambia según la actividad actual)
        bottomNavigationView.setSelectedItemId(R.id.nav_home); // 👈 actualízalo en cada Activity

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Class<?> currentClass = getClass(); // Detecta clase actual

            if (itemId == R.id.nav_home && !currentClass.equals(UserPanel.class)) {
                startActivity(new Intent(this, UserPanel.class));
                overridePendingTransition(0, 0);
                return true;

            } else if (itemId == R.id.nav_docs && !currentClass.equals(MisValesActivity.class)) {
                startActivity(new Intent(this, MisValesActivity.class));
                overridePendingTransition(0, 0);
                return true;

            } else if (itemId == R.id.nav_histori && !currentClass.equals(HistorialActivity.class)) {
                startActivity(new Intent(this, HistorialActivity.class));
                overridePendingTransition(0, 0);
                return true;

            } else if (itemId == R.id.nav_perfil && !currentClass.equals(PerfilActivity.class)) {
                startActivity(new Intent(this, PerfilActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }

            return true; // Si ya estás en la actividad, simplemente no hace nada
        });
    }

    private void setupCardListeners() {
        clothingCard.setOnClickListener(view -> startActivity(new Intent(UserPanel.this, CafeActivity.class)));
        clothingCard2.setOnClickListener(view -> startActivity(new Intent(UserPanel.this, DesayunoActivity.class)));
        clothingCard3.setOnClickListener(view -> startActivity(new Intent(UserPanel.this, ComidaActivity.class)));
        clothingCard4.setOnClickListener(view -> startActivity(new Intent(UserPanel.this, RefrescoActivity.class)));
    }

    private void setupLogoutButton() {
        logoutButton.setOnClickListener(v -> new AlertDialog.Builder(UserPanel.this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    auth.signOut();
                    Toast.makeText(UserPanel.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(UserPanel.this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show());
    }

    private void getUserData(String uid) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid);

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String nombre = snapshot.child("nombre").getValue(String.class);
                    String apellido = snapshot.child("apellido").getValue(String.class);
                    Integer puntos = snapshot.child("user_puntos").getValue(Integer.class);
                    String perfilUrl = snapshot.child("perfil").getValue(String.class);

                    textViewName.setText("¡Hola, " + nombre + "!");
                    textViewSaldo.setText("Saldo disponible: " + (puntos != null ? puntos : 0) + " pts");

                    if (perfilUrl != null && !perfilUrl.isEmpty()) {
                        Picasso.get()
                                .load(perfilUrl)
                                .transform(new CircleTransform())
                                .placeholder(R.drawable.user)
                                .error(R.drawable.user)
                                .into(imageViewProfile);
                    } else {
                        imageViewProfile.setImageResource(R.drawable.user);
                    }
                } else {
                    Toast.makeText(UserPanel.this, "No se encontraron datos del usuario.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("UserPanel", "Database error: " + error.getMessage());
            }
        });
    }
}