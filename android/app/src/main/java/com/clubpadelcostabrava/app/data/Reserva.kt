package com.clubpadelcostabrava.app.data

data class Reserva(
    val id: Int,
    val socioId: Int,
    val socioNombre: String,
    val pistaId: Int,
    val pistaNombre: String,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val duracionHoras: Int,
    val estado: String,
    val creadaEn: String,
    val canceladaEn: String?,
    val canceladaPor: String?,
    val motivoCancelacion: String?,
    val puedeCancelar: Boolean
)