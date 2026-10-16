package com.clubpadelcostabrava.server.api;

/** Formato común del código y mensaje que devuelve la API ante un error. */
public record ErrorApi(String codigo, String mensaje) {}
