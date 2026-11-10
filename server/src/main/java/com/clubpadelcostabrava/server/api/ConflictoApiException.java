package com.clubpadelcostabrava.server.api;

/**
 * Representa un conflicto entre la operación solicitada
 * y el estado actual de los datos.
 */
public class ConflictoApiException extends RuntimeException {

    private final String codigo;

    /**
     * Crea el conflicto con un código para la API y un mensaje legible.
     *
     * @param codigo  identificador del conflicto
     * @param mensaje explicación del conflicto
     */
    public ConflictoApiException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    /**
     * Obtiene el código que el controlador incluirá en la respuesta.
     *
     * @return identificador del conflicto
     */
    public String codigo() {
        return codigo;
    }
}
