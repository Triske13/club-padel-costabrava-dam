package com.clubpadelcostabrava.desktop.ui;

import com.clubpadelcostabrava.desktop.api.ApiCliente;
import com.clubpadelcostabrava.desktop.api.ApiException;
import com.clubpadelcostabrava.desktop.data.*;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Gestiona la consulta, creación y cancelación de reservas desde recepción. */
public class ReservasController {

    @FXML private TabPane reservasTabPane;
    @FXML private TableView<Reserva> reservasTable;
    @FXML private TableView<Reserva> historicoTable;
    @FXML private TableColumn<Reserva, Integer> idCol;
    @FXML private TableColumn<Reserva, String> socioCol;
    @FXML private TableColumn<Reserva, String> pistaCol;
    @FXML private TableColumn<Reserva, String> fechaCol;
    @FXML private TableColumn<Reserva, String> inicioCol;
    @FXML private TableColumn<Reserva, String> finCol;
    @FXML private TableColumn<Reserva, Integer> horasCol;
    @FXML private TableColumn<Reserva, String> estadoCol;
    @FXML private TableColumn<Reserva, Integer> historicoIdCol;
    @FXML private TableColumn<Reserva, String> historicoSocioCol;
    @FXML private TableColumn<Reserva, String> historicoPistaCol;
    @FXML private TableColumn<Reserva, String> historicoFechaCol;
    @FXML private TableColumn<Reserva, String> historicoInicioCol;
    @FXML private TableColumn<Reserva, String> historicoFinCol;
    @FXML private TableColumn<Reserva, Integer> historicoHorasCol;
    @FXML private TableColumn<Reserva, String> historicoEstadoCol;
    @FXML private Button anteriorActualButton;
    @FXML private Button siguienteActualButton;
    @FXML private Label paginaActualLabel;
    @FXML private DatePicker fechaHistoricoPicker;
    @FXML private Button anteriorHistoricoButton;
    @FXML private Button siguienteHistoricoButton;
    @FXML private Label paginaHistoricoLabel;
    @FXML private Label mensajeLabel;
    @FXML private ComboBox<Socio> socioBox;
    @FXML private ComboBox<Pista> pistaBox;
    @FXML private DatePicker fechaPicker;
    @FXML private ComboBox<String> horaBox;
    @FXML private ComboBox<Integer> duracionBox;
    @FXML private Button crearButton;
    @FXML private TextField motivoCancelacionField;
    @FXML private Button cancelarButton;

    private List<FranjaAgenda> franjasDelDia = List.of();
    private final ApiCliente api = new ApiCliente();
    private Sesion sesion;
    private Runnable onSesionInvalida;
    private int paginaActual = 0;
    private int paginaHistorico = 0;

    /** Recibe la sesión ADMIN al abrir esta pantalla. */
    public void setSesion(Sesion sesion, Runnable onSesionInvalida) {
        this.sesion = sesion;
        this.onSesionInvalida = onSesionInvalida;
        cargarReservas();
        cargarSocios();
    }

    @FXML
    private void initialize() {
        duracionBox.getItems().setAll(1, 2);
        duracionBox.setValue(1);
        socioBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Socio socio) {
                return socio == null
                        ? ""
                        : socio.nombre() + " " + socio.apellidos();
            }

            @Override
            public Socio fromString(String texto) {
                return null;
            }
        });
        pistaBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Pista pista) {
                return pista == null ? "" : pista.nombre();
            }

            @Override
            public Pista fromString(String texto) {
                return null;
            }
        });
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

        historicoIdCol.setCellValueFactory(idCol.getCellValueFactory());
        historicoSocioCol.setCellValueFactory(socioCol.getCellValueFactory());
        historicoPistaCol.setCellValueFactory(pistaCol.getCellValueFactory());
        historicoFechaCol.setCellValueFactory(fechaCol.getCellValueFactory());
        historicoInicioCol.setCellValueFactory(inicioCol.getCellValueFactory());
        historicoFinCol.setCellValueFactory(finCol.getCellValueFactory());
        historicoHorasCol.setCellValueFactory(horasCol.getCellValueFactory());
        historicoEstadoCol.setCellValueFactory(estadoCol.getCellValueFactory());

        cancelarButton.setDisable(true);

        reservasTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, anterior, actual) ->
                        cancelarButton.setDisable(
                                actual == null || !actual.puedeCancelar()
                        )
        );
        pistaBox.setDisable(true);
        fechaPicker.valueProperty().addListener(
                (observable, anterior, nuevaFecha) -> cargarPistas(nuevaFecha)
        );
        horaBox.setDisable(true);
        pistaBox.valueProperty().addListener(
                (obs, anterior, actual) -> actualizarHoras()
        );
        duracionBox.valueProperty().addListener(
                (obs, anterior, actual) -> actualizarHoras()
        );
        fechaHistoricoPicker.setDayCellFactory(selector -> new DateCell() {
            @Override
            public void updateItem(LocalDate fecha, boolean vacia) {
                super.updateItem(fecha, vacia);
                setDisable(
                        vacia || fecha == null
                                || !fecha.isBefore(LocalDate.now(ZoneId.of("Europe/Madrid")))
                );
            }
        });
        fechaHistoricoPicker.valueProperty().addListener(
                (observable, anterior, nuevaFecha) -> {
                    paginaHistorico = 0;
                    cargarHistorico();
                }
        );
    }

    /** Actualiza las horas al cambiar la pista o la duración. */
    private void actualizarHoras() {
        horaBox.getItems().clear();
        horaBox.setValue(null);

        Pista pista = pistaBox.getValue();
        Integer duracion = duracionBox.getValue();

        if (pista == null || duracion == null) {
            horaBox.setDisable(true);
            return;
        }

        List<String> horas = horasDisponibles(pista, duracion);
        horaBox.getItems().setAll(horas);
        horaBox.setDisable(horas.isEmpty());
    }

    @FXML
    private void onCrear() {
        if (fechaPicker.getValue() == null) {
            mensajeLabel.setText("Selecciona una fecha.");
            return;
        }
        Socio socioSeleccionado = socioBox.getValue();

        if (socioSeleccionado == null) {
            mensajeLabel.setText("Selecciona un socio.");
            return;
        }
        Pista pistaSeleccionada = pistaBox.getValue();

        if (pistaSeleccionada == null) {
            mensajeLabel.setText("Selecciona una pista.");
            return;
        }
        String horaSeleccionada = horaBox.getValue();

        if (horaSeleccionada == null) {
            mensajeLabel.setText("Selecciona una hora disponible.");
            return;
        }

        try {
            int socioId = socioSeleccionado.id();
            int pistaId = pistaSeleccionada.id();
            LocalTime hora = LocalTime.parse(
                    horaSeleccionada,
                    DateTimeFormatter.ofPattern("HH:mm")
            );

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
                            String detalle = causa.getMessage();
                            if (detalle == null || detalle.isBlank()) {
                                detalle = "No se pudo crear la reserva.";
                            }

                            Alert.AlertType tipo =
                                    causa instanceof ApiException error && error.estado() < 500
                                            ? Alert.AlertType.WARNING
                                            : Alert.AlertType.ERROR;

                            mensajeLabel.setText("");
                            new Alert(tipo, detalle).showAndWait();
                            return;
                        }

                        new Alert(
                                Alert.AlertType.INFORMATION,
                                "Reserva #" + reserva.id() + " confirmada para "
                                        + reserva.socioNombre()
                        ).show();

                        cargarPistas(fechaPicker.getValue());
                        reservasTabPane.getSelectionModel().selectFirst();
                        paginaActual = 0;
                        cargarReservas();
                    })
            );
        } catch (DateTimeParseException e) {
            mensajeLabel.setText("Indica una hora con formato HH:mm.");
        }
    }

    @FXML
    private void onRecargar() {
        if (reservasTabPane.getSelectionModel().getSelectedIndex() == 0) {
            cargarReservas();
        } else {
            cargarHistorico();
        }

        cargarPistas(fechaPicker.getValue());
    }

    private void cargarReservas() {
        mensajeLabel.setText("Cargando reservas...");
        anteriorActualButton.setDisable(true);
        siguienteActualButton.setDisable(true);

        api.listarPaginaReservasAdmin(sesion.token(), null, paginaActual)
                .whenComplete((pagina, fallo) ->
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

                    reservasTable.getItems().setAll(pagina.reservas());
                    paginaActualLabel.setText("Página " + (paginaActual + 1));
                    anteriorActualButton.setDisable(paginaActual == 0);
                    siguienteActualButton.setDisable(!pagina.hayMas());
                    mensajeLabel.setText(
                            "Reservas actuales en esta página: " + pagina.reservas().size()
                    );
                })
        );
    }

    /** Consulta una página de reservas de la fecha histórica seleccionada. */
    private void cargarHistorico() {
        LocalDate fecha = fechaHistoricoPicker.getValue();
        int paginaSolicitada = paginaHistorico;

        historicoTable.getItems().clear();
        anteriorHistoricoButton.setDisable(true);
        siguienteHistoricoButton.setDisable(true);
        paginaHistoricoLabel.setText("Página " + (paginaSolicitada + 1));

        if (fecha == null) {
            mensajeLabel.setText("Selecciona una fecha pasada.");
            return;
        }

        if (!fecha.isBefore(LocalDate.now(ZoneId.of("Europe/Madrid")))) {
            mensajeLabel.setText("El histórico requiere una fecha anterior a hoy.");
            return;
        }

        mensajeLabel.setText("Cargando histórico...");

        api.listarPaginaReservasAdmin(
                sesion.token(), fecha, paginaSolicitada
        ).whenComplete((pagina, fallo) ->
                Platform.runLater(() -> {
                    // Ignora una respuesta si ya se eligió otra fecha o página.
                    if (!fecha.equals(fechaHistoricoPicker.getValue())
                            || paginaSolicitada != paginaHistorico) {
                        return;
                    }

                    if (fallo != null) {
                        Throwable causa = fallo.getCause() != null
                                ? fallo.getCause() : fallo;
                        if (gestionarSesionInvalida(causa)) {
                            return;
                        }
                        mensajeLabel.setText("Error al cargar el histórico: "
                                + causa.getMessage());
                        return;
                    }

                    historicoTable.getItems().setAll(pagina.reservas());
                    paginaHistoricoLabel.setText(
                            "Página " + (paginaSolicitada + 1)
                    );
                    anteriorHistoricoButton.setDisable(paginaSolicitada == 0);
                    siguienteHistoricoButton.setDisable(!pagina.hayMas());
                    mensajeLabel.setText(
                            "Reservas del " + fecha + " en esta página: "
                                    + pagina.reservas().size()
                    );
                })
        );
    }

    @FXML
    private void onAnteriorActual() {
        if (paginaActual > 0) {
            paginaActual--;
            cargarReservas();
        }
    }

    @FXML
    private void onSiguienteActual() {
        paginaActual++;
        cargarReservas();
    }

    @FXML
    private void onAnteriorHistorico() {
        if (paginaHistorico > 0) {
            paginaHistorico--;
            cargarHistorico();
        }
    }

    @FXML
    private void onSiguienteHistorico() {
        if (fechaHistoricoPicker.getValue() != null) {
            paginaHistorico++;
            cargarHistorico();
        }
    }

    /** Carga en el desplegable únicamente los socios activos. */
    private void cargarSocios() {
        socioBox.setDisable(true);

        api.listarSocios(sesion.token()).whenComplete((socios, fallo) ->
                Platform.runLater(() -> {
                    if (fallo != null) {
                        Throwable causa = fallo.getCause() != null
                                ? fallo.getCause() : fallo;

                        if (gestionarSesionInvalida(causa)) {
                            return;
                        }

                        mensajeLabel.setText("No se pudieron cargar los socios: "
                                + causa.getMessage());
                        return;
                    }

                    socioBox.getItems().setAll(
                            socios.stream()
                                    .filter(socio -> "ACTIVO".equals(socio.estado()))
                                    .toList()
                    );
                    socioBox.setDisable(false);
                })
        );
    }

    /** Consulta las pistas de la fecha elegida y conserva sus franjas. */
    private void cargarPistas(LocalDate fecha) {
        franjasDelDia = List.of();
        pistaBox.getItems().clear();
        pistaBox.setValue(null);
        pistaBox.setDisable(true);

        if (fecha == null) {
            return;
        }

        api.consultarAgenda(sesion.token(), fecha).whenComplete((franjas, fallo) ->
                Platform.runLater(() -> {
                    // Ignora una respuesta si el usuario ya eligió otra fecha.
                    if (!fecha.equals(fechaPicker.getValue())) {
                        return;
                    }

                    if (fallo != null) {
                        Throwable causa = fallo.getCause() != null
                                ? fallo.getCause() : fallo;

                        if (gestionarSesionInvalida(causa)) {
                            return;
                        }

                        mensajeLabel.setText("No se pudieron cargar las pistas: "
                                + causa.getMessage());
                        return;
                    }

                    franjasDelDia = franjas;

                    List<Pista> pistas = franjas.stream()
                            .map(f -> new Pista(f.pistaId(), f.pistaNombre()))
                            .distinct()
                            .toList();

                    pistaBox.getItems().setAll(pistas);
                    pistaBox.setDisable(pistas.isEmpty());
                })
        );
    }

    /** Devuelve las horas libres de una pista que admiten la duración elegida. */
    private List<String> horasDisponibles(Pista pista, int duracion) {
        if (pista == null) {
            return List.of();
        }

        return franjasDelDia.stream()
                .filter(franja -> franja.pistaId() == pista.id())
                .filter(franja -> "LIBRE".equals(franja.estado()))
                .filter(franja -> franja.duracionesDisponibles().contains(duracion))
                .map(FranjaAgenda::horaInicio)
                .toList();
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
                        String detalle = causa.getMessage();
                        if (detalle == null || detalle.isBlank()) {
                            detalle = "No se pudo cancelar la reserva.";
                        }

                        Alert.AlertType tipo =
                                causa instanceof ApiException error && error.estado() < 500
                                        ? Alert.AlertType.WARNING
                                        : Alert.AlertType.ERROR;

                        mensajeLabel.setText("");
                        new Alert(tipo, detalle).showAndWait();
                        cancelarButton.setDisable(false);
                        return;
                    }

                    motivoCancelacionField.clear();
                    reservasTable.getSelectionModel().clearSelection();
                    cancelarButton.setDisable(true);
                    cargarReservas();
                    cargarPistas(fechaPicker.getValue());

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
