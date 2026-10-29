package com.clubpadelcostabrava.app.data

import java.time.LocalDate
import java.time.YearMonth

/** Estado de una fecha dentro del calendario mensual. */
data class DiaCalendario(
    val fecha: LocalDate,
    val consultable: Boolean,
    val completo: Boolean,
    val sinHorasFuturas: Boolean
)

/** Resumen de los días de un mes recibido del servidor. */
data class CalendarioMes(
    val mes: YearMonth,
    val dias: List<DiaCalendario>
)