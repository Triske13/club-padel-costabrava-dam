package com.clubpadelcostabrava.server.socio;

public record EditarSocioPeticion(
        String nombre,
        String apellidos,
        String telefono,
        String direccion,
        String poblacion
) {}
