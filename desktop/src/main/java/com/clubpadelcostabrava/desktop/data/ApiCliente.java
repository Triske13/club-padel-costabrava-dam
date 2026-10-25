package com.clubpadelcostabrava.desktop.data;

import com.clubpadelcostabrava.desktop.ApiConfig;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

    /** Devuelve el cuerpo esperado o lanza el mensaje de error del servidor. */
    private String leerRespuesta(HttpResponse<String> respuesta, int estadoEsperado) {
        int estado = respuesta.statusCode();

        if (estado == estadoEsperado) {
            return respuesta.body();
        }

        String mensaje = "Error HTTP " + estado;

        try {
            JsonObject error = JsonParser.parseString(respuesta.body()).getAsJsonObject();
            if (error.has("mensaje") && !error.get("mensaje").isJsonNull()) {
                mensaje = error.get("mensaje").getAsString();
            }
        } catch (RuntimeException ignorada) {
            // Si no hay JSON válido, conservamos el mensaje con el código HTTP.
        }

        throw new IllegalStateException(mensaje);
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
}
