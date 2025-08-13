package com.example.tecreciclaje.userpanel;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tecreciclaje.LoginActivity;
import com.example.tecreciclaje.Model.CircleTransform;
import com.example.tecreciclaje.Model.ValeModel;
import com.example.tecreciclaje.Model.ValesAdapter;
import com.example.tecreciclaje.R;
import com.example.tecreciclaje.UserPanel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.squareup.picasso.Picasso;

import java.util.*;

public class MisValesActivity extends AppCompatActivity {

    private RecyclerView recyclerVales;
    private ValesAdapter adapter;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private List<ValeModel> valesList = new ArrayList<>();
    private TextView textViewName, textViewSaldo;
    private ImageView imageViewProfile;
    private Spinner spinnerFiltro;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mis_vales);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initializeViews();
        getUserData(currentUser.getUid());
        obtenerVales(currentUser.getUid());
        setupLogoutButton();
        setupNavigation();

        spinnerFiltro = findViewById(R.id.spinnerFiltro);
        spinnerFiltro.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String filtroSeleccionado = parent.getItemAtPosition(position).toString();
                filtrarVales(filtroSeleccionado); // 👈 Aplica filtro al cambiar
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void initializeViews() {
        recyclerVales = findViewById(R.id.recyclerVales);
        logoutButton = findViewById(R.id.logout_button);
        textViewName = findViewById(R.id.textViewName);
        textViewSaldo = findViewById(R.id.textViewSaldo);
        imageViewProfile = findViewById(R.id.imageViewProfile);

        adapter = new ValesAdapter(valesList, this);
        recyclerVales.setLayoutManager(new LinearLayoutManager(this));
        recyclerVales.setAdapter(adapter);

    }

    private void filtrarVales(String estadoFiltro) {
        List<ValeModel> filtrados = new ArrayList<>();

        for (ValeModel vale : valesList) {
            if ("Todos".equalsIgnoreCase(estadoFiltro)) {
                filtrados.add(vale);
            } else if ("Válido".equalsIgnoreCase(estadoFiltro) && "Válido".equalsIgnoreCase(vale.estado)) {
                filtrados.add(vale);
            } else if ("Expirado".equalsIgnoreCase(estadoFiltro) && "Expirado".equalsIgnoreCase(vale.estado)) {
                filtrados.add(vale);
            } else if ("Expirado".equalsIgnoreCase(estadoFiltro) && "Reclamado".equalsIgnoreCase(vale.estado)) {
                filtrados.add(vale);
            }
        }

        adapter = new ValesAdapter(filtrados, this);
        recyclerVales.setAdapter(adapter);
    }


    private void getUserData(String uid) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid);

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String nombre = snapshot.child("nombre").getValue(String.class);
                    String apellido = snapshot.child("apellido").getValue(String.class);
                    int puntos = snapshot.child("user_puntos").exists() ? snapshot.child("user_puntos").getValue(Integer.class) : 0;
                    String perfilUrl = snapshot.child("perfil").getValue(String.class);

                    textViewName.setText("¡Hola, " + nombre + "!");
                    textViewSaldo.setText("Saldo disponible: " + puntos + " pts");

                    if (perfilUrl != null && !perfilUrl.isEmpty()) {
                        Picasso.get().load(perfilUrl).transform(new CircleTransform()).into(imageViewProfile);
                    } else {
                        imageViewProfile.setImageResource(R.drawable.user);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(MisValesActivity.this, "Error al obtener datos del usuario.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void obtenerVales(String uid) {
        DatabaseReference valesRef = FirebaseDatabase.getInstance().getReference("vales");

        valesRef.orderByChild("usuario_id").equalTo(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        valesList.clear();
                        long ahora = System.currentTimeMillis();

                        for (DataSnapshot s : snapshot.getChildren()) {
                            ValeModel vale = s.getValue(ValeModel.class);

                            if (vale != null) {
                                vale.vale_id = s.getKey(); // Asegúrate de guardar el ID
                                if ("Válido".equalsIgnoreCase(vale.estado)) {
                                    if (vale.fecha_expiracion > 0 && vale.fecha_expiracion < (System.currentTimeMillis() - 1000)) {
                                        // Solo marcar expirado si ya pasó un segundo (previene falsos positivos)
                                        s.getRef().child("estado").setValue("Expirado");
                                        vale.estado = "Expirado";
                                    }
                                }
                                valesList.add(vale);
                            }
                        }

                        // ✅ Aplica el filtro actual
                        if (spinnerFiltro != null && spinnerFiltro.getSelectedItem() != null) {
                            String filtroSeleccionado = spinnerFiltro.getSelectedItem().toString();
                            filtrarVales(filtroSeleccionado);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(MisValesActivity.this, "Error al obtener vales", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void setupLogoutButton() {
        logoutButton.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    auth.signOut();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show());
    }

    private void setupNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_docs);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, UserPanel.class));
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

    // 🔷 Método para mostrar el código QR como ventana emergente
    public void mostrarQrDialog(String valeId) {
        if (valeId == null) {
            Toast.makeText(this, "ID de vale inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Código QR del Vale");

        ImageView qrImage = new ImageView(this);
        int size = 800;

        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(valeId, BarcodeFormat.QR_CODE, size, size);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            qrImage.setImageBitmap(bitmap);

            builder.setView(qrImage);
            builder.setPositiveButton("Cerrar", null);
            builder.setNeutralButton("Descargar", (dialog, which) -> {
                String savedImageURL = MediaStore.Images.Media.insertImage(
                        getContentResolver(), bitmap, "QR_Vale", "Código QR generado");
                if (savedImageURL != null) {
                    Toast.makeText(this, "Imagen guardada en galería", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error al guardar imagen", Toast.LENGTH_SHORT).show();
                }
            });

            builder.show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al generar QR", Toast.LENGTH_SHORT).show();
        }
    }
}
