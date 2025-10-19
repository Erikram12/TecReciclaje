package com.example.tecreciclaje.domain.model

import com.google.firebase.Timestamp
import java.util.Date

data class Estadisticas(
    var id: String = "",
    var fecha: String = "",
    var plasticos: Int = 0,
    var aluminios: Int = 0,
    var ganancias: Double = 0.0,
    var fechaCreacion: Timestamp? = null
) {
    // Constructor vacío necesario para Firebase
    constructor() : this("", "", 0, 0, 0.0, null)
    
    // Constructor con parámetros principales
    constructor(
        id: String,
        fecha: String,
        plasticos: Int,
        aluminios: Int,
        ganancias: Double
    ) : this(
        id = id,
        fecha = fecha,
        plasticos = plasticos,
        aluminios = aluminios,
        ganancias = ganancias,
        fechaCreacion = Timestamp.now()
    )

    // Propiedades computadas
    val totalReciclado: Int
        get() = plasticos + aluminios

    val promedioPorItem: Double
        get() = if (totalReciclado > 0) ganancias / totalReciclado else 0.0

}
