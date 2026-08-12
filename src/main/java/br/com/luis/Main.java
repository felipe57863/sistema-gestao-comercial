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

    private boolean configuracaoInicialNecessaria;

    /**
     * Método principal da aplicação.
     * Apenas delega para o JavaFX iniciar o ciclo de vida.
     */
    public static void main(String[] args) {
        System.out.println("[INFO] Iniciando Sistema...");
        launch(args);
    }

    /**
     * Prepara a infraestrutura antes da criação da interface gráfica.
     *
     * Primeiro cria ou verifica a estrutura do banco por meio dos scripts SQL.
     * Em seguida, prepara os prazos de pagamento padrão e consulta se a instalação
     * ainda precisa configurar o primeiro administrador. Uma falha crítica em
     * qualquer dessas etapas interrompe o ciclo de inicialização.
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

            // 3. Define qual tela deve iniciar o fluxo da aplicação
            AuthService authService = new AuthService();
            configuracaoInicialNecessaria =
                    authService.precisaConfigurarAdministradorInicial();

            System.out.println("[INFO] Infraestrutura pronta.");

        } catch (Exception e) {
            // Falha crítica: não é seguro continuar sem banco
            System.err.println("[ERRO FATAL] Falha na inicialização: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Inicia a interface gráfica na configuração inicial ou no Login.
     *
     * Quando nenhum ADMIN existe, o responsável deve definir a senha definitiva
     * antes de usar o Login. Uma instalação já configurada abre diretamente a
     * autenticação normal.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {

        String caminhoFxml = configuracaoInicialNecessaria
                ? "/br/com/luis/view/ConfiguracaoInicial.fxml"
                : "/br/com/luis/view/Login.fxml";

        String titulo = configuracaoInicialNecessaria
                ? "ERP Comercial - Configuração Inicial"
                : "ERP Comercial - Login";

        // 1. Localiza o ficheiro FXML dentro da pasta resources
        URL fxmlLocation = getClass().getResource(caminhoFxml);

        // Fail-Fast: garante que o arquivo existe antes de continuar
        if (fxmlLocation == null) {
            throw new IllegalStateException(
                    "Tela inicial não encontrada: " + caminhoFxml
            );
        }

        // 2. Carrega o FXML e transforma em componentes JavaFX
        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();

        // 3. Cria a cena com base no layout carregado
        Scene scene = new Scene(root);

        // 4. Configura a janela principal
        primaryStage.setTitle(titulo);
        primaryStage.setScene(scene);

        // Mantém a janela principal maximizada.
        primaryStage.setMaximized(true);

        // 5. Exibe a interface para o usuário
        primaryStage.show();
    }
}
