module org.example.whiteboard {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.whiteboard to javafx.fxml;
    exports org.example.whiteboard;
}