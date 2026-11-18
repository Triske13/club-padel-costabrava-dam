package com.clubpadelcostabrava.desktop;

/**
 * Centraliza la dirección base de la API utilizada por el cliente Windows.
 */
public final class ApiConfig {

    /**
     * Dirección del servidor durante las pruebas locales.
     */
    public static final String BASE_URL = System.getProperty("padel.api.baseUrl", "http://localhost:8080");

    /**
     * Impide crear instancias de esta clase de configuración.
     */
    private ApiConfig() {
    }
}
