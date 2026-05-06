package com.example.optoapp.data.mappers

import com.example.optoapp.data.Paciente as PacienteEntity
import com.example.optoapp.domain.model.PacienteModel

object PacienteMapper {
    fun toDomain(entity: PacienteEntity): PacienteModel {
        return PacienteModel(
            id = entity.id,
            nombreCompleto = entity.nombreCompleto,
            edad = entity.edad,
            telefono = entity.telefono,
            fechaCreacion = entity.fechaCreacion,
            dni = entity.dni,
            fechaNacimiento = entity.fechaNacimiento,
            sexo = entity.sexo,
            email = entity.email,
            historiaOptometrica = entity.historiaOptometrica,
            direccion = entity.direccion,
            distrito = entity.distrito,
            ocupacion = entity.ocupacion,
            acompanante = entity.acompanante,
            hobbies = entity.hobbies,
            ultimasEtiquetas = entity.ultimasEtiquetas,
            opticaId = entity.opticaId
        )
    }

    fun toEntity(model: PacienteModel): PacienteEntity {
        return PacienteEntity(
            id = model.id,
            nombreCompleto = model.nombreCompleto,
            edad = model.edad,
            telefono = model.telefono,
            fechaCreacion = model.fechaCreacion,
            dni = model.dni,
            fechaNacimiento = model.fechaNacimiento,
            sexo = model.sexo,
            email = model.email,
            historiaOptometrica = model.historiaOptometrica,
            direccion = model.direccion,
            distrito = model.distrito,
            ocupacion = model.ocupacion,
            acompanante = model.acompanante,
            hobbies = model.hobbies,
            ultimasEtiquetas = model.ultimasEtiquetas,
            opticaId = model.opticaId
        )
    }

    fun toDomainList(entities: List<PacienteEntity>): List<PacienteModel> {
        return entities.map { toDomain(it) }
    }
}
