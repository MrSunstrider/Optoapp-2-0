package com.example.optoapp.testing

/**
 * Shared test tag constants for Compose UI E2E tests.
 *
 * Convention: `screen_element_action` or `screen_element_type`.
 * All values are snake_case strings for reliable semantic node queries.
 *
 * Placed in `main` source set so both production composables and test code
 * reference the same constants (compile-time safety, no typos at runtime).
 */
object TestTags {

    const val LOGIN_EMAIL_FIELD = "login_email_field"
    const val LOGIN_PASSWORD_FIELD = "login_password_field"
    const val LOGIN_INGRESAR_BTN = "login_ingresar_btn"
    const val LOGIN_ERROR_MESSAGE = "login_error_message"
    const val LOGIN_REMEMBER_ACCOUNT_CHECK = "login_remember_account_check"
    const val LOGIN_OLVIDASTE_BTN = "login_olvidaste_btn"
    const val LOGIN_SCREEN_ROOT = "login_screen_root"

    const val PIN_INPUT_FIELD = "pin_input_field"
    const val PIN_SCREEN_ROOT = "pin_screen_root"
    const val PIN_CREAR_BTN = "pin_crear_btn"
    const val PIN_CONFIRMAR_BTN = "pin_confirmar_btn"
    const val PIN_ERROR_MESSAGE = "pin_error_message"

    const val NAV_BOTTOM_PACIENTES = "nav_bottom_pacientes"
    const val NAV_BOTTOM_EVALUACIONES = "nav_bottom_evaluaciones"
    const val NAV_BOTTOM_DISPENSACIONES = "nav_bottom_dispensaciones"
    const val NAV_BOTTOM_AGENDA = "nav_bottom_agenda"
    const val NAV_DRAWER_TOGGLE = "nav_drawer_toggle"
    const val NAV_DRAWER_MENU = "nav_drawer_menu"
    const val NAV_DRAWER_CONFIGURACION = "nav_drawer_configuracion"
    const val NAV_DRAWER_CIERRE_CAJA = "nav_drawer_cierre_caja"
    const val NAV_DRAWER_REPORTES = "nav_drawer_reportes"

    const val PACIENTE_NOMBRE_FIELD = "paciente_nombre_field"
    const val PACIENTE_EDAD_FIELD = "paciente_edad_field"
    const val PACIENTE_FECHA_NAC_FIELD = "paciente_fecha_nac_field"
    const val PACIENTE_TELEFONO_FIELD = "paciente_telefono_field"
    const val PACIENTE_EMAIL_FIELD = "paciente_email_field"
    const val PACIENTE_DIRECCION_FIELD = "paciente_direccion_field"
    const val PACIENTE_DNI_FIELD = "paciente_dni_field"
    const val PACIENTE_GUARDAR_BTN = "paciente_guardar_btn"
    const val PACIENTE_LISTA = "paciente_lista"
    const val PACIENTE_SCREEN_ROOT = "paciente_screen_root"
    const val PACIENTE_ERROR_MESSAGE = "paciente_error_message"
    const val PACIENTE_CARD_LAST_EVAL_BTN = "paciente_card_last_eval_btn"
    const val PACIENTE_CARD_LAST_DISP_BTN = "paciente_card_last_disp_btn"

    const val EVALUACION_MOTIVO_FIELD = "evaluacion_motivo_field"
    const val EVALUACION_DIP_FIELD = "evaluacion_dip_field"
    const val EVALUACION_ESFERA_OD = "evaluacion_esfera_od"
    const val EVALUACION_CILINDRO_OD = "evaluacion_cilindro_od"
    const val EVALUACION_EJE_OD = "evaluacion_eje_od"
    const val EVALUACION_ESFERA_OI = "evaluacion_esfera_oi"
    const val EVALUACION_CILINDRO_OI = "evaluacion_cilindro_oi"
    const val EVALUACION_EJE_OI = "evaluacion_eje_oi"
    const val EVALUACION_GUARDAR_BTN = "evaluacion_guardar_btn"
    const val EVALUACION_AUTO_DIAGNOSTICO = "evaluacion_auto_diagnostico"
    const val EVALUACION_SCREEN_ROOT = "evaluacion_screen_root"
    const val EVALUACION_ERROR_MESSAGE = "evaluacion_error_message"

    const val DISPENSACION_AGREGAR_ITEM_BTN = "dispensacion_agregar_item_btn"
    const val DISPENSACION_OT_FIELD = "dispensacion_ot_field"
    const val DISPENSACION_MONTO_TOTAL = "dispensacion_monto_total"
    const val DISPENSACION_MONTO_PAGADO = "dispensacion_monto_pagado"
    const val DISPENSACION_METODO_PAGO = "dispensacion_metodo_pago"
    const val DISPENSACION_GUARDAR_BTN = "dispensacion_guardar_btn"
    const val DISPENSACION_SCREEN_ROOT = "dispensacion_screen_root"
    const val DISPENSACION_ERROR_MESSAGE = "dispensacion_error_message"
    const val DISPENSACION_ITEM_LISTA = "dispensacion_item_lista"
}
