package com.example.optoapp.util

/**
 * Calcula la Adición (ADD) sugerida según la edad del paciente.
 *
 * Fórmula: ADD = round((Edad - 30) / 10 * 4) / 4
 * La ADD sube +0.25 por cada ~2.5 años, capada a +3.50.
 *
 * @param edad edad del paciente en años
 * @return string con el valor de ADD formateado (ej: "+1.50"), o "" si < 40
 */
fun calcularAddPorEdad(edad: Int): String {
    if (edad < 40) return ""
    val raw = (edad - 30) / 10.0
    // Redondear al 0.25 más cercano
    val rounded = (raw * 4).coerceIn(4.0, 14.0).let { Math.round(it) / 4.0 }
    return "+%.2f".format(java.util.Locale.US, rounded)
}
