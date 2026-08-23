package com.example.optoapp.ui.navigation

/**
 * Type-safe navigation routes for OptoApp.
 * Replaces all raw string routes in NavHost composables.
 */
sealed class Route(val route: String) {
    // Auth
    data object Login : Route("login")
    data object Register : Route("register")
    data object Recovery : Route("recovery")
    data object NewPassword : Route("new_password")
    data object CreatePin : Route("create_pin")
    data object Pin : Route("pin")
    data object SinOptica : Route("sin_optica")
    data object SeleccionOptica : Route("seleccion_optica")
    data object Main : Route("main")

    // Main sections
    data object OperacionHoy : Route("operacion_hoy")
    data object Pacientes : Route("pacientes")
    data object Agenda : Route("agenda")
    data object NuevoPaciente : Route("nuevoPaciente")
    data object ServiciosExtra : Route("servicios_extra")
    data object NuevoServicio : Route("nuevo_servicio")
    data object Monturas : Route("monturas")
    data object Proveedores : Route("proveedores")
    data object OrdenesCompra : Route("ordenes_compra")
    data object InventarioFisico : Route("inventario_fisico")
    data object Gastos : Route("gastos")
    data object CierreCaja : Route("cierre_caja")
    data object Reportes : Route("reportes")
    data object CostosYGastos : Route("costos_y_gastos")
    data object EstadisticasBI : Route("estadisticas_bi")
    data object Configuracion : Route("configuracion")
    data object Conflictos : Route("conflictos")

    // Parameterized routes
    data class EditarPaciente(val id: String) : Route("editarPaciente/$id")
    data class DetallePaciente(val id: String) : Route("detallePaciente/$id")
    data class NuevaEvaluacion(val pacienteId: String) : Route("nuevaEvaluacion/$pacienteId")
    data class EditarEvaluacion(val pacienteId: String, val evalId: String) : Route("editarEvaluacion/$pacienteId/$evalId")
    data class NuevaDispensacion(val pacienteId: String) : Route("nuevaDispensacion/$pacienteId")
    data class EditarDispensacion(val pacienteId: String, val dispId: String) : Route("editarDispensacion/$pacienteId/$dispId")
    data class NuevoServicioPaciente(val pacienteId: String) : Route("nuevo_servicio/$pacienteId")
    data class EditarServicio(val id: String) : Route("editar_servicio/$id")
    data class CostosYGastosDisp(val dispensacionId: String) : Route("costos_y_gastos/$dispensacionId")
    data class InformacionFinanciera(val dispensacionId: String) : Route("informacion_financiera/$dispensacionId")
    data class AnalisisDetalle(val yearMonth: String) : Route("analisis_detalle/$yearMonth")
}
