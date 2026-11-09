package com.clubpadelcostabrava.server.reserva;

import java.util.List;

/** Una página de reservas y la indicación de si existe otra página. */
public record PaginaReservasRespuesta(List<ReservaRespuesta> reservas, boolean hayMas) {}
