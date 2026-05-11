# Constitucion de OptoApp

## Proposito
Definir las reglas no negociables del producto y de la implementacion tecnica. Este documento tiene prioridad sobre preferencias puntuales de implementacion.

## Principios no negociables
1. Offline-first: la app debe funcionar sin internet, persistiendo en Room y sincronizando con Supabase al recuperar conectividad.
2. Multitenant por diseno: toda entidad de negocio compartida entre usuarios debe estar aislada por `optica_id`.
3. Seguridad por defecto: autenticacion por email/contrasena y bloqueo por PIN de 6 digitos.
4. Integridad clinica: las reglas de diagnostico y calculos clinicos deben implementarse exactamente como en `spec.md`.
5. Trazabilidad de decisiones: todo cambio de arquitectura o contrato de datos debe registrarse primero en `plan.md`.
6. Evolucion controlada: cualquier nueva funcionalidad debe reflejarse en `spec.md`, aclararse en `clarification.md` y desglosarse en `tasks.md`.

## Stack oficial vigente
- Lenguaje: Kotlin
- UI: Jetpack Compose + Material 3
- Arquitectura: MVVM con ViewModel + StateFlow
- Persistencia local: Room
- Inyeccion de dependencias: Hilt
- Backend: Supabase (PostgreSQL, Auth, RLS)
- Red y serializacion: Ktor + kotlinx.serialization + supabase-kt

## Seguridad y sesiones
- El PIN oficial es de 6 digitos.
- El valor temporal de desarrollo (`123456`) solo es permitido como estado transitorio hasta configuracion de usuario.
- Preferencias sensibles deben vivir cifradas en `EncryptedSharedPreferences` mediante `SecurityManager`/`SessionManager`.
- Las politicas RLS son obligatorias en todas las tablas multi-tenant.

## Restricciones tecnicas
- No introducir pantallas nuevas en Fragments/ViewBinding como ruta principal: Compose es la base.
- No crear columnas `NOT NULL` en Supabase sin estrategia de compatibilidad para Room, DTOs y sincronizacion.
- No romper orden de sincronizacion entre entidades padre e hijas.
- No duplicar logica clinica en multiples capas sin una razon documentada en `plan.md`.

## Calidad minima
- Logica clinica: pruebas unitarias.
- Sincronizacion critica y reglas de permisos: pruebas de integracion/instrumentadas cuando aplique.
- Cambios de datos (migraciones/schema): incluir plan de rollback o mitigacion en `plan.md`.
