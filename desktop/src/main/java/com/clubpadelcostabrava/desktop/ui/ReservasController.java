package com.clubpadelcostabrava.desktop.ui;

import com.clubpadelcostabrava.desktop.api.ApiCliente;
import com.clubpadelcostabrava.desktop.api.ApiException;
import com.clubpadelcostabrava.desktop.data.Reserva;
import com.clubpadelcostabrava.desktop.data.Sesion;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Gestiona la consulta, creación y cancelación de reservas desde recepción. */
public class ReservasController {

    @FXML private TableView<Reserva> reservasTable;
    @FXML private TableColumn<Reserva, Integer> idCol;
    @FXML private TableColumn<Reserva, String> socioCol;
    @FXML private TableColumn<Reserva, String> pistaCol;
    @FXML private TableColumn<Reserva, String> fechaCol;
    @FXML private TableColumn<Reserva, String> inicioCol;
    @FXML private TableColumn<Reserva, String> finCol;
    @FXML private TableColumn<Reserva, Integer> horasCol;
    @FXML private TableColumn<Reserva, String> estadoCol;
    @FXML private Label mensajeLabel;
    @FXML private TextField socioIdField;
    @FXML private TextField pistaIdField;
    @FXML private DatePicker fechaPicker;
    @FXML private TextField horaInicioField;
    @FXML private ComboBox<Integer> duracionBox;
    @FXML private Button crearButton;
    @FXML private TextField motivoCancelacionField;
    @FXML private Button cancelarButton;
    private final ApiCliente api = new ApiCliente();
    private Sesion sesion;
    private Runnable onSesionInvalida;

    /** Recibe la sesión ADMIN al abrir esta pantalla. */
    public void setSesion(Sesion sesion, Runnable onSesionInvalida) {
        this.sesion = sesion;
        this.onSesionInvalida = onSesionInvalida;
        cargarReservas();
    }

    @FXML
    private void initialize() {
        duracionBox.getItems().setAll(1, 2);
        duracionBox.setValue(1);
        idCol.setCellValueFactory(fila ->
                new ReadOnlyObjectWrapper<>(fila.getValue().id()));
        socioCol.setCellValueFactory(fila ->
                new ReadOnlyStringWrapper(fila.getValue().socioNombre()));
        pistaCol.setCellValueFactory(fila ->
                new ReadOnlyStringWrapper(fila.getValue().pistaNombre()));
        fechaCol.setCellValueFactory(fila ->
                new ReadOnlyStringWrapper(fila.getValue().fecha()));
        inicioCol.setCellValueFactory(fila ->
                new ReadOnlyStringWrapper(fila.getValue().horaInicio()));
        finCol.setCellValueFactory(fila ->
                new ReadOnlyStringWrapper(fila.getValue().horaFin()));
        horasCol.setCellValueFactory(fila ->
                new ReadOnlyObjectWrapper<>(fila.getValue().duracionHoras()));
        estadoCol.setCellValueFactory(fila ->
                new ReadOnlyStringWrapper(fila.getValue().estado()));

        cancelarButton.setDisable(true);

        reservasTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, anterior, actual) ->
                        cancelarButton.setDisable(
                                actual == null || !actual.puedeCancelar()
                        )
        );
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
                            if (gestionarSesionInvalida(causa)) {
                                return;
                            }
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
                        if (gestionarSesionInvalida(causa)) {
                            return;
                        }
                        mensajeLabel.setText("Error: " + causa.getMessage());
                        return;
                    }

                    reservasTable.getItems().setAll(reservas);
                    mensajeLabel.setText("Reservas: " + reservas.size());
                })
        );
    }

    @FXML
    private void onCancelar() {
        Reserva seleccionada =
                reservasTable.getSelectionModel().getSelectedItem();

        if (seleccionada == null || !seleccionada.puedeCancelar()) {
            mensajeLabel.setText("Selecciona una reserva cancelable.");
            return;
        }

        String motivo = motivoCancelacionField.getText().strip();

        if (motivo.isBlank()) {
            mensajeLabel.setText("Indica el motivo de la cancelación.");
            return;
        }
        if (motivo.codePointCount(0, motivo.length()) > 200) {
            mensajeLabel.setText("El motivo no puede superar los 200 caracteres.");
            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Cancelar completamente la reserva #"
                        + seleccionada.id() + "?",
                ButtonType.OK,
                ButtonType.CANCEL
        );
        confirmacion.setTitle("Confirmar cancelación");
        confirmacion.setHeaderText(null);

        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL)
                != ButtonType.OK) {
            return;
        }

        cancelarButton.setDisable(true);
        mensajeLabel.setText("Cancelando reserva...");

        api.cancelarReservaAdmin(
                sesion.token(), seleccionada.id(), motivo
        ).whenComplete((resultado, fallo) ->
                Platform.runLater(() -> {
                    if (fallo != null) {
                        Throwable causa = fallo.getCause() != null
                                ? fallo.getCause() : fallo;
                        if (gestionarSesionInvalida(causa)) {
                            return;
                        }
                        mensajeLabel.setText(
                                causa.getMessage() != null
                                        ? causa.getMessage()
                                        : "No se pudo cancelar la reserva."
                        );
                        cancelarButton.setDisable(false);
                        return;
                    }

                    motivoCancelacionField.clear();
                    reservasTable.getSelectionModel().clearSelection();
                    cancelarButton.setDisable(true);
                    cargarReservas();

                    if ("FALLIDO".equals(resultado.avisoCorreo())) {
                        new Alert(
                                Alert.AlertType.WARNING,
                                "La reserva #" + resultado.reserva().id()
                                        + " quedó cancelada, pero no se pudo enviar el correo."
                        ).show();
                    } else {
                        new Alert(
                                Alert.AlertType.INFORMATION,
                                "Reserva #" + resultado.reserva().id()
                                        + " cancelada. El servidor de correo aceptó el aviso."
                        ).show();
                    }
                })
        );
    }

    private boolean gestionarSesionInvalida(Throwable causa) {
        if (causa instanceof ApiException errorApi
                && "SESION_INVALIDA".equals(errorApi.codigo())) {
            new Alert(
                    Alert.AlertType.WARNING,
                    "Tu sesión ya no es válida. Inicia sesión de nuevo."
            ).showAndWait();
            onSesionInvalida.run();
            return true;
        }
        return false;
    }
}
