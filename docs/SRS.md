# OptoApp SaaS — Especificación de Requisitos de Software (SRS)

| Campo | Valor |
|-------|--------|
| **Versión del documento** | 1.0 |
| **Fecha** | 2026-09-24 |
| **Producto** | OptoApp SaaS |
| **Versión Android de referencia** | 1.16.15 (versionCode 64) |
| **Autor / aprobador** | Desarrollador único (auto-aprobación) |
| **Estado** | Borrador aprobado por el autor |
| **Idioma** | Español |
| **Mercado objetivo** | Perú |

**Relación con otros documentos**

- Este **SRS** es la **fuente de verdad de requisitos** del software.
- El [PRD](PRD.md) es **complementario** (visión de producto y contexto). Si hay conflicto, **manda el SRS**.
- Las specs OpenSpec (`openspec/specs/`) y la constitución SDD describen contratos de implementación; deben alinearse con este SRS cuando se cambie comportamiento de producto.
- Inventario AS-IS basado en **código** (Android, Supabase, OptoWeb), no en planes de mejora obsoletos.

**Convención de estado de requisitos**

| Etiqueta | Significado |
|----------|------------|
| **I** | Implementado (comportamiento observable en código en producción/desarrollo actual) |
| **P** | Parcial (existe, pero incompleto, inconsistente o no maduro) |
| **F** | Futuro / planificado (TO-BE; no es criterio del MVP Android salvo que se indique) |

Prioridad MoSCoW en TO-BE: **Must** / **Should** / **Could** / **Won't (ahora)**.

---

## 1. Introducción

### 1.1 Propósito

Definir los requisitos funcionales y no funcionales de OptoApp SaaS para:

1. Documentar el sistema **tal como existe** (AS-IS).
2. Definir el **MVP Android** válido.
3. Priorizar el trabajo **TO-BE** hacia madurez de producto, Web, multi-sucursal, monetización e iOS.

Sirve al desarrollador como contrato de qué construir, madurar y verificar.

### 1.2 Alcance del producto

OptoApp es un sistema de gestión para ópticas y consultorios optométricos que cubre:

- Admisión de pacientes e historial clínico (evaluaciones).
- Dispensación (órdenes de trabajo), servicios extra, pagos y stock de monturas.
- Operación diaria, cierre de caja, costos/gastos, análisis y reportes.
- Multi-tenant por óptica (`optica_id`), autenticación y roles.
- App **Android offline-first** sincronizada con **Supabase** (PostgreSQL + Auth + RLS).
- Companion **OptoWeb** (backoffice) sobre la misma base de datos.

**Fuera de alcance del MVP Android (fase posterior):** paridad completa OptoWeb, multi-sucursal avanzada, iOS, suscripciones de pago reales, facturación electrónica.

### 1.3 Definiciones y acrónimos

| Término | Definición |
|---------|------------|
| **Óptica / tenant** | Unidad de aislamiento de datos (`optica_id`) |
| **HO** | Número de historia óptica del paciente |
| **OT** | Orden de trabajo / dispensación |
| **LWW** | Last Write Wins (resolución por `updated_at`) |
| **RLS** | Row Level Security en PostgreSQL |
| **MVP** | Producto mínimo viable — aquí: superficie Android operativa completa |
| **AS-IS** | Comportamiento actual del sistema |
| **TO-BE** | Comportamiento deseado / roadmap |

### 1.4 Referencias

- Código Android: módulo `:optoapp`
- Backend: `supabase/` (migraciones, `config.toml`, edge functions)
- Web: repositorio hermano `optoweb` (paquete `optoapp-web`)
- [docs/PRD.md](PRD.md), [docs/sdd/constitution.md](sdd/constitution.md), [docs/sdd/spec.md](sdd/spec.md)
- Guía de usuario: [docs/GUIA-USUARIO-OPTOAPP.md](GUIA-USUARIO-OPTOAPP.md)

---

## 2. Descripción general

### 2.1 Perspectiva del producto

```
┌─────────────────────┐     ┌─────────────────────┐
│  Android (MVP)      │     │  OptoWeb (fase 2)   │
│  Room + Compose     │     │  Next.js + SSR      │
│  Offline-first      │     │  Backoffice online  │
└──────────┬──────────┘     └──────────┬──────────┘
           │                           │
           └─────────────┬─────────────┘
                         ▼
              ┌─────────────────────┐
              │  Supabase           │
              │  Auth + Postgres 17 │
              │  RLS por optica_id  │
              │  RPCs BI / caja     │
              └─────────────────────┘
```

### 2.2 Funciones del producto (resumen)

| Área | Android | OptoWeb | Backend |
|------|---------|---------|---------|
| Auth / PIN / multi-óptica | I | I (parcial Google/PIN) | I |
| Pacientes / evaluaciones | I | I | I |
| Dispensaciones / servicios | I | I | I |
| Agenda | I | P (casi solo lectura) | vía evaluaciones |
| Inventario monturas | I | I | I |
| Proveedores / OC / conteo físico | I | F (ausente) | I |
| Cierre / BI / reportes | P (madurar) | I (básico) | I |
| Sync offline | I | N/A (telemetría) | PostgREST |
| Anulaciones / reclamos | P | P | I (ledger) |
| Suscripciones de pago | P (Play path incompleto) | P | P |
| Facturación electrónica | F | F | F |
| iOS | F | — | compartido |

### 2.3 Características de usuarios

| Actor | Descripción | Necesidad principal |
|-------|-------------|---------------------|
| **Admin** | Dueño / administrador de la óptica | Config, roles, backup, visión completa |
| **Gerente** | Supervisa operación y finanzas | Caja, BI, inventario, personal |
| **Especialista** | Optometrista | Evaluaciones clínicas, pacientes |
| **Asesor / Asesora / Ventas** | Mostrador | Dispensación, servicios, stock |
| **Invitado** | Acceso limitado | Lectura restringida (sin escritura BI/caja) |

### 2.4 Restricciones

- Android min SDK 24; offline-first obligatorio en punto de atención.
- Backend único: Supabase proyecto de producción compartido Android + Web.
- Multi-tenant: ninguna entidad de negocio sin aislamiento por óptica (requisito; ver TO-BE #5).
- Idioma de UI: español.
- Moneda operativa asumida: soles peruanos (S/) en copy de recomendaciones/UI actuales.
- Confirmación de email Auth: deshabilitada en configuración actual (decisión documentada; riesgo aceptado hasta revisión).

### 2.5 Supuestos y dependencias

- El desarrollador es el único aprobador del SRS y del producto.
- Hipótesis de precio inicial: **S/ 30 / mes** (puede cambiar; planes semestral/anual TBD).
- Normativa peruana de datos personales / salud: **pendiente de investigación** (sección 11).
- Google OAuth en producción puede estar configurado fuera de `config.toml` (Dashboard).
- OptoWeb evoluciona **después** de cerrar el MVP Android y las maduraciones Must de Android.

### 2.6 Criterio de MVP Android

El MVP Android se considera **válido** cuando:

1. Los módulos listados en §3–§4 con estado **I** o **P** cubren el flujo diario: login → pacientes → evaluación → dispensación/servicio → pagos → inventario básico → sync → cierre/reportes básicos.
2. Un usuario puede operar **sin internet** y sincronizar al recuperar conectividad sin pérdida sistemática de datos.
3. El aislamiento por óptica funciona en el camino feliz (login + datos locales + RLS remoto).
4. Las áreas **P** (anulaciones, costos/gastos, análisis “analista”, reportes ricos, multitenant exhaustivo) **no bloquean** el MVP, pero quedan como Must inmediatos del roadmap post-MVP (§10).

---

## 3. Actores, roles y permisos

### 3.1 Roles del sistema (AS-IS)

Roles en `usuario_optica` / `AppRoles`: `admin`, `gerente`, `especialista`, `asesor`, `asesora`, `ventas`, `invitado`.

| Capacidad (helpers `AppRoles`) | admin | gerente | especialista | asesor/a / ventas | invitado |
|--------------------------------|:-----:|:------:|:------------:|:-----------------:|:--------:|
| Operación hoy | ✓ | ✓ | ✓ | ✓ | |
| Cierre de caja | ✓ | ✓ | ✓ | | |
| BI / reportes gerenciales | ✓ | ✓ | | | |
| Crear/editar pacientes (helper) | ✓ | ✓ | ✓ | ✓ | |
| Crear/editar evaluaciones (helper) | ✓ | ✓ | ✓ | | |
| Crear/editar dispensaciones (helper) | ✓ | ✓ | ✓ | ✓ | |
| Editar inventario (helper) | ✓ | ✓ | ✓ | ✓ | |
| Borrar registros | ✓ | ✓ | | | |
| Gestionar usuarios | ✓ | ✓ | | | |
| Backup / restore | ✓ | | | | |
| Asignar rol admin | ✓ | | | | |

### 3.2 Inconsistencias conocidas (requisito de corrección)

**RF-SEC-01 (P / Must en TO-BE #5):** El sistema SHALL alinear permisos entre UI, ViewModels (`AuthorizationGuard`) y RLS remoto.

Hallazgos AS-IS a corregir:

- Crear paciente: UI/VM a menudo restringen a admin/gerente, mientras `canCreateEditPacientes` incluye roles comerciales.
- Evaluaciones: helper `canCreateEditEvaluaciones` existe pero pantallas pueden no aplicarlo.
- Monturas/dispensaciones: crear a menudo abierto; editar/borrar más restringido a admin/gerente.
- RLS remoto usa matrices por operación; clientes locales deben respetar la misma semántica.

### 3.3 Requisitos de autenticación (AS-IS)

| ID | Requisito | Estado |
|----|-----------|--------|
| RF-AUTH-01 | Login email/contraseña | I |
| RF-AUTH-02 | Registro de cuenta | I |
| RF-AUTH-03 | Google OAuth (Android; Web presente en código) | I / P (config no versionada) |
| RF-AUTH-04 | Recuperación de contraseña + deep link `optoapp://auth` | I |
| RF-AUTH-05 | PIN de 6 dígitos (EncryptedSharedPreferences) como segundo factor de sesión local | I |
| RF-AUTH-06 | Selección / creación de óptica; multi-membresía | I |
| RF-AUTH-07 | Plan FREE: máximo 1 óptica por creador (enforcement servidor) | I |
| RF-AUTH-08 | Cuota de eliminación de pacientes: 10/día/usuario (UTC) | I |

---

## 4. Requisitos funcionales por módulo

### 4.1 Pacientes

| ID | Requisito | Estado |
|----|-----------|--------|
| RF-PAC-01 | Crear, editar, listar y buscar pacientes (nombre, documento, HO) | I |
| RF-PAC-02 | Aislar pacientes por `optica_id`; no hay paciente global entre ópticas | I |
| RF-PAC-03 | Calcular edad desde fecha de nacimiento | I |
| RF-PAC-04 | Eliminar paciente solo admin/gerente con confirmación, auditoría y cuota diaria | I |
| RF-PAC-05 | Detalle de paciente con navegación a evaluaciones, dispensaciones y servicios | I |

### 4.2 Evaluaciones clínicas

| ID | Requisito | Estado |
|----|-----------|--------|
| RF-EVA-01 | Registrar evaluación con AV, refracción, contactología y campos clínicos asociados | I |
| RF-EVA-02 | Diagnóstico automático por ojo según esfera/cilindro (notación negativa); ver §8.2 | I |
| RF-EVA-03 | Detectar/derivar presbicia, anisometropía y ambliopía según reglas §8.2 | P |
| RF-EVA-06 | Ambliopía auto: parseo **ambidireccional** AV cc (Snellen fraccional **y** decimal) → logMAR → umbral §8.2 | I (Android; Web diferida) |
| RF-EVA-04 | Programar próxima cita (alimenta agenda) | I |
| RF-EVA-05 | Sugerencia de LC por astigmatismo corneal \|K1−K2\| | I |

### 4.3 Dispensaciones (OT)

| ID | Requisito | Estado |
|----|-----------|--------|
| RF-DIS-01 | Crear OT con múltiples ítems (lentes, montura, etc.) | I |
| RF-DIS-02 | Registrar pagos parciales/completos; calcular saldo | I |
| RF-DIS-03 | Estados de entrega: al menos Pendiente, Entregado, Anulado, Reclamada | I |
| RF-DIS-04 | Regalos / movimientos de stock asociados a la venta | I |
| RF-DIS-05 | Anular dispensación con reversos de pagos | P |
| RF-DIS-06 | Reclamo con reembolso opcional | P |
| RF-DIS-07 | Anulación/reclamo SHALL restaurar stock de forma consistente y auditable | F (Must #2) |
| RF-DIS-08 | Flujo de devolución/reclamo SHALL ser comprensible en UI y sin estados huérfanos | F (Must #2) |

### 4.4 Servicios extra

| ID | Requisito | Estado |
|----|-----------|--------|
| RF-SER-01 | Registrar servicios no ligados (o ligados opcionalmente) a paciente | I |
| RF-SER-02 | Multi-ítem, pagos y regalos | I |
| RF-SER-03 | Anular servicio con restock de monturas/regalos | I / P (madurar junto a #2) |

### 4.5 Agenda

| ID | Requisito | Estado |
|----|-----------|--------|
| RF-AGE-01 | Listar citas HOY/SEMANA/MES derivadas de evaluaciones | I |
| RF-AGE-02 | Reprogramar cita y notificar | I |
| RF-AGE-03 | Paridad de edición de agenda en OptoWeb | F (fase Web) |

### 4.6 Inventario

| ID | Requisito | Estado |
|----|-----------|--------|
| RF-INV-01 | CRUD monturas (SKU, tipo aro, stock, costos asociados) | I |
| RF-INV-02 | Movimientos de inventario trazables | I |
| RF-INV-03 | Proveedores y categorías de montura | I |
| RF-INV-04 | Órdenes de compra e ítems | I |
| RF-INV-05 | Conteo físico (sesión + detalle) | I |
| RF-INV-06 | Alertas de stock bajo / KPIs de inventario | I / P |

### 4.7 Finanzas — operación, caja, costos

| ID | Requisito | Estado |
|----|-----------|--------|
| RF-FIN-01 | Dashboard operación hoy (KPIs del día, pendientes) | I |
| RF-FIN-02 | Cierre de caja por fecha / métodos de pago | I |
| RF-FIN-03 | Registrar gastos operativos | P |
| RF-FIN-04 | Matrices de costos (productos, biselado, LC) | P |
| RF-FIN-05 | Modelo de costos/gastos SHALL alimentar márgenes reales del análisis | F (Must #3) |
| RF-FIN-06 | Recalcular `resumen_diario` vía RPC en sync | I |

### 4.8 Análisis financiero y recomendaciones

| ID | Requisito | Estado |
|----|-----------|--------|
| RF-ANA-01 | Análisis mensual (RPC + fallback Room) | I |
| RF-ANA-02 | Lista de deudores | I |
| RF-ANA-03 | Recomendaciones rule-based (cobranza, margen, stock, caída, gasto) | P |
| RF-ANA-04 | El módulo SHALL comportarse como **analista de negocio**: interpretar estadísticas, priorizar acciones y guiar al dueño/gerente hacia crecimiento | F (Must #4) |
| RF-ANA-05 | Recomendaciones accionables con impacto estimado y seguimiento | F (Must #4) |

### 4.9 Reportes

| ID | Requisito | Estado |
|----|-----------|--------|
| RF-REP-01 | Reportes por periodo Diario/Semanal/Mensual/Anual con KPIs de ventas/pagos | I |
| RF-REP-02 | Exportación PDF básica | I / P |
| RF-REP-03 | Reportes de pacientes (altas, retención, demografía) | F (Must #5) |
| RF-REP-04 | Reportes de ventas detallados y cuadros comparativos entre periodos | F (Must #5) |
| RF-REP-05 | Reportes de inventario (rotación, stock, monturas) | F (Must #5) |
| RF-REP-06 | Estadísticas clínicas (emetropías y diagnósticos, lentes, monturas asociadas) | F (Must #5) |

### 4.10 Sincronización y conflictos

| ID | Requisito | Estado |
|----|-----------|--------|
| RF-SYN-01 | Offline-first: lecturas/escrituras locales en Room | I |
| RF-SYN-02 | Sync orquestado en 8 módulos: pacientes → historial → finanzas → proveedores → OC → KPIs locales → inventario → inventario físico | I |
| RF-SYN-03 | Upload finanzas: padres antes que pagos; gate de padres remotos | I |
| RF-SYN-04 | Conflictos LWW por `updated_at`; UI de resolución (keep mine / accept cloud / merge) | I |
| RF-SYN-05 | Post-save sync debounced; sync silencioso al entrar a main | I |
| RF-SYN-06 | JWT: margen 300s pre-sync; retry con refresh en 401 | I |
| RF-SYN-07 | Indicadores claros de datos stale / última sync exitosa | P |

### 4.11 Configuración y administración

| ID | Requisito | Estado |
|----|-----------|--------|
| RF-CFG-01 | Config fiscal, laboratorio, horarios, seguridad PIN | I |
| RF-CFG-02 | Asignación de roles por email | I |
| RF-CFG-03 | Backup/restore JSON (admin) + backup SQLite automático local | I |
| RF-CFG-04 | Diagnóstico de sync | I |
| RF-CFG-05 | Suscripción / compra Play (path existente) | P (sin verificación real Play API) |

### 4.12 OptoWeb (fase posterior al MVP Android)

| ID | Requisito | Estado |
|----|-----------|--------|
| RF-WEB-01 | Auth, dashboard, pacientes, evaluaciones, dispensaciones, servicios, inventario monturas, cierre, reportes, configuración | I |
| RF-WEB-02 | Agenda editable (hoy botones no funcionales) | P / F |
| RF-WEB-03 | Proveedores, OC, gastos, inventario físico | F (Should tras #6) |
| RF-WEB-04 | Paridad operativa razonable con Android para backoffice | F (Must fase Web #7) |

### 4.13 Multi-tenant

| ID | Requisito | Estado |
|----|-----------|--------|
| RF-MT-01 | Toda entidad de negocio aislada por `optica_id` (o vía padre FK) | I / P |
| RF-MT-02 | RLS en tablas públicas multi-tenant | I |
| RF-MT-03 | Contexto de óptica activa en sesión Android y Web | I |
| RF-MT-04 | Auditoría end-to-end: DAOs, sync, RPCs, backups, UI, roles — sin fugas cross-tenant | F (Must #6) |
| RF-MT-05 | Pruebas/regresión que fallen si una query omite `optica_id` donde corresponde | F (Must #6) |

### 4.14 Multi-sucursal avanzada, iOS, suscripciones, facturación

| ID | Requisito | Estado | Roadmap |
|----|-----------|--------|---------|
| RF-MS-01 | Gestión avanzada de cadena multi-sucursal (políticas, consolidado, transferencia) | F | #7 |
| RF-IOS-01 | Cliente iOS con capacidades clínicas/operativas core | F | #10 Could |
| RF-SUB-01 | Suscripciones reales SaaS (mensual/semestral/anual) con enforcement servidor | F | #9 |
| RF-SUB-02 | Precio de referencia inicial: **S/ 30 mensuales** (hipótesis; ajustar tras validación) | F | #9 |
| RF-FAC-01 | Facturación electrónica según normativa peruana aplicable | F | #11 último |

---

## 5. Requisitos no funcionales

| ID | Categoría | Requisito | Estado |
|----|-----------|-----------|--------|
| RNF-01 | Disponibilidad offline | Operación clínica/comercial diaria sin red; sync al recuperar | I |
| RNF-02 | Integridad | No perder pagos/OT por orden de sync incorrecto; padres antes que hijos | I / P |
| RNF-03 | Seguridad | Sin service role en cliente; anon key vía config local; PIN cifrado | I |
| RNF-04 | Privacidad | No loguear email/PII en producción | I / P (revisar resto de logs) |
| RNF-05 | Multi-tenant | Imposible leer/escribir datos de otra óptica en camino correcto | I / Must reforzar #5 |
| RNF-06 | Rendimiento sync | Orquestador con timeout acotado; chunks de upload | I |
| RNF-07 | API | `max_rows` PostgREST 1000 — sync SHALL paginar cuando aplique | I / P |
| RNF-08 | Usabilidad | UI en español; flujos principales ≤ pocos taps desde drawer | I |
| RNF-09 | Calidad | Suite unit tests + umbral JaCoCo instrucción ≥ 30% (gate Gradle) | I |
| RNF-10 | Observabilidad | Telemetría de sync; errores visibles no silenciosos en finanzas | I / P |
| RNF-11 | Portabilidad | Android 7+; Web navegadores modernos | I |
| RNF-12 | Cumplimiento | Alinear con normativa peruana de datos/salud cuando se complete §11 | F |

---

## 6. Interfaces externas

### 6.1 Interfaz de usuario

- **Android:** Jetpack Compose, drawer por secciones (Gestión, Agenda, Inventario, Finanzas, Sistema).
- **Web:** App shell Next.js con navegación principal (dashboard, pacientes, agenda, inventario, operación, cierre, reportes, config).

### 6.2 Interfaces de software

| Interfaz | Descripción |
|----------|-------------|
| Supabase Auth | Email/password, OAuth, JWT ~1h, refresh rotation |
| PostgREST | CRUD tablas + RPC (`recalcular_resumen_diario`, `rpc_analisis_mensual`, `rpc_cierre_caja_resumen`, `rpc_deudores`, `assign_optica_role_by_email`, `create_optica_for_current_user`, `paciente_eliminaciones_restantes_hoy`, `check_rate_limit`, …) |
| Edge Functions | `track-release` (CI→`app_releases`); `verify-purchase` (plan Play — verificación incompleta) |
| Deep links | `optoapp://auth` |
| Play Billing | Path de compra (TO-BE #9 para hacerlo real) |

### 6.3 Interfaces de comunicación

- HTTPS hacia Supabase.
- Banner offline en Android cuando no hay red.
- Realtime: publicación en `usuario_optica` (membresías).

---

## 7. Modelo de datos (resumen)

### 7.1 Principios

- Tenant key: `optica_id` (texto).
- Timestamps: `created_at` / `updated_at` (UTC) para LWW.
- Room DB v54 ≈ 32 entidades locales; Postgres público ≈ 39 tablas con RLS.
- Hijos sin `optica_id` propio (p.ej. ítems OC) se aíslan vía FK al padre.

### 7.2 Dominios de entidades

| Dominio | Entidades principales |
|---------|----------------------|
| Identidad | `opticas`, `usuario_optica`, `user_profiles`, `optica_settings` |
| Clínica | `pacientes`, `evaluaciones` |
| Ventas | `dispensaciones`, `dispensacion_items`, `servicios_extra`, `servicio_extra_items`, `pagos`, regalos_* |
| Inventario | `monturas`, `montura_movimientos`, `proveedores`, `ordenes_compra`, `inventario_fisico*` |
| Finanzas | `resumen_diario`, `gastos_operativos`, `costos_*`, `configuracion_financiera`, `cierres_caja` |
| Sync local | `sync_entity_state`, `conflict_records`, `sync_telemetry_*` |

### 7.3 Tablas locales no (o poco) sincronizadas

Documentar y decidir en TO-BE: p.ej. `costos_lc`, parte de settings/feedback — riesgo de divergencia multi-dispositivo.

### 7.4 Ledger de pagos

Tipos reconocidos: `Abono`, `Pago completo`, `Reembolso`, `Reverso`, `Anulación`. Efecto firmado vía `pago_effect` en servidor. Estados OT: `Pendiente`, `Entregado`, `Anulado`, `Reclamada`.

---

## 8. Reglas de negocio

### 8.1 Reglas operativas

- Edición de paciente: UPDATE (no delete+insert) para preservar historial/FK.
- Soft-delete / anulación preferida frente a hard-delete donde haya stock/ledger.
- Delete paciente: admin/gerente + cuota 10/día.
- FREE: 1 óptica máxima para creador.

### 8.2 Reglas clínicas (diagnóstico)

Fuente alineada con `docs/sdd/spec.md` (mantener como contrato clínico):

- Notación de cilindro negativa.
- E=0,C=0 → Emetropía; E\<0,C=0 → Miopía; E\>0,C=0 → Hipermetropía; combinaciones de astigmatismo simple/compuesto/mixto según E, C y (E+C).
- `plano`/`neutro` → 0.00 D; `balance` → diagnóstico Balance.
- Presbicia: ADD \> 0.
- Anisometropía: \|EE OD − EE OI\| ≥ 2.00 D (excl. Balance).
- Ambliopía (auto, AV cc lejos OD vs OI):
  - **Umbral:** `|logMAR_OD − logMAR_OI| ≥ 0.19` (≈ 2 líneas / 0.2 logMAR).
  - **Normalización ambidireccional (requisito):** cada ojo SHALL aceptar **ambos** formatos de entrada y convertir a logMAR antes de comparar:
    - **Snellen fraccional:** `20/20`, `20/40`, `20 / 40` → `logMAR = −log10(numerador/denominador)`.
    - **Decimal:** `1.0`, `0.8`, `0.5` (coma o punto) → `logMAR = −log10(decimal)` (valores en (0, ∞)).
  - Si un ojo no es parseable en ninguno de los dos formatos, no se fuerza auto; se conserva el valor manual (`otrosAmbliopia`).
  - **AS-IS:** solo Snellen está implementado (`parseSnellenToLogMar`). Decimal aún no dispara el auto-cálculo → estado **P**; completar en roadmap TO-BE **#1**.
- LC por \|K1−K2\|: \<2.50 blando; 2.50–3.99 valorar RGP/tórico; ≥4.00 RGP.

### 8.3 Sync

- Un pipeline a la vez (mutex); timeout de orquestador.
- Conflictos: cuarentena + UI; no re-subir entidades conflictivas hasta resolver.
- Download tras upload por defecto para absorber timestamps de servidor.

---

## 9. Criterios de aceptación — MVP Android

| # | Criterio | Verificación |
|---|----------|--------------|
| CA-01 | Login + PIN + óptica activa | Flujo manual / tests auth |
| CA-02 | Alta paciente → evaluación → OT con pago offline | Flujo manual sin red |
| CA-03 | Sync full restaura/espeja en segundo dispositivo misma óptica | Dos devices o Room+remoto |
| CA-04 | Usuario de óptica A no ve datos de B | Prueba multi-tenant básica |
| CA-05 | Cierre de caja y reportes periodo muestran totales coherentes con pagos no anulados | Comparar con ledger |
| CA-06 | Inventario montura descuenta en venta y refleja movimiento | Stock antes/después |
| CA-07 | Suite `testDebugUnitTest` verde en CI | GitHub Actions |

**No exigible para declarar MVP:** RF-DIS-07/08 maduros, RF-ANA-04, RF-REP-03..06, RF-MT-04 exhaustivo, Web, iOS, facturación, suscripciones reales.

---

## 10. Roadmap TO-BE (orden de implementación)

Orden acordado por el autor. Cada ítem Must salvo que se indique Could.

| # | Tema | Requisitos ancla | Notas |
|---|------|------------------|-------|
| **1** | AV ambliopía ambidireccional (Snellen + decimal → logMAR) | RF-EVA-06, §8.2 | Completar parseo decimal; tests de equivalencia; alinear Web si aplica |
| **2** | Anulaciones / devoluciones / reclamos | RF-DIS-05..08, RF-SER-03 | Madurar UI + stock + ledger + reportes |
| **3** | Costos y gastos | RF-FIN-03..05 | Aterrizar modelo; alimentar márgenes |
| **4** | Análisis financiero “analista de negocio” | RF-ANA-03..05 | Ir más allá de reglas simples actuales |
| **5** | Reportes ricos | RF-REP-03..06 | Pacientes, ventas, inventario, comparativos, emetropías, monturas, lentes |
| **6** | Multitenant end-to-end | RF-MT-04..05, RF-SEC-01 | DAOs, sync, RLS, UI, backups, roles |
| **7** | OptoWeb | RF-WEB-02..04 | Tras MVP Android + Must 1–6 en curso/hecho según prioridad |
| **8** | Multi-sucursal avanzada | RF-MS-01 | Cadena / consolidado |
| **9** | Suscripciones reales SaaS | RF-SUB-01..02 | Mensual / semestral / anual; hyp. S/ 30/mes |
| **10** | iOS | RF-IOS-01 | **Could** |
| **11** | Facturación electrónica | RF-FAC-01 | **Último**; depende normativa Perú |

---

## 11. Cumplimiento y privacidad (Perú) — TBD

| ID | Tema | Estado |
|----|------|--------|
| CMP-01 | Ley de Protección de Datos Personales (Perú) y reglamentos aplicables | **Investigar** |
| CMP-02 | Tratamiento de datos de salud / historias clínicas | **Investigar** |
| CMP-03 | Conservación, consentimiento, derechos ARCO / equivalentes | **Investigar** |
| CMP-04 | Facturación electrónica (SUNAT / PSE) — solo relevante en #11 | **Investigar** al abordar #11 |
| CMP-05 | Política de privacidad y términos del SaaS | **Redactar** antes de cobro real (#9) |

El SRS **no inventa** obligaciones legales. Antes de cobro a clientes reales (#9) y de facturación (#11), el autor SHALL completar esta investigación y actualizar el SRS.

---

## 12. Modelo de negocio (producto)

| Aspecto | Decisión actual |
|---------|-----------------|
| Modelo | SaaS de pago |
| Ciclos | Mensual, semestral, anual |
| Precio de referencia | **S/ 30 / mes** (hipótesis; semestral/anual TBD con descuento) |
| Enforcement | Hoy parcial (planes en `opticas`, `verify-purchase` incompleto) → Must en #9 |
| Aprobador comercial | Autor único |

---

## 13. Atributos de calidad de los requisitos

- Cada RF/RNF tiene ID estable para trazabilidad en issues/PRs/OpenSpec.
- Estados I/P/F permiten distinguir deuda de alcance nuevo.
- Cambios de comportamiento de producto: actualizar este SRS **antes o junto** con la implementación (no solo OpenSpec).

---

## 14. Glosario rápido

| Término | Significado |
|---------|-------------|
| Dispensación | Venta/OT óptica con ítems y pagos |
| Servicio extra | Venta/servicio fuera del flujo OT clásico |
| Reverso | Pago que anula el efecto de otro pago |
| Reclamo | Estado OT + posible reembolso |
| Óptica activa | Tenant seleccionado en sesión |
| OpenSpec | Artefactos SDD de cambios incrementales |

---

## 15. Historial del documento

| Versión | Fecha | Cambios | Autor |
|---------|-------|---------|-------|
| 1.0 | 2026-09-24 | Primera versión completa AS-IS (código) + TO-BE acordado | Desarrollador único |
| 1.0.1 | 2026-09-25 | Ambliopía: requisito parseo ambidireccional Snellen + decimal → logMAR (RF-EVA-06); AS-IS solo Snellen | Desarrollador único |
| 1.0.2 | 2026-09-25 | Roadmap: AV ambliopía ambidireccional como TO-BE **#1**; resto corre +1 (11 ítems) | Desarrollador único |
| 1.0.3 | 2026-10-01 | RF-EVA-06 implementado en Android (Snellen + decimal → logMAR); OptoWeb diferido hasta cerrar el MVP Android | Desarrollador único |

---

## 16. Aprobación

| Rol | Nombre | Fecha | Firma |
|-----|--------|-------|-------|
| Autor / Product Owner | Desarrollador único | 2026-09-24 | Auto-aprobado |

*Fin del SRS v1.0.*
