package com.example.tecreciclaje.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.database.PropertyName
import java.util.Date

data class Historial(
    @PropertyName("historial_id")
    var historial_id: String = "",
    
    @PropertyName("historial_userId")
    var historial_userId: String = "",
    
    @PropertyName("historial_cantidad")
    var historial_cantidad: Int = 0,
    
    @PropertyName("historial_fecha")
    var historial_fecha: Any? = null,
    
    @PropertyName("historial_tipo")
    var historial_tipo: String = "",
    
    @PropertyName("historial_producto")
    var historial_producto: String = "",
    
    @PropertyName("historial_descripcion")
    var historial_descripcion: String = "",
    
    @PropertyName("punto_id")
    var punto_id: String = "",
    
    @PropertyName("punto_userId")
    var punto_userId: String = "",
    
    @PropertyName("punto_cantidad")
    var punto_cantidad: Int = 0,
    
    @PropertyName("punto_fecha")
    var punto_fecha: Any? = null,
    
    @PropertyName("punto_tipo")
    var punto_tipo: String = "",
    
    @PropertyName("punto_descripcion")
    var punto_descripcion: String = "",
    
    @PropertyName("id")
    var id: String = "",
    
    @PropertyName("userId")
    var userId: String = "",
    
    @PropertyName("cantidad")
    var cantidad: Int = 0,
    
    @PropertyName("fecha")
    var fecha: Any? = null,
    
    @PropertyName("tipo")
    var tipo: String = "",
    
    @PropertyName("producto")
    var producto: String = "",
    
    @PropertyName("descripcion")
    var descripcion: String = ""
) {
    // Constructor vacío necesario para Firebase
    constructor() : this(
        historial_id = "",
        historial_userId = "",
        historial_cantidad = 0,
        historial_fecha = null,
        historial_tipo = "",
        historial_producto = "",
        historial_descripcion = "",
        punto_id = "",
        punto_userId = "",
        punto_cantidad = 0,
        punto_fecha = null,
        punto_tipo = "",
        punto_descripcion = "",
        id = "",
        userId = "",
        cantidad = 0,
        fecha = null,
        tipo = "",
        producto = "",
        descripcion = ""
    )
    
    // Constructor con parámetros principales
    constructor(
        id: String,
        userId: String,
        cantidad: Int,
        fecha: Timestamp,
        tipo: String,
        producto: String
    ) : this(
        historial_id = id,
        historial_userId = userId,
        historial_cantidad = cantidad,
        historial_fecha = fecha,
        historial_tipo = tipo,
        historial_producto = producto
    )

    // Propiedades computadas
    val getId: String
        get() = historial_id.ifEmpty { punto_id.ifEmpty { id } }

    val getUserId: String
        get() = historial_userId.ifEmpty { punto_userId.ifEmpty { userId } }

    val getCantidad: Int
        get() = if (historial_cantidad != 0) historial_cantidad else 
                if (punto_cantidad != 0) punto_cantidad else cantidad

    val getFecha: Timestamp?
        get() {
            val fechaObj = historial_fecha ?: punto_fecha ?: fecha
            return when (fechaObj) {
                is Timestamp -> fechaObj
                is Long -> {
                    if (fechaObj > 1000000000000L) { // Milisegundos
                        Timestamp(fechaObj / 1000, 0)
                    } else { // Segundos
                        Timestamp(fechaObj, 0)
                    }
                }
                is Date -> Timestamp(fechaObj)
                else -> null
            }
        }

    val getTipo: String
        get() = historial_tipo.ifEmpty { punto_tipo.ifEmpty { tipo } }

    val getProducto: String
        get() = historial_producto.ifEmpty { producto }

    val getDescripcion: String
        get() = historial_descripcion.ifEmpty { punto_descripcion.ifEmpty { descripcion } }

    val isGanado: Boolean
        get() = getTipo == "ganado"

    val isCanjeado: Boolean
        get() = getTipo == "canjeado"

    val isRecompensa: Boolean
        get() = getTipo == "recompensa"

    val tipoFormateado: String
        get() = when {
            isGanado -> "Puntos Ganados"
            isCanjeado -> "Puntos Canjeados"
            isRecompensa -> "Recompensa de Logro"
            else -> "Desconocido"
        }

}
