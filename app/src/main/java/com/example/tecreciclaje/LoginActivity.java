package com.example.tecreciclaje;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "LoginActivity";

    private EditText loginEmail, loginPassword;
    private RelativeLayout loginButton, btnGoogle;
    private TextView signupRedirectText;
    private LottieAnimationView loginAnimation;

    private FirebaseAuth auth;
    private DatabaseReference userRef;

    // Google Sign In
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        initFirebase();
        setupGoogleSignIn();
        setListeners();
    }

    private void initViews() {
        loginEmail = findViewById(R.id.login_email);
        loginPassword = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.button_layout);
        btnGoogle = findViewById(R.id.btnGoogle);
        signupRedirectText = findViewById(R.id.signUpRedirectText);
        loginAnimation = findViewById(R.id.login_animation);

        // Configurar el ícono de Google
        ImageView googleIcon = findViewById(R.id.googleIcon);
        if (googleIcon != null) {
            googleIcon.setImageResource(R.drawable.ic_google);
        }
    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference("usuarios");
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setListeners() {
        loginButton.setOnClickListener(v -> loginUser());

        btnGoogle.setOnClickListener(v -> signInWithGoogle());

        signupRedirectText.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegistroActivity.class));
        });
    }

    private void loginUser() {
        String email = loginEmail.getText().toString().trim();
        String password = loginPassword.getText().toString().trim();

        if (!validateInput(email, password)) return;

        showLoading(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser currentUser = auth.getCurrentUser();
                    if (currentUser != null) {
                        checkUserRole(currentUser.getUid());
                    } else {
                        showError("No se pudo obtener el usuario actual.");
                        showLoading(false);
                    }
                })
                .addOnFailureListener(e -> {
                    showError("Error de inicio de sesión: " + e.getMessage());
                    showLoading(false);
                });
    }

    private void signInWithGoogle() {
        showLoading(true);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                showError("Falló el inicio de sesión con Google: " + e.getMessage());
                showLoading(false);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            checkIfUserExistsInDatabase(user);
                        }
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        showError("Falló la autenticación con Google.");
                        showLoading(false);
                    }
                });
    }

    private void checkIfUserExistsInDatabase(FirebaseUser user) {
        userRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Usuario existe, proceder con el login normal
                    checkUserRole(user.getUid());
                } else {
                    // Usuario no existe, crear automáticamente con datos de Google
                    createUserFromGoogleAccount(user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError("Error al verificar usuario en base de datos.");
                showLoading(false);
            }
        });
    }

    private void createUserFromGoogleAccount(FirebaseUser user) {
        // Extraer información de la cuenta de Google
        String email = user.getEmail();
        String displayName = user.getDisplayName();
        String photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() :
                "https://firebasestorage.googleapis.com/v0/b/resiclaje-39011.firebasestorage.app/o/user.png?alt=media&token=745bbda5-2229-4d42-af3f-16dd6ec8db23";

        // Dividir el nombre completo en nombre y apellido
        String[] nameParts = displayName != null ? displayName.split(" ", 2) : new String[]{"", ""};
        String nombre = nameParts.length > 0 ? nameParts[0] : "";
        String apellido = nameParts.length > 1 ? nameParts[1] : "";

        // Crear objeto Usuario
        Map<String, Object> userData = new HashMap<>();
        userData.put("nombre", nombre);
        userData.put("apellido", apellido);
        userData.put("numControl", ""); // Vacío para usuarios de Google
        userData.put("carrera", ""); // Vacío para usuarios de Google
        userData.put("email", email);
        userData.put("role", "user"); // Por defecto usuario normal
        userData.put("perfil", photoUrl);
        userData.put("fechaRegistro", ServerValue.TIMESTAMP);

        // Guardar en Firebase Database
        userRef.child(user.getUid()).setValue(userData)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this, "Cuenta creada exitosamente con Google", Toast.LENGTH_SHORT).show();
                    goTo(UserPanel.class); // Redirigir a panel de usuario
                })
                .addOnFailureListener(e -> {
                    showError("Error al crear cuenta: " + e.getMessage());
                    showLoading(false);
                    // Cerrar sesión si falla la creación
                    auth.signOut();
                    mGoogleSignInClient.signOut();
                });
    }

    private boolean validateInput(String email, String password) {
        if (email.isEmpty()) {
            loginEmail.setError("No se permiten campos vacíos");
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            loginEmail.setError("Por favor ingresa un correo válido");
            return false;
        }

        if (password.isEmpty()) {
            loginPassword.setError("No se permiten campos vacíos");
            return false;
        }

        return true;
    }

    private void checkUserRole(String uid) {
        userRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                showLoading(false);

                if (snapshot.exists()) {
                    String role = snapshot.child("role").getValue(String.class);
                    if ("admin".equals(role)) {
                        goTo(AdminPanel.class);
                    } else {
                        goTo(UserPanel.class);
                    }
                } else {
                    showError("Datos de usuario no encontrados en la base de datos.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError("Error al obtener datos del usuario.");
                showLoading(false);
            }
        });
    }

    private void goTo(Class<?> activityClass) {
        startActivity(new Intent(LoginActivity.this, activityClass));
        finish();
    }

    private void showLoading(boolean isLoading) {
        loginAnimation.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            loginAnimation.playAnimation();
        } else {
            loginAnimation.cancelAnimation();
        }
    }

    private void showError(String message) {
        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}