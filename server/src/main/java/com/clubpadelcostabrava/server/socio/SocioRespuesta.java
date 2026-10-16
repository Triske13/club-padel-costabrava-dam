package com.clubpadelcostabrava.server.socio;

/** Datos públicos de la ficha, incluido si la cuenta ya tiene contraseña. */
public record SocioRespuesta(
        int id,
        String nombre,
        String apellidos,
        String telefono,
        String direccion,
        String poblacion,
        String correo,
        String estado,
        boolean cuentaActivada
) {}
