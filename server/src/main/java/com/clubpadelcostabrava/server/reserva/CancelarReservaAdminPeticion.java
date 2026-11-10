package com.clubpadelcostabrava.server.reserva;

/**
 * Motivo enviado por administración al cancelar una reserva.
 *
 * @param motivo explicación que quedará registrada y verá el socio
 */
public record CancelarReservaAdminPeticion(String motivo) {
}
