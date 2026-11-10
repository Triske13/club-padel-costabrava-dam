package com.clubpadelcostabrava.desktop.api;

/**
 * Error HTTP de la API con su estado, código y mensaje.
 */
public class ApiException extends RuntimeException {

    private final int estado;
    private final String codigo;

    /**
     * Obtiene el código de error enviado por la API.
     *
     * @return código de error
     */
    public String codigo() {
        return codigo;
    }

    /**
     * Crea un error a partir de la respuesta recibida del servidor.
     *
     * @param estado  estado HTTP
     * @param codigo  código de error de la API
     * @param mensaje explicación del error
     */
    public ApiException(int estado, String codigo, String mensaje) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
    }

    /**
     * Obtiene el estado HTTP de la respuesta.
     *
     * @return estado HTTP
     */
    public int estado() {
        return estado;
    }
}