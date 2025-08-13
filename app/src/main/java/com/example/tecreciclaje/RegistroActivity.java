package com.example.tecreciclaje;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tecreciclaje.Model.NfcBottomSheetDialogFragment;
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

public class RegistroActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "RegistroActivity";

    EditText nombreEditText, apellidoEditText, numControlEditText, carreraEditText, emailEditText, passwordEditText;
    Switch roleSwitch;
    RelativeLayout btnSiguiente, btnGoogle;
    TextView loginRedirectText;

    // Firebase Auth
    private FirebaseAuth mAuth;

    // Google Sign In
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        initializeViews();
        setupFirebaseAuth();
        setupGoogleSignIn();
        setupClickListeners();
    }

    private void initializeViews() {
        nombreEditText = findViewById(R.id.nombreEditText);
        apellidoEditText = findViewById(R.id.apellidoEditText);
        numControlEditText = findViewById(R.id.numControlEditText);
        carreraEditText = findViewById(R.id.carreraEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        roleSwitch = findViewById(R.id.roleSwitch);
        btnSiguiente = findViewById(R.id.btnSiguiente);
        btnGoogle = findViewById(R.id.btnGoogle);
        loginRedirectText = findViewById(R.id.loginRedirectText);

        // Cambiar el ícono del botón de Google al ícono correcto
        ImageView googleIcon = findViewById(R.id.googleIcon);
        googleIcon.setImageResource(R.drawable.ic_google);
    }

    private void setupFirebaseAuth() {
        mAuth = FirebaseAuth.getInstance();
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupClickListeners() {
        btnSiguiente.setOnClickListener(v -> registerWithEmail());

        btnGoogle.setOnClickListener(v -> signInWithGoogle());

        loginRedirectText.setOnClickListener(v ->
                startActivity(new Intent(RegistroActivity.this, LoginActivity.class))
        );
    }

    private void registerWithEmail() {
        String nombre = nombreEditText.getText().toString().trim();
        String apellido = apellidoEditText.getText().toString().trim();
        String numControl = numControlEditText.getText().toString().trim();
        String carrera = carreraEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String role = roleSwitch.isChecked() ? "user" : "admin";
        String perfil = "https://firebasestorage.googleapis.com/v0/b/resiclaje-39011.firebasestorage.app/o/user.png?alt=media&token=745bbda5-2229-4d42-af3f-16dd6ec8db23";

        if (nombre.isEmpty() || apellido.isEmpty() || numControl.isEmpty() ||
                carrera.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        NfcBottomSheetDialogFragment sheet = NfcBottomSheetDialogFragment.newInstance(
                nombre, apellido, numControl, carrera, email, password, role, perfil, false // false = no es Google Sign-In
        );
        sheet.show(getSupportFragmentManager(), "NFC_SHEET");
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void handleGoogleSignInSuccess(FirebaseUser user) {
        if (user != null) {
            String email = user.getEmail();
            String displayName = user.getDisplayName();
            String photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() :
                    "https://firebasestorage.googleapis.com/v0/b/resiclaje-39011.firebasestorage.app/o/user.png?alt=media&token=745bbda5-2229-4d42-af3f-16dd6ec8db23";

            // Dividir el nombre completo en nombre y apellido
            String[] nameParts = displayName != null ? displayName.split(" ", 2) : new String[]{"", ""};
            String nombre = nameParts.length > 0 ? nameParts[0] : "";
            String apellido = nameParts.length > 1 ? nameParts[1] : "";
            String role = "user"; // Por defecto

            Toast.makeText(this, "Registro con el Google", Toast.LENGTH_SHORT).show();

            // Lanzar directamente el NFC BottomSheet con los datos de Google
            // Indicar que es registro con Google (password vacío)
            NfcBottomSheetDialogFragment sheet = NfcBottomSheetDialogFragment.newInstance(
                    nombre, apellido, "", "", email, "", role, photoUrl, true // true = es Google Sign-In
            );
            sheet.show(getSupportFragmentManager(), "NFC_SHEET");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Falló el registro con Google: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        handleGoogleSignInSuccess(user);
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(RegistroActivity.this, "Falló la autenticación con Google.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}