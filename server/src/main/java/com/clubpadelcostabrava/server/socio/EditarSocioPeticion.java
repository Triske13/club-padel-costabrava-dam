package com.clubpadelcostabrava.server.socio;

/** Datos personales editables; el correo se cambia mediante otra operación. */
public record EditarSocioPeticion(
        String nombre,
        String apellidos,
        String telefono,
        String direccion,
        String poblacion
) {}
