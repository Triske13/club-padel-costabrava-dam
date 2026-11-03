package com.clubpadelcostabrava.desktop.ui;

import com.clubpadelcostabrava.desktop.data.ApiCliente;
import com.clubpadelcostabrava.desktop.data.ApiException;
import com.clubpadelcostabrava.desktop.data.FranjaAgenda;
import com.clubpadelcostabrava.desktop.data.Sesion;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.ZoneId;

public class AgendaController {

    @FXML private DatePicker fechaPicker;
    @FXML private Label mensajeLabel;
    @FXML private ListView<FranjaAgenda> franjasList;

    private final ApiCliente api = new ApiCliente();
    private Sesion sesion;
    private Runnable onSesionInvalida;

    /** Recibe la sesión y consulta inicialmente el día de hoy. */
    public void setSesion(Sesion sesion, Runnable onSesionInvalida) {
        this.sesion = sesion;
        this.onSesionInvalida = onSesionInvalida;
        fechaPicker.setValue(LocalDate.now(ZoneId.of("Europe/Madrid")));
        onConsultar();
    }

    @FXML
    private void initialize() {
        franjasList.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(FranjaAgenda franja, boolean vacio) {
                super.updateItem(franja, vacio);

                if (vacio || franja == null) {
                    setText(null);
                    return;
                }

                setText(franja.pistaNombre()
                        + " · " + franja.horaInicio()
                        + " · " + franja.estado());
            }
        });
    }

    @FXML
    private void onConsultar() {
        LocalDate fecha = fechaPicker.getValue();

        if (fecha == null) {
            mensajeLabel.setText("Selecciona una fecha.");
            return;
        }

        mensajeLabel.setText("Cargando agenda...");

        api.consultarAgenda(sesion.token(), fecha).whenComplete((franjas, fallo) ->
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

                    franjasList.getItems().setAll(franjas);
                    mensajeLabel.setText("Franjas: " + franjas.size());
                })
        );
    }
}