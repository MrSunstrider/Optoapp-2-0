# OptoApp SaaS — Product Requirements Document (PRD)

> **Versión:** 1.1  
> **Fecha:** 2026-06-22  
> **Estado:** En desarrollo activo  
> **Repositorio:** [Optoapp-2-0](https://github.com/MrSunstrider/Optoapp-2-0)

---

## Índice

1. [Resumen Ejecutivo](#1-resumen-ejecutivo)
2. [Propósito y Visión](#2-propósito-y-visión)
3. [Mercado Objetivo y Usuarios](#3-mercado-objetivo-y-usuarios)
4. [Plataformas](#4-plataformas)
5. [Historias de Usuario y Features](#5-historias-de-usuario-y-features)
6. [Requisitos Funcionales por Módulo](#6-requisitos-funcionales-por-módulo)
7. [Requisitos No Funcionales](#7-requisitos-no-funcionales)
8. [Arquitectura del Sistema](#8-arquitectura-del-sistema)
9. [Modelo de Datos](#9-modelo-de-datos)
10. [Seguridad y Cumplimiento](#10-seguridad-y-cumplimiento)
11. [Roles y Permisos](#11-roles-y-permisos)
12. [Métrica de Éxito y KPIs](#12-métrica-de-éxito-y-kpis)
13. [Roadmap y Estado Actual](#13-roadmap-y-estado-actual)
14. [Restricciones Técnicas](#14-restricciones-técnicas)
15. [Glosario](#15-glosario)

---

## 1. Resumen Ejecutivo

OptoApp SaaS es un sistema de gestión clínica integral para ópticas y optometristas del mercado latinoamericano. Opera **offline-first** en dispositivos Android en el punto de atención, con una extensión web (OptoWeb) para backoffice, reporting gerencial y operación administrativa. El backend es Supabase (PostgreSQL + Auth + RLS), actuando como fuente única de verdad sincronizada.

El producto cubre el ciclo completo de atención optométrica: desde la admisión del paciente, la evaluación clínica (con 100+ campos y reglas de diagnóstico automatizadas), la dispensación de lentes/monturas, el control de inventario, hasta el cierre financiero diario y los reportes gerenciales.

**Estado actual:** Desarrollo activo. Android en producción (v1.8.0), Web en fase de hardening pre-release (P4-T5), 76 migraciones de base de datos, CI/CD implementado para Android y Supabase. App completamente gratuita (sin límites de suscripción).

---

## 2. Propósito y Visión

### 2.1 Problema

Las ópticas independientes y cadenas pequeñas en Latinoamérica carecen de un sistema moderno, asequible y offline-ready para gestionar sus operaciones. Las soluciones existentes son:
- **Demasiado caras** (suscripciones en USD con funciones que no se usan)
- **Requieren conexión permanente** (inviable en zonas con conectividad intermitente)
- **No están adaptadas** a la práctica optométrica latinoamericana (formularios, nomenclatura clínica, formatos de receta)
- **Sin integración móvil-web** (el óptico en mostrador y el gerente en oficina no comparten la misma base de datos)

### 2.2 Visión

Ser la plataforma estándar de gestión optométrica en Latinoamérica, donde:
- El optometrista evalúa y dispensa sin preocuparse por la conectividad
- El gerente ve KPIs financieros en tiempo real desde la web
- El dueño de cadena administra múltiples sucursales desde un solo panel
- Los datos clínicos viajan seguros entre dispositivos sin pérdida ni corrupción
- El modelo SaaS lo hace accesible a ópticas de cualquier tamaño

### 2.3 Principios rectores

| Principio | Implicancia |
|-----------|-------------|
| **Offline-first** | La app funciona sin internet. Room es el almacén de trabajo; Supabase es el espejo remoto. |
| **Multi-tenant por diseño** | Toda entidad de negocio aislada por `optica_id`. |
| **Seguridad por defecto** | RLS obligatorio, PIN de 6 dígitos, cifrado de preferencias sensibles. |
| **Integridad clínica** | Reglas de diagnóstico implementadas exactamente según especificación. |
| **Evolución controlada** | Todo cambio significativo pasa por SDD (spec → plan → tasks). |

---

## 3. Mercado Objetivo y Usuarios

### 3.1 Mercado

- **Geografía:** Latinoamérica (Argentina, Uruguay, Chile, Perú, Colombia, México)
- **Segmento:** Ópticas independientes, cadenas pequeñas/medianas (2-20 sucursales), consultorios optométricos
- **Idioma:** Español (interfaz y documentación)

### 3.2 Arquetipos de Usuario

| Arquetipo | Descripción | Necesidad principal |
|-----------|-------------|---------------------|
| **Optometrista** | Profesional de la salud visual que realiza evaluaciones | Herramienta clínica rápida, precisa, offline |
| **Asesor/Vendedor** | Atención al cliente en mostrador, dispensación | Catálogo de monturas, seguimiento de OT, pagos |
| **Gerente de óptica** | Supervisa operación diaria, personal, finanzas | KPIs, cierre de caja, reportes, control de inventario |
| **Administrador** | Dueño o administrador general | Backup/restore, asignación de roles, config fiscal, multi-sucursal |
| **Invitado** | Acceso temporal o de prueba | Visibilidad limitada sin capacidad de operación |

### 3.3 Volumen estimado por óptica

| Métrica | Estimado |
|---------|----------|
| Pacientes activos | 200-5,000 |
| Evaluaciones/mes | 50-500 |
| Dispensaciones/mes | 30-300 |
| Usuarios por óptica | 1-15 |

---

## 4. Plataformas

| Plataforma | Stack | Propósito | Estado |
|------------|-------|-----------|--------|
| **Android** | Kotlin + Jetpack Compose + Hilt + Room + supabase-kt | Punto de atención offline-first, operación clínica diaria | ✅ Producción (v1.8.0) |
| **Web (OptoWeb)** | Next.js 15 App Router + TypeScript 6 + Tailwind CSS 4 + Supabase SSR | Backoffice, reporting, dashboard gerencial, administración | 🔲 Hardening pre-release (P4-T5) |
| **Backend** | Supabase (PostgreSQL 17 + Auth + RLS + Edge Functions) | Fuente única de verdad, sincronización, reglas de negocio compartidas | ✅ Producción |

---

## 5. Historias de Usuario y Features

### 5.1 Epic: Gestión de Pacientes

| Historia | Prioridad | Estado |
|----------|-----------|--------|
| Como optometrista, quiero registrar un paciente con sus datos demográficos para tener su historial clínico | P0 | ✅ |
| Como optometrista, quiero buscar pacientes por nombre, documento o número de HO para acceder rápidamente | P0 | ✅ |
| Como optometrista, quiero editar los datos del paciente preservando el historial clínico | P0 | ✅ |
| Como admin, quiero eliminar pacientes con guardrails (límite diario, solo admin/gerente, auditoría) | P1 | ✅ |
| Como optometrista, quiero que el sistema sugiera el próximo número de HO para mantener la numeración correlativa | P1 | ✅ |

### 5.2 Epic: Evaluación Clínica

| Historia | Prioridad | Estado |
|----------|-----------|--------|
| Como optometrista, quiero realizar una evaluación visual completa con AV, refracción subjetiva/objetiva, y diagnóstico automático | P0 | ✅ |
| Como optometrista, quiero que el sistema calcule automáticamente el diagnóstico basado en esfera, cilindro y eje | P0 | ✅ |
| Como optometrista, quiero registrar datos de queratometría y sugerencia de lente de contacto | P1 | ✅ |
| Como optometrista, quiero registrar pruebas complementarias (cover test, motilidad, etc.) | P1 | ✅ |
| Como optometrista, quiero registrar datos de contactología (adaptación, marca, parámetros) | P1 | ✅ |
| Como optometrista, quiero que el sistema detecte anisometropía y ambliopía automáticamente | P1 | ✅ |
| Como optometrista, quiero calcular el ADD de presbicia automáticamente | P1 | ✅ |

### 5.3 Epic: Dispensación y Órdenes de Trabajo

| Historia | Prioridad | Estado |
|----------|-----------|--------|
| Como vendedor, quiero crear una orden de trabajo (OT) con lente, montura y servicios | P0 | ✅ |
| Como vendedor, quiero registrar múltiples ítems por dispensación (lentes + montura + armazón) | P0 | ✅ |
| Como vendedor, quiero registrar pagos parciales contra una dispensación | P0 | ✅ |
| Como vendedor, quiero consultar el estado de la OT (pendiente, en taller, entregado, anulado) | P0 | ✅ |
| Como vendedor, quiero generar un ticket/talón de laboratorio para el taller | P1 | ✅ |

### 5.4 Epic: Inventario de Monturas

| Historia | Prioridad | Estado |
|----------|-----------|--------|
| Como asesor, quiero registrar monturas con SKU, color, talle, material para controlar stock | P1 | ✅ |
| Como asesor, quiero registrar movimientos de inventario (ingreso, venta, ajuste) con trazabilidad | P1 | ✅ |
| Como gerente, quiero ver alertas de stock crítico para reposición | P2 | ✅ |
| Como asesor, quiero consultar disponibilidad de una montura desde la dispensación | P1 | ✅ |

### 5.5 Epic: Servicios Extra

| Historia | Prioridad | Estado |
|----------|-----------|--------|
| Como vendedor, quiero registrar ventas o servicios no ligados a una dispensación (ej. solución, accesorios) | P1 | ✅ |
| Como vendedor, quiero registrar pagos contra servicios extra | P1 | ✅ |

### 5.6 Epic: Gestión Financiera

| Historia | Prioridad | Estado |
|----------|-----------|--------|
| Como gerente, quiero hacer el cierre de caja diario con totales por método de pago | P0 | ✅ |
| Como gerente, quiero ver KPIs del dashboard (ingresos hoy/semana/mes, pacientes atendidos, etc.) | P0 | ✅ |
| Como especialista, quiero ver reportes financieros (diario, semanal, mensual, anual) | P0 | ✅ |
| Como gerente, quiero exportar reportes para contabilidad externa | P2 | ⏳ Parcial |

### 5.7 Epic: Sincronización y Offline

| Historia | Prioridad | Estado |
|----------|-----------|--------|
| Como optometrista, quiero trabajar sin internet y que los datos se sincronicen automáticamente al recuperar conexión | P0 | ✅ |
| Como admin, quiero ver el estado de sincronización (pendiente/synced/error) por entidad | P1 | ✅ |
| Como operador, quiero que el orden de sync respete dependencias (paciente antes que evaluación) | P0 | ✅ |

> **Estrategia de conflictos:** Last Write Wins puro — no hay download guard ni conflict helper. El último upload pisa al anterior en Supabase. Implementado así porque el guard anterior creaba un ciclo vicioso: download bloqueaba entidades conflictivas → nunca recibían timestamp del server → upload re-detectaba conflicto → loop infinito.

### 5.8 Epic: Administración y Configuración

| Historia | Prioridad | Estado |
|----------|-----------|--------|
| Como admin, quiero configurar los datos fiscales de la óptica (nombre, CUIT, dirección, etc.) | P0 | ✅ |
| Como admin, quiero gestionar usuarios y asignar roles dentro de mi óptica | P1 | ✅ |
| Como admin, quiero hacer backup y restore de los datos de mi óptica | P1 | ✅ |
| Como admin, quiero crear y gestionar sucursales | P1 | ✅ |
| Como admin, quiero ver el plan de suscripción actual y límites | P1 | ✅ |

### 5.9 Epic: Suscripciones y Monetización

| Historia | Prioridad | Estado |
|----------|-----------|--------|
| Como dueño, quiero que la app sea completamente gratuita sin límites | P0 | ✅ |
| Como dueño, quiero facturación integrada por Google Play Store (legacy) | P1 | ✅ |
| Como sistema, quiero que el plan FREE sea ilimitado (sin tope de pacientes/ópticas) | P1 | ✅ |

> **Nota:** Desde v1.8.0 la app es 100% gratuita — `Int.MAX_VALUE` pacientes, sin límite de ópticas. El esqueleto de suscripciones y Billing Library se mantiene para futura monetización si aplica.

### 5.10 Epic: Web (OptoWeb)

| Historia | Prioridad | Estado |
|----------|-----------|--------|
| Como gerente, quiero loguearme en la web con mi cuenta de Supabase | P0 | ✅ |
| Como gerente, quiero seleccionar mi óptica activa al iniciar sesión | P0 | ✅ |
| Como gerente, quiero ver el dashboard con KPIs reales | P0 | ✅ |
| Como admin, quiero gestionar pacientes desde la web (CRUD con búsqueda, filtros, paginación) | P0 | ✅ |
| Como admin, quiero editar la configuración fiscal de la óptica | P0 | ✅ |
| Como especialista, quiero ver reportes financieros con filtro por período | P0 | ✅ |
| Como gerente, quiero gestionar evaluaciones desde la web (5 tabs + automatismos) | P1 | ✅ |
| Como gerente, quiero gestionar dispensaciones desde la web (OT, stock, pagos/anulaciones) | P1 | ✅ |
| Como gerente, quiero gestionar servicios extra desde la web | P1 | ✅ |

---

## 6. Requisitos Funcionales por Módulo

### 6.1 Evaluación Clínica

**Cobertura de campos:** 100+ campos organizados en secciones:

| Sección | Contenido |
|---------|-----------|
| **Motivo de consulta** | Texto libre, tipo de consulta |
| **Antecedentes** | Patológicos, quirúrgicos, medicamentos, lentes anteriores, herencia |
| **Agudeza Visual** | Sin corrección, con corrección (SC/CC), agujero estenopeico — para cada ojo |
| **Refracción Objetiva** | Esfera, cilindro, eje (por ojo) |
| **Refracción Subjetiva** | Esfera, cilindro, eje, AV resultante (por ojo) |
| **Balance binocular** | Prisma horizontal/vertical, foria, test de Worth |
| **Queratometría** | K1, K2, eje, astigmatismo corneal, sugerencia de LC |
| **Contactología** | Marca, parámetros, adaptación, seguimiento |
| **Pruebas complementarias** | Cover test, motilidad, pupilas, campimetría por confrontación, test de Hirschberg |
| **Diagnóstico automático** | Ver reglas clínicas en sección 9 |
| **ADD / Presbicia** | Cálculo automático basado en edad y amplitud de acomodación |
| **OSDI** | Cuestionario de ojo seco con puntuación automática |
| **Notas** | Campo libre para el profesional |

### 6.2 Reglas de Diagnóstico Automático

| Condición (E=esfera, C=cilindro) | Diagnóstico |
|----------------------------------|-------------|
| E = 0 y C = 0 | Emetropía |
| E < 0 y C = 0 | Miopía |
| E > 0 y C = 0 | Hipermetropía |
| E = 0 y C < 0 | Astigmatismo miópico simple |
| E < 0 y C < 0 | Astigmatismo miópico compuesto |
| E > 0 y C < 0 y (E+C) > 0 | Astigmatismo hipermetrópico compuesto |
| E > 0 y C < 0 y (E+C) <= 0 | Astigmatismo mixto |
| E < 0 y C < 0 y (E+C) >= 0 | Astigmatismo mixto |

**Reglas adicionales:**
- Esfera `plano`/`neutro` → interpretar como 0.00 D
- Esfera `balance` → diagnóstico = **Balance**
- **Presbicia**: activa si ADD > 0
- **Anisometropía**: diferencia de equivalente esférico >= 2.00 D (excluyendo ojos Balance)
- **Ambliopía**: diferencia de AV con corrección >= 2 líneas (0.2 logMAR)

**Sugerencia de lente de contacto por astigmatismo corneal:**
| Diferencia K1-K2 | Sugerencia |
|------------------|------------|
| < 2.50 D | Lente blando |
| 2.50 a 3.99 D | Valorar RGP / Tórico |
| >= 4.00 D | Lente RGP |

### 6.3 Dispensación

- Órdenes de trabajo (OT) con múltiples ítems por dispensación
- Tipos de ítem: lente (monofocal, bifocal, progresivo), montura, armazón, tratamiento
- Estados: Pendiente → En Taller → Entregado / Anulado
- Pagos: parciales o totales, por método (efectivo, tarjeta, transferencia, etc.)
- Anulación con reversión de pagos y devolución a stock
- Ticket/talón de laboratorio (PDF)

### 6.4 Sincronización Offline-First

**Orden obligatorio de sincronización:**
1. Pacientes
2. Evaluaciones (requiere paciente remoto)
3. Dispensaciones (requiere paciente remoto)
4. ServiciosExtra
5. Pagos (requiere dispensación/servicio remoto)

**Estrategia:**
- Política de conflicto: **Last Write Wins** puro — no hay detección de conflictos ni download guard
- Upload: todas las entidades locales se suben directamente a Supabase sin filtro de conflictos
- Download: todas las entidades remotas se descargan e insertan en Room sin bloqueo por conflictos previos
- Reintentos: backoff exponencial (400ms, 800ms, 1200ms) para fallos de red transitorios
- Estado local: `pending / synced / error` por fila
- Observabilidad: telemetría en `sync_telemetry_optica` + UI de diagnóstico en Configuración
- Cancelaciones de corrutina (`CancellationException`) no se registran como error de negocio
- La sync pesada se ejecuta en scope de aplicación, no de pantalla

---

## 7. Requisitos No Funcionales

### 7.1 Rendimiento

| Requisito | Objetivo |
|-----------|----------|
| Carga de lista de pacientes | < 1 segundo (1000 registros locale) |
| Apertura de evaluación existente | < 2 segundos |
| Sincronización completa (100 pacientes + evaluaciones) | < 30 segundos en 4G |
| Dashboard web (KPIs mensuales) | < 3 segundos |
| APK Android | < 30 MB |

### 7.2 Disponibilidad y Offline

- Operación 100% funcional sin conexión a internet
- Sincronización automática al recuperar conectividad (background)
- Sin pérdida de datos en cortes de energía o cierres forzados de app

### 7.3 Seguridad

- Autenticación: email/password + Google OAuth (Android)
- Segundo factor: PIN de 6 dígitos cifrado con `EncryptedSharedPreferences`
- RLS obligatorio en todas las tablas multi-tenant
- Sin exposición de `service_role` en cliente
- Backup/restore: solo admin, con verificación de óptica origen

### 7.4 Portabilidad

- Android: minSdk 24, targetSdk 36
- Web: navegadores modernos (Chrome, Firefox, Edge, Safari últimos 2 versiones)
- Backend: PostgreSQL 17 (Supabase)

### 7.5 Mantenibilidad

- Clean Architecture en Android (data/domain/presentation)
- MVVM con ViewModel + StateFlow
- Version catalog centralizado (`gradle/libs.versions.toml`)
- Migraciones de base de datos inmutables (no modificar, solo agregar)
- Proceso SDD para cambios significativos

### 7.6 Pruebas

| Capa | Herramienta | Cobertura mínima |
|------|-------------|------------------|
| Android unit tests | JUnit 4 + Robolectric + MockK | 5% instrucciones (JaCoCo) |
| Web unit tests | Vitest 4 | 20% statements, 15% branches, 25% functions |
| CI Android | GitHub Actions | `testDebugUnitTest` + `assembleDebug` |
| CI Supabase | GitHub Actions | `supabase db lint` + `db diff` |

---

## 8. Arquitectura del Sistema

### 8.1 Diagrama conceptual

```
┌─────────────────────────────────────────────────────────────┐
│                      Supabase (Cloud)                        │
│  ┌──────────┐  ┌──────────┐  ┌───────────┐  ┌───────────┐  │
│  │ PostgreSQL│  │   Auth   │  │    RLS    │  │  Edge     │  │
│  │  + RLS    │  │  (JWT)   │  │ Policies  │  │ Functions │  │
│  └────┬─────┘  └──────────┘  └───────────┘  └───────────┘  │
│       │                      ▲                              │
└───────┼──────────────────────┼──────────────────────────────┘
        │ sync (supabase-kt)   │ SSR (@supabase/ssr)
        ▼                      │
┌──────────────────┐    ┌──────┴──────────────┐
│   Android App    │    │    OptoWeb (Next.js) │
│  ┌────────────┐  │    │  ┌───────────────┐  │
│  │ Room (local)│  │    │  │ Server Actions │  │
│  ├────────────┤  │    │  ├───────────────┤  │
│  │ ViewModel  │  │    │  │ Server Components│ │
│  ├────────────┤  │    │  ├───────────────┤  │
│  │ Compose UI │  │    │  │ Client Components│ │
│  └────────────┘  │    │  └───────────────┘  │
└──────────────────┘    └────────────────────┘
```

### 8.2 Stack Detallado

#### Android

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Kotlin 2.2 |
| UI | Jetpack Compose + Material 3 + Material Icons Extended |
| Arquitectura | MVVM (ViewModel + StateFlow) |
| Persistencia local | Room 2.8 |
| DI | Hilt 2.59 + KSP |
| Red | Ktor CIO + supabase-kt 3.6 + kotlinx.serialization |
| Navegación | Navigation Compose 2.7 |
| Seguridad | EncryptedSharedPreferences, Security Crypto |
| Facturación | Billing Library 7.1 |
| Build | Gradle 8.x, AGP 9.1, KSP |
| SDK | compileSdk 36, minSdk 24, targetSdk 36 |

#### Web (OptoWeb)

| Capa | Tecnología |
|------|-----------|
| Framework | Next.js 16 (App Router) |
| Lenguaje | TypeScript 6 |
| UI | Tailwind CSS 4 + shadcn/ui + Lucide React |
| Auth | @supabase/ssr 0.10 (cookies httpOnly) |
| Backend client | @supabase/supabase-js 2.49 |
| Validación | Zod 4 |
| Testing | Vitest 4 + @vitest/coverage-v8 |
| Linting | ESLint 9 (next/core-web-vitals) |
| PDF | pdf-lib |
| Despliegue | Vercel (recomendado) |

#### Backend (Supabase)

| Componente | Detalle |
|-----------|---------|
| Base de datos | PostgreSQL 17 |
| Migraciones | 76 (formato `YYYYMMDDHHMMSS_desc.sql`) |
| Auth | email/password + Google OAuth |
| RLS | Policies por `optica_id` + rol |
| Edge Functions | Deno 2 (1 activa: `track-release`) |
| Pooler | Transaction mode (deshabilitado en local) |

### 8.3 Estrategia Multi-Tenant

- Aislamiento por `optica_id` en todas las tablas de negocio
- RLS policies estrictas: `(select auth.uid())` en lugar de `auth.uid()` para evitar reevaluación por fila
- Funciones `SECURITY DEFINER` en esquema privado `app_private`
- Usuario puede pertenecer a múltiples ópticas (tabla puente `usuario_optica`)
- Contexto activo persiste por sesión (SharedPreferences en Android, cookie httpOnly en Web)

---

## 9. Modelo de Datos

### 9.1 Entidades Principales

| Entidad | Descripción | Sync Order | Multi-tenant |
|---------|-------------|------------|--------------|
| `opticas` | Óptica/tenant | — | — (raíz) |
| `pacientes` | Datos demográficos y contacto | 1 | ✅ `optica_id` |
| `evaluaciones` | Examen visual completo | 2 | ✅ `optica_id` |
| `dispensaciones` | OT con ítems y pagos | 3 | ✅ `optica_id` |
| `dispensacion_items` | Ítems individuales de una OT | 3 | ✅ `optica_id` |
| `servicios_extra` | Servicios no ligados a dispensación | 4 | ✅ `optica_id` |
| `pagos` | Abonos a dispensación o servicio | 5 | ✅ `optica_id` |
| `monturas` | Catálogo de inventario con SKU | Independiente | ✅ `optica_id` |
| `montura_movimientos` | Trazabilidad de inventario | Independiente | ✅ `optica_id` |
| `usuario_optica` | Puente usuario ↔ óptica con rol | — | ✅ `optica_id` |
| `cierres_caja` | Cierre financiero diario | — | ✅ `optica_id` |
| `app_releases` | Versiones de APK para actualización | — | No |
| `user_profiles` | Perfil de usuario | — | No |
| `sync_telemetry_optica` | Telemetría de sincronización | — | ✅ `optica_id` |
| `optica_settings` | Configuración por óptica | — | ✅ `optica_id` |

### 9.2 Convenciones

- **Naming:** `snake_case` en PostgreSQL y DTOs de red, `@SerialName` para mapeo
- **Timestamps:** `created_at`, `updated_at` con `timestamptz` (UTC)
- **Auditoría:** `updated_by` UUID en tablas sensibles, triggers `set_updated_audit_fields()`
- **Nullabilidad:** Compatibilidad estricta entre Room y Supabase (no crear `NOT NULL` sin plan de compatibilidad)
- **Sync status:** `sync_entity_state` en Room con valores `pending`, `synced`, `error`

---

## 10. Seguridad y Cumplimiento

### 10.1 Autenticación

| Método | Estado | Plataforma |
|--------|--------|------------|
| Email + contraseña | ✅ | Android + Web |
| Google OAuth | ✅ | Android |
| Google OAuth | 🔲 | Web (Pendiente) |

- Confirmación de email **deshabilitada** (`enable_confirmations = false`)
- JWT expiry: 1 hora
- Refresh token rotation: habilitado

### 10.2 PIN de Sesión

- PIN obligatorio de 6 dígitos en el ciclo de sesión en Android
- PIN por defecto `123456` (solo desarrollo, debe cambiarse en producción)
- Almacenamiento: `EncryptedSharedPreferences` vía `SecurityManager`/`SessionManager`
- Web: PIN manejado vía cookie httpOnly con HMAC pepper

### 10.3 RLS (Row Level Security)

- Obligatorio en TODAS las tablas multi-tenant
- Policies filtran por `optica_id` = óptica activa del usuario
- Uso de `(select auth.uid())` para evitar plan de consulta ineficiente
- Funciones `SECURITY DEFINER` restringidas a esquema `app_private`

### 10.4 Protecciones Operativas

| Operación | Guardrails |
|-----------|------------|
| Eliminación de pacientes | Solo admin/gerente, límite 10/día, confirmación explícita, auditoría |
| Backup/Restore | Solo admin, verificación de óptica origen |
| Asignación de admin | Solo admin actual |
| Configuración fiscal | Solo admin/gerente |

---

## 11. Roles y Permisos

### 11.1 Matriz Android

| Rol | BI/Reportes | Cierre de caja | Oper. hoy | Export. pend. | Export. cierre | Export. inventario |
|-----|:----------:|:--------------:|:---------:|:------------:|:-------------:|:-----------------:|
| admin | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| especialista | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| gerente | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| asesor/a | ❌ | ❌ | ❌ | ✅ | ❌ | ✅ |
| ventas | ❌ | ❌ | ❌ | ✅ | ❌ | ✅ |
| invitado | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |

### 11.2 Matriz Web (alineada con Android)

| Dominio | Web | Regla |
|---------|-----|-------|
| Contexto activo | Cookie httpOnly | Misma regla que Android |
| Dashboard | ✅ | Filtrado por `optica_id` |
| Reportes/BI | ✅ (roles con permiso) | Guardia server-side + menú por rol |
| Cierre de caja | 🔲 No implementado | Idem Android cuando se implemente |
| Exportaciones | 🔲 No implementado | Misma matriz cuando se implemente |

---

## 12. Métrica de Éxito y KPIs

### 12.1 KPIs de Producto

| KPI | Target | Cómo se mide |
|-----|--------|-------------|
| Tiempo promedio de evaluación | < 5 min | Telemetría de uso |
| Tasa de sync exitosa en 1er intento | > 95% | `sync_telemetry_optica` |
| Pacientes sin sync error | > 99% | Telemetría de sync |
| Adopción de PIN personalizado | > 80% | Config report |
| Uptime de Supabase | > 99.9% | Monitoreo externo |

### 12.2 Métricas de Ingeniería

| Métrica | Target |
|---------|--------|
| Cobertura de tests Android | > 5% (mínimo), escalando a > 30% |
| Cobertura de tests Web | > 20% statements |
| Tiempo de CI Android | < 10 min |
| Tiempo de build Web | < 3 min |
| Bugs críticos abiertos | 0 en producción |

---

## 13. Roadmap y Estado Actual

### 13.1 Estado por Componente

| Componente | Estado |
|------------|--------|
| Android: Core clínico (evaluación, diagnóstico, dispensación) | ✅ Completo |
| Android: Sync offline-first (con Last Write Wins) | ✅ Completo |
| Android: Inventario (monturas + movimientos) | ✅ Completo |
| Android: Financiero (cierres, KPIs, reportes) | ✅ Completo |
| Android: Multi-tenant + RLS | ✅ Completo |
| Android: Suscripciones (FREE ilimitado desde v1.8.0) | ✅ Completo |
| Android: Backup/Restore | ✅ Completo |
| Android: ADD auto-cálculo + fecha de nacimiento obligatoria | ✅ v1.8.0 |
| Web: Auth + middleware + selección de óptica | ✅ Completo |
| Web: Dashboard + KPIs | ✅ Completo |
| Web: Pacientes CRUD | ✅ Completo |
| Web: Evaluaciones | ✅ Completo |
| Web: Dispensaciones + servicios extra | ✅ Completo |
| Web: Configuración fiscal | ✅ Completo |
| Web: Reportes financieros | ✅ Completo |
| Web: Hardening + tests + release (P4-T5) | 🔲 En progreso |
| Supabase: Edge Functions compartidas | 🔲 Pendiente |
| Google OAuth Web | 🔲 Pendiente |

### 13.2 Hitos Recientes

| Fecha | Hito |
|-------|------|
| 2026-06-22 | Last Write Wins implementado (sincronización sin conflictos) |
| 2026-06-22 | App 100% gratuita (sin límites de suscripción) |
| 2026-06-22 | ADD auto-cálculo desde edad del paciente + fecha de nacimiento requerida |
| 2026-06-22 | v1.8.0 liberado con sync conflict protection y estabilización de CI |
| 2026-06-11 | Auditoría de columnas en monturas — triggers + `updated_by` |
| 2026-06-07 | Remoción de columnas legacy Virtual Try-On |
| 2026-06-01 | Grants de ejecución anónima para helpers `app_private` |
| 2026-05-29 | Metadatos de montura expandidos |
| 2026-05-27 | RLS para `dispensacion_items` + select público en `app_releases` |
| 2026-05-22 | Migración a multi-item por dispensación |
| 2026-05-10 | Fix race condition en post-save sync + migración monturas `tipo_aro`/`material` |
| 2026-05-05 | Modernización arquitectónica (Clean Architecture + patrones) |
| 2026-04-29 | Cierre funcional web P4-T4 |
| 2026-04-10 | Migración completa a modelo SaaS |

### 13.3 Próximos Pasos (Q2-Q3 2026)

1. **Completar P4-T5**: Hardening web, tests E2E humo, readiness check y release
2. **Pruebas de concurrencia**: Android + Web operando sobre mismos registros
3. **Google OAuth Web**: Habilitar login con Google en OptoWeb
4. **Cierre de caja web**: Implementar cierre financiero desde la web
5. **Exportaciones**: Reportes descargables desde web (PDF/CSV)
6. **Edge Functions**: Mover reglas de negocio compartibles a Supabase Edge Functions
7. **Cobertura de tests**: Escalar coverage Android y Web

---

## 14. Restricciones Técnicas

### 14.1 Android

| Restricción | Detalle |
|-------------|---------|
| SDK mínimo | 24 (Android 7.0) |
| SDK target | 36 (Android 16) |
| JDK | 17 |
| Gradle | 8.x (configuration cache habilitado) |
| JaCoCo mínimo | 5% instrucciones (piso bajo, escalando) |
| No usar Fragments/ViewBinding para rutas nuevas | Solo Jetpack Compose |

### 14.2 Web

| Restricción | Detalle |
|-------------|---------|
| `src/domain/` es código muerto | No usar. La lógica real vive en `src/lib/` |
| `NEXT_PUBLIC_*` | Solo URL y publishable key. Sin secrets. |
| Middleware obligatorio | Exige sesión + `optica_id` activo en rutas privadas |
| Sin `service_role` en frontend | Bajo ninguna circunstancia |

### 14.3 Base de Datos

| Restricción | Detalle |
|-------------|---------|
| Migraciones inmutables | No modificar migraciones aplicadas a producción |
| `NOT NULL` en Supabase | Requiere plan de compatibilidad con Room + DTOs + sync |
| RLS obligatorio | Toda tabla multi-tenant debe tener policies |
| Convención `snake_case` | En esquema de base de datos |

### 14.4 SDD (Spec-Driven Development)

- Todo cambio significativo pasa por: `explore → propose → spec → design → tasks → apply → verify → archive`
- Strict TDD habilitado: test changes before implementation
- Artifacts en `openspec/` y engram

---

## 15. Glosario

| Término | Definición |
|---------|-----------|
| **OT** | Orden de Trabajo — documento de dispensación de lentes/montura |
| **HO** | Historia Oftalmológica — número correlativo de paciente por óptica |
| **AV** | Agudeza Visual — medida de la capacidad visual (escala decimal o Snellen) |
| **SC/CC** | Sin Corrección / Con Corrección |
| **ADD** | Adición para presbicia |
| **K1/K2** | Curvatura corneal (queratometría) |
| **RLS** | Row Level Security — políticas de seguridad a nivel de fila en PostgreSQL |
| **OSDI** | Ocular Surface Disease Index — cuestionario estandarizado de ojo seco |
| **RGP** | Rígido Gas Permeable — tipo de lente de contacto |
| **MVVM** | Model-View-ViewModel — patrón arquitectónico |
| **DI** | Dependency Injection |
| **HO** | Historia Oftalmológica |
| **P0-P2** | Prioridad: P0 = bloqueante, P1 = importante, P2 = nice-to-have |

---

*Este documento es un artifact vivo. Se actualiza a medida que el producto evoluciona.*

*PRD mantenido por SDD Orchestrator — Junio 2026*
