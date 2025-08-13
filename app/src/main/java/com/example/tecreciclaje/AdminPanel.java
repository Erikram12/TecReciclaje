package com.example.tecreciclaje;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.tecreciclaje.Model.CircleTransform;
import com.example.tecreciclaje.adminpanel.EscanearQrActivity;
import com.example.tecreciclaje.userpanel.CafeActivity;
import com.example.tecreciclaje.userpanel.DesayunoActivity;
import com.example.tecreciclaje.userpanel.HistorialActivity;
import com.example.tecreciclaje.userpanel.MisValesActivity;
import com.example.tecreciclaje.userpanel.PerfilActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class AdminPanel extends AppCompatActivity {

    private CardView clothingCard, clothingCard2, clothingCard3, clothingCard4;
    private FirebaseAuth auth;
    private Button logoutButton;
    private TextView textViewName, textViewSaldo;
    private ImageView imageViewProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_panel);

        auth = FirebaseAuth.getInstance();
        initializeViews();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(AdminPanel.this, LoginActivity.class));
            getUserData(currentUser.getUid()); // 🔁 Fuerza el refresco de datos
            finish();
            return;
        }

        getUserData(currentUser.getUid());
        setupNavigation();
        setupCardListeners();
        setupLogoutButton();

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
        clothingCard.setOnClickListener(view -> startActivity(new Intent(AdminPanel.this, EscanearQrActivity.class)));
        clothingCard2.setOnClickListener(view -> startActivity(new Intent(AdminPanel.this, DesayunoActivity.class)));
        clothingCard3.setOnClickListener(view -> startActivity(new Intent(AdminPanel.this, MisValesActivity.class)));
        clothingCard4.setOnClickListener(view -> startActivity(new Intent(AdminPanel.this, CafeActivity.class)));
    }

    private void setupLogoutButton() {
        logoutButton.setOnClickListener(v -> new AlertDialog.Builder(AdminPanel.this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    auth.signOut();
                    Toast.makeText(AdminPanel.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(AdminPanel.this, LoginActivity.class));
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

                    textViewName.setText("¡Hola, " + nombre + " " + apellido + "!");
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
                    Toast.makeText(AdminPanel.this, "No se encontraron datos del usuario.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminPanel.this, "Error al obtener datos del usuario.", Toast.LENGTH_SHORT).show();
                Log.e("UserPanel", "Database error: " + error.getMessage());
            }
        });

    }
}
