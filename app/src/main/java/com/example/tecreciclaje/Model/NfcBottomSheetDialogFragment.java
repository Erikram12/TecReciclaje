package com.example.tecreciclaje.Model;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tecreciclaje.MainActivity;
import com.example.tecreciclaje.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class NfcBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_NOMBRE = "nombre";
    private static final String ARG_APELLIDO = "apellido";
    private static final String ARG_NUM_CONTROL = "numControl";
    private static final String ARG_CARRERA = "carrera";
    private static final String ARG_EMAIL = "email";
    private static final String ARG_PASSWORD = "password";
    private static final String ARG_ROLE = "role";
    private static final String ARG_PERFIL = "perfil";
    private static final String ARG_IS_GOOGLE = "isGoogle";

    private String nombre, apellido, numControl, carrera, email, password, role, perfil;
    private boolean isGoogleSignIn;
    private NfcAdapter nfcAdapter;

    public static NfcBottomSheetDialogFragment newInstance(
            String nombre, String apellido, String numControl, String carrera,
            String email, String password, String role, String perfil, boolean isGoogleSignIn
    ) {
        NfcBottomSheetDialogFragment f = new NfcBottomSheetDialogFragment();
        Bundle b = new Bundle();
        b.putString(ARG_NOMBRE, nombre);
        b.putString(ARG_APELLIDO, apellido);
        b.putString(ARG_NUM_CONTROL, numControl);
        b.putString(ARG_CARRERA, carrera);
        b.putString(ARG_EMAIL, email);
        b.putString(ARG_PASSWORD, password);
        b.putString(ARG_ROLE, role);
        b.putString(ARG_PERFIL, perfil);
        b.putBoolean(ARG_IS_GOOGLE, isGoogleSignIn);
        f.setArguments(b);
        return f;
    }

    // Método de compatibilidad para llamadas existentes sin isGoogle
    public static NfcBottomSheetDialogFragment newInstance(
            String nombre, String apellido, String numControl, String carrera,
            String email, String password, String role, String perfil
    ) {
        return newInstance(nombre, apellido, numControl, carrera, email, password, role, perfil, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle a = getArguments();
        if (a != null) {
            nombre = a.getString(ARG_NOMBRE);
            apellido = a.getString(ARG_APELLIDO);
            numControl = a.getString(ARG_NUM_CONTROL);
            carrera = a.getString(ARG_CARRERA);
            email = a.getString(ARG_EMAIL);
            password = a.getString(ARG_PASSWORD);
            role = a.getString(ARG_ROLE);
            perfil = a.getString(ARG_PERFIL);
            isGoogleSignIn = a.getBoolean(ARG_IS_GOOGLE, false);
        }
        setCancelable(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_nfc, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        Activity act = requireActivity();
        nfcAdapter = NfcAdapter.getDefaultAdapter(act);
        if (nfcAdapter != null) {
            nfcAdapter.enableReaderMode(act, this::onTagDiscovered,
                    NfcAdapter.FLAG_READER_NFC_A
                            | NfcAdapter.FLAG_READER_NFC_B
                            | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                    null);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableReaderMode(requireActivity());
        }
    }

    private void onTagDiscovered(Tag tag) {
        String nfcUid = bytesToHex(tag.getId());
        requireActivity().runOnUiThread(() -> {
            if (isGoogleSignIn) {
                // Para registro con Google, no crear nueva cuenta
                registrarUsuarioGoogle(nombre, apellido, numControl, carrera, email, role, perfil, nfcUid);
            } else {
                // Para registro normal, crear cuenta con email y password
                registrarUsuario(nombre, apellido, numControl, carrera, email, password, role, perfil, nfcUid);
            }
        });
    }

    private void registrarUsuarioGoogle(String nombre, String apellido, String numControl, String carrera,
                                        String email, String role, String perfil, String nfcUid) {

        // Verificar que el usuario esté autenticado
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        String authUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Toast.makeText(getContext(), "Usuario autenticado con UID: " + authUid, Toast.LENGTH_SHORT).show();

        // Obtener token FCM y guardar datos
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(tokenTask -> {
                    if (!tokenTask.isSuccessful()) {
                        Toast.makeText(getContext(), "Error obteniendo token FCM", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String token = tokenTask.getResult();

                    // Guardar usuario en la base de datos
                    DatabaseReference db = FirebaseDatabase.getInstance().getReference();

                    Map<String, Object> usuarioMap = new HashMap<>();
                    usuarioMap.put("nombre", nombre);
                    usuarioMap.put("apellido", apellido);
                    usuarioMap.put("numControl", numControl.isEmpty() ? "Sin asignar" : numControl);
                    usuarioMap.put("carrera", carrera.isEmpty() ? "Sin asignar" : carrera);
                    usuarioMap.put("email", email);
                    usuarioMap.put("role", role);
                    usuarioMap.put("perfil", perfil);
                    usuarioMap.put("nfcUid", nfcUid);
                    usuarioMap.put("fcm_token", token);
                    usuarioMap.put("provider", "google");
                    usuarioMap.put("authUid", authUid); // Agregar el UID para debugging

                    db.child("usuarios").child(authUid).setValue(usuarioMap)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Datos guardados correctamente", Toast.LENGTH_SHORT).show();

                                // Guardar índice NFC
                                db.child("nfc_index").child(nfcUid).setValue(authUid)
                                        .addOnSuccessListener(bVoid -> {
                                            Toast.makeText(getContext(), "Registro completo con Google y NFC", Toast.LENGTH_SHORT).show();

                                            // Ir a MainActivity
                                            Intent intent = new Intent(requireContext(), MainActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);

                                            dismissAllowingStateLoss();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(getContext(), "Error guardando índice NFC: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Error guardando datos: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                });
    }

    private void registrarUsuario(String nombre, String apellido, String numControl, String carrera,
                                  String email, String password, String role, String perfil,
                                  String nfcUid) {

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String authUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                        // Obtener token FCM antes de guardar usuario
                        FirebaseMessaging.getInstance().getToken()
                                .addOnCompleteListener(tokenTask -> {
                                    if (!tokenTask.isSuccessful()) {
                                        Toast.makeText(getContext(), "Error obteniendo token FCM", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    String token = tokenTask.getResult();

                                    // Guardar usuario en la base de datos con token
                                    DatabaseReference db = FirebaseDatabase.getInstance().getReference();

                                    Map<String, Object> usuarioMap = new HashMap<>();
                                    usuarioMap.put("nombre", nombre);
                                    usuarioMap.put("apellido", apellido);
                                    usuarioMap.put("numControl", numControl);
                                    usuarioMap.put("carrera", carrera);
                                    usuarioMap.put("email", email);
                                    usuarioMap.put("role", role);
                                    usuarioMap.put("perfil", perfil);
                                    usuarioMap.put("nfcUid", nfcUid);
                                    usuarioMap.put("fcm_token", token);
                                    usuarioMap.put("provider", "email"); // Indicar que es registro con email

                                    db.child("usuarios").child(authUid).setValue(usuarioMap);
                                    db.child("nfc_index").child(nfcUid).setValue(authUid);

                                    Toast.makeText(getContext(), "Registro completo con email y NFC", Toast.LENGTH_SHORT).show();

                                    // Ir a MainActivity
                                    Intent intent = new Intent(requireContext(), MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);

                                    dismissAllowingStateLoss();
                                });

                    } else {
                        Toast.makeText(getContext(),
                                "Error registrando usuario: " + (task.getException() != null ? task.getException().getMessage() : ""),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }
}