package com.clubpadelcostabrava.desktop.data;

/** Respuesta del servidor tras cambiar el estado de un socio. */
public record ResultadoEstadoSocio(String estado, int reservasCanceladas, String avisoCorreo) {}
