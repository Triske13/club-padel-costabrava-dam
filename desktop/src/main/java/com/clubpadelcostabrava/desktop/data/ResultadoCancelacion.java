package com.clubpadelcostabrava.desktop.data;

/** Resultado de cancelar desde recepción, incluido el aviso por correo. */
public record ResultadoCancelacion(Reserva reserva, String avisoCorreo) {}
