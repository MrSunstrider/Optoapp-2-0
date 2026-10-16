package com.example.optoapp.ui.screens

import androidx.annotation.StringRes
import com.example.optoapp.R
import com.example.optoapp.domain.Prioridad

@StringRes
fun Prioridad.labelRes(): Int = when (this) {
    Prioridad.ALTA -> R.string.analisis_prioridad_alta
    Prioridad.MEDIA -> R.string.analisis_prioridad_media
    Prioridad.BAJA -> R.string.analisis_prioridad_baja
}
