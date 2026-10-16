package com.clubpadelcostabrava.server.socio;

/** Correo nuevo y contraseña para volver a identificar al administrador. */
public record CambiarCorreoPeticion(
        String nuevoCorreo,
        String contrasenaAdmin
) {}
