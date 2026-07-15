package br.com.luis;

import br.com.luis.service.AuthService;
import br.com.luis.util.DatabaseBuilder;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

import br.com.luis.service.PrazoPagamentoService;

/**
 * Ponto de entrada do Sistema de Gestão Comercial (ERP).
 *
 * Esta classe segue o ciclo de vida do JavaFX:
 * - main() → inicia a aplicação
 * - init() → prepara infraestrutura (banco e dados iniciais)
 * - start() → carrega e exibe a interface gráfica
 */
public class Main extends Application {

    /**
     * Método principal da aplicação.
     * Apenas delega para o JavaFX iniciar o ciclo de vida.
     */
    public static void main(String[] args) {
        System.out.println("[INFO] Iniciando Sistema...");
        launch(args);
    }

    /**
     * Método executado ANTES da interface gráfica.
     * Ideal para tarefas pesadas como:
     * - criação do banco
     * - inicialização de dados (seed)
     */
    @Override
    public void init() {
        System.out.println("[INFO] Preparando infraestrutura...");

        try {
            // 1. Garante que as tabelas do banco existem
            DatabaseBuilder.buildTables();

            // 2. Inicializa os prazos padrão do sistema
            PrazoPagamentoService prazoPagamentoService = new PrazoPagamentoService();
            prazoPagamentoService.inicializarPrazosPadrao();

            // 3. Inicializa dados essenciais (usuário administrador padrão)
            AuthService authService = new AuthService();
            authService.inicializarAdminBase();

            System.out.println("[INFO] Infraestrutura pronta.");

        } catch (Exception e) {
            // Falha crítica: não é seguro continuar sem banco
            System.err.println("[ERRO FATAL] Falha na inicialização: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Método principal da interface gráfica.
     * Responsável por carregar o FXML e exibir a janela.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {

        // 1. Localiza o ficheiro FXML dentro da pasta resources
        URL fxmlLocation = getClass().getResource("/br/com/luis/view/Login.fxml");

        // Fail-Fast: garante que o arquivo existe antes de continuar
        if (fxmlLocation == null) {
            throw new IllegalStateException(
                    "Login.fxml não encontrado! Verifique o caminho em resources."
            );
        }

        // 2. Carrega o FXML e transforma em componentes JavaFX
        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();

        // 3. Cria a cena com base no layout carregado
        Scene scene = new Scene(root);

        // 4. Configura a janela principal
        primaryStage.setTitle("ERP Comercial - Login");
        primaryStage.setScene(scene);

        // Mantém a janela principal maximizada.
        primaryStage.setMaximized(true);

        // 5. Exibe a interface para o usuário
        primaryStage.show();
    }
}