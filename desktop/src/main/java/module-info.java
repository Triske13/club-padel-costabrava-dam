module com.clubpadelcostabrava.desktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;


    opens com.clubpadelcostabrava.desktop to javafx.fxml;
    exports com.clubpadelcostabrava.desktop;
}