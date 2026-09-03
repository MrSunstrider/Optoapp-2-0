package com.example.optoapp.domain.inventario

import com.example.optoapp.data.Montura

/** Active inventory (frames + accessories) eligible for servicios extra product picker. */
fun inventarioParaServicioExtra(items: List<Montura>): List<Montura> =
    items.filter { it.activo }
