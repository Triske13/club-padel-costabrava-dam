package com.clubpadelcostabrava.app.data

import java.io.IOException

/**
 * Representa una respuesta de error HTTP recibida de la API.
 *
 * @property estado código de estado HTTP devuelto por el servidor
 * @param message mensaje de error que se muestra o procesa en la interfaz
 */
class ApiException(val estado: Int, message: String) : IOException(message)