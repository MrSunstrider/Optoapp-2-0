# OptoApp – Gestion optometrica SaaS

Aplicacion Android para gestion de consultas opticas, dispensaciones y servicios extra.
**Multitenant** (por `optica_id`), **offline-first**, sincronizacion con **Supabase**.

---

## Caracteristicas principales

- Registro y gestion de pacientes con historial clinico.
- Evaluaciones optometricas completas:
  - Examen visual (AV, otros tests).
  - Refraccion final (esfera, cilindro, eje, AV, ADD, DIP, prismas).
  - Contactologia (queratometria, sugerencia automatica de lente de contacto).
  - Diagnostico automatico (Miopia, Hipermetropia, Astigmatismos, Presbicia, Anisometropia, Ambliopia).
- Dispensaciones de lentes y monturas (control de saldos, estado de entrega).
- Modulo de servicios extra (ventas de productos no asociados a dispensacion).
- Reportes financieros (diario, semanal, mensual, anual).
- Dashboard con KPIs y graficos.
- Seguridad: autenticacion email/contrasena + PIN de 6 digitos (cifrado); por defecto de desarrollo `123456` hasta que el usuario lo cambie en Configuracion.
- Sincronizacion bidireccional con Supabase (Room + supabase-kt).

---

## Arquitectura

- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose, Material 3
- **Arquitectura**: MVVM (ViewModel + StateFlow / flujos reactivos)
- **Inyeccion de dependencias**: Hilt
- **Persistencia local**: Room (tablas con `optica_id`)
- **Backend / BaaS**: Supabase (PostgreSQL + Auth + RLS)
- **Cliente HTTP**: Ktor (motor CIO)
- **Serializacion**: kotlinx.serialization
- **Sincronizacion**: casos de uso de sync (orden: pacientes, evaluaciones, dispensaciones, servicios extra, pagos)
- **Credenciales Supabase**: `local.properties` inyectadas en `BuildConfig` (no incluir claves en el codigo fuente)

---

## Estructura del proyecto (paquete `com.example.optoapp`)

```
com.example.optoapp/
├── data/           # Room, DAOs, entidades, repositorio, sesion, seguridad
├── domain/         # Casos de uso (sync, logica de negocio)
├── di/             # Modulos Hilt (DB, red, Supabase)
├── viewmodel/      # ViewModels por pantalla / flujo
├── ui/             # Pantallas Compose, componentes, tema
├── notifications/  # Workers y recordatorios
└── util/           # Utilidades (fechas, WhatsApp, etc.)
```

---

## Requisitos previos

- Android Studio Ladybug o superior
- JDK 17
- Gradle 8.0+
- Cuenta en [Supabase](https://supabase.com/) (URL del proyecto y clave anon o publishable del panel)

---

## Configuracion del proyecto

1. Clona el repositorio:

   ```bash
   git clone https://github.com/MrSunstrider/Optoapp-2-0.git
   cd Optoapp-2-0
   git checkout version-saas
   ```

2. Abre la carpeta del proyecto en Android Studio (la que contiene `settings.gradle.kts`).

3. Crea o edita **`local.properties` en la raiz del proyecto** (junto a `settings.gradle.kts`). Este archivo esta en `.gitignore` y no debe subirse a Git. Define las claves de Supabase; Android Studio suele anadir `sdk.dir` automaticamente. Ejemplo:

   ```properties
   sdk.dir=C\:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk
   supabase.url=https://TU-PROYECTO.supabase.co
   supabase.anon.key=TU_CLAVE_ANON_O_PUBLISHABLE
   ```

4. Sincroniza el proyecto con Gradle (File - Sync Project with Gradle Files).

---

## Pruebas

- Unitarias: `./gradlew test`
- Instrumentadas: `./gradlew connectedAndroidTest`

---

## Generar APK

```bash
./gradlew assembleRelease
```

El APK queda en `app/build/outputs/apk/release/`.

---

## Sincronizacion con Supabase

- La app esta pensada **offline-first**: datos en Room y sincronizacion en segundo plano.
- Los casos de uso de sync respetan el orden de dependencias (padres antes que hijos).
- Politicas RLS por `optica_id` y usuario (definidas en tu proyecto de Supabase).
- Reintentos con backoff ante fallos de red.
- Conflictos: enfoque tipo **Last Write Wins** usando `updated_at` donde aplique.

Guia operativa detallada de autenticacion, sync y guardrails de seguridad:

- `docs/guia-operativa-auth-sync-seguridad.md`
- `docs/changelog-operativo.md`
- `docs/guia-web-ecosistema-seguro.md` (ruta oficial para version web segura/confiable/persistente)

---

## Reglas de negocio clave (referencia)

- **Diagnostico OD/OI**: segun esfera y cilindro (logica en dominio / evaluacion).
- **Presbicia**: si ADD > 0, puede marcarse automaticamente en "Otros".
- **Anisometropia**: diferencia de equivalente esferico (EE = esfera + cilindro/2) mayor o igual a 2.00 D.
- **Ambliopia**: diferencia de AV con correccion mayor o igual a 2 lineas (0.2 logMAR).
- **Balance**: si un ojo va en "Balance", no entra en el calculo de anisometropia.
- **Sugerencia de lente de contacto** segun |K1 - K2|:
  - Menos de 2.50 D: lente blando
  - 2.50 a 3.99 D: valorar RGP o torico
  - Desde 4.00 D: RGP

### Tabla de diagnostico automatico (por ojo)

| Condicion | Diagnostico |
|-----------|-------------|
| E = 0, C = 0 | Emetropia |
| E < 0, C = 0 | Miopia |
| E > 0, C = 0 | Hipermetropia |
| E = 0, C < 0 | Astigmatismo miopico simple |
| E < 0, C < 0 | Astigmatismo miopico compuesto |
| E > 0, C < 0 y (E + C) > 0 | Astigmatismo hipermetropico compuesto |
| E > 0, C < 0 y (E + C) <= 0 | Astigmatismo mixto |
| E < 0, C < 0 y (E + C) >= 0 | Astigmatismo mixto |

---

## Roadmap

- Version web (Next.js + Supabase)
- Recordatorios por email / notificaciones
- Exportacion de informes a PDF
- Integracion con Google Calendar para citas
- Modulo de auditoria (logs de cambios)
- Sincronizacion incremental (solo cambios)

---

## Licencia

Este proyecto es privado. No esta autorizado su uso sin permiso explicito.

---

## Contacto

Desarrollado por Jaer - [jaermadear@gmail.com](mailto:jaermadear@gmail.com)