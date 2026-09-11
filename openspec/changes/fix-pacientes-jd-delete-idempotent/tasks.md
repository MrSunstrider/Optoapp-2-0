# Tasks: fix-pacientes-jd-delete-idempotent

- [x] 1.1 RED: PacienteViewModelTest — second delete after remote IOException does not increment again (tombstone already present)
- [x] 1.2 GREEN: OptoRepository/ViewModel — detect pending deletion; increment only on new tombstone
- [x] 1.3 RED/GREEN: DetallePaciente — local-success/remote-fail navigates away (result type or message contract)
- [x] 1.4 Verify focused PacienteViewModel* tests green
