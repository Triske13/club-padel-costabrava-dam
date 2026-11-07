package com.clubpadelcostabrava.desktop;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class DesktopApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                DesktopApplication.class.getResource("login-view.fxml")
        );
        Scene scene = new Scene(fxmlLoader.load(), 420, 420);

        stage.setTitle("Club Pádel Costa Brava - Administración");
        stage.getIcons().add(new Image(
                DesktopApplication.class.getResourceAsStream("images/logo_club.png")
        ));
        stage.setScene(scene);
        stage.show();
    }
}
