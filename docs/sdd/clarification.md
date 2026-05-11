# Clarificaciones de OptoApp

## Estado de decisiones
Documento de ambiguedades resueltas. Toda nueva duda de alcance o comportamiento debe agregarse aqui antes de tocar `plan.md` o `tasks.md`.

## Decisiones cerradas
1. **Baseline UI/arquitectura**
   - Se adopta Compose + Material 3 + MVVM + StateFlow como base oficial.
   - Fragments/ViewBinding no se usan como ruta principal nueva.

2. **Politica de PIN**
   - El PIN oficial de producto es de 6 digitos.
   - El valor temporal `123456` es solo de desarrollo y debe migrar a PIN definido por usuario.

3. **Modelo multi-optica**
   - Un usuario puede pertenecer a multiples opticas.
   - La optica activa se selecciona al iniciar sesion cuando aplique.
   - La app persiste `optica_id` activo y filtra datos locales/remotos con ese contexto.

4. **Comparticion de pacientes entre opticas**
   - No hay paciente global compartido entre opticas.
   - Una misma persona puede existir como registros separados por `optica_id`.

5. **Sincronizacion y dependencias**
   - Orden obligatorio: Pacientes -> Evaluaciones -> Dispensaciones -> ServiciosExtra -> Pagos.
   - Si una entidad hija falla por FK o padre ausente, se reintenta con backoff.

6. **Edicion de pacientes**
   - La actualizacion es por UPDATE y no por DELETE+INSERT.
   - Se evita perdida de historial clinico y rupturas de FK.

7. **Reglas clinicas derivadas**
   - Anisometropia: diferencia EE >= 2.00 D, excluyendo ojos Balance.
   - Ambliopia: diferencia AV corregida >= 2 lineas.
   - Presbicia: ADD > 0.

8. **Suscripcion y paywall (alcance futuro controlado)**
   - Se admite plan gratuito con limites y planes de pago.
   - Billing y reglas comerciales se tratan como fase posterior (no bloquean estabilizacion de sync actual).

## Convencion de registro
- Formato recomendado para nuevas entradas:
  - Fecha
  - Pregunta
  - Decision
  - Impacto (spec/plan/tasks)
