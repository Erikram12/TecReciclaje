package com.example.tecreciclaje.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

object FCMTokenManager {
    private const val TAG = "FCMTokenManager"

    /**
     * Obtiene y actualiza el token FCM para el usuario actual
     */
    fun updateTokenForCurrentUser() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w(TAG, "No hay usuario autenticado para actualizar token")
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener(OnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val token = task.result
                    Log.d(TAG, "Token FCM obtenido: $token")
                    updateTokenInDatabase(currentUser.uid, token)
                } else {
                    Log.e(TAG, "Error obteniendo token FCM", task.exception)
                }
            })
    }

    /**
     * Actualiza el token FCM en la base de datos
     */
    fun updateTokenInDatabase(uid: String, token: String) {
        if (uid.isEmpty()) {
            Log.w(TAG, "UID inválido para actualizar token")
            return
        }

        val userRef = FirebaseDatabase.getInstance()
            .getReference("usuarios")
            .child(uid)

        // Actualizar ambos campos posibles para compatibilidad
        userRef.child("fcm_token").setValue(token)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Token FCM actualizado exitosamente en fcm_token")
                } else {
                    Log.e(TAG, "Error actualizando fcm_token", task.exception)
                }
            }

        userRef.child("tokenFCM").setValue(token)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Token FCM actualizado exitosamente en tokenFCM")
                } else {
                    Log.e(TAG, "Error actualizando tokenFCM", task.exception)
                }
            }
    }

    /**
     * Limpia el token FCM del usuario (al cerrar sesión)
     */
    fun clearTokenForCurrentUser() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w(TAG, "No hay usuario autenticado para limpiar token")
            return
        }

        val uid = currentUser.uid
        val userRef = FirebaseDatabase.getInstance()
            .getReference("usuarios")
            .child(uid)

        // Limpiar ambos campos
        userRef.child("fcm_token").removeValue()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Token FCM limpiado exitosamente de fcm_token")
                } else {
                    Log.e(TAG, "Error limpiando fcm_token", task.exception)
                }
            }

        userRef.child("tokenFCM").removeValue()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Token FCM limpiado exitosamente de tokenFCM")
                } else {
                    Log.e(TAG, "Error limpiando tokenFCM", task.exception)
                }
            }
    }

    /**
     * Verifica si el token actual es válido
     */
    fun validateCurrentToken() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w(TAG, "No hay usuario autenticado para validar token")
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val currentToken = task.result
                    Log.d(TAG, "Token actual válido: $currentToken")
                    
                    // Verificar si el token en la base de datos coincide
                    checkTokenInDatabase(currentUser.uid, currentToken)
                } else {
                    Log.e(TAG, "Error validando token actual", task.exception)
                }
            }
    }

    /**
     * Verifica si el token en la base de datos coincide con el actual
     */
    private fun checkTokenInDatabase(uid: String, currentToken: String) {
        val userRef = FirebaseDatabase.getInstance()
            .getReference("usuarios")
            .child(uid)

        userRef.child("fcm_token").get().addOnCompleteListener { task ->
            if (task.isSuccessful && task.result.exists()) {
                val storedToken = task.result.getValue(String::class.java)
                if (currentToken != storedToken) {
                    Log.d(TAG, "Token en BD no coincide, actualizando...")
                    updateTokenInDatabase(uid, currentToken)
                } else {
                    Log.d(TAG, "Token en BD coincide con el actual")
                }
            } else {
                Log.d(TAG, "No hay token almacenado, guardando actual...")
                updateTokenInDatabase(uid, currentToken)
            }
        }
    }

    /**
     * Fuerza la regeneración del token FCM
     */
    fun forceTokenRefresh() {
        FirebaseMessaging.getInstance().deleteToken()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Token eliminado exitosamente")
                    // Después de eliminar, obtener uno nuevo
                    updateTokenForCurrentUser()
                } else {
                    Log.e(TAG, "Error eliminando token", task.exception)
                }
            }
    }

    /**
     * Verifica y usa tokens FCM pendientes después de la autenticación
     */
    fun checkAndUsePendingToken(context: Context) {
        val pendingToken = context.getSharedPreferences("FCM_PREFS", Context.MODE_PRIVATE)
            .getString("pending_token", null)
        
        if (pendingToken != null) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                updateTokenInDatabase(currentUser.uid, pendingToken)
                // Limpiar el token pendiente
                context.getSharedPreferences("FCM_PREFS", Context.MODE_PRIVATE)
                    .edit()
                    .remove("pending_token")
                    .apply()
                Log.d(TAG, "Token FCM pendiente usado y limpiado")
            }
        }
    }
}
