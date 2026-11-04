package com.clubpadelcostabrava.desktop.ui;

import com.clubpadelcostabrava.desktop.api.ApiCliente;
import com.clubpadelcostabrava.desktop.api.ApiException;
import com.clubpadelcostabrava.desktop.data.*;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.Optional;

/** Gestiona la consulta y modificación de socios desde administración. */
public class SociosController {

    @FXML private Label mensajeLabel;
    @FXML private TableView<Socio> sociosTable;
    @FXML private TableColumn<Socio, Integer> idCol;
    @FXML private TableColumn<Socio, String> nombreCol;
    @FXML private TableColumn<Socio, String> apellidosCol;
    @FXML private TableColumn<Socio, String> telefonoCol;
    @FXML private TableColumn<Socio, String> correoCol;
    @FXML private TableColumn<Socio, String> estadoCol;
    @FXML private TableColumn<Socio, String> cuentaCol;
    @FXML private Label detalleLabel;

    private final ApiCliente api = new ApiCliente();
    private Sesion sesion;
    private Runnable onSesionInvalida;

    /** Recibe la sesión al abrir la pantalla y carga los socios. */
    public void setSesion(Sesion sesion, Runnable onSesionInvalida) {
        this.sesion = sesion;
        this.onSesionInvalida = onSesionInvalida;
        cargarSocios();
    }

    /** Configura las columnas y muestra la ficha del socio seleccionado. */
    @FXML
    private void initialize() {
        idCol.setCellValueFactory(fila ->
                new ReadOnlyObjectWrapper<>(fila.getValue().id()));
        nombreCol.setCellValueFactory(fila ->
                new ReadOnlyStringWrapper(fila.getValue().nombre()));
        apellidosCol.setCellValueFactory(fila ->
                new ReadOnlyStringWrapper(fila.getValue().apellidos()));
        telefonoCol.setCellValueFactory(fila ->
                new ReadOnlyStringWrapper(fila.getValue().telefono()));
        correoCol.setCellValueFactory(fila ->
                new ReadOnlyStringWrapper(fila.getValue().correo()));
        estadoCol.setCellValueFactory(fila ->
                new ReadOnlyStringWrapper(fila.getValue().estado()));
        cuentaCol.setCellValueFactory(fila ->
                new ReadOnlyStringWrapper(
                        fila.getValue().cuentaActivada() ? "Activada" : "Pendiente"
                ));
        sociosTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, anterior, socio) -> {
                    if (socio == null) {
                        detalleLabel.setText("");
                        return;
                    }

                    detalleLabel.setText(
                            "Nombre: " + socio.nombre() + " " + socio.apellidos()
                                    + "\nTeléfono: " + socio.telefono()
                                    + "\nDirección: " + socio.direccion()
                                    + "\nPoblación: " + socio.poblacion()
                                    + "\nCorreo: " + socio.correo()
                                    + "\nEstado: " + socio.estado()
                                    + "\nCuenta activada: "
                                    + (socio.cuentaActivada() ? "Sí" : "No")
                    );
                });
    }

    /** Vuelve a consultar la lista de socios. */
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

                        if (gestionarSesionInvalida(causa)) {
                            return;
                        }

                        mensajeLabel.setText("Error: " + causa.getMessage());
                        return;
                    }

                    sociosTable.getItems().setAll(socios);
                    mensajeLabel.setText("Socios: " + socios.size());
                })
        );
    }

    /** Abre el mismo formulario vacío para alta o relleno para edición. */
    private Optional<DatosSocio> mostrarFormulario(Socio socio) throws IOException {
        FXMLLoader cargador = new FXMLLoader(
                getClass().getResource(
                        "/com/clubpadelcostabrava/desktop/socio-formulario-view.fxml"
                )
        );

        Parent vista = cargador.load();
        SocioFormularioController formulario = cargador.getController();

        if (socio != null) {
            formulario.cargarSocio(socio);
        }

        Dialog<DatosSocio> dialogo = new Dialog<>();
        dialogo.initOwner(sociosTable.getScene().getWindow());
        dialogo.setTitle(socio == null ? "Nuevo socio" : "Editar socio");
        dialogo.getDialogPane().setContent(vista);

        ButtonType guardar = new ButtonType(
                "Guardar", ButtonBar.ButtonData.OK_DONE
        );
        dialogo.getDialogPane().getButtonTypes()
                .addAll(guardar, ButtonType.CANCEL);

        dialogo.setResultConverter(boton ->
                boton == guardar ? formulario.leerDatos() : null
        );

        return dialogo.showAndWait();
    }

    /** Abre el alta y envía la nueva ficha al servidor. */
    @FXML
    private void onNuevo() {
        try {
            Optional<DatosSocio> resultado = mostrarFormulario(null);

            if (resultado.isEmpty()) {
                return; // Se pulsó Cancelar.
            }

            mensajeLabel.setText("Creando socio...");

            api.crearSocio(sesion.token(), resultado.get())
                    .whenComplete((creado, fallo) ->
                            Platform.runLater(() -> {
                                if (fallo != null) {
                                    Throwable causa = fallo.getCause() != null
                                            ? fallo.getCause() : fallo;

                                    if (gestionarSesionInvalida(causa)) {
                                        return;
                                    }

                                    mensajeLabel.setText("");
                                    new Alert(
                                            Alert.AlertType.WARNING,
                                            causa.getMessage() != null
                                                    ? causa.getMessage()
                                                    : "No se pudo guardar la ficha."
                                    ).showAndWait();
                                    return;
                                }

                                cargarSocios();
                                new Alert(
                                        Alert.AlertType.INFORMATION,
                                        "Socio #" + creado.id() + " creado."
                                ).show();
                            })
                    );

        } catch (IOException e) {
            mensajeLabel.setText("No se pudo abrir el formulario.");
        }
    }

    /** Modifica los datos personales del socio seleccionado. */
    @FXML
    private void onEditar() {
        Socio socio = sociosTable.getSelectionModel().getSelectedItem();

        if (socio == null) {
            mensajeLabel.setText("Selecciona un socio.");
            return;
        }

        try {
            Optional<DatosSocio> resultado = mostrarFormulario(socio);

            if (resultado.isEmpty()) {
                return; // Se pulsó Cancelar.
            }

            mensajeLabel.setText("Guardando cambios...");

            api.editarSocio(sesion.token(), socio.id(), resultado.get())
                    .whenComplete((actualizado, fallo) ->
                            Platform.runLater(() -> {
                                if (fallo != null) {
                                    Throwable causa = fallo.getCause() != null
                                            ? fallo.getCause() : fallo;

                                    if (gestionarSesionInvalida(causa)) {
                                        return;
                                    }

                                    mensajeLabel.setText("");
                                    new Alert(
                                            Alert.AlertType.WARNING,
                                            causa.getMessage() != null
                                                    ? causa.getMessage()
                                                    : "No se pudo guardar la ficha."
                                    ).showAndWait();
                                    return;
                                }

                                sociosTable.getSelectionModel().clearSelection();
                                cargarSocios();
                                new Alert(
                                        Alert.AlertType.INFORMATION,
                                        "Ficha del socio #" + actualizado.id() + " actualizada."
                                ).show();
                            })
                    );

        } catch (IOException e) {
            mensajeLabel.setText("No se pudo abrir el formulario.");
        }
    }

    /** Pide el nuevo correo y la contraseña ADMIN antes del cambio. */
    @FXML
    private void onCambiarCorreo() {
        Socio socio = sociosTable.getSelectionModel().getSelectedItem();

        if (socio == null) {
            mensajeLabel.setText("Selecciona un socio.");
            return;
        }

        TextField correoField = new TextField();
        correoField.setPromptText("Nuevo correo");

        PasswordField contrasenaField = new PasswordField();
        contrasenaField.setPromptText("Contraseña ADMIN");

        VBox campos = new VBox(
                8,
                new Label("Nuevo correo"),
                correoField,
                new Label("Confirma tu contraseña"),
                contrasenaField
        );
        campos.setPrefWidth(380);

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.initOwner(sociosTable.getScene().getWindow());
        dialogo.setTitle("Cambiar correo");
        dialogo.setHeaderText("Correo actual: " + socio.correo());
        dialogo.getDialogPane().setContent(campos);

        ButtonType confirmar = new ButtonType(
                "Cambiar correo", ButtonBar.ButtonData.OK_DONE
        );
        dialogo.getDialogPane().getButtonTypes()
                .addAll(confirmar, ButtonType.CANCEL);

        Optional<ButtonType> resultado = dialogo.showAndWait();

        if (resultado.isEmpty() || resultado.get() != confirmar) {
            contrasenaField.clear();
            return;
        }

        String nuevoCorreo = correoField.getText().strip();
        String contrasenaAdmin = contrasenaField.getText();
        contrasenaField.clear();

        if (nuevoCorreo.isBlank() || contrasenaAdmin.isBlank()) {
            mensajeLabel.setText("Indica el nuevo correo y tu contraseña.");
            return;
        }

        mensajeLabel.setText("Cambiando correo...");

        api.cambiarCorreoSocio(
                sesion.token(), socio.id(), nuevoCorreo, contrasenaAdmin
        ).whenComplete((correoActualizado, fallo) ->
                Platform.runLater(() -> {
                    if (fallo != null) {
                        Throwable causa = fallo.getCause() != null
                                ? fallo.getCause() : fallo;

                        if (gestionarSesionInvalida(causa)) {
                            return;
                        }

                        mensajeLabel.setText("");
                        new Alert(
                                Alert.AlertType.WARNING,
                                causa.getMessage() != null
                                        ? causa.getMessage()
                                        : "No se pudo cambiar el correo."
                        ).showAndWait();
                        return;
                    }

                    sociosTable.getSelectionModel().clearSelection();
                    cargarSocios();
                    new Alert(
                            Alert.AlertType.INFORMATION,
                            "Correo actualizado a " + correoActualizado
                    ).show();
                })
        );
    }

    /** Confirma el cambio y muestra las reservas canceladas y el aviso. */
    @FXML
    private void onCambiarEstado() {
        Socio socio = sociosTable.getSelectionModel().getSelectedItem();

        if (socio == null) {
            mensajeLabel.setText("Selecciona un socio.");
            return;
        }

        String nuevoEstado = "ACTIVO".equals(socio.estado())
                ? "INACTIVO"
                : "ACTIVO";

        String pregunta = "INACTIVO".equals(nuevoEstado)
                ? "Se inactivará a " + socio.nombre()
                + " y se cancelarán sus reservas futuras. ¿Continuar?"
                : "Se reactivará a " + socio.nombre() + ". ¿Continuar?";

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                pregunta,
                ButtonType.OK,
                ButtonType.CANCEL
        );
        confirmacion.setHeaderText("Cambiar estado del socio");

        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL)
                != ButtonType.OK) {
            return;
        }

        mensajeLabel.setText("Cambiando estado...");

        api.cambiarEstadoSocio(sesion.token(), socio.id(), nuevoEstado)
                .whenComplete((resultado, fallo) ->
                        Platform.runLater(() -> {
                            if (fallo != null) {
                                Throwable causa = fallo.getCause() != null
                                        ? fallo.getCause() : fallo;

                                if (gestionarSesionInvalida(causa)) {
                                    return;
                                }

                                mensajeLabel.setText("");
                                new Alert(
                                        Alert.AlertType.ERROR,
                                        causa.getMessage() != null
                                                ? causa.getMessage()
                                                : "No se pudo cambiar el estado."
                                ).showAndWait();
                                return;
                            }

                            sociosTable.getSelectionModel().clearSelection();
                            cargarSocios();

                            String mensaje = "Estado: " + resultado.estado()
                                    + "\nReservas canceladas: "
                                    + resultado.reservasCanceladas();

                            if ("ACEPTADO".equals(resultado.avisoCorreo())) {
                                mensaje += "\nAviso por correo enviado.";
                            } else if ("FALLIDO".equals(resultado.avisoCorreo())) {
                                mensaje += "\nNo se pudo enviar el aviso por correo.";
                            }

                            Alert.AlertType tipo = "FALLIDO".equals(
                                    resultado.avisoCorreo()
                            ) ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION;

                            new Alert(tipo, mensaje).showAndWait();
                        })
                );
    }

    /** Devuelve al login cuando el servidor rechaza la sesión. */
    private boolean gestionarSesionInvalida(Throwable causa) {
        if (causa instanceof ApiException error
                && "SESION_INVALIDA".equals(error.codigo())) {
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