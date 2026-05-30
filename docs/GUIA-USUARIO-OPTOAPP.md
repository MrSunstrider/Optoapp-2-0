# Guía de Usuario — OptoApp SaaS

> Versión 1.2.0 — Mayo 2026
> Sistema de Gestión Optométrica

---

## Índice

1. [Introducción](#1-introducción)
2. [Primeros Pasos](#2-primeros-pasos)
3. [Pacientes](#3-pacientes)
4. [Evaluaciones Optométricas](#4-evaluaciones-optométricas)
5. [Dispensación (Órdenes de Trabajo)](#5-dispensación-órdenes-de-trabajo)
6. [Inventario de Monturas](#6-inventario-de-monturas)
7. [Prueba Virtual de Monturas](#7-prueba-virtual-de-monturas)
8. [Servicios Extras](#8-servicios-extras)
9. [Pagos y Caja](#9-pagos-y-caja)
10. [Sincronización y Offline](#10-sincronización-y-offline)
11. [Configuración del Sistema](#11-configuración-del-sistema)
12. [Roles y Permisos](#12-roles-y-permisos)
13. [Solución de Problemas](#13-solución-de-problemas)

---

## 1. Introducción

### 1.1 ¿Qué es OptoApp?

OptoApp es un sistema de gestión optométrica integral diseñado para ópticas y consultorios oftalmológicos. Funciona 100% offline-first: podés trabajar sin conexión a internet y los datos se sincronizan automáticamente cuando haya conexión.

### 1.2 Arquitectura

```
┌─────────────────────────────────────────────────┐
│                Dispositivo Android              │
│  ┌─────────────┐  ┌──────────────────────────┐ │
│  │  App (UI)   │  │  Base Local (Room)       │ │
│  │  Compose    │←→│  SQLite offline-first     │ │
│  └─────────────┘  └──────────┬───────────────┘ │
│                              │ sync              │
│              ┌───────────────┴───────────────┐  │
│              │  Supabase (Cloud)             │  │
│              │  PostgreSQL + Auth + Storage  │  │
│              └───────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

### 1.3 Requisitos del Sistema

- **Android**: 7.0 (API 24) o superior
- **Almacenamiento**: ~100 MB libres
- **Conexión**: No requerida para operación diaria (solo para sincronizar)
- **Cámara**: Requerida para prueba virtual de monturas

---

## 2. Primeros Pasos

### 2.1 Inicio de Sesión

Al abrir la app por primera vez verás la pantalla de inicio de sesión:

1. Ingresá tu **correo electrónico** y **contraseña** (proporcionados por el administrador)
2. Presioná **Iniciar Sesión**
3. Si es tu primer inicio, se te pedirá crear un **PIN de 6 dígitos** para acceso rápido

> **Tip**: El PIN te permite acceder sin internet. No lo compartas.

### 2.2 Pantalla Principal

Una vez dentro, ves el **menú lateral** con las siguientes secciones:

| Sección | Descripción |
|---------|-------------|
| 📋 Pacientes | Gestión de pacientes |
| 📝 Evaluaciones | Exámenes optométricos |
| 🔧 Dispensaciones | Órdenes de trabajo |
| 🕶️ Monturas | Inventario de monturas |
| 👁️ Prueba Virtual | Try-on de monturas con cámara |
| ⚙️ Configuración | Ajustes del sistema |

### 2.3 Navegación

Deslizá desde el borde izquierdo o presioná el ícono ☰ en la esquina superior izquierda para abrir el menú de navegación.

---

## 3. Pacientes

### 3.1 Registrar un Paciente Nuevo

1. Desde el menú, seleccioná **Pacientes**
2. Presioná el botón **+** (esquina inferior derecha)
3. Completá los campos obligatorios (*):
   - **Nombre completo**
   - **Teléfono**
   - **Edad**
4. Opcionalmente agregá DNI, email, dirección, ocupación, etc.
5. Presioná **Guardar**

### 3.2 Buscar Pacientes

Usá el campo de búsqueda en la parte superior de la lista de pacientes. Buscá por nombre, teléfono o DNI.

### 3.3 Historial del Paciente

Al seleccionar un paciente, ves su ficha con:

- **Información general**: datos personales
- **Historia optométrica**: código único de historial
- **Evaluaciones**: lista de exámenes realizados
- **Dispensaciones**: órdenes de trabajo asociadas
- **Saldo pendiente**: deuda actual del paciente

### 3.4 Editar o Eliminar Paciente

- Presioná el ícono ✏️ para editar
- Presioná el ícono 🗑️ para eliminar (requiere permisos de administrador)

> **IMPORTANTE**: Eliminar un paciente es permanente. Solo administradores pueden hacerlo.

---

## 4. Evaluaciones Optométricas

### 4.1 Crear una Evaluación

1. Desde la ficha del paciente, presioná **Nueva Evaluación**
2. La fecha se establece automáticamente al día de hoy
3. Completá las secciones del examen:

#### 4.1.1 Anamnesis
- Motivo de consulta
- Sintomas
- Antecedentes (personales oculares, sistémicos, familiares)
- Medicación y alergias
- Necesidad visual

#### 4.1.2 Agudeza Visual
- **Sin corrección (SC)**: Lejos y cerca, OD, OI y AO
- **Con corrección (CC)**: Lejos y cerca, OD, OI y AO

#### 4.1.3 Refracción
- **Objetiva**: Esfera, Cilindro, Eje (OD y OI)
- **Subjetiva**: Esfera, Cilindro, Eje (OD y OI)
- **Fórmula final**: Receta con AV

#### 4.1.4 Visión Binocular
- Cover test (6m, 40cm, 10cm)
- PPC (Or, Luz, Frl)
- Reflejos (fotomotor, consensual, acomodativo)
- Estereopsis, Lang, Worth
- Forias (pH Od/Oi), Kappa

#### 4.1.5 Salud Ocular
- Schirmer (OD/OI)
- OSDI (puntuación y clasificación)
- Sensibilidad de contraste
- Amsler, Campo visual
- Ishihara, Farnsworth

#### 4.1.6 Diagnóstico Automático

OptoApp detecta automáticamente:
- **Presbicia**: basado en edad + ADD
- **Anisometropia**: diferencia ≥ 1.00 D entre ojos
- **Ambliopía**: basado en diferencias de AV

#### 4.1.7 Contactología
- Lentes de contacto (OD/OI): Esf, Cil, Eje
- Radio base, diámetro
- Laboratorio, material, tipo
- Fecha de adaptación

### 4.2 DIP y DNP

La evaluación incluye campos para:
- **DIP** (Distancia Interpupilar): lejos, cerca, intermedio
- **DIP Total** en mm
- **DNP** (Distancia Naso-Pupilar): OD y OI en mm

> Estos valores son críticos para la prueba virtual de monturas y la dispensación de lentes.

### 4.3 Próxima Cita

Configurá la fecha de la próxima cita y el estado (programada, confirmada, asistió, no asistió, reprogramada).

---

## 5. Dispensación (Órdenes de Trabajo)

### 5.1 Crear una Dispensación

1. Desde la ficha del paciente, presioná **Nueva Dispensación**
2. Seleccioná la **montura** del inventario
3. Configurá los detalles del lente:
   - Tipo de lente (monofocal, bifocal, progresivo, ocupacional)
   - Material (orgánico, policarbonato, 1.67, 1.74, etc.)
   - Color
   - Tratamientos (antirreflejo, fotocromático, blue-cut, etc.)
   - Distancia lente y altura
   - Para bifocales: subtipo (Flaptop, Invisible)
   - Para progresivos/ocupacionales: altura segmento
4. Agregá notas de diseño si es necesario

### 5.2 Múltiples Lentes por OT

Cada dispensación puede tener 1 o más items (lentes). Usá el botón **Agregar Lente** para añadir lentes adicionales.

### 5.3 Precios y Pagos

- Ingresá el **monto total** de la dispensación
- Especificá el **método de pago** (efectivo, débito, crédito, transferencia, etc.)
- Registrá el **monto pagado** (puede ser parcial)
- El saldo pendiente se calcula automáticamente

### 5.4 Estado de Entrega

| Estado | Descripción |
|--------|-------------|
| Pendiente | OT creada, no entregada |
| Entregado | OT completa, entregada al paciente |

---

## 6. Inventario de Monturas

### 6.1 Agregar una Montura

1. Desde el menú, seleccioná **Monturas**
2. Presioná **+** (esquina inferior derecha)
3. Completá los campos:
   - **SKU**: código único del producto (*)
   - **Marca / Fabricante** (*)
   - **Modelo / Nombre** (*)
   - Color / Variedad
   - Talla / Tamaño
   - **Tipo de Aro**: Aro Completo, Semi al aire, Al aire (*)
   - **Material**: Acetato, Metal, Carey, Econ (*)
   - Costo unitario
   - Precio de venta
   - Stock inicial y mínimo
   - **Ancho (mm)**: ancho total del frente
   - **Puente (mm)**: distancia del puente
   - **Altura (mm)**: altura del lente
   - **Imagen**: URI de la imagen PNG de la montura (para prueba virtual)

### 6.2 Imagen para Prueba Virtual

Para que la montura funcione en la Prueba Virtual, necesita:
1. Una imagen PNG con **fondo transparente**
2. La imagen debe mostrar la montura de frente
3. Cargá la ruta de la imagen en el campo **Imagen URI**

> **Formato recomendado**: PNG 512×512px, fondo transparente, montura centrada.

### 6.3 Movimientos de Stock

Cada entrada, salida o ajuste de stock se registra automáticamente en el libro de movimientos. Podés ver el historial desde la ficha de cada montura.

### 6.4 Alerta de Stock Mínimo

Cuando el stock actual está por debajo del mínimo configurado, la montura se marca visualmente para alertar al usuario.

---

## 7. Prueba Virtual de Monturas

### 7.1 ¿Qué es la Prueba Virtual?

La Prueba Virtual usa la cámara de tu dispositivo para mostrar cómo se ve una montura en el rostro del paciente. Usa **MediaPipe Face Mesh** para detectar 468 puntos faciales y superpone la montura 2D escalada según la **DIP** del paciente.

### 7.2 Requisitos

- Paciente con **evaluación** que tenga **DIP** registrada
- Montura con **ancho (mm)**, **puente (mm)** e **imagen PNG**
- Permiso de cámara y almacenamiento

### 7.3 Cómo Usarla

1. Desde el menú, seleccioná **Prueba Virtual**
2. Seleccioná el **paciente** (se cargará su DIP automáticamente)
3. Elegí una foto:
   - **Galería**: seleccioná una foto existente del paciente
   - **Cámara**: sacá una foto (próximamente)
4. La app detectará el rostro automáticamente
5. Seleccioná una **montura** del inventario
6. La montura se superpondrá escalada según la DIP
7. Ajustá posición y escala si es necesario:
   - Deslizá para mover
   - Pellizcá para escalar
8. **Guardá las medidas**: las mediciones faciales se guardan en la evaluación del paciente
9. **Guardá la imagen**: exportá el resultado a la galería
10. **Compartí**: enviá el resultado por WhatsApp u otras apps

### 7.4 Mediciones que se Toman

| Medida | Descripción | Se guarda en |
|--------|-------------|-------------|
| DIP | Distancia entre pupilas (mm) | Evaluación → dipTotalMm |
| DNP OD | Distancia nariz-pupila derecha | Evaluación → dnpOdMm |
| DNP OI | Distancia nariz-pupila izquierda | Evaluación → dnpOiMm |
| Altura segmento | Altura para lente progresivo/ocupacional | Evaluación (para dispensación) |

### 7.5 Tipos de Lente y Altura de Segmento

| Tipo | Cálculo de altura |
|------|-------------------|
| **Flaptop** | Párpado inferior + 3mm |
| **Invisible** | En el párpado inferior |
| **Progresivo** | Centro de la pupila (0mm de offset) |
| **Ocupacional** | Centro de la pupila (mismo que progresivo) |

### 7.6 Solución de Problemas

| Problema | Causa | Solución |
|----------|-------|----------|
| "No se detectó un rostro" | Foto sin rostro visible | Usá una foto frontal con buena iluminación |
| "Rostro poco claro" | Baja confianza (>0.8) | Mejor iluminación, rostro de frente |
| Montura no aparece | Falta imagen o medidas | Cargá PNG transparente y ancho/puente en mm |
| Medidas no se guardan | DIP no disponible | Registrá una evaluación con DIP primero |

---

## 8. Servicios Extras

### 8.1 Crear un Servicio Extra

1. Desde la ficha del paciente, seleccioná la pestaña **Servicios**
2. Presioná **Nuevo Servicio**
3. Completá:
   - Descripción del servicio
   - Monto total
   - A cuenta (pago inicial)
   - Método de pago

### 8.2 Estados

- **Pendiente**: servicio creado, no completado
- **Entregado**: servicio finalizado

---

## 9. Pagos y Caja

### 9.1 Registrar un Pago

Los pagos se registran automáticamente al crear una dispensación o servicio extra. También podés registrar pagos adicionales desde la ficha correspondiente.

### 9.2 Métodos de Pago

- Efectivo
- Débito
- Crédito
- Transferencia
- QR
- Otro

### 9.3 Control de Caja

El sistema mantiene un registro de todos los pagos con:
- Fecha
- Monto
- Método de pago
- Nota (opcional)

---

## 10. Sincronización y Offline

### 10.1 ¿Cómo Funciona?

OptoApp usa una arquitectura **offline-first**:

1. **Todo se guarda localmente** en el dispositivo (SQLite vía Room)
2. **La sincronización es automática** cuando hay conexión a internet
3. **Podés trabajar sin internet** todo el día

### 10.2 Orden de Sincronización

```
1. Pacientes
2. Evaluaciones
3. Dispensaciones
4. Servicios Extra
5. Pagos
```

### 10.3 Forzar Sincronización

En Configuración → Sincronización, presioná el botón **Sincronizar Ahora**.

### 10.4 Respaldo

El sistema realiza respaldos automáticos de la base de datos local. También podés exportar un respaldo manual desde Configuración → Respaldo.

> **Tip**: Hacé un respaldo antes de actualizar la app.

---

## 11. Configuración del Sistema

### 11.1 Perfil de la Óptica

Desde Configuración podés actualizar:
- Nombre de la óptica
- Dirección
- Teléfono
- Configuración fiscal

### 11.2 Gestión de Miembros

Los administradores pueden invitar nuevos miembros, asignar roles y gestionar permisos.

### 11.3 Plan y Suscripción

Desde Configuración → Plan podés ver el plan actual (Gratuito, Profesional, Multisede) y gestionar la suscripción.

### 11.4 PIN de Acceso

Podés cambiar tu PIN de 6 dígitos desde Configuración → Seguridad.

---

## 12. Roles y Permisos

### 12.1 Roles Disponibles

| Rol | Descripción |
|-----|-------------|
| **Admin** | Acceso completo a todas las funciones |
| **Gerente** | Gestión operativa sin configurar plan |
| **Cajero** | Pagos y cierre de caja |
| **Especialista** | Evaluaciones y dispensaciones |
| **Asesor / Ventas** | Pacientes, monturas y pruebas virtuales |
| **Invitado** | Solo lectura |

### 12.2 Permisos por Rol

| Función | Admin | Gerente | Cajero | Especialista | Asesor | Invitado |
|---------|-------|---------|--------|-------------|--------|----------|
| Pacientes CRUD | ✅ | ✅ | ❌ | ✅ | ✅ | ✅ (solo lectura) |
| Evaluaciones CRUD | ✅ | ✅ | ❌ | ✅ | ❌ | ✅ (solo lectura) |
| Dispensaciones | ✅ | ✅ | ❌ | ✅ | ✅ | ✅ (solo lectura) |
| Monturas | ✅ | ✅ | ❌ | ❌ | ✅ | ✅ (solo lectura) |
| Prueba Virtual | ✅ | ✅ | ❌ | ✅ | ✅ | ✅ (solo lectura) |
| Pagos | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| Configuración | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| Miembros | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Cierre de Caja | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |

---

## 13. Solución de Problemas

### 13.1 La app no sincroniza

1. Verificá que tengas conexión a internet
2. Andá a Configuración → Sincronización
3. Presioná **Sincronizar Ahora**
4. Si el problema persiste, cerrá sesión y volvé a iniciar

### 13.2 Error de migración al abrir

Si ves "No se pudo abrir la base local por un conflicto de migración":
1. No desinstales la app (tus datos están seguros)
2. Contactá al administrador
3. Exportá un respaldo desde la versión anterior si está disponible

### 13.3 Prueba Virtual no funciona

Verificá:
- ¿La montura tiene imagen PNG? (campo Imagen URI)
- ¿La montura tiene ancho y puente en mm?
- ¿El paciente tiene una evaluación con DIP?
- ¿La foto tiene un rostro visible y bien iluminado?

### 13.4 Contacto y Soporte

Ante cualquier problema:
- Consultá con el administrador de tu óptica
- Revisá esta guía
- Verificá que tengas la última versión de la app

---

## Apéndice A: Glosario

| Término | Significado |
|---------|-------------|
| **OD** | Ojo derecho (Oculus Dexter) |
| **OI** | Ojo izquierdo (Oculus Sinister) |
| **AO** | Ambos ojos (Ambos Oculi) |
| **AV** | Agudeza visual |
| **SC** | Sin corrección |
| **CC** | Con corrección |
| **DIP** | Distancia Interpupilar |
| **DNP** | Distancia Naso-Pupilar |
| **OT** | Orden de Trabajo |
| **ADD** | Adición (para presbicia) |
| **Esf** | Esfera (graduación) |
| **Cil** | Cilindro (graduación) |
| **Eje** | Eje del cilindro |

---

## Apéndice B: Atajos y Tips

- **Búsqueda rápida**: usá el campo de búsqueda en cualquier lista
- **PIN rápido**: configurá un PIN para acceso sin internet
- **Respaldo automático**: la app respalda al abrir si pasaron más de 24h
- **Offline-first**: todo funciona sin internet, la sincronización es automática

---

> © 2026 OptoApp SaaS — Todos los derechos reservados.
> Documentación generada para la versión 1.2.0
