package com.clubpadelcostabrava.desktop.api;

import com.clubpadelcostabrava.desktop.ApiConfig;
import com.clubpadelcostabrava.desktop.data.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Comunica la aplicación de escritorio con la API del servidor.
 * Envía las peticiones de forma asíncrona para no bloquear la interfaz JavaFX
 * y convierte las respuestas JSON en los datos que utilizan las pantallas.
 */
public final class ApiCliente {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * Prepara una petición HTTP con la URL, los tiempos de espera y las cabeceras comunes.
     * Añade la autorización cuando la operación requiere una sesión iniciada.
     *
     * @param ruta  ruta de la API que se añade a la URL base
     * @param token token de la sesión, o null para una operación pública
     * @return constructor de la petición HTTP, listo para indicar el método y el cuerpo
     */
    private HttpRequest.Builder prepararPeticion(String ruta, String token) {
        HttpRequest.Builder peticion = HttpRequest.newBuilder()
                .uri(URI.create(ApiConfig.BASE_URL + ruta))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json");

        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }

        return peticion;
    }

    /**
     * Envía el correo y la contraseña para iniciar sesión.
     *
     * @param correo     correo de la cuenta
     * @param contrasena contraseña introducida por el usuario
     * @return operación asíncrona que proporciona el token, el rol y la caducidad
     * de la sesión cuando el servidor acepta las credenciales
     */
    public CompletableFuture<Sesion> iniciarSesion(
            String correo, String contrasena
    ) {
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("correo", correo);
        cuerpo.addProperty("contrasena", contrasena);

        HttpRequest peticion = prepararPeticion("/api/auth/login", null)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(
                        cuerpo.toString(), StandardCharsets.UTF_8
                ))
                .build();

        return httpClient.sendAsync(
                peticion,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        ).thenApply(respuesta -> {
            String texto = leerRespuesta(respuesta, 200);
            JsonObject json = JsonParser.parseString(texto).getAsJsonObject();

            return new Sesion(
                    json.get("token").getAsString(),
                    json.get("rol").getAsString(),
                    json.get("expiraEn").getAsString()
            );
        });
    }

    /**
     * Solicita al servidor que invalide el token de la sesión actual.
     *
     * @param token token de la sesión que se quiere cerrar
     * @return operación asíncrona que termina cuando el servidor confirma el cierre
     */
    public CompletableFuture<Void> cerrarSesion(String token) {
        HttpRequest peticion = prepararPeticion("/api/auth/logout", token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return httpClient.sendAsync(
                peticion,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        ).thenAccept(respuesta -> {
            leerRespuesta(respuesta, 204);
        });
    }

    /**
     * Cambia la contraseña de la cuenta autenticada.
     *
     * @param token            token de la sesión actual
     * @param contrasenaActual contraseña vigente de la cuenta
     * @param nuevaContrasena  contraseña que se quiere establecer
     * @return operación asíncrona que termina cuando el cambio queda confirmado
     */
    public CompletableFuture<Void> cambiarContrasena(
            String token,
            String contrasenaActual,
            String nuevaContrasena
    ) {
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("contrasenaActual", contrasenaActual);
        cuerpo.addProperty("nuevaContrasena", nuevaContrasena);

        HttpRequest peticion = prepararPeticion(
                "/api/mi-cuenta/contrasena", token
        )
                .header("Content-Type", "application/json; charset=utf-8")
                .PUT(HttpRequest.BodyPublishers.ofString(
                        cuerpo.toString(), StandardCharsets.UTF_8
                ))
                .build();

        return httpClient.sendAsync(
                peticion,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        ).thenAccept(respuesta -> {
            leerRespuesta(respuesta, 204);
        });
    }

    /**
     * Solicita un código para establecer o recuperar la contraseña.
     * El servidor acepta la solicitud sin revelar si el correo corresponde a una cuenta.
     *
     * @param correo correo para el que se solicita el código
     * @return operación asíncrona que termina cuando el servidor acepta la solicitud
     */
    public CompletableFuture<Void> solicitarCodigo(String correo) {
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("correo", correo);

        HttpRequest peticion = prepararPeticion("/api/auth/codigo", null)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(
                        cuerpo.toString(), StandardCharsets.UTF_8
                ))
                .build();

        return httpClient.sendAsync(
                peticion,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        ).thenAccept(respuesta -> {
            leerRespuesta(respuesta, 202);
        });
    }

    /**
     * Establece una nueva contraseña mediante un código recibido por correo.
     *
     * @param correo          correo de la cuenta
     * @param codigo          código de seguridad recibido
     * @param nuevaContrasena contraseña que se quiere establecer
     * @return operación asíncrona que termina cuando el servidor confirma el cambio
     */
    public CompletableFuture<Void> establecerContrasena(
            String correo,
            String codigo,
            String nuevaContrasena
    ) {
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("correo", correo);
        cuerpo.addProperty("codigo", codigo);
        cuerpo.addProperty("nuevaContrasena", nuevaContrasena);

        HttpRequest peticion = prepararPeticion("/api/auth/contrasena", null)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(
                        cuerpo.toString(), StandardCharsets.UTF_8
                ))
                .build();

        return httpClient.sendAsync(
                peticion,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        ).thenAccept(respuesta -> {
            leerRespuesta(respuesta, 204);
        });
    }

    /**
     * Consulta una página de reservas para la vista de administración.
     * Si no se indica una fecha, el servidor devuelve reservas desde el día actual;
     * si se indica, consulta las reservas de esa fecha.
     *
     * @param token  token de la sesión ADMIN
     * @param fecha  fecha concreta del histórico, o null para las reservas actuales
     * @param pagina número de página, comenzando por cero
     * @return operación asíncrona con las reservas de la página y la indicación
     * de si existe una página siguiente
     */
    public CompletableFuture<PaginaReservas> listarPaginaReservasAdmin(
            String token, LocalDate fecha, int pagina
    ) {
        String ruta = "/api/admin/reservas/pagina?pagina=" + pagina;
        if (fecha != null) {
            ruta += "&fecha=" + fecha;
        }

        HttpRequest peticion = prepararPeticion(ruta, token)
                .GET()
                .build();

        return httpClient.sendAsync(
                peticion,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        ).thenApply(respuesta -> {
            String texto = leerRespuesta(respuesta, 200);
            JsonObject json = JsonParser.parseString(texto).getAsJsonObject();

            List<Reserva> reservas = new ArrayList<>();
            for (JsonElement elemento : json.getAsJsonArray("reservas")) {
                reservas.add(parsearReserva(elemento.getAsJsonObject()));
            }

            return new PaginaReservas(
                    List.copyOf(reservas),
                    json.get("hayMas").getAsBoolean()
            );
        });
    }

    /**
     * Consulta la disponibilidad de todas las pistas en una fecha y transforma
     * cada hora recibida en una franja que puede mostrar la agenda.
     *
     * @param token token de la sesión ADMIN
     * @param fecha fecha que se quiere consultar
     * @return operación asíncrona con las franjas de todas las pistas
     */
    public CompletableFuture<List<FranjaAgenda>> consultarAgenda(
            String token, LocalDate fecha
    ) {
        HttpRequest peticion = prepararPeticion(
                "/api/disponibilidad?fecha=" + fecha, token
        ).GET().build();

        return httpClient.sendAsync(
                peticion,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        ).thenApply(respuesta -> {
            String texto = leerRespuesta(respuesta, 200);
            JsonObject dia = JsonParser.parseString(texto).getAsJsonObject();
            List<FranjaAgenda> franjas = new ArrayList<>();

            for (JsonElement elementoPista : dia.getAsJsonArray("pistas")) {
                JsonObject pista = elementoPista.getAsJsonObject();
                int pistaId = pista.get("id").getAsInt();
                String nombre = pista.get("nombre").getAsString();

                for (JsonElement elementoFranja : pista.getAsJsonArray("franjas")) {
                    JsonObject franja = elementoFranja.getAsJsonObject();
                    List<Integer> duraciones = new ArrayList<>();
                    for (JsonElement valor : franja.getAsJsonArray("duracionesDisponibles")) {
                        duraciones.add(valor.getAsInt());
                    }
                    franjas.add(new FranjaAgenda(
                            pistaId,
                            nombre,
                            franja.get("horaInicio").getAsString(),
                            franja.get("estado").getAsString(),
                            List.copyOf(duraciones)
                    ));
                }
            }

            return List.copyOf(franjas);
        });
    }

    /**
     * Consulta los bloqueos manuales existentes en una fecha.
     *
     * @param token token de la sesión ADMIN
     * @param fecha fecha de los bloqueos que se quieren mostrar
     * @return operación asíncrona con los bloqueos encontrados
     */
    public CompletableFuture<List<Bloqueo>> listarBloqueos(
            String token, LocalDate fecha
    ) {
        HttpRequest peticion = prepararPeticion(
                "/api/admin/bloqueos?fecha=" + fecha,
                token
        ).GET().build();

        return httpClient.sendAsync(
                peticion,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        ).thenApply(respuesta -> {
            String texto = leerRespuesta(respuesta, 200);
            JsonArray elementos = JsonParser.parseString(texto).getAsJsonArray();
            List<Bloqueo> bloqueos = new ArrayList<>();

            for (JsonElement elemento : elementos) {
                bloqueos.add(parsearBloqueo(elemento.getAsJsonObject()));
            }

            return List.copyOf(bloqueos);
        });
    }

    /**
     * Crea un bloqueo manual para una pista y las horas indicadas.
     *
     * @param token       token de la sesión ADMIN
     * @param pistaId     identificador de la pista
     * @param fecha       fecha del bloqueo
     * @param horaInicio  hora a la que comienza
     * @param numeroHoras número de horas consecutivas que ocupa
     * @param descripcion motivo o descripción del bloqueo
     * @return operación asíncrona con el bloqueo creado por el servidor
     */
    public CompletableFuture<Bloqueo> crearBloqueo(
            String token,
            int pistaId,
            LocalDate fecha,
            LocalTime horaInicio,
            int numeroHoras,
            String descripcion
    ) {
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("pistaId", pistaId);
        cuerpo.addProperty("fecha", fecha.toString());
        cuerpo.addProperty("horaInicio", horaInicio.toString());
        cuerpo.addProperty("numeroHoras", numeroHoras);
        cuerpo.addProperty("descripcion", descripcion);

        HttpRequest peticion = prepararPeticion("/api/admin/bloqueos", token)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(
                        cuerpo.toString(), StandardCharsets.UTF_8
                ))
                .build();

        return httpClient.sendAsync(
                peticion,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        ).thenApply(respuesta -> {
            String texto = leerRespuesta(respuesta, 201);
            return parsearBloqueo(JsonParser.parseString(texto).getAsJsonObject());
        });
    }

    /**
     * Solicita la retirada de un bloqueo manual.
     *
     * @param token     token de la sesión ADMIN
     * @param bloqueoId identificador del bloqueo que se quiere retirar
     * @return operación asíncrona que termina cuando se confirma su retirada
     */
    public CompletableFuture<Void> retirarBloqueo(String token, int bloqueoId) {
        HttpRequest peticion = prepararPeticion(
                "/api/admin/bloqueos/" + bloqueoId, token
        ).DELETE().build();

        return httpClient.sendAsync(
                peticion,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        ).thenAccept(respuesta -> leerRespuesta(respuesta, 204));
    }

    /**
     * Crea desde administración la ficha de un socio y su cuenta asociada.
     *
     * @param token token de la sesión ADMIN
     * @param datos datos personales y correo del nuevo socio
     * @return operación asíncrona con la ficha creada
     */
    public CompletableFuture<Socio> crearSocio(String token, DatosSocio datos) {
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("nombre", datos.nombre());
        cuerpo.addProperty("apellidos", datos.apellidos());
        cuerpo.addProperty("telefono", datos.telefono());
        cuerpo.addProperty("direccion", datos.direccion());
        cuerpo.addProperty("poblacion", datos.poblacion());
        cuerpo.addProperty("correo", datos.correo());

        HttpRequest peticion = prepararPeticion("/api/admin/socios", token)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(
                        cuerpo.toString(), StandardCharsets.UTF_8
                ))
                .build();

        return httpClient.sendAsync(
                peticion,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        ).thenApply(respuesta -> {
            String texto = leerRespuesta(respuesta, 201);
            return parsearSocio(JsonParser.parseString(texto).getAsJsonObject());
        });
    }

    /**
     * Modifica los datos personales de la ficha de un socio.
     * El cambio de correo se realiza mediante otra operación.
     *
     * @param token   token de la sesión ADMIN
     * @param socioId identificador del socio
     * @param datos   datos personales que se quieren guardar
     * @return operación asíncrona con la ficha actualizada
     */
    public CompletableFuture<Socio> editarSocio(String token, int socioId, DatosSocio datos) {
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("nombre", datos.nombre());
        cuerpo.addProperty("apellidos", datos.apellidos());
        cuerpo.addProperty("telefono", datos.telefono());
        cuerpo.addProperty("direccion", datos.direccion());
        cuerpo.addProperty("poblacion", datos.poblacion());

        HttpRequest peticion = prepararPeticion("/api/admin/socios/" + socioId, token)
                .header("Content-Type", "application/json; charset=utf-8")
                .PUT(HttpRequest.BodyPublishers.ofString(
                        cuerpo.toString(), StandardCharsets.UTF_8
                ))
                .build();

        return httpClient.sendAsync(
                peticion,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        ).thenApply(respuesta -> {
            String texto = leerRespuesta(respuesta, 200);
            return parsearSocio(JsonParser.parseString(texto).getAsJsonObject());
        });
    }

    /**
     * Cambia el correo asociado a un socio tras confirmar la contraseña de ADMIN.
     *
     * @param token           token de la sesión ADMIN
     * @param socioId         identificador del socio
     * @param nuevoCorreo     correo que sustituirá al actual
     * @param contrasenaAdmin contraseña de ADMIN para autorizar el cambio
     * @return operación asíncrona con el correo guardado por el servidor
     */
    public CompletableFuture<String> cambiarCorreoSocio(String token, int socioId, String nuevoCorreo, String contrasenaAdmin) {
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("nuevoCorreo", nuevoCorreo);
        cuerpo.addProperty("contrasenaAdmin", contrasenaAdmin);

        HttpRequest peticion = prepararPeticion("/api/admin/socios/" + socioId + "/correo", token)
                .header("Content-Type", "application/json; charset=utf-8")
                .PUT(HttpRequest.BodyPublishers.ofString(
                        cuerpo.toString(), StandardCharsets.UTF_8
                ))
                .build();

        return httpClient.sendAsync(
                peticion,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        ).thenApply(respuesta -> {
            String texto = leerRespuesta(respuesta, 200);
            return JsonParser.parseString(texto)
                    .getAsJsonObject()
                    .get("correo")
                    .getAsString();
        });
    }

    /**
     * Activa o inactiva a un socio y obtiene el resultado de la operación.
     * Al inactivarlo, el resultado incluye las reservas canceladas y el estado
     * del aviso enviado por correo.
     *
     * @param token   token de la sesión ADMIN
     * @param socioId identificador del socio
     * @param estado  nuevo estado solicitado
     * @return operación asíncrona con el estado, el número de reservas
     * canceladas y el resultado del aviso
     */
    public CompletableFuture<ResultadoEstadoSocio> cambiarEstadoSocio(String token, int socioId, String estado) {
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("estado", estado);

        HttpRequest peticion = prepararPeticion("/api/admin/socios/" + socioId + "/estado", token)
                .header("Content-Type", "application/json; charset=utf-8")
                .PUT(HttpRequest.BodyPublishers.ofString(
                        cuerpo.toString(), StandardCharsets.UTF_8
                ))
                .build();

        return httpClient.sendAsync(
                peticion,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        ).thenApply(respuesta -> {
            String texto = leerRespuesta(respuesta, 200);
            JsonObject json = JsonParser.parseString(texto).getAsJsonObject();

            return new ResultadoEstadoSocio(
                    json.get("estado").getAsString(),
                    json.get("reservasCanceladas").getAsInt(),
                    json.get("avisoCorreo").getAsString()
            );
        });
    }

    /**
     * Crea desde administración una reserva para el socio indicado.
     *
     * @param token         token de la sesión ADMIN
     * @param socioId       identificador del socio titular
     * @param pistaId       identificador de la pista
     * @param fecha         fecha de la reserva en formato ISO
     * @param horaInicio    hora de inicio de la reserva
     * @param duracionHoras duración de una o dos horas
     * @return operación asíncrona con la reserva confirmada por el servidor
     */
    public CompletableFuture<Reserva> crearReservaAdmin(
            String token,
            int socioId,
            int pistaId,
            String fecha,
            String horaInicio,
            int duracionHoras
    ) {
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("socioId", socioId);
        cuerpo.addProperty("pistaId", pistaId);
        cuerpo.addProperty("fecha", fecha);
        cuerpo.addProperty("horaInicio", horaInicio);
        cuerpo.addProperty("duracionHoras", duracionHoras);

        HttpRequest peticion = prepararPeticion("/api/admin/reservas", token)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(
                        cuerpo.toString(), StandardCharsets.UTF_8
                ))
                .build();

        return httpClient.sendAsync(
                peticion,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        ).thenApply(respuesta -> {
            String texto = leerRespuesta(respuesta, 201);
            return parsearReserva(JsonParser.parseString(texto).getAsJsonObject());
        });
    }

    /**
     * Cancela una reserva desde administración con un motivo obligatorio.
     * El resultado permite informar si el aviso por correo se pudo enviar.
     *
     * @param token     token de la sesión ADMIN
     * @param reservaId identificador de la reserva
     * @param motivo    explicación de la cancelación
     * @return operación asíncrona con la reserva cancelada y el resultado del aviso
     */
    public CompletableFuture<ResultadoCancelacion> cancelarReservaAdmin(
            String token, int reservaId, String motivo
    ) {
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("motivo", motivo);

        HttpRequest peticion = prepararPeticion(
                "/api/admin/reservas/" + reservaId + "/cancelacion",
                token
        )
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(
                        cuerpo.toString(), StandardCharsets.UTF_8
                ))
                .build();

        return httpClient.sendAsync(
                peticion,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        ).thenApply(respuesta -> {
            String texto = leerRespuesta(respuesta, 200);
            JsonObject json = JsonParser.parseString(texto).getAsJsonObject();

            return new ResultadoCancelacion(
                    parsearReserva(json.getAsJsonObject("reserva")),
                    json.get("avisoCorreo").getAsString()
            );
        });
    }

    /**
     * Comprueba el estado HTTP de una respuesta.
     * Devuelve su cuerpo si coincide con el esperado; en caso contrario,
     * extrae el código y el mensaje de error enviados por la API.
     *
     * @param respuesta      respuesta HTTP recibida
     * @param estadoEsperado estado HTTP que indica que la operación tuvo éxito
     * @return cuerpo de la respuesta correcta
     * @throws ApiException si el estado HTTP no coincide con el esperado
     */
    private String leerRespuesta(HttpResponse<String> respuesta, int estadoEsperado) {
        int estado = respuesta.statusCode();

        if (estado == estadoEsperado) {
            return respuesta.body();
        }

        String mensaje = "Error HTTP " + estado;

        String codigo = "ERROR_HTTP";
        try {
            JsonObject error = JsonParser.parseString(respuesta.body()).getAsJsonObject();
            if (error.has("codigo") && !error.get("codigo").isJsonNull()) {
                codigo = error.get("codigo").getAsString();
            }
            if (error.has("mensaje") && !error.get("mensaje").isJsonNull()) {
                mensaje = error.get("mensaje").getAsString();
            }
        } catch (RuntimeException ignorada) {
            // Si no hay JSON válido, conservamos el mensaje con el código HTTP.
        }

        throw new ApiException(estado, codigo, mensaje);
    }

    /**
     * Convierte el objeto JSON de una reserva en los datos utilizados por la interfaz.
     *
     * @param json objeto JSON recibido de la API
     * @return reserva con sus datos principales y la posibilidad de cancelarla
     */
    private Reserva parsearReserva(JsonObject json) {
        return new Reserva(
                json.get("id").getAsInt(),
                json.get("socioId").getAsInt(),
                json.get("socioNombre").getAsString(),
                json.get("pistaId").getAsInt(),
                json.get("pistaNombre").getAsString(),
                json.get("fecha").getAsString(),
                json.get("horaInicio").getAsString(),
                json.get("horaFin").getAsString(),
                json.get("duracionHoras").getAsInt(),
                json.get("estado").getAsString(),
                json.get("puedeCancelar").getAsBoolean()
        );
    }

    /**
     * Consulta las fichas de socios disponibles para administración.
     *
     * @param token token de la sesión ADMIN
     * @return operación asíncrona con la lista de socios recibida de la API
     */
    public CompletableFuture<List<Socio>> listarSocios(String token) {
        HttpRequest peticion = prepararPeticion("/api/admin/socios", token)
                .GET()
                .build();

        return httpClient.sendAsync(
                peticion,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        ).thenApply(respuesta -> {
            String texto = leerRespuesta(respuesta, 200);
            JsonArray elementos = JsonParser.parseString(texto).getAsJsonArray();
            List<Socio> socios = new ArrayList<>();

            for (JsonElement elemento : elementos) {
                socios.add(parsearSocio(elemento.getAsJsonObject()));
            }

            return List.copyOf(socios);
        });
    }

    /**
     * Convierte el objeto JSON de una ficha de socio en el modelo de la aplicación.
     *
     * @param json objeto JSON recibido de la API
     * @return ficha de socio con sus datos personales y el estado de la cuenta
     */
    private Socio parsearSocio(JsonObject json) {
        return new Socio(
                json.get("id").getAsInt(),
                json.get("nombre").getAsString(),
                json.get("apellidos").getAsString(),
                json.get("telefono").getAsString(),
                json.get("direccion").getAsString(),
                json.get("poblacion").getAsString(),
                json.get("correo").getAsString(),
                json.get("estado").getAsString(),
                json.get("cuentaActivada").getAsBoolean()
        );
    }

    /**
     * Convierte el objeto JSON de un bloqueo manual en el modelo de la aplicación.
     * Admite que la descripción no esté presente o tenga valor null.
     *
     * @param json objeto JSON recibido de la API
     * @return bloqueo con su pista, fecha, horario y descripción
     */
    private Bloqueo parsearBloqueo(JsonObject json) {
        JsonElement descripcionJson = json.get("descripcion");
        String descripcion = descripcionJson == null || descripcionJson.isJsonNull()
                ? null
                : descripcionJson.getAsString();

        return new Bloqueo(
                json.get("id").getAsInt(),
                json.get("pistaId").getAsInt(),
                LocalDate.parse(json.get("fecha").getAsString()),
                LocalTime.parse(json.get("horaInicio").getAsString()),
                json.get("numeroHoras").getAsInt(),
                descripcion
        );
    }
}
