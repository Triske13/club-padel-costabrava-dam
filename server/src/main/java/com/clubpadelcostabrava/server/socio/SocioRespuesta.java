package com.clubpadelcostabrava.server.socio;

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
