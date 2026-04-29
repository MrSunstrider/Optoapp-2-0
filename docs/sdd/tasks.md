# Tasks de implementacion de OptoApp

## Uso de este backlog
- Lista de verificación para PRs y revisiones de código: `code-review-checklist.md`.
- Cada tarea debe referenciar `constitution.md`, `spec.md` y `plan.md`.
- No iniciar tareas mayores sin estabilizar P0; el bloque P0 actual está cerrado (ver resumen).
- Marcar estado por tarea: TODO, IN_PROGRESS, DONE, BLOCKED, DEFERRED (diferido con motivo breve).

## P0 - Estabilidad y sincronizacion critica

Resumen: **P0-T1 … T5 = DONE** para la fase actual (bloque estabilidad/sync operativa + trazabilidad por registro en Room).

### P0-T1 Alinear nullabilidad Room/Supabase [DONE]
- Objetivo: eliminar errores `null value violates not-null constraint`.
- Acciones:
  - auditar columnas `NOT NULL` remotas vs DTOs/entidades locales.
  - decidir por campo: relajar schema o enviar valor por defecto controlado.
- Definition of Done:
  - sync completa sin errores de nullabilidad en flujo base.
- Implementado: valores por defecto seguros en `Paciente.toRemoto()` (textos opcionales como `""`, nombre/teléfono no vacíos).

### P0-T2 Orden estricto de sincronizacion [DONE]
- Objetivo: evitar fallos de FK por envio fuera de secuencia.
- Acciones:
  - aplicar secuencia Pacientes -> Evaluaciones -> Dispensaciones -> ServiciosExtra -> Pagos.
  - registrar logs de inicio/fin por etapa.
- Definition of Done:
  - no aparecen errores de FK en una corrida de sincronizacion completa.
- Implementado: orden ya era correcto en código; documentado en `SyncFinanzasUseCase` y logs `SyncViewModel` + cada use case.

### P0-T3 Trazabilidad de sincronizacion (operacion) [DONE]
- Objetivo: reintentos inteligentes y trazabilidad operativa **a nivel de corrida de sync** (fase actual: un solo operador / implementación incremental).
- Definition of Done (alcance acordado):
  - queda registro de última sync exitosa y del último fallo **global**; logs por etapa para depuración sin migrar cada tabla.
- Implementado:
  - `SyncTelemetry`: timestamp de última sync OK y último error global en DataStore (`sync_last_full_success_ms`, `sync_last_full_error`), mensaje acotado con `SyncErrorSanitizer`.
  - Logs por etapa en `SyncViewModel` y use cases de sync.
- **Fuera de alcance (delegado en P0-T5):** `sync_status` por fila, motivo de error por entidad, UI “pendiente de subir”.

### P0-T4 Robustecer cliente de serializacion y auth [DONE]
- Objetivo: reducir fallos por coercion y expiracion de sesion.
- Acciones:
  - habilitar configuracion JSON tolerante donde aplique.
  - asegurar refresh/retry ante errores de auth.
- Definition of Done:
  - sincronizacion resiste expiracion de token sin cerrar sesion abruptamente.
- Implementado: `Json` ya tolerante en `SupabaseModule`; `refreshCurrentSession()` antes de sync manual y silenciosa; reintento de sync completa tras error que parece de auth.

### P0-T5 Sync por registro / cola offline [DONE]
- Objetivo: saber **qué fila** falló en sync y exponer motivo para soporte (alcance MVP: Room local + UI de diagnóstico; sin cola de reintento automático por fila).
- Acciones realizadas:
  - tabla Room `sync_entity_state` (`SyncEntityState`, `SyncEntityStateDao`) con `opticaId` + `entityType` + `entityId`, mensaje de error y `SyncStateTracker` (marcar synced / error).
  - integración en subidas/bajadas de `SyncPacientesUseCase`, `SyncHistorialUseCase`, `SyncFinanzasUseCase`.
  - pantalla Configuración: lista de errores por registro vía `SyncDiagnosticsViewModel`.
- Definition of Done (alcance entregado):
  - tras una corrida, los registros con fallo quedan registrados localmente con mensaje; el usuario puede verlos en Configuración.
- **Fuera de alcance en esta iteración:** `sync_status` en cada tabla de negocio, espejo en Supabase, reintento automático por fila.

## P1 - Multi-optica y permisos

### P1-T1 Modelo `usuario_optica` [DONE]
- Objetivo: soportar N:N usuario-optica con rol.
- Definition of Done:
  - existe contrato de tabla y consumo en app para resolver contexto activo.
- Implementado: tabla Supabase `usuario_optica`; `MembershipRepository.fetchMembershipsForCurrentUser()`; mapeo a `OpticaMembership` (`data/OpticaMembership.kt`).

### P1-T2 Selector de optica activa [DONE]
- Objetivo: permitir cambio de contexto de trabajo cuando aplique.
- Definition of Done:
  - al login, usuarios multi-optica seleccionan optica y se persiste contexto.
- Implementado: `SeleccionOpticaScreen` + `AuthViewModel.selectOptica()`; `SessionManager` persiste `opticaId` / `opticaRol`; flujo post-login con varias membresías.

### P1-T3 Filtrado consistente por `optica_id` [DONE]
- Objetivo: evitar mezcla de datos entre tenants.
- Definition of Done:
  - todas las consultas criticas en Room/remoto filtran por optica activa.
- Implementado: entidades Room con `opticaId`; DAOs con variantes `*ForOptica` / `*ByOptica`; repositorio y view models usan `sessionManager.opticaId`; sync fuerza óptica en subidas.

### P1-T4 PermissionManager por rol [DONE]
- Objetivo: controlar acceso a pantallas y acciones por rol.
- Definition of Done:
  - administrador, optometrista y asesor tienen permisos esperados sin escalaciones.
- Implementado: `AppRoles` (`canViewBiAndReports`, `canViewCierreCaja`) según rol de `usuario_optica`; drawer en `MainDrawerScreen` oculta BI, reportes y cierre de caja para rol tipo asesor/ventas (MVP; ampliar matriz de roles cuando haya más pantallas).

## P2 - Monetizacion y escalado

**Resumen: P2-T1 y P2-T3 = DONE en código.** **P2-T2:** integración en app **DONE**; **validación de compra en dispositivo con Play Console** queda **DEFERRED** hasta después de encuestas / estudio de mercado (priorizar aprendizaje de precio y disposición a pagar antes de configurar producto, pistas de prueba y licencias).

### P2-T1 Modelo de suscripciones [DONE]
- Objetivo: soportar plan gratuito y planes de pago.
- Definition of Done:
  - existe estructura y validacion minima de estado de plan.
- Implementado: `SubscriptionManager` (`SubscriptionTier` FREE/PRO), caché DataStore (`sub_cached_plan`, `sub_dev_pro`), `MembershipRepository.fetchOpticaPlan` leyendo `OpticaDto.plan`; migración SQL `20260418140000_opticas_plan.sql` (`ALTER TABLE opticas ADD COLUMN plan`).

### P2-T2 Integracion Billing [DONE código | DEFERRED validación Play]
- Objetivo: habilitar compra/restauracion de planes de pago.
- Definition of Done (alcance cerrado):
  - código integrado con Play Billing Library y flujo de compra desde la app.
- Implementado: `PlayBillingManager` (producto `optoapp_pro_monthly`), `SubscriptionViewModel.launchProPurchase`; compra exitosa llama a `setProFromLocalCache`.
- **Pendiente (diferido a post-estudio):** comprobar de punta a punta en dispositivo con producto creado en Play Console, pista de prueba e internos/licencias de prueba. **Motivo del aplazamiento:** hacer antes encuestas o estudios de mercado para afinar oferta y pricing antes de invertir en setup de tienda y pruebas de billing.

### P2-T3 Paywall por limites de plan [DONE]
- Objetivo: aplicar restricciones del plan gratuito de forma clara.
- Definition of Done:
  - al superar limites, se bloquea accion y se muestra CTA de upgrade.
- Implementado: límite FREE `FREE_MAX_PACIENTES = 50` (`SubscriptionViewModel.canAddPaciente`); `PacientesListScreen` / `NuevoPacienteScreen` bloquean alta con diálogo y CTA; `Configuración` muestra plan, producto Play, override dev PRO y refresco de plan tras sync.

## P3 - Operacion optica (OT, Inventario, Agenda)

### P3-T1 OT en Dispensaciones (MVP) [DONE]
- Objetivo: registrar orden de trabajo de laboratorio por venta.
- Acciones:
  - agregar campo `ot` en `dispensaciones` (obligatorio al confirmar).
  - validar unicidad por optica (`optica_id + ot`).
  - definir formato inicial `OT-YYYY-####` editable.
- Definition of Done:
  - no se puede guardar dispensacion confirmada sin OT.
  - no se permiten OT duplicadas dentro de la misma optica.

### P3-T2 Ticket de Laboratorio en pantalla [DONE]
- Objetivo: mostrar solo la informacion tecnica para copiar/compartir.
- Acciones:
  - crear vista/modal "Ticket Laboratorio" desde Dispensacion.
  - incluir: OT, receta, DIP/DNP, altura, tipo/material/tratamientos, montura.
  - botones: Copiar y Compartir (sin PDF/impresion).
- Definition of Done:
  - el usuario puede copiar el ticket completo en texto en un toque.
  - no se muestran datos financieros ni diagnostico clinico.

### P3-T3 Contacto de laboratorio por optica [DONE]
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

### P3-T7 Agenda visual de citas [DONE]
- Objetivo: ver semana/mes de un vistazo a partir de `proxima_cita`.
- Acciones:
  - pantalla Agenda (filtros Hoy / Semana / Mes con navegación de mes en modo Mes).
  - campo `cita_estado` local + Supabase; estados: `programada`, `confirmada`, `asistio`, `no_asistio`, `reprogramada`.
- Definition of Done:
  - se pueden listar y filtrar citas del día/semana/mes por óptica.
  - reprogramar cita actualiza `proxima_cita`, marca `reprogramada` y sincroniza historial.

### P3-T8 Integracion BI con inventario y entregas [DONE]
- Objetivo: medir impacto operativo de laboratorio e inventario.
- Acciones:
  - metricas en panel BI: entregas pendientes/completadas en el periodo, ventas con montura de catalogo, referencias en stock bajo (instantaneo), salidas de inventario por venta (`SALIDA_VENTA` en periodo).
  - mismos filtros de periodo que el resto del BI; datos acotados a `sessionManager.opticaId`.
- Definition of Done:
  - BI muestra indicadores minimos operativos sin mezclar datos entre opticas.
- Implementado: `BIViewModel` + tarjeta "Operación e inventario" en `BIScreen`.

## Matriz de trazabilidad minima
- Reglas clinicas (`spec.md`) -> P0-T1, P0-T4
- Integridad de datos y sync (`plan.md`) -> P0-T1, P0-T2, P0-T3 (operación); granularidad por registro -> P0-T5 cuando aplique
- Multi-tenant y roles (`clarification.md` + `plan.md`) -> P1-T1, P1-T2, P1-T3, P1-T4
- Monetizacion (`spec.md`) -> P2-T1, P2-T2, P2-T3 (código MVP; validación Play en dispositivo diferida; webhooks Play / Edge Functions si hace falta verdad servidor-side)
- Operacion optica (nuevo alcance) -> P3-T1, P3-T2, P3-T3, P3-T4, P3-T5, P3-T6, P3-T7, P3-T8

## P4 - Web ecosistema (seguro, confiable y persistente)

Referencia principal: `docs/guia-web-ecosistema-seguro.md`.

### P4-T1 Baseline web segura (Next.js + Supabase SSR) [DONE]
- Objetivo: habilitar base web con sesion segura y rutas protegidas.
- Acciones:
  - setup Next.js + TypeScript + Tailwind + shadcn/ui.
  - configurar clientes Supabase SSR (`server/client`) y `middleware`.
  - login/logout + persistencia de sesion.
- Definition of Done:
  - rutas privadas protegidas sin sesion.
  - no uso de `service_role` en frontend.
- Avance:
  - carpeta `web/` bootstrap manual completado (config base, Supabase SSR, login, middleware, dashboard inicial).
  - dependencias instaladas y build/lint validados localmente.
  - seleccion de optica real implementada con lectura de `usuario_optica`, persistencia de contexto activo en cookie httpOnly y auto-seleccion cuando hay una sola membresia.

### P4-T2 Contexto multi-optica y permisos [DONE]
- Objetivo: reproducir el modelo de `optica activa` y visibilidad por rol.
- Acciones:
  - selector de optica para usuarios con multiples membresias.
  - persistencia de optica activa en sesion web.
  - menu/rutas condicionadas por rol.
- Definition of Done:
  - datos aislados por `optica_id`.
  - no hay acceso a modulos no permitidos por rol.
- Avance:
  - permisos por rol aplicados en web para navegacion y guardia server-side de rutas de modulo.
  - `reportes` restringido a roles con visibilidad BI (`admin`, `especialista`, `gerente`) alineado con `AppRoles`.
  - matriz web↔Android documentada en `docs/sdd/plan.md` con regla obligatoria para futuros modulos sensibles.

#### Criterio obligatorio para siguientes modulos sensibles web
- Antes de publicar cualquier modulo sensible nuevo (BI, cierre de caja, exportaciones, finanzas, auditoria), incluir en el mismo PR:
  1) guardia server-side por rol,
  2) filtro de navegacion por rol,
  3) actualizacion de matriz web↔Android en `plan.md`,
  4) validacion tecnica (`lint/build`) y validacion funcional por rol.

### P4-T3 Dashboard y navegacion operativa [DONE]
- Objetivo: entregar shell web productiva para operacion diaria.
- Acciones:
  - layout autenticado con sidebar + header de optica activa.
  - dashboard inicial con KPIs base.
  - placeholders de modulos principales.
- Definition of Done:
  - navegacion estable end-to-end con sesion activa.
  - KPIs visibles sin mezclar tenants.
- Avance:
  - dashboard web ya consume KPIs reales por `optica_id` desde `pagos`, `pacientes`, `dispensaciones` y `servicios_extra`.
  - validado con `npm run lint` y `npm run build`.
  - se agrega tarjeta de estado operativo (salud de fuentes + ultima actualizacion) y etiquetas visuales de estado por modulo en sidebar.
  - optimizaciones T1/T2/T3 aplicadas: validacion de pertenencia real de `optica_id` en middleware, cookie segura por entorno (`secure` solo en produccion) y correccion de calculo de fechas locales para KPIs diarios/mensuales.
- Verificacion de DoD:
  - `navegacion estable end-to-end con sesion activa`: cumplido (guardia en middleware + seleccion de optica + shell con rutas de modulos).
  - `KPIs visibles sin mezclar tenants`: cumplido (consultas dashboard filtradas por `optica_id` activo).

### P4-T4 Operacion administrativa web (MVP) [DONE]
- Objetivo: cubrir uso de backoffice de alto valor.
- Acciones:
  - pacientes (listado, detalle, alta/edicion).
  - configuracion fiscal segura por rol.
  - reportes financieros base.
- Definition of Done:
  - flujo CRUD minimo de pacientes funcionando con RLS.
  - restricciones por rol respetadas.
- Avance:
  - pacientes web MVP implementado: listado con busqueda, alta y edicion basica.
  - rutas `pacientes`, `pacientes/nuevo`, `pacientes/[id]` con guardias por rol y aislamiento por `optica_id`.
  - validacion tecnica completada con `npm run lint` y `npm run build`.
  - se agrega eliminacion protegida en detalle (confirmacion explicita + delete filtrado por `optica_id`).
  - se agrega paginacion real (20 por pagina) y filtros extra por rango de edad.
  - se agrega feedback UX de exito/error para crear, editar y eliminar paciente.
  - configuracion fiscal web implementada con lectura por `optica_id` y edicion restringida a `admin/gerente` (guardia server-side + formulario readonly para otros roles).
  - reportes financieros base web implementados con filtro por periodo (`dia/semana/mes/anio`), KPIs de ingresos/ventas/saldo/ticket y guardia de rol financiero.
  - hardening adicional: guardrails anti-falso-exito por RLS (`0 rows`) en crear/editar/eliminar paciente y en guardado fiscal (verificación de persistencia), más clamping de paginación fuera de rango.
- Verificacion de DoD:
  - `flujo CRUD minimo de pacientes funcionando con RLS`: cumplido (alta/listado/edicion/eliminacion acotados por `optica_id` y RLS server-side).
  - `restricciones por rol respetadas`: cumplido (lectura/edicion pacientes por rol, configuracion fiscal solo admin/gerente, reportes solo roles con BI).

### P4-T5 Hardening, pruebas y readiness [IN_PROGRESS]
- Objetivo: salir a produccion sin degradar seguridad ni confiabilidad.
- Acciones:
  - pruebas E2E de flujos criticos (auth, multi-optica, permisos, CRUD).
  - telemetria de errores y health checks operativos.
  - checklist de release/rollback documentado.
- Definition of Done:
  - criterios de salida cumplidos y validados por QA funcional.
- Avance:
  - se crea checklist formal de release en `docs/web-readiness-checklist.md` con:
    - seguridad, multi-tenant, pruebas funcionales por modulo/rol,
    - criterios GO/NO-GO,
    - protocolo de rollback y evidencia de cierre.
  - se ejecuta runbook tecnico automatico (lint/build + auditoria de secretos + auditoria de tenanting/guardias) con resultado PASS.
  - queda pendiente smoke test manual por rol para decision GO final.

## Cierre recomendado por prioridad (App vs Web Pacientes)

### P1 crítico [DONE]
- Evaluaciones (automatismos clínicos): tabs completos + autodiagnóstico por refracción, auto de otros diagnósticos, normalización/transposición y OSDI operativo en create/edit.
- Dispensaciones pago/adelanto: validaciones de monto/tope, ciclo de abonos (alta/edición/eliminación) y anulación en caja al eliminar pagos persistidos.
- Ficha (acciones críticas): WhatsApp con plantillas, PDF desde última evaluación con bloqueo sin evaluaciones, borrado protegido con confirmación y límite diario.
- Paywall por plan: bloqueo de alta por cupo en listado/form/actions + CTA de upgrade en UI.
- Riesgo residual: bajo (principalmente UX fino y pruebas manuales por rol/dispositivo).

### P2 importante [DONE]
- Formulario completo de paciente: campos clínico-demográficos equivalentes a app en web create/edit.
- HO sugerida + validación de duplicado por óptica: implementado en acciones server-side y feedback UI.
- Riesgo residual: bajo (depende de smoke funcional y QA de datos reales).

### P3 mejora UX [IN_PROGRESS]
- Filtros/chips en listados: implementados (incluye chip "Todos" + chips clínico-operativos), con ajustes de uso.
- Paridad visual fina en listados/tabs: avance alto; quedan micro-ajustes cosméticos no bloqueantes (spacing, tipografía, labels con tildes según baseline final).
- Riesgo residual: bajo-medio (no funcional, sí percepción UX).
