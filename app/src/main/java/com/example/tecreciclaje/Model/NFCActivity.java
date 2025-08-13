package com.example.tecreciclaje.Model;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.tecreciclaje.MainActivity;
import com.example.tecreciclaje.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class NFCActivity extends AppCompatActivity {

    NfcAdapter nfcAdapter;
    String nombre, apellido, numControl, carrera, email, password, role, perfil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);

        nombre = getIntent().getStringExtra("nombre");
        apellido = getIntent().getStringExtra("apellido");
        numControl = getIntent().getStringExtra("numControl");
        carrera = getIntent().getStringExtra("carrera");
        email = getIntent().getStringExtra("email");
        password = getIntent().getStringExtra("password");
        role = getIntent().getStringExtra("role");
        perfil = getIntent().getStringExtra("perfil");

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            Toast.makeText(this, "Este dispositivo no soporta NFC", Toast.LENGTH_LONG).show();
            finish();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_MUTABLE);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter[] filters = new IntentFilter[]{tagDetected};
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, filters, null);
    }
    @Override
    protected void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String nfcUid = bytesToHex(tag.getId());
            registrarUsuario(nombre, apellido, numControl, carrera, email, password, role, perfil, nfcUid);
        }
    }
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
    private void registrarUsuario(String nombre, String apellido, String numControl, String carrera, String email, String password, String role, String perfil, String nfcUid) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String authUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                        Usuario usuario = new Usuario(nombre, apellido, numControl, carrera, email, role, perfil, nfcUid);

                        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
                        db.child("usuarios").child(authUid).setValue(usuario);
                        db.child("nfc_index").child(nfcUid).setValue(authUid);

                        Toast.makeText(this, "Registro completo con NFC", Toast.LENGTH_SHORT).show();
                        // Redirige a MainActivity
                        Intent intent = new Intent(NFCActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}