package com.example.tecreciclaje.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.tecreciclaje.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import java.io.File

object SessionManager {
    private const val TAG = "SessionManager"
    private const val PREF_NAME = "TecReciclajePrefs"
    private const val KEY_FCM_PREFS = "FCM_PREFS"

    /**
     * Limpia completamente la sesión del usuario
     * Incluye: Firebase Auth, Google Sign-In, FCM Token, SharedPreferences
     */
    fun clearCompleteSession(context: Context) {
        Log.d(TAG, "Iniciando limpieza completa de sesión")
        
        try {
            // 1. Limpiar token FCM
            FCMTokenManager.clearTokenForCurrentUser()
            
            // 2. Cerrar sesión de Firebase Auth
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser != null) {
                Log.d(TAG, "Cerrando sesión de Firebase Auth")
                auth.signOut()
            }
            
            // 3. Cerrar sesión de Google Sign-In
            clearGoogleSignIn(context)
            
            // 4. Limpiar SharedPreferences
            clearSharedPreferences(context)
            
            // 5. Limpiar caché de la aplicación
            clearAppCache(context)
            
            Log.d(TAG, "Limpieza completa de sesión finalizada")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la limpieza de sesión", e)
        }
    }

    /**
     * Cierra la sesión de Google Sign-In
     */
    private fun clearGoogleSignIn(context: Context) {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            val googleSignInClient = GoogleSignIn.getClient(context, gso)
            
            // Primero revocar acceso (más agresivo)
            googleSignInClient.revokeAccess().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Acceso de Google revocado exitosamente")
                } else {
                    Log.w(TAG, "Error revocando acceso de Google", task.exception)
                }
                
                // Luego cerrar sesión
                googleSignInClient.signOut().addOnCompleteListener { signOutTask ->
                    if (signOutTask.isSuccessful) {
                        Log.d(TAG, "Sesión de Google Sign-In cerrada exitosamente")
                    } else {
                        Log.w(TAG, "Error cerrando sesión de Google Sign-In", signOutTask.exception)
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando Google Sign-In para limpieza", e)
        }
    }

    /**
     * Limpia todas las SharedPreferences de la aplicación
     */
    private fun clearSharedPreferences(context: Context) {
        try {
            // Limpiar preferencias principales
            val mainPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            mainPrefs.edit().clear().apply()
            
            // Limpiar preferencias de FCM
            val fcmPrefs = context.getSharedPreferences(KEY_FCM_PREFS, Context.MODE_PRIVATE)
            fcmPrefs.edit().clear().apply()
            
            // Limpiar otras posibles preferencias
            val defaultPrefs = context.getSharedPreferences("default", Context.MODE_PRIVATE)
            defaultPrefs.edit().clear().apply()
            
            Log.d(TAG, "SharedPreferences limpiadas exitosamente")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando SharedPreferences", e)
        }
    }

    /**
     * Limpia la caché de la aplicación
     */
    private fun clearAppCache(context: Context) {
        try {
            // Limpiar caché interna
            val cacheDir = context.cacheDir
            if (cacheDir != null && cacheDir.exists()) {
                deleteDir(cacheDir)
                Log.d(TAG, "Caché interna limpiada")
            }
            
            // Limpiar caché externa si existe
            val externalCacheDir = context.externalCacheDir
            if (externalCacheDir != null && externalCacheDir.exists()) {
                deleteDir(externalCacheDir)
                Log.d(TAG, "Caché externa limpiada")
            }
            
            // Limpiar archivos temporales
            val tempDir = File(context.cacheDir, "temp")
            if (tempDir.exists()) {
                deleteDir(tempDir)
                Log.d(TAG, "Archivos temporales limpiados")
            }
            
            Log.d(TAG, "Caché de aplicación limpiada exitosamente")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando caché de aplicación", e)
        }
    }

    /**
     * Elimina recursivamente un directorio y su contenido
     */
    private fun deleteDir(dir: File): Boolean {
        return if (dir.exists()) {
            val files = dir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isDirectory) {
                        deleteDir(file)
                    } else {
                        file.delete()
                    }
                }
            }
            dir.delete()
        } else {
            false
        }
    }

    /**
     * Verifica si hay una sesión activa
     */
    fun isSessionActive(): Boolean {
        val auth = FirebaseAuth.getInstance()
        return auth.currentUser != null
    }

    /**
     * Obtiene el UID del usuario actual
     */
    fun getCurrentUserId(): String? {
        val auth = FirebaseAuth.getInstance()
        return auth.currentUser?.uid
    }
}
