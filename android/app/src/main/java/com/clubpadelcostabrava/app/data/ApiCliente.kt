package com.clubpadelcostabrava.app.data

import com.clubpadelcostabrava.app.ApiConfig
import java.net.HttpURLConnection
import java.net.URL
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import org.json.JSONArray
import java.time.YearMonth

/**
 * Comunica la aplicación Android con la API del servidor.
 * Ejecuta las peticiones de red en el contexto de entrada y salida,
 * comprueba los estados HTTP y convierte las respuestas JSON en modelos
 * utilizados por las pantallas.
 */
class ApiCliente {

    /**
     * Prepara una conexión HTTP con la ruta, el método, los tiempos de espera
     * y las cabeceras comunes. Añade el token si la ruta requiere autenticación.
     *
     * @param ruta ruta de la API que se añade a la URL base
     * @param metodo método HTTP que utilizará la petición
     * @param token token de sesión, o null para una ruta pública
     * @return conexión HTTP preparada para enviar la petición
     */
    private fun abrirConexion(
        ruta: String,
        metodo: String,
        token: String? = null
    ): HttpURLConnection {
        return (URL("${ApiConfig.BASE_URL}$ruta").openConnection() as HttpURLConnection).apply {
            requestMethod = metodo
            connectTimeout = 5000
            readTimeout = 5000
            setRequestProperty("Accept", "application/json")

            if (token != null) {
                setRequestProperty("Authorization", "Bearer $token")
            }
        }
    }

    /**
     * Lee el cuerpo de una respuesta HTTP y comprueba su estado.
     * Si el estado no es el esperado, extrae el mensaje devuelto por la API
     * y lo convierte en una excepción.
     *
     * @param conexion conexión que contiene la respuesta del servidor
     * @param estadoEsperado código HTTP que indica éxito para esta operación
     * @return cuerpo de la respuesta correcta como texto
     * @throws ApiException si el servidor devuelve otro estado HTTP
     */
    private fun leerRespuesta(
        conexion: HttpURLConnection,
        estadoEsperado: Int
    ): String {
        val estado = conexion.responseCode
        val entrada = if (estado == estadoEsperado) {
            conexion.inputStream
        } else {
            conexion.errorStream
        }

        val texto = entrada?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            .orEmpty()

        if (estado != estadoEsperado) {
            val mensaje = try {
                JSONObject(texto).optString("mensaje")
                    .ifBlank { "Error HTTP $estado" }
            } catch (_: Exception) {
                "Error HTTP $estado"
            }
            throw ApiException(estado, mensaje)
        }

        return texto
    }

    /**
     * Envía el correo y la contraseña al servidor para iniciar sesión.
     * La petición se realiza fuera del hilo principal de Android.
     *
     * @param correo correo de la cuenta
     * @param contrasena contraseña introducida
     * @return sesión con el token, el rol y su fecha de caducidad
     */
    suspend fun iniciarSesion(correo: String, contrasena: String): Sesion =
        withContext(Dispatchers.IO) {
            val conexion = abrirConexion("/api/auth/login", "POST")

            try {
                conexion.doOutput = true
                conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")

                val cuerpo = JSONObject()
                    .put("correo", correo)
                    .put("contrasena", contrasena)
                    .toString()

                conexion.outputStream.use { salida ->
                    salida.write(cuerpo.toByteArray(Charsets.UTF_8))
                }

                val texto = leerRespuesta(conexion, HttpURLConnection.HTTP_OK)

                val respuesta = JSONObject(texto)
                Sesion(
                    token = respuesta.getString("token"),
                    rol = respuesta.getString("rol"),
                    expiraEn = respuesta.getString("expiraEn")
                )
            } finally {
                conexion.disconnect()
            }
        }

    /**
     * Solicita al servidor que invalide el token de la sesión actual.
     *
     * @param token token de la sesión que se quiere cerrar
     */
    suspend fun cerrarSesion(token: String) {
        withContext(Dispatchers.IO) {
            val conexion = abrirConexion("/api/auth/logout", "POST", token)

            try {
                leerRespuesta(conexion, HttpURLConnection.HTTP_NO_CONTENT)
            } finally {
                conexion.disconnect()
            }
        }
    }

    /**
     * Solicita un código para activar la cuenta o recuperar la contraseña.
     * La respuesta de la API no revela si el correo corresponde a una cuenta.
     *
     * @param correo correo para el que se solicita el código
     */
    suspend fun solicitarCodigo(correo: String) {
        withContext(Dispatchers.IO) {
            val conexion = abrirConexion("/api/auth/codigo", "POST")

            try {
                conexion.doOutput = true
                conexion.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
                )

                val cuerpo = JSONObject()
                    .put("correo", correo)
                    .toString()

                conexion.outputStream.use { salida ->
                    salida.write(cuerpo.toByteArray(Charsets.UTF_8))
                }

                leerRespuesta(conexion, HttpURLConnection.HTTP_ACCEPTED)
            } finally {
                conexion.disconnect()
            }
        }
    }

    /**
     * Utiliza el código recibido por correo para establecer una contraseña nueva.
     *
     * @param correo correo de la cuenta
     * @param codigo código de seguridad recibido
     * @param nuevaContrasena contraseña que se quiere establecer
     */
    suspend fun establecerContrasena(
        correo: String,
        codigo: String,
        nuevaContrasena: String
    ) {
        withContext(Dispatchers.IO) {
            val conexion = abrirConexion("/api/auth/contrasena", "POST")

            try {
                conexion.doOutput = true
                conexion.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
                )

                val cuerpo = JSONObject()
                    .put("correo", correo)
                    .put("codigo", codigo)
                    .put("nuevaContrasena", nuevaContrasena)
                    .toString()

                conexion.outputStream.use { salida ->
                    salida.write(cuerpo.toByteArray(Charsets.UTF_8))
                }

                leerRespuesta(conexion, HttpURLConnection.HTTP_NO_CONTENT)
            } finally {
                conexion.disconnect()
            }
        }
    }

    /**
     * Cambia la contraseña de la cuenta autenticada mediante su contraseña actual.
     *
     * @param token token de la sesión actual
     * @param contrasenaActual contraseña vigente de la cuenta
     * @param nuevaContrasena contraseña que la sustituirá
     */
    suspend fun cambiarContrasena(
        token: String,
        contrasenaActual: String,
        nuevaContrasena: String
    ) {
        withContext(Dispatchers.IO) {
            val conexion = abrirConexion(
                "/api/mi-cuenta/contrasena",
                "PUT",
                token
            )

            try {
                conexion.doOutput = true
                conexion.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
                )

                val cuerpo = JSONObject()
                    .put("contrasenaActual", contrasenaActual)
                    .put("nuevaContrasena", nuevaContrasena)
                    .toString()

                conexion.outputStream.use { salida ->
                    salida.write(cuerpo.toByteArray(Charsets.UTF_8))
                }

                leerRespuesta(conexion, HttpURLConnection.HTTP_NO_CONTENT)
            } finally {
                conexion.disconnect()
            }
        }
    }

    /**
     * Convierte la disponibilidad diaria recibida en JSON en los modelos
     * de día, pistas y franjas utilizados por la aplicación.
     *
     * @param json respuesta JSON de disponibilidad de una fecha
     * @return disponibilidad del día con todas sus pistas y horas
     */
    private fun parsearDisponibilidad(json: JSONObject): DisponibilidadDia {
        val pistasJson = json.getJSONArray("pistas")

        val pistas = List(pistasJson.length()) { indicePista ->
            val pistaJson = pistasJson.getJSONObject(indicePista)
            val franjasJson = pistaJson.getJSONArray("franjas")

            val franjas = List(franjasJson.length()) { indiceFranja ->
                val franjaJson = franjasJson.getJSONObject(indiceFranja)
                val duracionesJson = franjaJson.getJSONArray("duracionesDisponibles")

                val duraciones = List(duracionesJson.length()) { indice ->
                    duracionesJson.getInt(indice)
                }

                FranjaDisponible(
                    horaInicio = franjaJson.getString("horaInicio"),
                    estado = franjaJson.getString("estado"),
                    duracionesDisponibles = duraciones
                )
            }

            PistaDisponible(
                id = pistaJson.getInt("id"),
                nombre = pistaJson.getString("nombre"),
                franjas = franjas
            )
        }

        return DisponibilidadDia(
            fecha = json.getString("fecha"),
            horaApertura = json.getString("horaApertura"),
            horaCierre = json.getString("horaCierre"),
            dentroPlazoReserva = json.getBoolean("dentroPlazoReserva"),
            pistas = pistas
        )
    }

    /**
     * Convierte el resumen mensual de la API en un calendario con el estado
     * de cada fecha.
     *
     * @param json respuesta JSON del calendario mensual
     * @return mes y lista de días con sus indicadores de disponibilidad
     */
    private fun parsearCalendario(json: JSONObject): CalendarioMes {
        val diasJson = json.getJSONArray("dias")

        val dias = List(diasJson.length()) { indice ->
            val diaJson = diasJson.getJSONObject(indice)

            DiaCalendario(
                fecha = LocalDate.parse(diaJson.getString("fecha")),
                consultable = diaJson.getBoolean("consultable"),
                completo = diaJson.getBoolean("completo"),
                sinHorasFuturas = diaJson.getBoolean("sinHorasFuturas")
            )
        }

        return CalendarioMes(
            mes = YearMonth.parse(json.getString("mes")),
            dias = dias
        )
    }

    /**
     * Convierte una reserva recibida en JSON en el modelo utilizado
     * por las pantallas de Android.
     *
     * @param json objeto JSON que contiene los datos de la reserva
     * @return reserva con sus datos de creación, estado y cancelación
     */
    private fun parsearReserva(json: JSONObject): Reserva {

        /**
         * Lee un campo de texto que puede contener null en la respuesta JSON.
         *
         * @param clave nombre del campo que se quiere leer
         * @return contenido del campo, o null si la API lo envió como null
         */
        fun textoOpcional(clave: String): String? {
            return if (json.isNull(clave)) null else json.getString(clave)
        }

        return Reserva(
            id = json.getInt("id"),
            socioId = json.getInt("socioId"),
            socioNombre = json.getString("socioNombre"),
            pistaId = json.getInt("pistaId"),
            pistaNombre = json.getString("pistaNombre"),
            fecha = json.getString("fecha"),
            horaInicio = json.getString("horaInicio"),
            horaFin = json.getString("horaFin"),
            duracionHoras = json.getInt("duracionHoras"),
            estado = json.getString("estado"),
            creadaEn = json.getString("creadaEn"),
            canceladaEn = textoOpcional("canceladaEn"),
            canceladaPor = textoOpcional("canceladaPor"),
            motivoCancelacion = textoOpcional("motivoCancelacion"),
            puedeCancelar = json.getBoolean("puedeCancelar")
        )
    }

    /**
     * Solicita la creación de una reserva para el socio identificado por el token.
     *
     * @param token token de la sesión del socio
     * @param pistaId identificador de la pista elegida
     * @param fecha fecha de la reserva
     * @param horaInicio hora a la que comenzará
     * @param duracionHoras duración solicitada, de una o dos horas
     * @return reserva confirmada y devuelta por el servidor
     */
    suspend fun crearReserva(
        token: String,
        pistaId: Int,
        fecha: LocalDate,
        horaInicio: String,
        duracionHoras: Int
    ): Reserva = withContext(Dispatchers.IO) {
        val conexion = abrirConexion("/api/mis-reservas", "POST", token)

        try {
            conexion.doOutput = true
            conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")

            val cuerpo = JSONObject()
                .put("pistaId", pistaId)
                .put("fecha", fecha.toString())
                .put("horaInicio", horaInicio)
                .put("duracionHoras", duracionHoras)
                .toString()

            conexion.outputStream.use { salida ->
                salida.write(cuerpo.toByteArray(Charsets.UTF_8))
            }

            val texto = leerRespuesta(conexion, HttpURLConnection.HTTP_CREATED)
            parsearReserva(JSONObject(texto))
        } finally {
            conexion.disconnect()
        }
    }

    /**
     * Consulta el horario y la disponibilidad de todas las pistas para una fecha.
     *
     * @param token token de la sesión del socio
     * @param fecha día cuya disponibilidad se quiere consultar
     * @return horario del día con las pistas y sus franjas
     */
    suspend fun consultarDisponibilidad(
        token: String,
        fecha: LocalDate
    ): DisponibilidadDia = withContext(Dispatchers.IO) {
        val conexion = abrirConexion(
            ruta = "/api/disponibilidad?fecha=$fecha",
            metodo = "GET",
            token = token
        )

        try {
            val texto = leerRespuesta(conexion, HttpURLConnection.HTTP_OK)
            parsearDisponibilidad(JSONObject(texto))
        } finally {
            conexion.disconnect()
        }
    }

    /**
     * Consulta los indicadores de disponibilidad de los días de un mes.
     *
     * @param token token de la sesión del socio
     * @param mes mes que se quiere mostrar en el calendario
     * @return resumen mensual con el estado de cada día
     */
    suspend fun consultarCalendario(
        token: String,
        mes: YearMonth
    ): CalendarioMes = withContext(Dispatchers.IO) {
        val conexion = abrirConexion(
            ruta = "/api/calendario?mes=$mes",
            metodo = "GET",
            token = token
        )

        try {
            val texto = leerRespuesta(conexion, HttpURLConnection.HTTP_OK)
            parsearCalendario(JSONObject(texto))
        } finally {
            conexion.disconnect()
        }
    }

    /**
     * Consulta las reservas del socio identificado por el token.
     *
     * @param token token de la sesión del socio
     * @return lista de reservas recibida de la API
     */
    suspend fun listarMisReservas(token: String): List<Reserva> =
        withContext(Dispatchers.IO) {
            val conexion = abrirConexion("/api/mis-reservas", "GET", token)

            try {
                val texto = leerRespuesta(conexion, HttpURLConnection.HTTP_OK)
                val json = JSONArray(texto)

                List(json.length()) { indice ->
                    parsearReserva(json.getJSONObject(indice))
                }
            } finally {
                conexion.disconnect()
            }
        }

    /**
     * Solicita la cancelación completa de una reserva propia.
     *
     * @param token token de la sesión del socio
     * @param reservaId identificador de la reserva que se quiere cancelar
     * @return reserva con el estado actualizado tras la cancelación
     */
    suspend fun cancelarMiReserva(token: String, reservaId: Int): Reserva =
        withContext(Dispatchers.IO) {
            val conexion = abrirConexion(
                "/api/mis-reservas/$reservaId/cancelacion",
                "POST",
                token
            )

            try {
                val texto = leerRespuesta(conexion, HttpURLConnection.HTTP_OK)
                parsearReserva(JSONObject(texto))
            } finally {
                conexion.disconnect()
            }
        }
}