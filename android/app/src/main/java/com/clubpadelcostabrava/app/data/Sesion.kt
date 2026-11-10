package com.clubpadelcostabrava.app.data

/**
 * Datos devueltos por la API después de iniciar sesión.
 *
 * @property token token utilizado para autorizar las siguientes peticiones
 * @property rol rol de la cuenta autenticada
 * @property expiraEn fecha y hora en que caduca la sesión
 */
data class Sesion(
    val token: String,
    val rol: String,
    val expiraEn: String
)