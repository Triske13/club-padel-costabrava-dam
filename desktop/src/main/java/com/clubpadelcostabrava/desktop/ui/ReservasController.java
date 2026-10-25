package com.clubpadelcostabrava.desktop.ui;

import com.clubpadelcostabrava.desktop.data.ApiCliente;
import com.clubpadelcostabrava.desktop.data.Sesion;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javafx.scene.control.Alert;

/** Controla la consulta y creación de reservas desde recepción. */
public class ReservasController {

    @FXML private ListView<String> reservasList;
    @FXML private Label mensajeLabel;
    @FXML private TextField socioIdField;
    @FXML private TextField pistaIdField;
    @FXML private DatePicker fechaPicker;
    @FXML private TextField horaInicioField;
    @FXML private ComboBox<Integer> duracionBox;
    @FXML private Button crearButton;
    private final ApiCliente api = new ApiCliente();
    private Sesion sesion;

    /** Recibe la sesión ADMIN al abrir esta pantalla. */
    public void setSesion(Sesion sesion) {
        this.sesion = sesion;
        cargarReservas();
    }

    @FXML
    private void initialize() {
        duracionBox.getItems().setAll(1, 2);
        duracionBox.setValue(1);
    }

    @FXML
    private void onCrear() {
        if (fechaPicker.getValue() == null) {
            mensajeLabel.setText("Selecciona una fecha.");
            return;
        }

        try {
            int socioId = Integer.parseInt(socioIdField.getText().strip());
            int pistaId = Integer.parseInt(pistaIdField.getText().strip());
            LocalTime hora = LocalTime.parse(
                    horaInicioField.getText().strip(),
                    DateTimeFormatter.ofPattern("HH:mm")
            );

            if (socioId <= 0 || pistaId <= 0) {
                mensajeLabel.setText("Los ID del socio y la pista deben ser positivos.");
                return;
            }

            crearButton.setDisable(true);
            mensajeLabel.setText("Creando reserva...");

            api.crearReservaAdmin(
                    sesion.token(),
                    socioId,
                    pistaId,
                    fechaPicker.getValue().toString(),
                    hora.toString(),
                    duracionBox.getValue()
            ).whenComplete((reserva, fallo) ->
                    Platform.runLater(() -> {
                        crearButton.setDisable(false);

                        if (fallo != null) {
                            Throwable causa = fallo.getCause() != null
                                    ? fallo.getCause() : fallo;
                            mensajeLabel.setText(
                                    causa.getMessage() != null
                                            ? causa.getMessage()
                                            : "No se pudo crear la reserva."
                            );
                            return;
                        }

                        new Alert(
                                Alert.AlertType.INFORMATION,
                                "Reserva #" + reserva.id() + " confirmada para "
                                        + reserva.socioNombre()
                        ).show();

                        horaInicioField.clear();
                        cargarReservas();
                    })
            );
        } catch (NumberFormatException | DateTimeParseException e) {
            mensajeLabel.setText("Indica ID numéricos y una hora con formato HH:mm.");
        }
    }

    @FXML
    private void onRecargar() {
        cargarReservas();
    }

    private void cargarReservas() {
        mensajeLabel.setText("Cargando reservas...");

        api.listarReservasAdmin(sesion.token()).whenComplete((reservas, fallo) ->
                Platform.runLater(() -> {
                    if (fallo != null) {
                        Throwable causa = fallo.getCause() != null
                                ? fallo.getCause() : fallo;
                        mensajeLabel.setText("Error: " + causa.getMessage());
                        return;
                    }

                    reservasList.getItems().setAll(
                            reservas.stream()
                                    .map(reserva -> "#%d · %s · %s · %s %s (%d h) · %s"
                                            .formatted(
                                                    reserva.id(),
                                                    reserva.socioNombre(),
                                                    reserva.pistaNombre(),
                                                    reserva.fecha(),
                                                    reserva.horaInicio(),
                                                    reserva.duracionHoras(),
                                                    reserva.estado()
                                            ))
                                    .toList()
                    );
                    mensajeLabel.setText("Reservas: " + reservas.size());
                })
        );
    }
}
