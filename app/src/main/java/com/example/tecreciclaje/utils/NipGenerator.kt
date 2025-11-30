package com.example.tecreciclaje.utils

import java.util.Random

/**
 * Utilidad para generar NIPs (Números de Identificación Personal) únicos
 */
object NipGenerator {
    
    /**
     * Genera un NIP único de 6 dígitos
     * Formato: 6 dígitos numéricos (000000-999999)
     */
    fun generateNip(): String {
        val random = Random()
        val nip = random.nextInt(1000000) // Genera un número entre 0 y 999999
        return String.format("%06d", nip) // Formatea con ceros a la izquierda si es necesario
    }
    
    /**
     * Genera un NIP único de longitud personalizada
     * @param length Longitud del NIP (por defecto 6)
     */
    fun generateNip(length: Int = 6): String {
        val random = Random()
        val maxValue = Math.pow(10.0, length.toDouble()).toInt() - 1
        val nip = random.nextInt(maxValue + 1)
        return String.format("%0${length}d", nip)
    }
}

