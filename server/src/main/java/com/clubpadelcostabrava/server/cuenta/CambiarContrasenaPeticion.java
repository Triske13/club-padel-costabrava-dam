package com.clubpadelcostabrava.server.cuenta;

/** Datos enviados para cambiar la contraseña. */
public record CambiarContrasenaPeticion(String contrasenaActual, String nuevaContrasena) {}
