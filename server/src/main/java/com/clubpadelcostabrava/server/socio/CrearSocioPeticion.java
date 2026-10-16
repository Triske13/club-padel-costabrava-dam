package com.clubpadelcostabrava.server.socio;

/** Campos enviados por administración para dar de alta a un socio. */
public record CrearSocioPeticion(
        String nombre,
        String apellidos,
        String telefono,
        String direccion,
        String poblacion,
        String correo
) {}
