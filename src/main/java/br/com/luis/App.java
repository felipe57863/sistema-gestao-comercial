package br.com.luis;

import br.com.luis.util.DatabaseBuilder;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Classe temporária para testes rápidos de telas JavaFX durante o desenvolvimento.
 *
 * IMPORTANTE:
 * Esta classe NÃO representa o fluxo oficial do sistema.
 *
 * Fluxo oficial:
 * Launcher -> Main -> DatabaseBuilder -> AuthService -> Login.fxml
 *
 * Fluxo temporário desta classe:
 * App -> DatabaseBuilder -> Produto.fxml
 *
 * Regras:
 * - Não deve ser chamada pelo Launcher.
 * - Não substitui a classe Main.
 * - Não deve conter regra de negócio.
 * - Pode ser removida futuramente quando a navegação oficial estiver estabilizada.
 */
public class App extends Application {

    /**
     * Inicialização mínima para testes diretos de telas.
     * Garante que as tabelas existam antes de abrir o FXML de teste.
     */
    @Override
    public void init() {
        System.out.println("[INFO] App de teste: preparando banco para tela isolada...");

        try {
            DatabaseBuilder.buildTables();
            System.out.println("[INFO] App de teste: banco preparado.");

        } catch (Exception e) {
            System.err.println("[ERRO] Falha ao preparar banco no App de teste: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start(Stage stage) throws IOException {

        URL fxmlLocation = App.class.getResource("/br/com/luis/view/Produto.fxml");

        if (fxmlLocation == null) {
            throw new IllegalStateException(
                    "Produto.fxml não encontrado! Verifique o caminho em resources/br/com/luis/view."
            );
        }

        FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root);

        stage.setTitle("Teste de Tela - Cadastro de Produto");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}