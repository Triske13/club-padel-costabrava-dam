package com.clubpadelcostabrava.server.reserva;

/**
 * Resultado de una cancelación administrativa y del intento de aviso.
 *
 * @param reserva     reserva ya cancelada
 * @param avisoCorreo ACEPTADO si el servidor de correo aceptó el envío,
 *                    o FALLIDO si no pudo enviarse
 */
public record CancelarReservaAdminRespuesta(ReservaRespuesta reserva, String avisoCorreo) {
}
