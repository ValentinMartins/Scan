module org.example.scan {
    requires javafx.controls;
    requires javafx.fxml;

    opens org.example.scan.controller to javafx.fxml;
    opens org.example.scan            to javafx.graphics;    // <fx:controller="…controller.MainController">

    exports org.example.scan;
}