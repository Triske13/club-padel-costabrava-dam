package com.clubpadelcostabrava.server.socio;

public record CrearSocioPeticion(
        String nombre,
        String apellidos,
        String telefono,
        String direccion,
        String poblacion,
        String correo
) {}
