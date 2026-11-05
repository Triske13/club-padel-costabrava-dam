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

/** Comunica el cliente Windows con la API del servidor sin bloquear JavaFX. */
public final class ApiCliente {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /** Prepara la dirección, el tiempo máximo y el token si existe. */
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

    /** Inicia sesión sin bloquear la ventana de JavaFX. */
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

    /** Invalida en el servidor el token de la sesión actual. */
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

    /** Cambia la contraseña de la cuenta autenticada. */
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

    /** Solicita un código para establecer o recuperar la contraseña. */
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

    /** Establece una nueva contraseña mediante un código recibido por correo. */
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

    /** Consulta las reservas visibles para administración. */
    public CompletableFuture<List<Reserva>> listarReservasAdmin(String token) {
        HttpRequest peticion = prepararPeticion("/api/admin/reservas", token)
                .GET()
                .build();

        return httpClient.sendAsync(
                peticion,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        ).thenApply(respuesta -> {
            String texto = leerRespuesta(respuesta, 200);
            JsonArray elementos = JsonParser.parseString(texto).getAsJsonArray();
            List<Reserva> reservas = new ArrayList<>();

            for (JsonElement elemento : elementos) {
                reservas.add(parsearReserva(elemento.getAsJsonObject()));
            }

            return List.copyOf(reservas);
        });
    }

    /** Consulta las horas de todas las pistas para una fecha. */
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

    /** Consulta los bloqueos manuales de una fecha. */
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

    /** Crea un bloqueo manual para una pista. */
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

    /** Retira un bloqueo futuro. */
    public CompletableFuture<Void> retirarBloqueo(String token, int bloqueoId) {
        HttpRequest peticion = prepararPeticion(
                "/api/admin/bloqueos/" + bloqueoId, token
        ).DELETE().build();

        return httpClient.sendAsync(
                peticion,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        ).thenAccept(respuesta -> leerRespuesta(respuesta, 204));
    }

    /** Crea la ficha y la cuenta de un socio desde ADMIN. */
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

    /** Edita la ficha de un socio desde ADMIN. */
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

    /** Edita el correo de un socio desde ADMIN. */
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

    /** Cambia el estado de un socio desde ADMIN. */
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

    /** Crea una reserva para un socio desde la cuenta ADMIN. */
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

    /** Cancela una reserva desde ADMIN y lee el resultado del correo. */
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

    /** Devuelve el cuerpo esperado o lanza el mensaje de error del servidor. */
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

    /** Convierte los datos JSON de la API en una reserva para la interfaz. */
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

    /** Consulta las fichas de socios visibles para ADMIN. */
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

    /** Convierte el JSON de una ficha en un objeto Socio. */
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

    /** Convierte un bloqueo recibido en JSON en datos para la interfaz. */
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
