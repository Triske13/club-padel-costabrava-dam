package com.clubpadelcostabrava.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicación Spring Boot del servidor.
 */
@SpringBootApplication
public class ServerApplication {

    /**
     * Inicia el servidor y carga el contexto de Spring.
     *
     * @param args argumentos de la línea de comandos
     */
    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

}
