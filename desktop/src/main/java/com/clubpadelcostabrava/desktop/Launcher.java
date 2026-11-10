package com.clubpadelcostabrava.desktop;

import javafx.application.Application;

/** Punto de entrada que lanza la aplicación JavaFX. */
public class Launcher {

    /**
     * Inicia la aplicación de escritorio.
     *
     * @param args argumentos de la línea de comandos
     */
    public static void main(String[] args) {
        Application.launch(DesktopApplication.class, args);
    }
}
