package com.example.optoapp.util.refraction

@JvmInline
value class Sphere(val value: String) {
    fun toDisplayString(): String = value.ifBlank { "" }
    val isValid: Boolean get() = value.isBlank() || SPHERE_REGEX.matches(value.trim())

    companion object {
        private val SPHERE_REGEX = Regex("^[+-]?\\d+(\\.\\d{1,2})?$")
        fun fromString(raw: String): Sphere = Sphere(raw.trim())
    }
}

@JvmInline
value class Cylinder(val value: String) {
    fun toDisplayString(): String = value.ifBlank { "" }
    val isValid: Boolean get() = value.isBlank() || CYLINDER_REGEX.matches(value.trim())

    companion object {
        private val CYLINDER_REGEX = Regex("^[+-]?\\d+(\\.\\d{1,2})?$")
        fun fromString(raw: String): Cylinder = Cylinder(raw.trim())
    }
}

@JvmInline
value class Axis(val value: String) {
    fun toDisplayString(): String = value.ifBlank { "" }
    val isValid: Boolean get() = value.isBlank() || AXIS_REGEX.matches(value.trim())

    companion object {
        private val AXIS_REGEX = Regex("^\\d{1,3}$")
        fun fromString(raw: String): Axis = Axis(raw.trim())
    }
}

@JvmInline
value class VisualAcuity(val value: String) {
    fun toDisplayString(): String = value.ifBlank { "" }
    val isValid: Boolean get() = value.isBlank() || VA_REGEX.matches(value.trim())

    companion object {
        private val VA_REGEX = Regex("^\\d{1,2}(\\.\\d{1,2})?/?\\d{0,2}$|^\\d{1,3}$")
        fun fromString(raw: String): VisualAcuity = VisualAcuity(raw.trim())
    }
}

@JvmInline
value class Prism(val value: String) {
    fun toDisplayString(): String = value.ifBlank { "" }

    companion object {
        fun fromString(raw: String): Prism = Prism(raw.trim())
    }
}

@JvmInline
value class DipMeasurement(val value: String) {
    fun toDisplayString(): String = value.ifBlank { "" }

    companion object {
        fun fromString(raw: String): DipMeasurement = DipMeasurement(raw.trim())
    }
}
