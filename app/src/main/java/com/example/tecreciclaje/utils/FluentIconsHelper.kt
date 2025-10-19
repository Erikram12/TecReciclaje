package com.example.tecreciclaje.utils

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

/**
 * Helper para usar Phosphor Icons (alternativa moderna a Fluent System Icons)
 * Mapea los iconos actuales de Material Design a Phosphor Icons
 */
object FluentIconsHelper {
    
    // Mapeo de iconos Material Design a Phosphor Icons
    object Icons {
        // Navegación
        const val ARROW_LEFT = "ic_arrow_left"
        const val ARROW_RIGHT = "ic_arrow_right"
        const val HOME = "ic_house"
        const val BACK = "ic_arrow_left"
        
        // Usuario y perfil
        const val PERSON = "ic_user"
        const val PERSON_ADD = "ic_user_plus"
        const val EDIT = "ic_pencil"
        const val SETTINGS = "ic_gear"
        
        // Autenticación
        const val LOCK = "ic_lock"
        const val LOCK_OPEN = "ic_lock_open"
        const val VISIBILITY = "ic_eye"
        const val VISIBILITY_OFF = "ic_eye_slash"
        
        // Comunicación
        const val EMAIL = "ic_envelope"
        const val NOTIFICATIONS = "ic_bell"
        const val SEARCH = "ic_magnifying_glass"
        
        // Reciclaje y medio ambiente
        const val RECYCLE = "ic_recycle"
        const val LEAF = "ic_leaf"
        const val TREE = "ic_tree"
        
        // Productos y comida
        const val COFFEE = "ic_coffee"
        const val FOOD = "ic_fork_knife"
        const val DRINK = "ic_wine_bottle"
        
        // Estadísticas y datos
        const val CHART = "ic_chart_bar"
        const val ANALYTICS = "ic_chart_line"
        const val HISTORY = "ic_clock"
        
        // Tecnología
        const val NFC = "ic_nfc"
        const val QR_CODE = "ic_qr_code"
        const val POWER = "ic_power"
        
        // Educación
        const val SCHOOL = "ic_graduation_cap"
        const val BOOK = "ic_book"
        const val GRADUATION = "ic_graduation_cap"
        
        // Tiempo y calendario
        const val CALENDAR = "ic_calendar"
        const val CLOCK = "ic_clock"
        const val TIMER = "ic_timer"
        
        // Estado y validación
        const val CHECK = "ic_check"
        const val WARNING = "ic_warning"
        const val ERROR = "ic_x_circle"
        const val SUCCESS = "ic_check_circle"
    }
    
    /**
     * Obtiene un Drawable de Phosphor Icons por nombre
     */
    fun getFluentIcon(context: Context, iconName: String): Drawable? {
        return try {
            // Obtener el ID del recurso por nombre
            val resourceId = context.resources.getIdentifier(
                iconName, 
                "drawable", 
                context.packageName
            )
            
            if (resourceId != 0) {
                ContextCompat.getDrawable(context, resourceId)
            } else {
                // Fallback a icono por defecto si no se encuentra
                ContextCompat.getDrawable(context, android.R.drawable.ic_menu_help)
            }
        } catch (e: Exception) {
            // Fallback en caso de error
            ContextCompat.getDrawable(context, android.R.drawable.ic_menu_help)
        }
    }
    
    /**
     * Obtiene el ID del recurso de Phosphor Icons
     */
    @DrawableRes
    fun getFluentIconResId(context: Context, iconName: String): Int {
        return context.resources.getIdentifier(
            iconName, 
            "drawable", 
            context.packageName
        )
    }
    
    /**
     * Mapea iconos Material Design a Phosphor Icons
     */
    fun mapMaterialToFluent(materialIconName: String): String {
        return when (materialIconName) {
            // Navegación
            "baseline_arrow_back_24" -> Icons.ARROW_LEFT
            "baseline_home_24" -> Icons.HOME
            
            // Usuario
            "baseline_person_24" -> Icons.PERSON
            "baseline_person_add_24" -> Icons.PERSON_ADD
            "baseline_edit_24" -> Icons.EDIT
            
            // Autenticación
            "baseline_lock_24" -> Icons.LOCK
            "ic_visibility" -> Icons.VISIBILITY
            "ic_visibility_off" -> Icons.VISIBILITY_OFF
            
            // Comunicación
            "baseline_email_24" -> Icons.EMAIL
            "baseline_notifications_24" -> Icons.NOTIFICATIONS
            "baseline_search_24" -> Icons.SEARCH
            
            // Reciclaje
            "resicle" -> Icons.RECYCLE
            "ic_contenedor" -> Icons.RECYCLE
            
            // Productos
            "cafesito1" -> Icons.COFFEE
            "comida" -> Icons.FOOD
            "refresco" -> Icons.DRINK
            "desayuno" -> Icons.FOOD
            
            // Estadísticas
            "baseline_area_chart_24" -> Icons.CHART
            "baseline_history_24" -> Icons.HISTORY
            
            // Tecnología
            "baseline_nfc_24" -> Icons.NFC
            "baseline_power_settings_new_24" -> Icons.POWER
            
            // Educación
            "baseline_school_24" -> Icons.SCHOOL
            
            // Tiempo
            "baseline_calendar_24", "baseline_calendar_today_24" -> Icons.CALENDAR
            
            // Estado
            "baseline_emergency_24" -> Icons.WARNING
            
            else -> Icons.PERSON // Fallback por defecto
        }
    }
}
