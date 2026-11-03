package com.clubpadelcostabrava.desktop.data;

/** Datos de una ficha de socio recibidos de la API. */
public record Socio(
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