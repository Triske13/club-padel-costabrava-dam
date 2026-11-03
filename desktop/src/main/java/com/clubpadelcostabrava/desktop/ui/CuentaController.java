package com.clubpadelcostabrava.desktop.ui;

import com.clubpadelcostabrava.desktop.data.ApiCliente;
import com.clubpadelcostabrava.desktop.data.ApiException;
import com.clubpadelcostabrava.desktop.data.Sesion;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

public class CuentaController {

    @FXML private PasswordField actualField;
    @FXML private PasswordField nuevaField;
    @FXML private Button cambiarButton;
    @FXML private Label mensajeLabel;

    private final ApiCliente api = new ApiCliente();
    private Sesion sesion;
    private Runnable onSesionTerminada;

    public void setSesion(Sesion sesion, Runnable onSesionTerminada) {
        this.sesion = sesion;
        this.onSesionTerminada = onSesionTerminada;
    }

    @FXML
    private void onCambiarContrasena() {
        String actual = actualField.getText();
        String nueva = nuevaField.getText();

        if (actual.isBlank()) {
            mensajeLabel.setText("Indica tu contraseña actual.");
            return;
        }

        int longitud = nueva.codePointCount(0, nueva.length());
        if (longitud < 10 || longitud > 64) {
            mensajeLabel.setText(
                    "La nueva contraseña debe tener entre 10 y 64 caracteres."
            );
            return;
        }
        cambiarButton.setDisable(true);
        mensajeLabel.setText("Guardando contraseña...");

        api.cambiarContrasena(sesion.token(), actual, nueva)
                .whenComplete((ignorado, fallo) ->
                        Platform.runLater(() -> {
                            cambiarButton.setDisable(false);
                            actualField.clear();

                            if (fallo != null) {
                                Throwable causa = fallo.getCause() != null
                                        ? fallo.getCause() : fallo;
                                if (causa instanceof ApiException errorApi
                                        && "SESION_INVALIDA".equals(errorApi.codigo())) {
                                    new Alert(
                                            Alert.AlertType.WARNING,
                                            "Tu sesión ya no es válida. Inicia sesión de nuevo."
                                    ).showAndWait();
                                    onSesionTerminada.run();
                                    return;
                                }
                                mensajeLabel.setText(
                                        causa.getMessage() != null
                                                ? causa.getMessage()
                                                : "No se pudo cambiar la contraseña."
                                );
                                return;
                            }

                            nuevaField.clear();
                            new Alert(
                                    Alert.AlertType.INFORMATION,
                                    "Contraseña actualizada. Inicia sesión de nuevo."
                            ).showAndWait();

                            onSesionTerminada.run();
                        })
                );
    }
}