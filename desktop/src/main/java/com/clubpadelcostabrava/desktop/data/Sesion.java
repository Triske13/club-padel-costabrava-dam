package com.clubpadelcostabrava.desktop.data;

/**
 * Datos de autenticación devueltos por la API al iniciar sesión.
 *
 * @param token    token utilizado para autorizar las peticiones posteriores
 * @param rol      rol de la cuenta autenticada
 * @param expiraEn fecha y hora en que caduca el token
 */
public record Sesion(String token, String rol, String expiraEn) {
}
