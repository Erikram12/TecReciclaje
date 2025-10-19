package com.example.tecreciclaje.domain.model

data class Logro(
    var id: String = "",
    var titulo: String = "",
    var descripcion: String = "",
    var icono: String = "",
    var objetivo: Int = 0,
    var progreso: Int = 0,
    var recompensa: Int = 0,
    var desbloqueado: Boolean = false,
    var reclamado: Boolean = false,
    var tipo: String = ""
) {
    // Constructor vacío necesario para Firebase
    constructor() : this(
        id = "",
        titulo = "",
        descripcion = "",
        icono = "",
        objetivo = 0,
        progreso = 0,
        recompensa = 0,
        desbloqueado = false,
        reclamado = false,
        tipo = ""
    )
    
    // Constructor con parámetros principales
    constructor(
        id: String,
        titulo: String,
        descripcion: String,
        icono: String,
        objetivo: Int,
        recompensa: Int,
        tipo: String
    ) : this(
        id = id,
        titulo = titulo,
        descripcion = descripcion,
        icono = icono,
        objetivo = objetivo,
        progreso = 0,
        recompensa = recompensa,
        desbloqueado = false,
        reclamado = false,
        tipo = tipo
    )

    // Propiedades computadas
    val porcentajeCompletado: Double
        get() = if (objetivo == 0) 0.0 else kotlin.math.min(100.0, (progreso.toDouble() / objetivo * 100))

    val estaCompletado: Boolean
        get() = progreso >= objetivo

    val textoProgreso: String
        get() = "$progreso/$objetivo completado"

    // Función para actualizar progreso con verificación de desbloqueo
    fun actualizarProgreso(nuevoProgreso: Int) {
        progreso = nuevoProgreso
        // Verificar si se desbloquea el logro
        if (progreso >= objetivo && !desbloqueado) {
            desbloqueado = true
        }
    }

}
