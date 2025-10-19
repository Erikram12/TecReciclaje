package com.example.tecreciclaje.utils

import android.content.Context
import android.widget.ImageView

/**
 * EJEMPLO DE USO: Fluent System Icons en TecReciclaje
 * 
 * Este archivo muestra cómo usar los Fluent System Icons de Microsoft
 * en diferentes partes de la aplicación.
 */
object FluentIconsExample {
    
    /**
     * Ejemplo 1: Configurar iconos en botones de navegación
     */
    fun configurarIconosNavegacion(context: Context, backButton: ImageView, homeButton: ImageView) {
        // Botón de regreso
        val backIconId = FluentIconsHelper.getFluentIconResId(context, FluentIconsHelper.Icons.ARROW_LEFT)
        if (backIconId != 0) {
            backButton.setImageResource(backIconId)
        }
        
        // Botón de inicio
        val homeIconId = FluentIconsHelper.getFluentIconResId(context, FluentIconsHelper.Icons.HOME)
        if (homeIconId != 0) {
            homeButton.setImageResource(homeIconId)
        }
    }
    
    /**
     * Ejemplo 2: Configurar iconos de productos según el tipo
     */
    fun configurarIconoProducto(context: Context, imageView: ImageView, tipoProducto: String) {
        val iconName = when (tipoProducto.lowercase()) {
            "café", "cafe", "taza" -> FluentIconsHelper.Icons.COFFEE
            "comida", "desayuno", "almuerzo" -> FluentIconsHelper.Icons.FOOD
            "refresco", "bebida", "jugo" -> FluentIconsHelper.Icons.DRINK
            else -> FluentIconsHelper.Icons.FOOD
        }
        
        val iconId = FluentIconsHelper.getFluentIconResId(context, iconName)
        if (iconId != 0) {
            imageView.setImageResource(iconId)
        }
    }
    
    /**
     * Ejemplo 3: Configurar iconos de estado
     */
    fun configurarIconoEstado(context: Context, imageView: ImageView, estado: String) {
        val iconName = when (estado.lowercase()) {
            "válido", "disponible" -> FluentIconsHelper.Icons.SUCCESS
            "expirado", "caducado" -> FluentIconsHelper.Icons.WARNING
            "canjeado" -> FluentIconsHelper.Icons.CHECK
            "error" -> FluentIconsHelper.Icons.ERROR
            else -> FluentIconsHelper.Icons.PERSON
        }
        
        val iconId = FluentIconsHelper.getFluentIconResId(context, iconName)
        if (iconId != 0) {
            imageView.setImageResource(iconId)
        }
    }
    
    /**
     * Ejemplo 4: Configurar iconos de funcionalidad
     */
    fun configurarIconosFuncionalidad(context: Context, searchIcon: ImageView, profileIcon: ImageView, settingsIcon: ImageView) {
        // Icono de búsqueda
        val searchIconId = FluentIconsHelper.getFluentIconResId(context, FluentIconsHelper.Icons.SEARCH)
        if (searchIconId != 0) {
            searchIcon.setImageResource(searchIconId)
        }
        
        // Icono de perfil
        val profileIconId = FluentIconsHelper.getFluentIconResId(context, FluentIconsHelper.Icons.PERSON)
        if (profileIconId != 0) {
            profileIcon.setImageResource(profileIconId)
        }
        
        // Icono de configuración
        val settingsIconId = FluentIconsHelper.getFluentIconResId(context, FluentIconsHelper.Icons.SETTINGS)
        if (settingsIconId != 0) {
            settingsIcon.setImageResource(settingsIconId)
        }
    }
    
    /**
     * Ejemplo 5: Configurar iconos de autenticación
     */
    fun configurarIconosAutenticacion(context: Context, emailIcon: ImageView, lockIcon: ImageView, visibilityIcon: ImageView) {
        // Icono de email
        val emailIconId = FluentIconsHelper.getFluentIconResId(context, FluentIconsHelper.Icons.EMAIL)
        if (emailIconId != 0) {
            emailIcon.setImageResource(emailIconId)
        }
        
        // Icono de contraseña
        val lockIconId = FluentIconsHelper.getFluentIconResId(context, FluentIconsHelper.Icons.LOCK)
        if (lockIconId != 0) {
            lockIcon.setImageResource(lockIconId)
        }
        
        // Icono de visibilidad
        val visibilityIconId = FluentIconsHelper.getFluentIconResId(context, FluentIconsHelper.Icons.VISIBILITY)
        if (visibilityIconId != 0) {
            visibilityIcon.setImageResource(visibilityIconId)
        }
    }
    
    /**
     * Ejemplo 6: Configurar iconos de reciclaje
     */
    fun configurarIconosReciclaje(context: Context, recycleIcon: ImageView, containerIcon: ImageView) {
        // Icono de reciclaje
        val recycleIconId = FluentIconsHelper.getFluentIconResId(context, FluentIconsHelper.Icons.RECYCLE)
        if (recycleIconId != 0) {
            recycleIcon.setImageResource(recycleIconId)
        }
        
        // Icono de contenedor (usar reciclaje como fallback)
        val containerIconId = FluentIconsHelper.getFluentIconResId(context, FluentIconsHelper.Icons.RECYCLE)
        if (containerIconId != 0) {
            containerIcon.setImageResource(containerIconId)
        }
    }
    
    /**
     * Ejemplo 7: Configurar iconos de estadísticas
     */
    fun configurarIconosEstadisticas(context: Context, chartIcon: ImageView, historyIcon: ImageView, analyticsIcon: ImageView) {
        // Icono de gráficos
        val chartIconId = FluentIconsHelper.getFluentIconResId(context, FluentIconsHelper.Icons.CHART)
        if (chartIconId != 0) {
            chartIcon.setImageResource(chartIconId)
        }
        
        // Icono de historial
        val historyIconId = FluentIconsHelper.getFluentIconResId(context, FluentIconsHelper.Icons.HISTORY)
        if (historyIconId != 0) {
            historyIcon.setImageResource(historyIconId)
        }
        
        // Icono de análisis
        val analyticsIconId = FluentIconsHelper.getFluentIconResId(context, FluentIconsHelper.Icons.ANALYTICS)
        if (analyticsIconId != 0) {
            analyticsIcon.setImageResource(analyticsIconId)
        }
    }
    
    /**
     * Ejemplo 8: Configurar iconos de tecnología
     */
    fun configurarIconosTecnologia(context: Context, nfcIcon: ImageView, qrIcon: ImageView, powerIcon: ImageView) {
        // Icono de NFC
        val nfcIconId = FluentIconsHelper.getFluentIconResId(context, FluentIconsHelper.Icons.NFC)
        if (nfcIconId != 0) {
            nfcIcon.setImageResource(nfcIconId)
        }
        
        // Icono de QR
        val qrIconId = FluentIconsHelper.getFluentIconResId(context, FluentIconsHelper.Icons.QR_CODE)
        if (qrIconId != 0) {
            qrIcon.setImageResource(qrIconId)
        }
        
        // Icono de energía
        val powerIconId = FluentIconsHelper.getFluentIconResId(context, FluentIconsHelper.Icons.POWER)
        if (powerIconId != 0) {
            powerIcon.setImageResource(powerIconId)
        }
    }
}
