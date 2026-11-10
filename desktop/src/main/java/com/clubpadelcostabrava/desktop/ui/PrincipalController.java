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

/**
 * Controla la pantalla principal de la aplicación de administración.
 * Mantiene la sesión ADMIN y carga en el área central las vistas de agenda,
 * reservas, socios y cuenta.
 */
public class PrincipalController {

    @FXML
    private StackPane contenidoPane;

    private Sesion sesion;

    /**
     * Guarda la sesión recibida después del login y abre la agenda como vista inicial.
     *
     * @param sesion sesión ADMIN que utilizarán las distintas pantallas
     */
    public void setSesion(Sesion sesion) {
        this.sesion = sesion;
        onAgenda();
    }

    /**
     * Carga la vista de reservas y le proporciona la sesión actual.
     * Si no se puede abrir el archivo FXML, muestra un mensaje en el área central.
     */
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

    /**
     * Carga la vista de gestión de socios y le proporciona la sesión actual.
     * Si falla la carga del FXML, muestra un mensaje en el área central.
     */
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

    /**
     * Carga la agenda de pistas y le proporciona la sesión actual.
     * También se utiliza para mostrar la primera vista tras iniciar sesión.
     */
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

    /**
     * Carga la vista desde la que ADMIN puede cambiar su contraseña.
     * Le proporciona la sesión y la acción para volver al login si termina.
     */
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

    /**
     * Solicita al servidor el cierre de la sesión actual.
     * Después vuelve al login; si el servidor no confirma el cierre,
     * muestra un aviso antes de abandonar la pantalla principal.
     */
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

    /**
     * Sustituye la vista principal por el formulario de acceso.
     * Si no puede cargarlo, muestra un error y cierra la ventana.
     */
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