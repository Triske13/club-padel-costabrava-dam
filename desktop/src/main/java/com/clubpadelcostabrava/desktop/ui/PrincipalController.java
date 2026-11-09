package com.clubpadelcostabrava.desktop.ui;

import com.clubpadelcostabrava.desktop.api.ApiCliente;
import com.clubpadelcostabrava.desktop.data.Sesion;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class PrincipalController {

    @FXML
    private StackPane contenidoPane;

    private Sesion sesion;

    public void setSesion(Sesion sesion) {
        this.sesion = sesion;
        onAgenda();
    }

    @FXML
    private void onReservas() {
        try {
            FXMLLoader cargador = new FXMLLoader(
                    getClass().getResource(
                            "/com/clubpadelcostabrava/desktop/reservas-view.fxml"
                    )
            );

            Parent vista = cargador.load();
            ReservasController controlador = cargador.getController();
            controlador.setSesion(sesion, this::abrirLogin);
            contenidoPane.getChildren().setAll(vista);

        } catch (IOException e) {
            contenidoPane.getChildren().setAll(
                    new Label("No se pudo abrir Reservas.")
            );
        }
    }

    @FXML
    private void onSocios() {
        try {
            FXMLLoader cargador = new FXMLLoader(
                    getClass().getResource(
                            "/com/clubpadelcostabrava/desktop/socios-view.fxml"
                    )
            );

            Parent vista = cargador.load();
            SociosController controlador = cargador.getController();
            controlador.setSesion(sesion, this::abrirLogin);
            contenidoPane.getChildren().setAll(vista);

        } catch (IOException e) {
            contenidoPane.getChildren().setAll(
                    new Label("No se pudo abrir Socios.")
            );
        }
    }

    @FXML
    private void onAgenda() {
        try {
            FXMLLoader cargador = new FXMLLoader(
                    getClass().getResource(
                            "/com/clubpadelcostabrava/desktop/agenda-view.fxml"
                    )
            );

            Parent vista = cargador.load();
            AgendaController controlador = cargador.getController();
            controlador.setSesion(sesion, this::abrirLogin);
            contenidoPane.getChildren().setAll(vista);

        } catch (IOException e) {
            contenidoPane.getChildren().setAll(
                    new Label("No se pudo abrir Agenda.")
            );
        }
    }

    @FXML
    private void onMiCuenta() {
        try {
            FXMLLoader cargador = new FXMLLoader(
                    getClass().getResource(
                            "/com/clubpadelcostabrava/desktop/cuenta-view.fxml"
                    )
            );

            Parent vista = cargador.load();
            CuentaController controlador = cargador.getController();
            controlador.setSesion(sesion, this::abrirLogin);
            contenidoPane.getChildren().setAll(vista);

        } catch (IOException e) {
            contenidoPane.getChildren().setAll(
                    new Label("No se pudo abrir Mi cuenta.")
            );
        }
    }

    @FXML
    private Button salirButton;
    private final ApiCliente api = new ApiCliente();

    @FXML
    private void onSalir() {
        salirButton.setDisable(true);

        api.cerrarSesion(sesion.token()).whenComplete((ignorado, fallo) ->
                Platform.runLater(() -> {
                    if (fallo != null) {
                        new Alert(
                                Alert.AlertType.WARNING,
                                "Has salido de la aplicación, pero no se pudo " +
                                        "confirmar el cierre en el servidor."
                        ).showAndWait();
                    }
                    abrirLogin();
                })
        );
    }

    private void abrirLogin() {
        Stage ventana = (Stage) contenidoPane.getScene().getWindow();

        try {
            FXMLLoader cargador = new FXMLLoader(
                    getClass().getResource(
                            "/com/clubpadelcostabrava/desktop/login-view.fxml"
                    )
            );
            Parent vistaLogin = cargador.load();

            ventana.setMaximized(false);
            ventana.getScene().setRoot(vistaLogin);
            ventana.sizeToScene();
            ventana.centerOnScreen();
        } catch (IOException e) {
            new Alert(
                    Alert.AlertType.ERROR,
                    "No se pudo abrir la pantalla de acceso."
            ).showAndWait();
            ventana.close();
        }
    }
}