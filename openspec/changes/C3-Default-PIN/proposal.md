# Proposal: Eliminar PIN por defecto y forzar creación en primer uso

## Intent

El PIN local de la app Android tiene un valor por defecto hardcodeado (`"123456"`). Todo usuario que nunca abra Configuración > Seguridad opera con este PIN trivial. En dispositivos compartidos esto es acceso total sin fricción. El objetivo es eliminar el valor por defecto y forzar la creación de un PIN personalizado en el primer flujo post-login (o en la primera vez que la app lo requiera).

## Scope

### In Scope
- Eliminar `DEFAULT_PIN` de `SecurityManager.kt`
- Agregar flag de migración `pin_has_been_set` en DataStore
- Forzar pantalla de creación de PIN cuando el flag sea falso
- Adaptar `MainActivity` nav graph para ruta `create_pin`
- Adaptar `AuthViewModel` para exponer el estado del flag

### Out of Scope
- Rate-limiting de intentos PIN (cubierto por otro cambio)
- Biometría
- Cambios en Web o Supabase

## Capabilities

### New Capabilities
- `android-pin-setup`: Flujo de creación obligatoria de PIN en primer uso. Incluye UI, validaciones y persistencia del flag.

### Modified Capabilities
- `android-auth`: El estado de autenticación local ahora depende de que el PIN haya sido configurado al menos una vez. El nav graph de `MainActivity` debe considerar `pinHasBeenSet` junto a `isPinRequired`.

## Approach

1. **Migración de estado**: `SessionManager` agrega `PIN_HAS_BEEN_SET` (boolean, default `false` en DataStore). Al guardar un PIN por primera vez (`SecurityManager.savePin`), se marca `true`.
2. **Detección de PIN no configurado**: `SecurityManager` expone `isPinSet: Flow<Boolean>` basado en el flag. Si es `false`, el valor almacenado se ignora (tratado como vacío).
3. **Nueva pantalla `CreatePinScreen`**: Compose, 6 dígitos, confirmación de PIN (dos campos). Reutiliza estilo de `PinScreen` pero sin validación de PIN anterior.
4. **Navegación**:
   - `loading` → si `isLoggedIn && !pinHasBeenSet` → `create_pin`
   - `loading` → si `isLoggedIn && pinHasBeenSet && isPinRequired` → `pin`
   - Post-login / post-onboarding / post-selección-óptica redirige a `create_pin` si aún no está seteado.
5. **UX de cambio de PIN en Configuración**: si el PIN nunca fue seteado (flag falso), el campo "PIN actual" se omite y la acción es "Crear PIN" en lugar de "Cambiar PIN".

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `app/.../data/SecurityManager.kt` | Modified | Quita `DEFAULT_PIN`; agrega `isPinSet`; limpia legacy en DataStore |
| `app/.../data/SessionManager.kt` | Modified | Agrega `PIN_HAS_BEEN_SET` y helpers |
| `app/.../viewmodel/AuthViewModel.kt` | Modified | Expone `pinHasBeenSet`; lógica de navegación post-login |
| `app/.../MainActivity.kt` | Modified | NavHost ruta `create_pin`; condición de redirección inicial |
| `app/.../ui/screens/CreatePinScreen.kt` | New | Pantalla de creación obligatoria de PIN |
| `app/.../ui/screens/ConfiguracionScreen.kt` | Modified | Modo "crear PIN" cuando flag es falso |
| `app/.../ui/screens/PinScreen.kt` | Modified | Mensaje/UX sin cambios funcionales |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Usuarios existentes con PIN `123456` quedan bloqueados al actualizar la app | Med | Al detectar `pin_has_been_set == false` + stored value == `"123456"`, se fuerza `create_pin` en el próximo inicio. No hay bloqueo: es un flujo de setup, no de denegación. |
| Flag perdido por limpieza de datos del sistema | Low | El flag vive en DataStore junto a otras prefs críticas; mismo riesgo que `isLoggedIn`. |
| Fragmentación de estado si `savePin` falla después de setear el flag | Low | Escribir PIN en EncryptedSharedPreferences primero, luego setear flag en DataStore en la misma corrutina. |

## Rollback Plan

Revertir el commit que introduce el cambio. `DEFAULT_PIN` vuelve a existir. Los usuarios que ya crearon su PIN siguen con su valor en EncryptedSharedPreferences; los que no, vuelven a `123456`. No hay migraciones destructivas de schema.

## Dependencies

- Ninguna externa. Solo depende del flujo de login/onboarding existente.

## Success Criteria

- [ ] No existe la cadena `"123456"` en `SecurityManager.kt` ni como fallback
- [ ] Usuario fresco (instalación nueva) es obligado a crear PIN antes de ver `main`
- [ ] Usuario existente que nunca cambió PIN es redirigido a `create_pin` en el próximo inicio
- [ ] Configuración permite crear PIN sin pedir "PIN actual" cuando aún no fue seteado
- [ ] Una vez creado, el flujo de login con PIN funciona idéntico al actual
