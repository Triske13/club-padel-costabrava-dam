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

class ApiCliente {

    /** Prepara una petición HTTP al servidor. */
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

    /** Lee la respuesta y convierte un error HTTP en un mensaje útil. */
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
            throw IOException(mensaje)
        }

        return texto
    }

    /** Envía las credenciales y devuelve la sesión recibida. */
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

    /** Convierte el JSON de disponibilidad en objetos de la aplicación. */
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

    /** Convierte el JSON de una reserva en un objeto de la aplicación. */
    private fun parsearReserva(json: JSONObject): Reserva {
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

    /** Crea una reserva del socio identificado por el token. */
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

    /** Consulta las pistas y horas disponibles para una fecha. */
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

    /** Obtiene las reservas del socio identificado por el token. */
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
}