package com.clubpadelcostabrava.server.reserva;

/** Reserva cancelada y resultado del intento de aviso por correo. */
public record CancelarReservaAdminRespuesta(ReservaRespuesta reserva, String avisoCorreo) {}
