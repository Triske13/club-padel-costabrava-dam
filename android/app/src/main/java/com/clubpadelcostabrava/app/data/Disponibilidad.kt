package com.clubpadelcostabrava.app.data

data class FranjaDisponible(
    val horaInicio: String,
    val estado: String,
    val duracionesDisponibles: List<Int>
)

data class PistaDisponible(
    val id: Int,
    val nombre: String,
    val franjas: List<FranjaDisponible>
)

data class DisponibilidadDia(
    val fecha: String,
    val horaApertura: String,
    val horaCierre: String,
    val dentroPlazoReserva: Boolean,
    val pistas: List<PistaDisponible>
)