package com.example.tecreciclaje.Model

import com.google.firebase.database.PropertyName

data class HistorialModel(
    @PropertyName("historial_cantidad")
    var cantidad: Int = 0,
    
    @PropertyName("historial_fecha")
    var fecha: Long = 0L,
    
    @PropertyName("historial_tipo")
    var tipo: String = "",
    
    @PropertyName("historial_producto")
    var producto: String = "" // solo presente si el tipo es "canjeado"
) {
    // Constructor vacío requerido por Firebase
    constructor() : this(0, 0L, "", "")
}
