package com.clubpadelcostabrava.desktop.data;

import java.util.List;

/** Una página de reservas recibida de la API. */
public record PaginaReservas(List<Reserva> reservas, boolean hayMas) {}
