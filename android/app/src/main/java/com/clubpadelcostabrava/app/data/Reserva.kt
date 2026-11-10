package com.clubpadelcostabrava.app.data

/**
 * Reserva recibida de la API y mostrada en la aplicación del socio.
 *
 * @property id identificador de la reserva
 * @property socioId identificador del socio titular
 * @property socioNombre nombre del socio titular
 * @property pistaId identificador de la pista reservada
 * @property pistaNombre nombre de la pista reservada
 * @property fecha fecha de la reserva
 * @property horaInicio hora a la que comienza la reserva
 * @property horaFin hora a la que termina la reserva
 * @property duracionHoras duración de la reserva en horas
 * @property estado estado actual de la reserva
 * @property creadaEn fecha y hora en que se creó
 * @property canceladaEn fecha y hora de cancelación, o null si no se canceló
 * @property canceladaPor rol de quien la canceló, o null si sigue confirmada
 * @property motivoCancelacion motivo registrado, si lo hubo
 * @property puedeCancelar indica si el socio puede solicitar su cancelación
 */
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