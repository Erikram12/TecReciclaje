package com.example.tecreciclaje.utils

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.tecreciclaje.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/**
 * Clase utilitaria para manejar tutoriales y guías de usuario
 * Gestiona la visualización de tutoriales basados en el estado del usuario
 */
object TutorialManager {
    
    private const val PREF_NAME = "TutorialPreferences"
    private const val KEY_MIS_VALES_TUTORIAL = "mis_vales_tutorial_shown"
    private const val KEY_HISTORIAL_TUTORIAL = "historial_tutorial_shown"
    private const val KEY_PERFIL_TUTORIAL = "perfil_tutorial_shown"
    private const val KEY_PRODUCTO_TUTORIAL = "producto_tutorial_shown"
    private const val KEY_USER_FIRST_TIME = "user_first_time"
    private const val KEY_TUTORIAL_DISABLED = "tutorial_disabled"
    
    /**
     * Verifica si el usuario es nuevo (primera vez que usa la app)
     */
    fun isUserFirstTime(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_USER_FIRST_TIME, true)
    }
    
    /**
     * Marca al usuario como no nuevo (ya ha usado la app)
     */
    fun markUserNotFirstTime(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_USER_FIRST_TIME, false).apply()
    }
    
    /**
     * Verifica si el tutorial de Mis Vales ya se mostró para el usuario actual
     */
    fun isMisValesTutorialShown(context: Context): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return false
        
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = "${KEY_MIS_VALES_TUTORIAL}_${currentUser.uid}"
        return prefs.getBoolean(key, false)
    }
    
    /**
     * Marca el tutorial de Mis Vales como mostrado para el usuario actual
     */
    fun markMisValesTutorialShown(context: Context) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return
        
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = "${KEY_MIS_VALES_TUTORIAL}_${currentUser.uid}"
        prefs.edit().putBoolean(key, true).apply()
    }
    
    /**
     * Verifica si el tutorial de Historial ya se mostró para el usuario actual
     */
    fun isHistorialTutorialShown(context: Context): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return false
        
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = "${KEY_HISTORIAL_TUTORIAL}_${currentUser.uid}"
        return prefs.getBoolean(key, false)
    }
    
    /**
     * Marca el tutorial de Historial como mostrado para el usuario actual
     */
    fun markHistorialTutorialShown(context: Context) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return
        
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = "${KEY_HISTORIAL_TUTORIAL}_${currentUser.uid}"
        prefs.edit().putBoolean(key, true).apply()
    }
    
    /**
     * Verifica si el tutorial de Perfil ya se mostró para el usuario actual
     */
    fun isPerfilTutorialShown(context: Context): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return false
        
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = "${KEY_PERFIL_TUTORIAL}_${currentUser.uid}"
        return prefs.getBoolean(key, false)
    }
    
    /**
     * Marca el tutorial de Perfil como mostrado para el usuario actual
     */
    fun markPerfilTutorialShown(context: Context) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return
        
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = "${KEY_PERFIL_TUTORIAL}_${currentUser.uid}"
        prefs.edit().putBoolean(key, true).apply()
    }
    
    /**
     * Verifica si el tutorial de Producto ya se mostró para el usuario actual
     */
    fun isProductoTutorialShown(context: Context): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return false
        
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = "${KEY_PRODUCTO_TUTORIAL}_${currentUser.uid}"
        return prefs.getBoolean(key, false)
    }
    
    /**
     * Marca el tutorial de Producto como mostrado para el usuario actual
     */
    fun markProductoTutorialShown(context: Context) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return
        
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = "${KEY_PRODUCTO_TUTORIAL}_${currentUser.uid}"
        prefs.edit().putBoolean(key, true).apply()
    }
    
    /**
     * Verifica si los tutoriales están deshabilitados
     */
    fun areTutorialsDisabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_TUTORIAL_DISABLED, false)
    }
    
    /**
     * Deshabilita todos los tutoriales
     */
    fun disableTutorials(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_TUTORIAL_DISABLED, true).apply()
    }
    
    /**
     * Habilita todos los tutoriales
     */
    fun enableTutorials(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_TUTORIAL_DISABLED, false).apply()
    }
    
    /**
     * Muestra el tutorial de Mis Vales si es necesario
     */
    fun showMisValesTutorialIfNeeded(context: Context) {
        // Verificar si el usuario está logueado
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return
        
        // Verificar si los tutoriales están deshabilitados
        if (areTutorialsDisabled(context)) return
        
        // Verificar si ya se mostró el tutorial
        if (isMisValesTutorialShown(context)) return
        
        // Mostrar el tutorial
        showMisValesTutorial(context)
    }
    
    /**
     * Muestra el tutorial de Mis Vales
     */
    fun showMisValesTutorial(context: Context) {
        // Crear el diálogo personalizado
        val dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)
            setContentView(R.layout.dialog_tutorial_mis_vales)
        }
        
        // Quitar la sombra/overlay oscuro del diálogo
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Configurar botón
        val btnEntendido = dialog.findViewById<Button>(R.id.btnEntendido)
        
        btnEntendido.setOnClickListener {
            // Marcar tutorial como mostrado
            markMisValesTutorialShown(context)
            dialog.dismiss()
        }
        
        // Mostrar el diálogo
        dialog.show()
    }
    
    /**
     * Muestra el tutorial de Historial si es necesario
     */
    fun showHistorialTutorialIfNeeded(context: Context) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return
        
        if (areTutorialsDisabled(context)) return
        if (isHistorialTutorialShown(context)) return
        
        showHistorialTutorial(context)
    }
    
    /**
     * Muestra el tutorial de Historial
     */
    fun showHistorialTutorial(context: Context) {
        val dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)
            setContentView(R.layout.dialog_tutorial_historial)
        }
        
        // Quitar la sombra/overlay oscuro del diálogo
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val btnEntendido = dialog.findViewById<Button>(R.id.btnEntendido)
        
        btnEntendido.setOnClickListener {
            markHistorialTutorialShown(context)
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    /**
     * Muestra el tutorial de Perfil si es necesario
     */
    fun showPerfilTutorialIfNeeded(context: Context) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return
        
        if (areTutorialsDisabled(context)) return
        if (isPerfilTutorialShown(context)) return
        
        showPerfilTutorial(context)
    }
    
    /**
     * Muestra el tutorial de Perfil
     */
    fun showPerfilTutorial(context: Context) {
        val dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)
            setContentView(R.layout.dialog_tutorial_perfil)
        }
        
        // Quitar la sombra/overlay oscuro del diálogo
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val btnEntendido = dialog.findViewById<Button>(R.id.btnEntendido)
        
        btnEntendido.setOnClickListener {
            markPerfilTutorialShown(context)
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    /**
     * Muestra el tutorial de Producto si es necesario
     */
    fun showProductoTutorialIfNeeded(context: Context, productName: String, imageResource: Int) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return
        
        if (areTutorialsDisabled(context)) return
        if (isProductoTutorialShown(context)) return
        
        showProductoTutorial(context, productName, imageResource)
    }
    
    /**
     * Muestra el tutorial de Producto
     */
    fun showProductoTutorial(context: Context, productName: String, imageResource: Int) {
        val dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)
            setContentView(R.layout.dialog_tutorial_producto)
        }
        
        // Quitar la sombra/overlay oscuro del diálogo
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Configurar contenido del tutorial
        val tituloTutorial = dialog.findViewById<TextView>(R.id.tituloTutorial)
        val iconoProducto = dialog.findViewById<ImageView>(R.id.iconoProducto)
        val btnEntendido = dialog.findViewById<Button>(R.id.btnEntendido)
        
        tituloTutorial?.text = "¡Bienvenido a $productName!"
        iconoProducto?.setImageResource(imageResource)
        
        btnEntendido.setOnClickListener {
            markProductoTutorialShown(context)
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    /**
     * Muestra un diálogo de bienvenida para usuarios nuevos
     */
    fun showWelcomeDialog(context: Context) {
        val dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)
            setContentView(R.layout.dialog_tutorial_producto)
        }
        
        // Quitar la sombra/overlay oscuro del diálogo
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val btnComenzar = dialog.findViewById<Button>(R.id.btnEntendido)
        
        btnComenzar.setOnClickListener {
            markUserNotFirstTime(context)
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    /**
     * Muestra un diálogo de configuración de tutoriales
     */
    fun showTutorialSettingsDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Configuración de Tutoriales")
            .setMessage("¿Deseas habilitar los tutoriales de la aplicación?")
            .setPositiveButton("Habilitar") { _, _ ->
                enableTutorials(context)
            }
            .setNegativeButton("Deshabilitar") { _, _ ->
                disableTutorials(context)
            }
            .show()
    }
    
    /**
     * Resetea todos los tutoriales (para testing)
     */
    fun resetAllTutorials(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
