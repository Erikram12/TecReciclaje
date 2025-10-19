package com.example.tecreciclaje.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.database.PropertyName
import java.util.Date

data class Vale(
    @PropertyName("vale_id")
    var vale_id: String = "",
    
    @PropertyName("vale_userId")
    var vale_userId: String = "",
    
    @PropertyName("vale_usuario_id")
    var vale_usuario_id: String = "",
    
    @PropertyName("vale_tipo")
    var vale_tipo: String = "",
    
    @PropertyName("vale_descripcion")
    var vale_descripcion: String = "",
    
    @PropertyName("vale_producto")
    var vale_producto: String = "",
    
    @PropertyName("vale_puntosRequeridos")
    var vale_puntosRequeridos: Int = 0,
    
    @PropertyName("vale_estado")
    var vale_estado: String = "",
    
    @PropertyName("vale_fechaCreacion")
    var vale_fechaCreacion: Timestamp? = null,
    
    @PropertyName("vale_fecha_creacion")
    var vale_fecha_creacion: Any? = null,
    
    @PropertyName("vale_fechaCanje")
    var vale_fechaCanje: Timestamp? = null,
    
    @PropertyName("vale_fecha_expiracion")
    var vale_fecha_expiracion: Any? = null,
    
    @PropertyName("vale_codigoQR")
    var vale_codigoQR: String = "",
    
    @PropertyName("vale_establecimiento")
    var vale_establecimiento: String = "",
    
    @PropertyName("vale_imagen_url")
    var vale_imagen_url: String = ""
) {
    // Constructor vacío necesario para Firebase
    constructor() : this(
        vale_id = "",
        vale_userId = "",
        vale_usuario_id = "",
        vale_tipo = "",
        vale_descripcion = "",
        vale_producto = "",
        vale_puntosRequeridos = 0,
        vale_estado = "",
        vale_fechaCreacion = null,
        vale_fecha_creacion = null,
        vale_fechaCanje = null,
        vale_fecha_expiracion = null,
        vale_codigoQR = "",
        vale_establecimiento = "",
        vale_imagen_url = ""
    )
    
    // Constructor con parámetros principales
    constructor(
        id: String,
        userId: String,
        tipo: String,
        descripcion: String,
        puntosRequeridos: Int,
        establecimiento: String
    ) : this(
        vale_id = id,
        vale_userId = userId,
        vale_tipo = tipo,
        vale_descripcion = descripcion,
        vale_puntosRequeridos = puntosRequeridos,
        vale_establecimiento = establecimiento,
        vale_estado = "disponible",
        vale_fechaCreacion = Timestamp.now()
    )

    // Propiedades computadas
    val isDisponible: Boolean
        get() = vale_estado == "disponible" || vale_estado == "Válido"

    val isCanjeado: Boolean
        get() = vale_estado == "canjeado"

    val isExpirado: Boolean
        get() = vale_estado == "expirado"

    val isValido: Boolean
        get() = vale_estado == "Válido"

    // Funciones de utilidad
    fun canjear() {
        vale_estado = "canjeado"
        vale_fechaCanje = Timestamp.now()
    }

    fun expirar() {
        vale_estado = "expirado"
    }

}
