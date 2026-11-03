package com.clubpadelcostabrava.desktop.data;

/** Error devuelto por la API, con su estado HTTP. */
public class ApiException extends RuntimeException {

    private final int estado;
    private final String codigo;

    public String codigo() {
        return codigo;
    }

    public ApiException(int estado, String codigo, String mensaje) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
    }

    public int estado() {
        return estado;
    }
}