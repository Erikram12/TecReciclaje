package com.example.tecreciclaje.userpanel;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.example.tecreciclaje.LoginActivity;
import com.example.tecreciclaje.Model.CircleTransform;
import com.example.tecreciclaje.R;
import com.example.tecreciclaje.UserPanel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PerfilActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private DatabaseReference userRef;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private Button btnActualizar, btnSelectImage;
    private EditText editTextNombre, editTextApellido, editTextCorreo, editTextEdad;
    private ImageView imageViewProfile;

    private Uri imageUri;
    private String imageUrlActual;
    private Dialog loadingDialog;

    private final ActivityResultLauncher<CropImageContractOptions> cropImage =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    Uri uriContent = result.getUriContent();
                    imageUri = uriContent;
                    imageViewProfile.setImageURI(imageUri);
                } else {
                    Toast.makeText(this, "Error al recortar imagen.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        initializeViews();
        setupNavigation();
        setupLoadingDialog();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(PerfilActivity.this, LoginActivity.class));
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(currentUser.getUid());
        getUserData();

        btnActualizar.setOnClickListener(v -> confirmUpdateProfile());
        btnSelectImage.setOnClickListener(v -> showImageSourceDialog());
    }

    private void initializeViews() {
        btnActualizar = findViewById(R.id.btnActualizar);
        btnSelectImage = findViewById(R.id.btnSelectImage);

        editTextNombre = findViewById(R.id.editTextNombre);
        editTextApellido = findViewById(R.id.editTextApellido);
        editTextCorreo = findViewById(R.id.editTextCorreo);
        editTextEdad = findViewById(R.id.editTextEdad);
        imageViewProfile = findViewById(R.id.imageViewProfile2);
    }

    private void setupNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_perfil);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Class<?> currentClass = getClass();

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

            return true;
        });
    }

    private void setupLoadingDialog() {
        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.dialog_loading);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        loadingDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void showImageSourceDialog() {
        String[] options = {"Cámara", "Galería"};
        new AlertDialog.Builder(this)
                .setTitle("Selecciona la fuente de la imagen")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(cameraIntent, 100);
                    } else {
                        selectImage();
                    }
                })
                .show();
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Selecciona una imagen"), 200);
    }

    private void getUserData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String nombre = snapshot.child("nombre").getValue(String.class);
                    String apellido = snapshot.child("apellido").getValue(String.class);
                    String correo = snapshot.child("email").getValue(String.class);
                    String edad = snapshot.child("edad").getValue(String.class);
                    imageUrlActual = snapshot.child("perfil").getValue(String.class);

                    editTextNombre.setText(nombre);
                    editTextApellido.setText(apellido);
                    editTextCorreo.setText(correo);
                    editTextEdad.setText(edad);

                    if (imageUrlActual != null) {
                        Picasso.get().load(imageUrlActual).transform(new CircleTransform()).into(imageViewProfile);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(PerfilActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmUpdateProfile() {
        new AlertDialog.Builder(this)
                .setTitle("Actualizar Perfil")
                .setMessage("¿Deseas guardar los cambios?")
                .setPositiveButton("Sí", (dialog, which) -> updateUserProfile())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void updateUserProfile() {
        loadingDialog.show();
        String nombre = editTextNombre.getText().toString().trim();
        String apellido = editTextApellido.getText().toString().trim();
        String correo = editTextCorreo.getText().toString().trim();
        String edad = editTextEdad.getText().toString().trim();

        if (nombre.isEmpty() || apellido.isEmpty() || correo.isEmpty() || edad.isEmpty()) {
            loadingDialog.dismiss();
            Toast.makeText(this, "Completa todos los campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri != null) {
            uploadImageAndSave(nombre, apellido, correo, edad);
        } else {
            guardarDatosEnDatabase(nombre, apellido, correo, edad, imageUrlActual);
        }
    }

    private void uploadImageAndSave(String nombre, String apellido, String correo, String edad) {
        StorageReference imgRef = storageRef.child("Perfiles/" + UUID.randomUUID().toString());
        imgRef.putFile(imageUri).addOnSuccessListener(taskSnapshot ->
                imgRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    guardarDatosEnDatabase(nombre, apellido, correo, edad, uri.toString());
                })
        ).addOnFailureListener(e -> {
            loadingDialog.dismiss();
            Toast.makeText(this, "Error al subir imagen.", Toast.LENGTH_SHORT).show();
        });
    }

    private void guardarDatosEnDatabase(String nombre, String apellido, String correo, String edad, String imagenUrl) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", nombre);
        datos.put("apellido", apellido);
        datos.put("email", correo);
        datos.put("edad", edad);
        if (imagenUrl != null) datos.put("perfil", imagenUrl);

        userRef.updateChildren(datos).addOnSuccessListener(aVoid -> {
            loadingDialog.dismiss();
            Toast.makeText(this, "Perfil actualizado.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, UserPanel.class));
            finish();
        }).addOnFailureListener(e -> {
            loadingDialog.dismiss();
            Toast.makeText(this, "Error al actualizar perfil.", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == 100) {
                Uri cameraImageUri = data.getData();
                if (cameraImageUri != null) {
                    launchCropper(cameraImageUri);
                } else if (data.getExtras() != null) {
                    imageUri = (Uri) data.getExtras().get("data");
                    launchCropper(imageUri);
                }
            } else if (requestCode == 200 && data != null) {
                Uri selectedImage = data.getData();
                if (selectedImage != null) {
                    launchCropper(selectedImage);
                }
            }
        }
    }

    private void launchCropper(Uri imageUri) {
        CropImageOptions options = new CropImageOptions();
        options.cropShape = CropImageView.CropShape.OVAL;
        options.aspectRatioX = 1;
        options.aspectRatioY = 1;
        options.fixAspectRatio = true;

        cropImage.launch(new CropImageContractOptions(imageUri, options));
    }
}
