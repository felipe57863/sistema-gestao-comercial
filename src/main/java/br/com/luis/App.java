package br.com.luis;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Carrega o FXML que acabámos de criar
        // Certifique-se de que o caminho corresponde à pasta em resources
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("view/Cliente.fxml"));
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root);
        stage.setTitle("Sistema de Gestão Comercial - Cadastro de Clientes");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}