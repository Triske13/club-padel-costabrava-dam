package com.clubpadelcostabrava.desktop.ui;

import com.clubpadelcostabrava.desktop.data.ApiCliente;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Locale;

public class CodigoController {

    @FXML
    private TextField correoField;
    @FXML
    private TextField codigoField;
    @FXML
    private PasswordField nuevaField;
    @FXML
    private Button solicitarButton;
    @FXML
    private Button guardarButton;
    @FXML
    private Label mensajeLabel;

    private final ApiCliente api = new ApiCliente();

    @FXML
    private void onSolicitarCodigo() {
        String correo = correoField.getText()
                .strip()
                .toLowerCase(Locale.ROOT);

        if (correo.isBlank()) {
            mensajeLabel.setText("Indica un correo.");
            return;
        }

        solicitarButton.setDisable(true);
        mensajeLabel.setText("Solicitando código...");

        api.solicitarCodigo(correo).whenComplete((ignorado, fallo) ->
                Platform.runLater(() -> {
                    solicitarButton.setDisable(false);

                    if (fallo != null) {
                        Throwable causa = fallo.getCause() != null
                                ? fallo.getCause() : fallo;
                        mensajeLabel.setText(
                                causa.getMessage() != null
                                        ? causa.getMessage()
                                        : "No se pudo solicitar el código."
                        );
                        return;
                    }

                    mensajeLabel.setText(
                            "Si el correo corresponde a una cuenta habilitada, " +
                                    "recibirás un código."
                    );
                })
        );
    }

    @FXML
    private void onGuardarContrasena() {
        String correo = correoField.getText()
                .strip()
                .toLowerCase(Locale.ROOT);
        String codigo = codigoField.getText().strip();
        String nueva = nuevaField.getText();

        if (correo.isBlank()) {
            mensajeLabel.setText("Indica un correo.");
            return;
        }
        if (!codigo.matches("[0-9]{6}")) {
            mensajeLabel.setText("El código debe tener exactamente 6 cifras.");
            return;
        }

        int longitud = nueva.codePointCount(0, nueva.length());
        if (longitud < 10 || longitud > 64) {
            mensajeLabel.setText(
                    "La nueva contraseña debe tener entre 10 y 64 caracteres."
            );
            return;
        }

        guardarButton.setDisable(true);
        solicitarButton.setDisable(true);
        mensajeLabel.setText("Guardando contraseña...");

        api.establecerContrasena(correo, codigo, nueva)
                .whenComplete((ignorado, fallo) ->
                        Platform.runLater(() -> {
                            guardarButton.setDisable(false);
                            solicitarButton.setDisable(false);

                            if (fallo != null) {
                                Throwable causa = fallo.getCause() != null
                                        ? fallo.getCause() : fallo;
                                mensajeLabel.setText(
                                        causa.getMessage() != null
                                                ? causa.getMessage()
                                                : "No se pudo guardar la contraseña."
                                );
                                return;
                            }

                            codigoField.clear();
                            nuevaField.clear();
                            new Alert(
                                    Alert.AlertType.INFORMATION,
                                    "Contraseña guardada. Inicia sesión de nuevo."
                            ).showAndWait();
                            onVolver();
                        })
                );
    }

    @FXML
    private void onVolver() {
        try {
            FXMLLoader cargador = new FXMLLoader(
                    getClass().getResource(
                            "/com/clubpadelcostabrava/desktop/login-view.fxml"
                    )
            );
            Parent vistaLogin = cargador.load();

            Stage ventana = (Stage) correoField.getScene().getWindow();
            ventana.getScene().setRoot(vistaLogin);
            ventana.sizeToScene();
        } catch (IOException e) {
            mensajeLabel.setText("No se pudo abrir la pantalla de acceso.");
        }
    }
}