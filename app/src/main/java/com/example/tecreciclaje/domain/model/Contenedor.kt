package com.example.tecreciclaje.domain.model

data class Contenedor(
    var id: String = "",
    var nombre: String = "",
    var ubicacion: String = "",
    var tipo: String = "",
    var estado: String = "",
    var capacidad: Int = 0,
    var cantidadActual: Int = 0,
    var ultimaActualizacion: String = "",
    var responsable: String = ""
) {
    // Constructor vacío necesario para Firebase
    constructor() : this("", "", "", "", "", 0, 0, "", "")
    
    // Constructor con parámetros principales
    constructor(
        id: String,
        nombre: String,
        ubicacion: String,
        tipo: String,
        estado: String,
        capacidad: Int,
        cantidadActual: Int
    ) : this(
        id = id,
        nombre = nombre,
        ubicacion = ubicacion,
        tipo = tipo,
        estado = estado,
        capacidad = capacidad,
        cantidadActual = cantidadActual,
        ultimaActualizacion = "",
        responsable = ""
    )

    // Propiedades computadas
    val porcentajeLlenado: Int
        get() = if (capacidad == 0) 0 else (cantidadActual * 100) / capacidad

    val isLleno: Boolean
        get() = cantidadActual >= capacidad

    val isVacio: Boolean
        get() = cantidadActual == 0

    val isDisponible: Boolean
        get() = estado == "disponible"

}
