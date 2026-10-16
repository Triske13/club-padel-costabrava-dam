package com.clubpadelcostabrava.server.auth;

/** Datos enviados para establecer o recuperar la contraseña con un código. */
public record EstablecerContrasenaPeticion(String correo, String codigo, String nuevaContrasena) {}
