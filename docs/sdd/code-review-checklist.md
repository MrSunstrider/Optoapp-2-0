# Lista de verificación — revisión de código (OptoApp)

Uso sugerido: copiar la sección relevante en la descripción del PR o marcar ítems en comentarios de revisión.  
Contexto obligatorio: `constitution.md`, `spec.md`, `plan.md` y `tasks.md` cuando el cambio toque producto o contratos.

---

## 1. Alcance y trazabilidad

- [ ] El cambio está acotado al problema o tarea (sin refactors o archivos colaterales innecesarios).
- [ ] Si afecta comportamiento o datos, está alineado con `spec.md` / `plan.md` o hay nota explícita de excepción temporal.
- [ ] Migraciones Room / SQL: versión coherente, nombre descriptivo, y riesgo de datos documentado si aplica.

---

## 2. Multitenant y datos (`optica_id`)

- [ ] Consultas y escrituras locales filtran por óptica activa cuando la entidad es por tenant.
- [ ] Subidas a Supabase incluyen / respetan `optica_id` como en el resto del módulo de sync.
- [ ] No se mezclan datos de dos ópticas por estado compartido global sin justificación.

---

## 3. Sincronización

- [ ] Orden de dependencias respetado donde aplique (p. ej. padres antes que hijos en finanzas).
- [ ] Errores de sync no exponen tokens ni cabeceras crudas en UI (usar sanitizado si aplica).
- [ ] Cambios en entidades sincronizables: considerar P0-T5 (`sync_entity_state`) si el fallo debe ser trazable por fila.

---

## 4. Supabase, RLS y seguridad en red

- [ ] No se introduce la **service role** ni claves secretas en el cliente; solo anon key vía `BuildConfig` / `local.properties`.
- [ ] Cambios en tablas expuestas: políticas RLS revisadas o documentadas para el equipo.
- [ ] Campos sensibles a negocio (p. ej. `plan`): no asumir que el cliente es la única barrera; validar servidor/triggers si aplica.

---

## 5. Autenticación, sesión y secretos locales

- [ ] Flujos que usan sesión Supabase manejan ausencia de usuario o error sin dejar la app en estado inconsistente.
- [ ] PIN y preferencias sensibles siguen el patrón existente (`SecurityManager` / `SessionManager` / cifrado).
- [ ] Logs en release: sin PII innecesaria; en debug, uso razonable de `Log` (o acordado con el equipo).

---

## 6. Kotlin y arquitectura

- [ ] ViewModels sin lógica de UI directa; estado expuesto de forma clara (`StateFlow` / eventos).
- [ ] Corrutinas: scope correcto (`viewModelScope` / `Application`); cancelación y `ExperimentalCoroutinesApi` acotados donde toque.
- [ ] Inyección: constructores `@Inject` donde corresponda; módulos Hilt solo con lo necesario.
- [ ] Sin `!!` innecesarios; nulabilidad coherente con Room y DTOs remotos.

---

## 7. UI (Compose + Material 3)

- [ ] Estados de carga / error / vacío visibles para el usuario cuando el flujo lo requiere.
- [ ] Strings y accesibilidad: `contentDescription` en iconos interactivos relevantes.
- [ ] No se añaden dependencias de UI pesadas sin consenso.

---

## 8. Reglas clínicas y negocio

- [ ] Cálculos o textos clínicos coinciden con `spec.md` (o el cambio está explícitamente acordado).
- [ ] Límites de plan / suscripción: no se elude en release con flags de debug (revisar `BuildConfig.DEBUG` donde aplique).

---

## 9. Rendimiento y robustez

- [ ] Listas largas: `key` estable en `LazyColumn` / `items` donde corresponda.
- [ ] Lecturas de disco o red no bloquean el hilo principal sin `Dispatchers` adecuado.
- [ ] Importación de archivos (p. ej. JSON): validación de tamaño y forma antes de aplicar a la BD.

---

## 10. Calidad de build y estilo

- [ ] `compileDebugKotlin` (y tests si el PR los incluye) pasan en CI o localmente antes de merge.
- [ ] Lint: sin nuevos errores; advertencias nuevas justificadas o corregidas.
- [ ] Estilo del proyecto respetado (imports, nombres, comentarios solo donde aportan).

---

## 11. Checklist rápido de seguridad (recordatorio)

- [ ] No secretos en el repositorio; `local.properties` y keystores fuera de Git.
- [ ] Cambios en backup / permisos / `FileProvider`: superficie de exposición revisada.
- [ ] Billing / pagos: IDs de producto y flujos alineados con Play Console cuando se toquen.

---

## Referencia cruzada

| Tema | Documento |
|------|-----------|
| Principios y stack | `constitution.md` |
| Reglas de negocio y clínica | `spec.md` |
| Arquitectura y datos | `plan.md` |
| Backlog | `tasks.md` |

---

## Verificación de proyecto (snapshot 2026-04-17)

Ejecución puntual contra el código y build local; **no sustituye** revisar cada PR con la lista anterior.

| Sección | Estado | Notas breves |
|--------|--------|----------------|
| **1. Alcance** | Manual | La checklist aplica por cambio; el backlog en `tasks.md` describe el alcance global. |
| **2. Multitenant** | Cumple | Patrón `sessionManager.opticaId` / DAOs `*ForOptica` extendido en el código revisado. |
| **3. Sincronización** | Cumple | Orden en `SyncViewModel`: pacientes → historial → finanzas; `SyncErrorSanitizer` en errores mostrados; P0-T5 (`sync_entity_state`) integrado en use cases de sync. |
| **4. Supabase / RLS** | Cumple | Sin `service_role` en cliente; clave anon vía `BuildConfig`; trigger `opticas_lock_plan` en remoto para `plan`. |
| **5. Auth / logs** | Parcial | PIN/prefs cifrados según diseño; logs con PII acotados a `BuildConfig.DEBUG` en `AuthViewModel`. Otros `Log.d` en sync siguen en release (ruido, no siempre PII). |
| **6. Kotlin** | Parcial | Varios `!!` en navegación (`MainDrawerScreen`) y tras `Resource.Success` (`DispensacionViewModel`, etc.): aceptable si la ruta garantiza datos; riesgo si cambia el grafo. |
| **7. UI** | Parcial | Estados de carga/error en flujos principales; revisión de `contentDescription` pantalla a pantalla no automatizada. |
| **8. Clínica / plan** | Manual | Contrastar cambios futuros con `spec.md`; PRO dev acotado a `BuildConfig.DEBUG` en suscripción. |
| **9. Robustez** | Cumple | `BackupImportValidator` en import JSON; listas con `key` donde se revisó (p. ej. errores de sync). |
| **10. Build / lint** | Parcial | `compileDebugKotlin` OK. **`lintDebug` reintentado con éxito** tras `gradlew --stop`, `clean` y `lintDebug` (evita bloqueo de caché en Windows). Tests unitarios: cobertura mínima (`DateUtilsTest`); constitution pide más para lógica clínica crítica. |
| **11. Seguridad rápida** | Cumple | `local.properties` en `.gitignore`; `allowBackup=false`; `FileProvider` no exportado; Billing con ID acotado en código. |

**Conclusión:** el proyecto **cumple la mayoría** de criterios estructurales; los puntos **a vigilar en PRs** son: `!!`, cobertura de tests de reglas clínicas, accesibilidad, y ejecutar **lint** en entorno sin bloqueo de archivos.
