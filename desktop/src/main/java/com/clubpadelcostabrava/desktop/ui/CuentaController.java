package com.clubpadelcostabrava.desktop.ui;

import com.clubpadelcostabrava.desktop.api.ApiCliente;
import com.clubpadelcostabrava.desktop.api.ApiException;
import com.clubpadelcostabrava.desktop.data.Sesion;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

/**
 * Controla el cambio de contraseña de la cuenta ADMIN autenticada.
 * Tras un cambio correcto, informa al usuario y termina la sesión actual.
 */
public class CuentaController {

    @FXML
    private PasswordField actualField;
    @FXML
    private PasswordField nuevaField;
    @FXML
    private Button cambiarButton;
    @FXML
    private Label mensajeLabel;

    private final ApiCliente api = new ApiCliente();
    private Sesion sesion;
    private Runnable onSesionTerminada;

    /**
     * Recibe la sesión con la que se autorizará el cambio de contraseña
     * y la acción que debe ejecutarse cuando esa sesión deje de ser válida.
     *
     * @param sesion            sesión ADMIN actual
     * @param onSesionTerminada acción para volver al inicio de sesión
     */
    public void setSesion(Sesion sesion, Runnable onSesionTerminada) {
        this.sesion = sesion;
        this.onSesionTerminada = onSesionTerminada;
    }

    /**
     * Valida la contraseña actual y la longitud de la nueva antes de enviar
     * el cambio al servidor. Muestra el resultado y vuelve al inicio de sesión
     * si el cambio se confirma o si la sesión ha caducado.
     */
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