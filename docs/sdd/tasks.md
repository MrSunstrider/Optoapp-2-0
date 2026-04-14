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

## Matriz de trazabilidad minima
- Reglas clinicas (`spec.md`) -> P0-T1, P0-T4
- Integridad de datos y sync (`plan.md`) -> P0-T1, P0-T2, P0-T3
- Multi-tenant y roles (`clarification.md` + `plan.md`) -> P1-T1, P1-T2, P1-T3, P1-T4
- Monetizacion futura (`spec.md`) -> P2-T1, P2-T2, P2-T3
