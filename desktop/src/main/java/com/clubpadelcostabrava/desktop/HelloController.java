package com.clubpadelcostabrava.desktop;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HelloController {
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    private Label welcomeText;

    @FXML
    protected void onCheckServerClick() {
        welcomeText.setText("Consultando servidor...");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ApiConfig.BASE_URL + "/api/diagnostico"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        welcomeText.setText("No se pudo conectar: " + error.getMessage());
                    } else if (response.statusCode() != 200) {
                        welcomeText.setText("Error HTTP " + response.statusCode());
                    } else {
                        welcomeText.setText(response.body());
                    }
                }));
    }
}
