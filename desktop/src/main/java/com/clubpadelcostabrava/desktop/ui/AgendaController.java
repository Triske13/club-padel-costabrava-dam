package com.clubpadelcostabrava.desktop.ui;

import com.clubpadelcostabrava.desktop.api.ApiCliente;
import com.clubpadelcostabrava.desktop.api.ApiException;
import com.clubpadelcostabrava.desktop.data.Bloqueo;
import com.clubpadelcostabrava.desktop.data.FranjaAgenda;
import com.clubpadelcostabrava.desktop.data.Sesion;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controla la agenda diaria de administración.
 * Muestra la disponibilidad de cada pista en una pestaña y permite crear
 * o retirar bloqueos manuales para las franjas seleccionadas.
 */
public class AgendaController {

    @FXML
    private DatePicker fechaPicker;
    @FXML
    private Label mensajeLabel;
    @FXML
    private TabPane pistasTabPane;
    @FXML
    private TableView<Bloqueo> bloqueosTable;
    @FXML
    private TableColumn<Bloqueo, String> pistaBloqueoCol;
    @FXML
    private TableColumn<Bloqueo, String> inicioBloqueoCol;
    @FXML
    private TableColumn<Bloqueo, String> horasBloqueoCol;
    @FXML
    private TableColumn<Bloqueo, String> descripcionBloqueoCol;
    @FXML
    private ComboBox<Integer> horasBloqueoBox;
    @FXML
    private TextField descripcionBloqueoField;
    @FXML
    private Button crearBloqueoButton;
    @FXML
    private Button retirarBloqueoButton;


    private final ApiCliente api = new ApiCliente();
    private Sesion sesion;
    private Runnable onSesionInvalida;
    private final Map<Tab, TableView<FranjaAgenda>> tablasPorPestaña = new LinkedHashMap<>();
    private LocalDate fechaAgendaCargada;
    private boolean creandoBloqueo;
    private boolean retirandoBloqueo;
    private long versionConsulta;

    /**
     * Configura las columnas de bloqueos y los cambios de selección.
     * Actualiza las duraciones disponibles y el botón de retirada cuando
     * cambian la fecha, la pista, la franja o el bloqueo seleccionado.
     */
    @FXML
    private void initialize() {
        pistaBloqueoCol.setCellValueFactory(fila ->
                new ReadOnlyStringWrapper(
                        String.valueOf(fila.getValue().pistaId())
                ));

        inicioBloqueoCol.setCellValueFactory(fila ->
                new ReadOnlyStringWrapper(
                        fila.getValue().horaInicio().toString()
                ));

        horasBloqueoCol.setCellValueFactory(fila ->
                new ReadOnlyStringWrapper(
                        String.valueOf(fila.getValue().numeroHoras())
                ));

        descripcionBloqueoCol.setCellValueFactory(fila ->
                new ReadOnlyStringWrapper(
                        fila.getValue().descripcion() == null
                                ? ""
                                : fila.getValue().descripcion()
                ));

        bloqueosTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );
        pistasTabPane.getSelectionModel().selectedItemProperty().addListener(
                (observable, anterior, actual) -> actualizarDuracionesBloqueo()
        );
        fechaPicker.valueProperty().addListener((observable, anterior, actual) ->
                actualizarDuracionesBloqueo()
        );
        bloqueosTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, anterior, actual) -> actualizarBotonRetirar()
        );
        fechaPicker.valueProperty().addListener(
                (observable, anterior, actual) -> actualizarBotonRetirar()
        );
    }

    /**
     * Recibe la sesión ADMIN y consulta inicialmente la agenda del día actual.
     *
     * @param sesion           sesión utilizada para autorizar las peticiones
     * @param onSesionInvalida acción para volver al login si caduca la sesión
     */
    public void setSesion(Sesion sesion, Runnable onSesionInvalida) {
        this.sesion = sesion;
        this.onSesionInvalida = onSesionInvalida;
        fechaPicker.setValue(LocalDate.now(ZoneId.of("Europe/Madrid")));
        onConsultar();
    }

    /**
     * Agrupa las franjas recibidas por pista y crea una pestaña con su tabla
     * de horas y estados para cada una.
     *
     * @param franjas franjas de todas las pistas devueltas por la API
     */
    private void mostrarPistas(List<FranjaAgenda> franjas) {
        Map<Integer, List<FranjaAgenda>> porPista = new LinkedHashMap<>();

        for (FranjaAgenda franja : franjas) {
            porPista.computeIfAbsent(
                    franja.pistaId(), id -> new ArrayList<>()
            ).add(franja);
        }

        pistasTabPane.getTabs().clear();
        tablasPorPestaña.clear();

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
            tablasPorPestaña.put(pestaña, tabla);
            tabla.getSelectionModel().selectedItemProperty().addListener(
                    (observable, anterior, actual) -> actualizarDuracionesBloqueo()
            );
            pistasTabPane.getTabs().add(pestaña);
        }
    }

    /**
     * Calcula las duraciones de bloqueo que pueden comenzar en la franja
     * seleccionada. Solo ofrece horas consecutivas libres de una fecha cargada
     * y futura, y habilita el botón de creación cuando existe una opción válida.
     */
    private void actualizarDuracionesBloqueo() {
        horasBloqueoBox.getItems().clear();
        crearBloqueoButton.setDisable(true);

        LocalDate fecha = fechaPicker.getValue();
        if (fecha == null || !fecha.equals(fechaAgendaCargada)) {
            return;
        }

        Tab pestaña = pistasTabPane.getSelectionModel().getSelectedItem();
        TableView<FranjaAgenda> tabla = tablasPorPestaña.get(pestaña);
        if (tabla == null) {
            return;
        }

        int indice = tabla.getSelectionModel().getSelectedIndex();
        if (indice < 0) {
            return;
        }

        LocalTime horaEsperada =
                LocalTime.parse(tabla.getItems().get(indice).horaInicio());

        if (!LocalDateTime.of(fecha, horaEsperada)
                .isAfter(LocalDateTime.now(ZoneId.of("Europe/Madrid")))) {
            return;
        }

        List<Integer> duraciones = new ArrayList<>();

        for (int posicion = indice; posicion < tabla.getItems().size(); posicion++) {
            FranjaAgenda franja = tabla.getItems().get(posicion);

            if (!"LIBRE".equals(franja.estado())
                    || !horaEsperada.equals(LocalTime.parse(franja.horaInicio()))) {
                break;
            }

            duraciones.add(posicion - indice + 1);
            horaEsperada = horaEsperada.plusHours(1);
        }

        horasBloqueoBox.getItems().setAll(duraciones);

        if (!duraciones.isEmpty()) {
            horasBloqueoBox.getSelectionModel().selectFirst();
            crearBloqueoButton.setDisable(creandoBloqueo);
        }
    }

    /**
     * Consulta los bloqueos manuales de la fecha mostrada.
     * Descarta la respuesta si corresponde a una consulta anterior o si
     * el usuario ha cambiado la fecha mientras se cargaban los datos.
     *
     * @param fecha   fecha cuyos bloqueos se quieren mostrar
     * @param version identificador de la consulta de agenda que originó la carga
     */
    private void cargarBloqueos(LocalDate fecha, long version) {
        api.listarBloqueos(sesion.token(), fecha).whenComplete((bloqueos, fallo) ->
                Platform.runLater(() -> {

                    if (version != versionConsulta
                            || !fecha.equals(fechaPicker.getValue())) {
                        return;
                    }

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

                        mensajeLabel.setText(
                                "No se pudieron cargar los bloqueos: " + causa.getMessage()
                        );
                        return;
                    }

                    bloqueosTable.getItems().setAll(bloqueos);
                })
        );
    }

    /**
     * Valida la pista, la fecha, la franja y la duración seleccionadas y solicita
     * la creación de un bloqueo manual. Muestra el resultado y vuelve a consultar
     * la agenda para reflejar las horas ocupadas.
     */
    @FXML
    private void onCrearBloqueo() {
        if (creandoBloqueo) {
            return;
        }

        LocalDate fecha = fechaPicker.getValue();
        Tab pestaña = pistasTabPane.getSelectionModel().getSelectedItem();
        TableView<FranjaAgenda> tabla = tablasPorPestaña.get(pestaña);
        FranjaAgenda franja = tabla == null
                ? null
                : tabla.getSelectionModel().getSelectedItem();
        Integer numeroHoras = horasBloqueoBox.getValue();

        if (fecha == null
                || !fecha.equals(fechaAgendaCargada)
                || franja == null
                || numeroHoras == null
                || !horasBloqueoBox.getItems().contains(numeroHoras)) {
            new Alert(
                    Alert.AlertType.WARNING,
                    "Selecciona una hora libre y una duración válida."
            ).showAndWait();
            return;
        }

        String textoDescripcion = descripcionBloqueoField.getText().strip();
        if (textoDescripcion.length() > 200) {
            new Alert(
                    Alert.AlertType.WARNING,
                    "La descripción no puede superar los 200 caracteres."
            ).showAndWait();
            return;
        }

        String descripcion = textoDescripcion.isEmpty()
                ? null
                : textoDescripcion;

        creandoBloqueo = true;
        actualizarDuracionesBloqueo();

        api.crearBloqueo(
                sesion.token(),
                franja.pistaId(),
                fecha,
                LocalTime.parse(franja.horaInicio()),
                numeroHoras,
                descripcion
        ).whenComplete((creado, fallo) -> Platform.runLater(() -> {
            creandoBloqueo = false;

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

                new Alert(
                        Alert.AlertType.ERROR,
                        "No se pudo crear el bloqueo: " + causa.getMessage()
                ).showAndWait();
                onConsultar();
                return;
            }

            descripcionBloqueoField.clear();
            new Alert(
                    Alert.AlertType.INFORMATION,
                    "Bloqueo #" + creado.id() + " creado."
            ).showAndWait();
            onConsultar();
        }));
    }

    /**
     * Habilita la retirada únicamente si hay un bloqueo seleccionado en
     * la fecha cargada, todavía no ha comenzado y no hay otra retirada en curso.
     */
    private void actualizarBotonRetirar() {
        Bloqueo elegido = bloqueosTable.getSelectionModel().getSelectedItem();

        boolean permitido = !retirandoBloqueo
                && elegido != null
                && elegido.fecha().equals(fechaPicker.getValue())
                && elegido.fecha().equals(fechaAgendaCargada)
                && LocalDateTime.of(elegido.fecha(), elegido.horaInicio())
                .isAfter(LocalDateTime.now(ZoneId.of("Europe/Madrid")));

        retirarBloqueoButton.setDisable(!permitido);
    }

    /**
     * Pide confirmación para retirar el bloqueo seleccionado.
     * Envía la petición al servidor y vuelve a consultar la agenda cuando
     * recibe el resultado.
     */
    @FXML
    private void onRetirarBloqueo() {
        actualizarBotonRetirar();
        if (retirarBloqueoButton.isDisabled()) {
            return;
        }

        Bloqueo elegido = bloqueosTable.getSelectionModel().getSelectedItem();

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Retirar el bloqueo #" + elegido.id() + "?",
                ButtonType.OK,
                ButtonType.CANCEL
        );
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        retirandoBloqueo = true;
        actualizarBotonRetirar();

        api.retirarBloqueo(sesion.token(), elegido.id())
                .whenComplete((ignorado, fallo) -> Platform.runLater(() -> {
                    retirandoBloqueo = false;

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

                        new Alert(
                                Alert.AlertType.ERROR,
                                "No se pudo retirar el bloqueo: " + causa.getMessage()
                        ).showAndWait();
                        onConsultar();
                        return;
                    }

                    new Alert(
                            Alert.AlertType.INFORMATION,
                            "Bloqueo retirado."
                    ).showAndWait();
                    onConsultar();
                }));
    }

    /**
     * Consulta la disponibilidad de la fecha seleccionada y carga después
     * sus bloqueos manuales. Utiliza una versión de consulta para descartar
     * respuestas antiguas si el usuario cambia de fecha antes de recibirlas.
     */
    @FXML
    private void onConsultar() {
        LocalDate fecha = fechaPicker.getValue();

        if (fecha == null) {
            mensajeLabel.setText("Selecciona una fecha.");
            return;
        }
        long version = ++versionConsulta;
        bloqueosTable.getItems().clear();
        mensajeLabel.setText("Cargando agenda...");

        fechaAgendaCargada = null;
        actualizarDuracionesBloqueo();
        api.consultarAgenda(sesion.token(), fecha).whenComplete((franjas, fallo) ->
                Platform.runLater(() -> {
                    if (version != versionConsulta
                            || !fecha.equals(fechaPicker.getValue())) {
                        return;
                    }
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
                    fechaAgendaCargada = fecha;
                    actualizarDuracionesBloqueo();
                    cargarBloqueos(fecha, version);
                    mensajeLabel.setText(
                            "Pistas: " + pistasTabPane.getTabs().size()
                                    + " · Franjas: " + franjas.size()
                    );
                })
        );
    }
}