# Proposal — lista de pacientes: sexo, filtros y búsqueda OT

## Intent

En la lista de pacientes: el glifo masculino es azul y el femenino rojo/rosado en claro y oscuro; los chips **Saldo Pendiente** y **Estado de entrega** listan el conjunto correcto; la búsqueda incluye el número de OT asignado (dispensación o servicio extra).

## Evidence

- El avatar usa `colorScheme.primary` para todos los sexos (`PacientesListScreen` `PacienteCard`).
- `searchPacientesForOptica` no mira `dispensaciones.ot` ni `servicios_extra.ot`. Con un filtro activo, el recorte in-memory tampoco usa OT.
- Saldo pendiente incluye ventas `Anulado`/`Reclamada` si el cache `(montoTotal - montoPagado) > 0`.
- Los chips ya envían `"Saldo Pendiente"` / `"Estado de entrega"`, alineados con el ViewModel. Tests de caracterización aún dicen `"Entrega"`.

## Scope

- **IN**: color de avatar (Marte/Venus), tokens claro/oscuro; SQL de búsqueda OT (disp + serv); intersectar búsqueda con filtros; excluir anulados/reclamadas del filtro de saldo; tests.
- **OUT**: migraciones remotas; PagoEffect en el filtro de saldo (sigue el cache del padre; el change `fix-monto-pagado-single-writer` es el writer).

## Approach

Tokens semánticos azul/rosa (no `alertRed`). Un matcher de búsqueda (nombre, id, teléfono, HO, OT). Los filtros SQL excluyen estados cancelados. Con búsqueda + filtro, intersección por id.

## Causal invariant

INV-1: el color del glifo depende solo de `sexoSymbolOf` + tema, nunca del primary teal.
INV-2: una OT de dispensación o de servicio extra del mismo paciente produce el mismo hit de búsqueda.
INV-3: Saldo Pendiente y Estado de entrega no incluyen entidades `Anulado`/`Reclamada`.
