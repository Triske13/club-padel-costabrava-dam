package com.clubpadelcostabrava.desktop.ui;

import com.clubpadelcostabrava.desktop.api.ApiCliente;
import com.clubpadelcostabrava.desktop.api.ApiException;
import com.clubpadelcostabrava.desktop.data.FranjaAgenda;
import com.clubpadelcostabrava.desktop.data.Sesion;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Muestra la disponibilidad diaria de cada pista en una pestaña. */
public class AgendaController {

    @FXML private DatePicker fechaPicker;
    @FXML private Label mensajeLabel;
    @FXML private TabPane pistasTabPane;

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

    /** Crea una pestaña y una tabla por cada pista recibida. */
    private void mostrarPistas(List<FranjaAgenda> franjas) {
        Map<Integer, List<FranjaAgenda>> porPista = new LinkedHashMap<>();

        for (FranjaAgenda franja : franjas) {
            porPista.computeIfAbsent(
                    franja.pistaId(), id -> new ArrayList<>()
            ).add(franja);
        }

        pistasTabPane.getTabs().clear();

        for (List<FranjaAgenda> horas : porPista.values()) {
            TableView<FranjaAgenda> tabla = new TableView<>();

            TableColumn<FranjaAgenda, String> horaCol =
                    new TableColumn<>("Hora");
            horaCol.setCellValueFactory(fila ->
                    new ReadOnlyStringWrapper(fila.getValue().horaInicio()));

            TableColumn<FranjaAgenda, String> estadoCol =
                    new TableColumn<>("Estado");
            estadoCol.setCellValueFactory(fila ->
                    new ReadOnlyStringWrapper(fila.getValue().estado()));

            tabla.getColumns().addAll(horaCol, estadoCol);
            tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tabla.getItems().setAll(horas);

            Tab pestaña = new Tab(horas.get(0).pistaNombre(), tabla);
            pestaña.setClosable(false);
            pistasTabPane.getTabs().add(pestaña);
        }
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

                    mostrarPistas(franjas);
                    mensajeLabel.setText(
                            "Pistas: " + pistasTabPane.getTabs().size()
                                    + " · Franjas: " + franjas.size()
                    );
                })
        );
    }
}