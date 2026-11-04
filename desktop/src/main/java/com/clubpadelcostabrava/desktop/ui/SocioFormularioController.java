package com.clubpadelcostabrava.desktop.ui;

import com.clubpadelcostabrava.desktop.data.DatosSocio;
import com.clubpadelcostabrava.desktop.data.Socio;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

/** Lee y rellena los campos del formulario de socio. */
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

    /** Recoge lo escrito para enviarlo a la API. */
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

    /** Rellena el formulario al editar; el correo tiene otra operación. */
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