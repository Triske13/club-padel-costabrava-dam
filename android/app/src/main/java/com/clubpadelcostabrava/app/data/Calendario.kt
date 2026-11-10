package com.clubpadelcostabrava.app.data

import java.time.LocalDate
import java.time.YearMonth

/**
 * Resumen de una fecha dentro del calendario mensual.
 *
 * @property fecha día representado
 * @property consultable indica si se puede consultar su disponibilidad
 * @property completo indica si no quedan opciones de reserva
 * @property sinHorasFuturas indica si ya no quedan horas futuras en ese día
 */
data class DiaCalendario(
    val fecha: LocalDate,
    val consultable: Boolean,
    val completo: Boolean,
    val sinHorasFuturas: Boolean
)

/**
 * Calendario mensual resumido recibido de la API.
 *
 * @property mes mes al que pertenecen los días
 * @property dias estado de cada fecha incluida en el mes
 */
data class CalendarioMes(
    val mes: YearMonth,
    val dias: List<DiaCalendario>
)