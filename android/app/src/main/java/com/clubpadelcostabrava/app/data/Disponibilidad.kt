package com.clubpadelcostabrava.app.data

/**
 * Estado de una hora de una pista.
 *
 * @property horaInicio hora de inicio de la franja
 * @property estado indica si está libre, reservada, bloqueada o ya pasó
 * @property duracionesDisponibles duraciones que pueden comenzar en esta hora
 */
data class FranjaDisponible(
    val horaInicio: String,
    val estado: String,
    val duracionesDisponibles: List<Int>
)

/**
 * Disponibilidad de una pista durante el día consultado.
 *
 * @property id identificador de la pista
 * @property nombre nombre mostrado al socio
 * @property franjas horas y estados de la pista durante ese día
 */
data class PistaDisponible(
    val id: Int,
    val nombre: String,
    val franjas: List<FranjaDisponible>
)

/**
 * Disponibilidad de todas las pistas para una fecha.
 *
 * @property fecha fecha consultada
 * @property horaApertura hora de apertura aplicable a ese día
 * @property horaCierre hora de cierre aplicable a ese día
 * @property dentroPlazoReserva indica si la fecha está dentro del plazo reservable
 * @property pistas disponibilidad de cada pista
 */
data class DisponibilidadDia(
    val fecha: String,
    val horaApertura: String,
    val horaCierre: String,
    val dentroPlazoReserva: Boolean,
    val pistas: List<PistaDisponible>
)