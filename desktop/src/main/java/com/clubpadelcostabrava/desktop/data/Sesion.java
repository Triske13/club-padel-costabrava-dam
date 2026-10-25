package com.clubpadelcostabrava.desktop.data;

/** Datos que devuelve la API al iniciar sesión. */
public record Sesion(String token, String rol, String expiraEn) {}
