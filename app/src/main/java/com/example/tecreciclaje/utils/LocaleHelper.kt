package com.example.tecreciclaje.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "TecReciclajePrefs"
    private const val KEY_IDIOMA = "idioma_seleccionado"
    
    /**
     * Aplica el idioma guardado al contexto
     */
    fun attachBaseContext(context: Context): Context {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val idioma = prefs.getString(KEY_IDIOMA, "es") ?: "es"
        return setLocale(context, idioma)
    }
    
    /**
     * Establece el locale para el contexto
     * Para Zapoteco, usamos "zu" (zulú) como código de idioma válido
     * Los recursos están en values-zu (renombrado de values-zap)
     */
    fun setLocale(context: Context, idioma: String): Context {
        val locale = when (idioma) {
            "zap" -> {
                // Para zapoteco, usar "zu" (zulú) como código de idioma válido
                // Android cargará automáticamente los recursos de values-zu
                Locale("zu")
            }
            else -> Locale(idioma)
        }
        
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }
    
    /**
     * Obtiene el idioma actual guardado
     */
    fun getCurrentLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_IDIOMA, "es") ?: "es"
    }
    
    /**
     * Guarda el idioma seleccionado
     */
    fun saveLanguage(context: Context, idioma: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_IDIOMA, idioma).apply()
    }
    
    /**
     * Obtiene el contexto con el idioma aplicado para usar en getString()
     */
    fun getLocalizedContext(context: Context): Context {
        val idioma = getCurrentLanguage(context)
        return setLocale(context, idioma)
    }
}

