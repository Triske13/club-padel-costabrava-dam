package com.clubpadelcostabrava.desktop.ui;

import com.clubpadelcostabrava.desktop.data.ApiCliente;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

/** Controla el formulario de acceso de administración. */
public class LoginController {

    @FXML private TextField correoField;
    @FXML private PasswordField contrasenaField;
    @FXML private Button entrarButton;
    @FXML private Label mensajeLabel;
    private final ApiCliente api = new ApiCliente();

    @FXML
    private void onEntrar() {
        String correo = correoField.getText().strip();
        String contrasena = contrasenaField.getText();

        if (correo.isBlank() || contrasena.isBlank()) {
            mensajeLabel.setText("Indica el correo y la contraseña.");
            return;
        }

        entrarButton.setDisable(true);
        mensajeLabel.setText("Conectando...");

        api.iniciarSesion(correo, contrasena).whenComplete((sesion, fallo) ->
                Platform.runLater(() -> {
                    entrarButton.setDisable(false);

                    if (fallo != null) {
                        Throwable causa = fallo.getCause() != null
                                ? fallo.getCause() : fallo;
                        mensajeLabel.setText(
                                causa.getMessage() != null
                                        ? causa.getMessage()
                                        : "No se pudo iniciar sesión."
                        );
                        return;
                    }

                    if (!"ADMIN".equals(sesion.rol())) {
                        mensajeLabel.setText("Esta aplicación es solo para administración.");
                        return;
                    }

                    try {
                        FXMLLoader cargador = new FXMLLoader(
                                LoginController.class.getResource(
                                        "/com/clubpadelcostabrava/desktop/reservas-view.fxml"
                                )
                        );
                        Parent vistaReservas = cargador.load();

                        ReservasController controlador = cargador.getController();
                        Stage ventana = (Stage) entrarButton.getScene().getWindow();
                        ventana.getScene().setRoot(vistaReservas);
                        ventana.sizeToScene();

                        controlador.setSesion(sesion);
                    } catch (IOException e) {
                        mensajeLabel.setText("No se pudo abrir la pantalla de reservas.");
                    }
                })
        );
    }
}
