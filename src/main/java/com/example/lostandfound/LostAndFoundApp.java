package com.example.lostandfound;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class LostAndFoundApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("lostandfound.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);

        // Set Application Icon
        try {
            Image icon = new Image(getClass().getResourceAsStream("logo.png"));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("Could not load application icon: " + e.getMessage());
        }

        stage.setTitle("Lost and Found System");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
