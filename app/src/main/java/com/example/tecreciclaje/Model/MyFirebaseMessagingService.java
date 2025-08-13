package com.example.tecreciclaje.Model;

import android.util.Log;
import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Nuevo token FCM generado: " + token);

        // Guardar el token en la base de datos si hay usuario autenticado
        saveTokenToDatabase(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Mensaje recibido de: " + remoteMessage.getFrom());

        // Aquí puedes manejar las notificaciones cuando la app esté en foreground
        // Por ejemplo, mostrar un diálogo o actualizar la UI
    }

    private void saveTokenToDatabase(String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseDatabase.getInstance()
                    .getReference("usuarios")
                    .child(user.getUid())
                    .child("fcm_token")
                    .setValue(token)
                    .addOnSuccessListener(aVoid ->
                            Log.d(TAG, "Token FCM guardado exitosamente"))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Error al guardar token FCM: " + e.getMessage()));
        } else {
            Log.w(TAG, "No hay usuario autenticado, token no guardado");
        }
    }

    // Método estático para actualizar token manualmente
    public static void updateTokenForCurrentUser() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Error al obtener token FCM", task.getException());
                        return;
                    }

                    // Obtener nuevo token
                    String token = task.getResult();
                    Log.d(TAG, "Token FCM obtenido: " + token);

                    // Guardar en base de datos
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        FirebaseDatabase.getInstance()
                                .getReference("usuarios")
                                .child(user.getUid())
                                .child("fcm_token")
                                .setValue(token)
                                .addOnSuccessListener(aVoid ->
                                        Log.d(TAG, "Token FCM actualizado exitosamente"))
                                .addOnFailureListener(e ->
                                        Log.e(TAG, "Error al actualizar token FCM: " + e.getMessage()));
                    }
                });
    }
}