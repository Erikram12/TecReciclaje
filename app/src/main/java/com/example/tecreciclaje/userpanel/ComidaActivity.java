package com.example.tecreciclaje.userpanel;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.tecreciclaje.LoginActivity;
import com.example.tecreciclaje.Model.CircleTransform;
import com.example.tecreciclaje.R;
import com.example.tecreciclaje.UserPanel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.squareup.picasso.Picasso;

public class ComidaActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private TextView textViewName, textViewSaldo, textViewProgreso;
    private ImageView imageViewProfile;
    private Button btnCanjear, logoutButton;
    private ProgressBar progressBar;
    private static final int PUNTOS_NECESARIOS = 700;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comida);

        auth = FirebaseAuth.getInstance();
        initializeViews();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        getUserData(currentUser.getUid());
        setupNavigation();
        setupLogoutButton();
    }

    private void initializeViews() {
        textViewName = findViewById(R.id.textViewName);
        textViewSaldo = findViewById(R.id.textViewSaldo);
        textViewProgreso = findViewById(R.id.textViewProgreso);
        imageViewProfile = findViewById(R.id.imageViewProfile);
        btnCanjear = findViewById(R.id.btnCanjear);
        logoutButton = findViewById(R.id.logout_button);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, UserPanel.class));
                return true;
            } else if (itemId == R.id.nav_docs) {
                startActivity(new Intent(this, MisValesActivity.class));
                return true;
            } else if (itemId == R.id.nav_histori) {
                startActivity(new Intent(this, HistorialActivity.class));
                return true;
            } else if (itemId == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupLogoutButton() {
        logoutButton.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    auth.signOut();
                    Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show());
    }

    private void getUserData(String uid) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String nombre = snapshot.child("nombre").getValue(String.class);
                    String apellido = snapshot.child("apellido").getValue(String.class);
                    int puntos = snapshot.child("user_puntos").exists() ? snapshot.child("user_puntos").getValue(Integer.class) : 0;
                    String perfilUrl = snapshot.child("perfil").getValue(String.class);

                    textViewName.setText(nombre + " " + apellido);
                    textViewSaldo.setText("Saldo disponible: " + puntos + " pts");
                    progressBar.setMax(PUNTOS_NECESARIOS);
                    progressBar.setProgress(puntos);
                    textViewProgreso.setText(puntos + "/" + PUNTOS_NECESARIOS);

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

                    setupCanjearButton(puntos, uid);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ComidaActivity.this, "Error al obtener datos del usuario.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupCanjearButton(int puntos, String uid) {
        btnCanjear.setOnClickListener(v -> {
            if (puntos >= PUNTOS_NECESARIOS) {
                new AlertDialog.Builder(this)
                        .setTitle("Confirmar Canje")
                        .setMessage("¿Estás seguro de que deseas canjear 700 puntos por una comida?")
                        .setPositiveButton("Sí", (dialog, which) -> {
                            realizarCanje(uid, puntos);
                        })
                        .setNegativeButton("No", null)
                        .show();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Puntos insuficientes")
                        .setMessage("Necesitas al menos " + PUNTOS_NECESARIOS + " puntos para canjear este producto.")
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void realizarCanje(String uid, int puntosActuales) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid);
        DatabaseReference historialRef = userRef.child("historial");
        DatabaseReference valesRef = FirebaseDatabase.getInstance().getReference("vales");

        int nuevosPuntos = puntosActuales - PUNTOS_NECESARIOS;
        long fechaActual = System.currentTimeMillis();
        long fechaExpira = fechaActual + (3 * 24 * 60 * 60 * 1000); // 3 días en milisegundos


        userRef.child("user_puntos").setValue(nuevosPuntos);

        // Crear vale
        String valeId = valesRef.push().getKey();
        if (valeId != null) {
            DatabaseReference nuevoValeRef = valesRef.child(valeId);
            nuevoValeRef.child("usuario_id").setValue(uid);
            nuevoValeRef.child("producto").setValue("Comida");
            nuevoValeRef.child("estado").setValue("Válido");
            nuevoValeRef.child("imagen_url").setValue("https://firebasestorage.googleapis.com/v0/b/resiclaje-39011.firebasestorage.app/o/comida.png?alt=media&token=bc60a2bd-647f-4fe7-b19e-03a1ba3533af");
            nuevoValeRef.child("fecha_creacion").setValue(fechaActual);
            nuevoValeRef.child("fecha_expiracion").setValue(fechaExpira);
        }

        // Registrar en historial
        String historialId = historialRef.push().getKey();
        if (historialId != null) {
            historialRef.child(historialId).child("tipo").setValue("canjeado");
            historialRef.child(historialId).child("cantidad").setValue(-PUNTOS_NECESARIOS);
            historialRef.child(historialId).child("producto").setValue("Comida");
            historialRef.child(historialId).child("fecha").setValue(fechaActual);
        }

        new AlertDialog.Builder(this)
                .setTitle("¡Canje exitoso!")
                .setMessage("Tu vale ha sido generado correctamente.")
                .setPositiveButton("Ver vale", (dialog, which) -> {
                    Intent intent = new Intent(ComidaActivity.this, MisValesActivity.class);
                    startActivity(intent);
                    finish(); // opcional: si quieres cerrar CafeActivity
                })
                .setNegativeButton("Cerrar", null)
                .show();

    }
}
