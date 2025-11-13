package com.example.tecreciclaje.utils

import android.util.Log

/**
 * Clase de utilidad para el manejo centralizado de logs en la aplicación.
 * Todos los mensajes de depuración y error deben pasar por esta clase.
 */
object AppLogger {
    
    private const val TAG = "TecReciclaje"
    
    /**
     * Registra un mensaje de depuración.
     * Solo se muestra en LogCat, no en la interfaz de usuario.
     */
    fun d(message: String) {
        Log.d(TAG, message)
    }
    
    /**
     * Registra un mensaje de error.
     * Solo se muestra en LogCat, no en la interfaz de usuario.
     */
    fun e(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, message, throwable)
        } else {
            Log.e(TAG, message)
        }
    }
    
    /**
     * Registra un mensaje informativo.
     * Solo se muestra en LogCat, no en la interfaz de usuario.
     */
    fun i(message: String) {
        Log.i(TAG, message)
    }
    
    /**
     * Registra un mensaje de advertencia.
     * Solo se muestra en LogCat, no en la interfaz de usuario.
     */
    fun w(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(TAG, message, throwable)
        } else {
            Log.w(TAG, message)
        }
    }
}
