# Optica Config Settings Specification

## Purpose

Per-tenant optica identity and business-hours configuration that drives WhatsApp message templates. Replaces hardcoded strings with tenant-specific values sourced from `OpticaHeaderViewModel.nombreOptica` and `optica_settings.config_json`.

## Requirements

### Requirement: Per-Tenant Optica Name in WhatsApp Messages

WhatsApp message templates MUST use the current optica's name from `OpticaHeaderViewModel.nombreOptica` instead of a hardcoded business name. Every tenant MUST see its own identity in outgoing messages.

(Previously: hardcoded `"Óptica Sersa Visual y Preventiva"` in "Invitación Control Anual" and "Recordar Próxima Cita" templates.)

#### Scenario: Optica name shown in invitation message

- GIVEN an optica named `"Vision Center SAS"` is the current tenant
- WHEN the user triggers "Invitación Control Anual" for patient "Juan"
- THEN the message body MUST contain `"te saludamos de Vision Center SAS"`
- AND it MUST NOT contain any hardcoded optica name

#### Scenario: Optica name shown in appointment reminder

- GIVEN an optica named `"Óptica del Valle"` is the current tenant
- AND the patient has a scheduled `proximaCita`
- WHEN the user triggers "Recordar Próxima Cita"
- THEN the message body MUST contain `"te saludamos de Óptica del Valle"`

#### Scenario: Empty or null optica name fallback

- GIVEN `nombreOptica` is empty or null
- WHEN any WhatsApp template that includes the greeting is sent
- THEN the greeting MUST use `"Su óptica"` as a generic fallback

### Requirement: Business Hours from optica_settings

The "Entrega de Lentes" WhatsApp template MUST use business hours from `optica_settings.config_json->>'business_hours'` via Room. The system MUST NOT use hardcoded hours of any single tenant.

(Previously: hardcoded `"Martes a Sábado de 10am a 6:30pm y Domingos de 10am a 2pm"`.)

#### Scenario: Configured business hours appear in delivery message

- GIVEN the current optica has `business_hours` = `"Lunes a Viernes de 9am a 7pm"` in `optica_settings.config_json`
- WHEN the user triggers "Entrega de Lentes" for a patient
- THEN the message MUST include `"puede venir a recogerlos en este horario: Lunes a Viernes de 9am a 7pm"`
- AND it MUST NOT include any other optica's hours

#### Scenario: Business hours not configured — omit hours sentence

- GIVEN the `optica_settings` row for the current optica has no `business_hours` field
- OR the field value is empty, null, or the row does not exist
- WHEN the user triggers "Entrega de Lentes"
- THEN the message MUST omit the hours sentence entirely
- AND the message MUST still read as a coherent delivery notification (e.g., `"sus lentes ya están listos, lo esperamos."`)

#### Scenario: Offline — use cached business hours from Room

- GIVEN the device is offline
- AND Room holds a previously synced `OpticaSettingsEntity` with `business_hours`
- WHEN the user triggers "Entrega de Lentes"
- THEN the message MUST use the cached `business_hours` value from Room
- AND the send-via-intent action MUST still work

### Requirement: WhatsApp Message Structure Preservation

Template structure (greeting, body, recipient name extraction, honorific logic) MUST remain unchanged. Only the optica name and business hours placeholders change. The `onSendMessage` callback and Android intent-handling MUST work identically.

#### Scenario: Non-affected templates remain unchanged

- GIVEN any tenant
- WHEN the user sends "Mensaje Libre" or "Pendiente de Recojo"
- THEN the message body MUST be identical to pre-change behavior

#### Scenario: Send via intent still functional

- GIVEN any WhatsApp template is composed with tenant-specific values
- WHEN `onSendMessage(mensaje)` is invoked
- THEN the Android share intent MUST open WhatsApp with the composed message pre-filled
