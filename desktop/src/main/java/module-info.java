module com.clubpadelcostabrava.desktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires jdk.crypto.ec;
    requires com.google.gson;


    opens com.clubpadelcostabrava.desktop to javafx.fxml;
    opens com.clubpadelcostabrava.desktop.ui to javafx.fxml;
    exports com.clubpadelcostabrava.desktop;
}