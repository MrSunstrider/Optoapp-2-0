package com.example.optoapp.util

fun Double.formatAsCurrency(): String = "S/ %.2f".format(this)

fun Int.formatAsInteger(): String = "%,d".format(this)
