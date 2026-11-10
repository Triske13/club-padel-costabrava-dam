package com.clubpadelcostabrava.desktop.ui;

import com.clubpadelcostabrava.desktop.data.DatosSocio;
import com.clubpadelcostabrava.desktop.data.Socio;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

/**
 * Controla los campos del formulario utilizado para crear o editar socios.
 * Recoge los datos escritos y, en modo edición, carga la ficha existente
 * dejando el correo como campo de solo lectura.
 */
public class SocioFormularioController {

    @FXML
    private TextField nombreField;
    @FXML
    private TextField apellidosField;
    @FXML
    private TextField telefonoField;
    @FXML
    private TextField direccionField;
    @FXML
    private TextField poblacionField;
    @FXML
    private TextField correoField;

    /**
     * Lee los campos del formulario y elimina los espacios de los extremos
     * antes de preparar los datos para enviarlos a la API.
     *
     * @return datos personales y correo introducidos en el formulario
     */
    public DatosSocio leerDatos() {
        return new DatosSocio(
                nombreField.getText().strip(),
                apellidosField.getText().strip(),
                telefonoField.getText().strip(),
                direccionField.getText().strip(),
                poblacionField.getText().strip(),
                correoField.getText().strip()
        );
    }

    /**
     * Rellena el formulario con la ficha del socio seleccionado.
     * Impide editar el correo aquí porque su cambio requiere una operación
     * independiente con confirmación de la contraseña ADMIN.
     *
     * @param socio ficha cuyos datos se mostrarán en el formulario
     */
    public void cargarSocio(Socio socio) {
        nombreField.setText(socio.nombre());
        apellidosField.setText(socio.apellidos());
        telefonoField.setText(socio.telefono());
        direccionField.setText(socio.direccion());
        poblacionField.setText(socio.poblacion());
        correoField.setText(socio.correo());
        correoField.setEditable(false);
    }
}