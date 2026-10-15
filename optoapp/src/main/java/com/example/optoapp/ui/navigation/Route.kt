package com.example.optoapp.ui.navigation

import com.example.optoapp.util.MesArg
import java.time.YearMonth

sealed class Route(val route: String) {
    data object Login : Route("login")
    data object Register : Route("register")
    data object Recovery : Route("recovery")
    data object NewPassword : Route("new_password")
    data object CreatePin : Route("create_pin")
    data object Pin : Route("pin")
    data object SinOptica : Route("sin_optica")
    data object SeleccionOptica : Route("seleccion_optica")
    data object Main : Route("main")

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
    data object AnalisisDetalle : Route("analisis_detalle?${MesArg.KEY}={${MesArg.KEY}}") {
        fun createRoute(mes: YearMonth): String = "analisis_detalle?${MesArg.KEY}=${MesArg.format(mes)}"
    }
    data object Configuracion : Route("configuracion")
    data object Conflictos : Route("conflictos")

    data class EditarPaciente(val id: String) : Route("editarPaciente/$id")
    data class DetallePaciente(val id: String) : Route("detallePaciente/$id")
    data class NuevaEvaluacion(val pacienteId: String) : Route("nuevaEvaluacion/$pacienteId")
    data class EditarEvaluacion(val pacienteId: String, val evalId: String) : Route("editarEvaluacion/$pacienteId/$evalId")
    data class NuevaDispensacion(val pacienteId: String) : Route("nuevaDispensacion/$pacienteId")
    data class EditarDispensacion(val pacienteId: String, val dispId: String, val seccion: String? = null) :
        Route("editarDispensacion/$pacienteId/$dispId" + (seccion?.let { "?$SECCION_ARG=$it" } ?: "")) {
        companion object {
            const val SECCION_ARG = "seccion"
            const val SECCION_COSTO = "costo"
            const val PATTERN = "editarDispensacion/{pacienteId}/{dispId}?$SECCION_ARG={$SECCION_ARG}"
        }
    }
    data class NuevoServicioPaciente(val pacienteId: String) : Route("nuevo_servicio/$pacienteId")
    data class EditarServicio(val id: String) : Route("editar_servicio/$id")
    data class InformacionFinanciera(val dispensacionId: String) : Route("informacion_financiera/$dispensacionId")


}
