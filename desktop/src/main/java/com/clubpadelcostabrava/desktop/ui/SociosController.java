package com.clubpadelcostabrava.desktop.ui;

import com.clubpadelcostabrava.desktop.data.ApiCliente;
import com.clubpadelcostabrava.desktop.data.ApiException;
import com.clubpadelcostabrava.desktop.data.Sesion;
import com.clubpadelcostabrava.desktop.data.Socio;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;

public class SociosController {

    @FXML private Label mensajeLabel;
    @FXML private ListView<Socio> sociosList;

    private final ApiCliente api = new ApiCliente();
    private Sesion sesion;
    private Runnable onSesionInvalida;

    /** Recibe la sesión al abrir la pantalla y carga los socios. */
    public void setSesion(Sesion sesion, Runnable onSesionInvalida) {
        this.sesion = sesion;
        this.onSesionInvalida = onSesionInvalida;
        cargarSocios();
    }

    @FXML
    private void initialize() {
        sociosList.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(Socio socio, boolean vacio) {
                super.updateItem(socio, vacio);

                if (vacio || socio == null) {
                    setText(null);
                    return;
                }

                String cuenta = socio.cuentaActivada()
                        ? "Cuenta activada"
                        : "Cuenta pendiente";

                setText("#" + socio.id() + " · "
                        + socio.nombre() + " " + socio.apellidos()
                        + " · " + socio.correo()
                        + " · " + socio.estado()
                        + " · " + cuenta);
            }
        });
    }

    @FXML
    private void onRecargar() {
        cargarSocios();
    }

    /** Consulta la API sin bloquear la ventana. */
    private void cargarSocios() {
        mensajeLabel.setText("Cargando socios...");

        api.listarSocios(sesion.token()).whenComplete((socios, fallo) ->
                Platform.runLater(() -> {
                    if (fallo != null) {
                        Throwable causa = fallo.getCause() != null
                                ? fallo.getCause() : fallo;

                        if (causa instanceof ApiException error
                                && "SESION_INVALIDA".equals(error.codigo())) {
                            new Alert(
                                    Alert.AlertType.WARNING,
                                    "Tu sesión ya no es válida. Inicia sesión de nuevo."
                            ).showAndWait();
                            onSesionInvalida.run();
                            return;
                        }

                        mensajeLabel.setText("Error: " + causa.getMessage());
                        return;
                    }

                    sociosList.getItems().setAll(socios);
                    mensajeLabel.setText("Socios: " + socios.size());
                })
        );
    }
}