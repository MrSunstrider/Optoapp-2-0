# Especificacion de OptoApp

## Objetivo de producto
OptoApp es una aplicacion SaaS para opticas y optometristas que gestiona pacientes, evaluaciones clinicas, dispensaciones y servicios extra con enfoque offline-first y sincronizacion con Supabase.

## Actores y permisos funcionales
- Administrador de optica: acceso a configuracion, reportes, indicadores y gestion operativa.
- Optometrista: gestiona pacientes, evaluaciones y diagnostico clinico.
- Asesor de ventas: gestiona dispensaciones, servicios extra y seguimiento de saldos.

## Alcance funcional

### Modulo pacientes
- Crear, editar y consultar pacientes.
- Campos base: identificacion, contacto, sexo, fecha de nacimiento y metadatos de registro.
- Edad calculada automaticamente.
- Aislamiento de datos por `optica_id`.

### Modulo evaluacion clinica
- Registro de AV sin correccion y con correccion (OD, OI, binocular lejos y cerca).
- Registro de refraccion final: esfera, cilindro, eje, AV por ojo, ADD, DIP y prismas.
- Contactologia: queratometria (K1/K2), recomendacion de lente de contacto y parametros adicionales.
- Diagnostico automatico por ojo segun esfera/cilindro, con opcion de sobreescritura a Balance.
- Diagnosticos derivados: presbicia, anisometropia y ambliopia.
- Programacion de proxima cita.

### Modulo dispensacion
- Registro de tipo de lente, montura y detalle comercial.
- Registro financiero: monto total, a cuenta, saldo y metodo de pago.
- Estados de entrega: Pendiente o Entregado.
- OT opcional y fecha editable.

### Modulo servicios extra
- Registro de ventas/servicios no ligados a dispensacion.
- Paciente opcional.
- Campos financieros y de estado equivalentes a dispensacion.

### Modulo reportes y dashboard
- Reportes por periodo (diario, semanal, mensual, anual).
- KPI de ventas, pacientes atendidos y saldo pendiente.
- Visualizaciones de tendencia y metodos de pago.

## Reglas de negocio clinicas (fuente de verdad)
- Diagnostico por esfera (E) y cilindro (C), notacion negativa:
  - E=0 y C=0: Emetropia
  - E<0 y C=0: Miopia
  - E>0 y C=0: Hipermetropia
  - E=0 y C<0: Astigmatismo miopico simple
  - E<0 y C<0: Astigmatismo miopico compuesto
  - E>0 y C<0 y (E+C)>0: Astigmatismo hipermetropico compuesto
  - E>0 y C<0 y (E+C)<=0: Astigmatismo mixto
  - E<0 y C<0 y (E+C)>=0: Astigmatismo mixto
- Si el campo esfera contiene `plano` o `neutro`, interpretar como 0.00 D.
- Si el campo esfera contiene `balance`, el diagnostico de ese ojo es Balance.
- Presbicia: activa si ADD > 0.
- Anisometropia: diferencia de equivalente esferico >= 2.00 D, excluyendo ojos Balance.
- Ambliopia: diferencia de AV con correccion >= 2 lineas (0.2 logMAR).
- Sugerencia de LC por astigmatismo corneal (`|K1-K2|`):
  - < 2.50 D: Lente blando
  - 2.50 a 3.99 D: Valorar RGP/Torico
  - >= 4.00 D: Lente RGP

## Criterios de aceptacion globales
- Toda accion de negocio opera en modo offline y se sincroniza despues sin perdida de datos.
- Ningun usuario accede a datos de otra optica.
- Toda regla clinica automatica produce resultado reproducible para una misma entrada.
