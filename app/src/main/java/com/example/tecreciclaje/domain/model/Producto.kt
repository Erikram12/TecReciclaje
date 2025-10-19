package com.example.tecreciclaje.domain.model

import java.io.Serializable

data class Producto(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val precioPuntos: Int = 0,
    val imagenUrl: String = "",
    val activo: Boolean = true,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaActualizacion: Long = System.currentTimeMillis()
) : Serializable
