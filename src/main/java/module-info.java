module com.example.lostandfound {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.lostandfound to javafx.fxml;
    exports com.example.lostandfound;
}