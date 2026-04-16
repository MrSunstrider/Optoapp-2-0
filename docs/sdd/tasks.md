# Tasks de implementacion de OptoApp

## Uso de este backlog
- Cada tarea debe referenciar `constitution.md`, `spec.md` y `plan.md`.
- No iniciar tareas P1/P2 sin estabilizar P0.
- Marcar estado por tarea: TODO, IN_PROGRESS, DONE, BLOCKED.

## P0 - Estabilidad y sincronizacion critica

### P0-T1 Alinear nullabilidad Room/Supabase
- Objetivo: eliminar errores `null value violates not-null constraint`.
- Acciones:
  - auditar columnas `NOT NULL` remotas vs DTOs/entidades locales.
  - decidir por campo: relajar schema o enviar valor por defecto controlado.
- Definition of Done:
  - sync completa sin errores de nullabilidad en flujo base.

### P0-T2 Orden estricto de sincronizacion
- Objetivo: evitar fallos de FK por envio fuera de secuencia.
- Acciones:
  - aplicar secuencia Pacientes -> Evaluaciones -> Dispensaciones -> ServiciosExtra -> Pagos.
  - registrar logs de inicio/fin por etapa.
- Definition of Done:
  - no aparecen errores de FK en una corrida de sincronizacion completa.

### P0-T3 Estado de sincronizacion por registro
- Objetivo: reintentos inteligentes y trazabilidad operativa.
- Acciones:
  - agregar `sync_status` y actualizar transiciones `pending/synced/error`.
  - incluir motivo de error para depuracion.
- Definition of Done:
  - cada registro sincronizable refleja estado actual real.

### P0-T4 Robustecer cliente de serializacion y auth
- Objetivo: reducir fallos por coercion y expiracion de sesion.
- Acciones:
  - habilitar configuracion JSON tolerante donde aplique.
  - asegurar refresh/retry ante errores de auth.
- Definition of Done:
  - sincronizacion resiste expiracion de token sin cerrar sesion abruptamente.

## P1 - Multi-optica y permisos

### P1-T1 Modelo `usuario_optica`
- Objetivo: soportar N:N usuario-optica con rol.
- Definition of Done:
  - existe contrato de tabla y consumo en app para resolver contexto activo.

### P1-T2 Selector de optica activa
- Objetivo: permitir cambio de contexto de trabajo cuando aplique.
- Definition of Done:
  - al login, usuarios multi-optica seleccionan optica y se persiste contexto.

### P1-T3 Filtrado consistente por `optica_id`
- Objetivo: evitar mezcla de datos entre tenants.
- Definition of Done:
  - todas las consultas criticas en Room/remoto filtran por optica activa.

### P1-T4 PermissionManager por rol
- Objetivo: controlar acceso a pantallas y acciones por rol.
- Definition of Done:
  - administrador, optometrista y asesor tienen permisos esperados sin escalaciones.

## P2 - Monetizacion y escalado

### P2-T1 Modelo de suscripciones
- Objetivo: soportar plan gratuito y planes de pago.
- Definition of Done:
  - existe estructura y validacion minima de estado de plan.

### P2-T2 Integracion Billing
- Objetivo: habilitar compra/restauracion de planes de pago.
- Definition of Done:
  - flujo de compra funcional en entorno de prueba.

### P2-T3 Paywall por limites de plan
- Objetivo: aplicar restricciones del plan gratuito de forma clara.
- Definition of Done:
  - al superar limites, se bloquea accion y se muestra CTA de upgrade.

## P3 - Operacion optica (OT, Inventario, Agenda)

### P3-T1 OT en Dispensaciones (MVP) [TODO]
- Objetivo: registrar orden de trabajo de laboratorio por venta.
- Acciones:
  - agregar campo `ot` en `dispensaciones` (obligatorio al confirmar).
  - validar unicidad por optica (`optica_id + ot`).
  - definir formato inicial `OT-YYYY-####` editable.
- Definition of Done:
  - no se puede guardar dispensacion confirmada sin OT.
  - no se permiten OT duplicadas dentro de la misma optica.

### P3-T2 Ticket de Laboratorio en pantalla [TODO]
- Objetivo: mostrar solo la informacion tecnica para copiar/compartir.
- Acciones:
  - crear vista/modal "Ticket Laboratorio" desde Dispensacion.
  - incluir: OT, receta, DIP/DNP, altura, tipo/material/tratamientos, montura.
  - botones: Copiar y Compartir (sin PDF/impresion).
- Definition of Done:
  - el usuario puede copiar el ticket completo en texto en un toque.
  - no se muestran datos financieros ni diagnostico clinico.

### P3-T3 Contacto de laboratorio por optica [TODO]
- Objetivo: agilizar envio por WhatsApp/Telegram sin crear usuario tecnico.
- Acciones:
  - guardar `laboratorio_nombre` y `laboratorio_contacto` en configuracion de optica.
  - habilitar accion "Abrir WhatsApp" con mensaje prellenado.
- Definition of Done:
  - con contacto configurado, abre canal de envio con texto OT listo.

### P3-T4 Catalogo de monturas (MVP) [DONE]
- Objetivo: dejar de registrar monturas solo como texto libre.
- Acciones:
  - crear entidad `monturas` (sku, marca, modelo, color, talla, costo, precio, stock_actual, stock_minimo, activo, optica_id).
  - agregar `montura_id` opcional en `dispensaciones` para compatibilidad legacy.
- Definition of Done:
  - se puede crear/editar/desactivar monturas por optica.
  - una dispensacion puede vincular montura de catalogo sin romper flujo actual.

### P3-T5 Movimientos de inventario de montura [DONE]
- Objetivo: tener trazabilidad de entradas/salidas y stock confiable.
- Acciones:
  - crear `movimientos_montura` (tipo, cantidad, fecha, motivo, ref_dispensacion_id, optica_id).
  - descontar stock al evento de negocio definido (entrega o confirmacion).
- Definition of Done:
  - cada cambio de stock tiene movimiento asociado y auditable.
  - el stock actual coincide con suma de movimientos.

### P3-T6 Alertas de stock bajo [DONE]
- Objetivo: prevenir quiebres de inventario en modelos de alta rotacion.
- Acciones:
  - generar alerta cuando `stock_actual <= stock_minimo`.
  - exponer contador en pantalla de inventario y listado de "por reponer".
- Definition of Done:
  - el usuario visualiza rapidamente monturas criticas por reposicion.

### P3-T7 Agenda visual de citas [TODO]
- Objetivo: ver semana/mes de un vistazo a partir de `proxima_cita`.
- Acciones:
  - crear pantalla Agenda (hoy/manana + calendario mensual).
  - estados de cita: `programada`, `confirmada`, `asistio`, `no_asistio`, `reprogramada`.
- Definition of Done:
  - se pueden listar y filtrar citas del dia/semana.
  - reprogramar cita actualiza agenda y proxima fecha del paciente.

### P3-T8 Integracion BI/Reportes con inventario y OT [TODO]
- Objetivo: medir impacto operativo de laboratorio e inventario.
- Acciones:
  - agregar metricas: rotacion de monturas, stock bajo, OTs pendientes/listas.
  - mantener filtros por periodo y optica activa.
- Definition of Done:
  - BI muestra indicadores minimos operativos sin mezclar datos entre opticas.

## Matriz de trazabilidad minima
- Reglas clinicas (`spec.md`) -> P0-T1, P0-T4
- Integridad de datos y sync (`plan.md`) -> P0-T1, P0-T2, P0-T3
- Multi-tenant y roles (`clarification.md` + `plan.md`) -> P1-T1, P1-T2, P1-T3, P1-T4
- Monetizacion futura (`spec.md`) -> P2-T1, P2-T2, P2-T3
- Operacion optica (nuevo alcance) -> P3-T1, P3-T2, P3-T3, P3-T4, P3-T5, P3-T6, P3-T7, P3-T8
