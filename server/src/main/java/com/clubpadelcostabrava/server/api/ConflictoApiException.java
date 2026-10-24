package com.clubpadelcostabrava.server.api;

/** Error de una operación que entra en conflicto con el estado actual. */
public class ConflictoApiException extends RuntimeException {

    private final String codigo;

    public ConflictoApiException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
