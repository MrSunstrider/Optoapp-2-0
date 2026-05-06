package com.example.optoapp.domain.factory

import com.example.optoapp.domain.model.EvaluacionModel

interface DiagnosticoEngine {
    fun generateDiagnostico(evaluacion: EvaluacionModel): String
}

class StandardDiagnosticoEngine : DiagnosticoEngine {
    override fun generateDiagnostico(evaluacion: EvaluacionModel): String {
        return "Diagnóstico Estándar basado en refracción"
    }
}

class AdvancedDiagnosticoEngine : DiagnosticoEngine {
    override fun generateDiagnostico(evaluacion: EvaluacionModel): String {
        return "Diagnóstico Avanzado con IA y análisis binocular"
    }
}

class DiagnosticoFactory {
    fun createEngine(role: String): DiagnosticoEngine {
        return when (role) {
            "admin", "optometrista" -> AdvancedDiagnosticoEngine()
            else -> StandardDiagnosticoEngine()
        }
    }
}
