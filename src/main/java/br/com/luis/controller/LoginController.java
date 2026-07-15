package br.com.luis.controller;

import br.com.luis.model.Usuario;
import br.com.luis.service.AuthService;
import br.com.luis.util.SessaoUsuario;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Controller da Tela de Login.
 *
 * Captura login e senha da interface, delega a autenticação ao AuthService e,
 * após o sucesso, armazena o Usuario autenticado na SessaoUsuario e abre a tela
 * principal. Não contém a regra interna de autenticação nem acessa DAO diretamente.
 * Também controla o feedback visual e o estado do botão durante a tentativa.
 */
public class LoginController {

    @FXML private TextField txtLogin;
    @FXML private PasswordField txtSenha;
    @FXML private Button btnEntrar;

    private AuthService authService;

    /**
     * Inicializa o AuthService e posiciona o foco no campo de login.
     */
    @FXML
    public void initialize() {
        this.authService = new AuthService();

        // Posiciona o foco inicial no campo de login.
        txtLogin.requestFocus();
    }

    /**
     * Processa a ação do botão "Entrar".
     *
     * Lê as credenciais da interface, evita múltiplos cliques durante o fluxo e
     * delega a autenticação ao AuthService. Quando autenticado, mantém o usuário
     * na SessaoUsuario, exibe a confirmação e abre a tela principal no mesmo Stage.
     * Em caso de erro, limpa a senha e apresenta a mensagem recebida; ao final,
     * reabilita o botão de entrada.
     */
    @FXML
    public void fazerLogin(ActionEvent event) {

        String login = txtLogin.getText();
        String senha = txtSenha.getText();

        try {
            // Evita múltiplos cliques durante a tentativa de autenticação.
            btnEntrar.setDisable(true);

            // Delega a autenticação ao AuthService.
            Usuario usuarioAutenticado = authService.autenticar(login, senha);

            // Armazena o usuário autenticado na sessão da aplicação.
            SessaoUsuario.getInstance().setUsuarioLogado(usuarioAutenticado);

            // Apresenta o feedback visual de autenticação bem-sucedida.
            mostrarAlerta(Alert.AlertType.INFORMATION,
                    "Sucesso",
                    "Bem-vindo(a), " + usuarioAutenticado.getNome() + "!");

            // Redireciona para a tela principal do sistema.
            abrirTelaPrincipalTemporaria();

        } catch (RuntimeException e) {

            // Limpa a senha após falha na autenticação ou na abertura da tela.
            txtSenha.clear();

            mostrarAlerta(Alert.AlertType.ERROR,
                    "Erro de Acesso",
                    e.getMessage());

        } finally {
            btnEntrar.setDisable(false);
        }
    }

    /**
     * Abre a tela principal do sistema após a autenticação.
     *
     * Apesar do nome histórico do método, este é o fluxo real de entrada no
     * sistema. Carrega TelaPrincipal.fxml, reutiliza o Stage da tela de login,
     * substitui a Scene, atualiza o título e mantém a janela maximizada.
     */
    private void abrirTelaPrincipalTemporaria() {

        try {
            URL fxmlLocation = getClass().getResource("/br/com/luis/view/TelaPrincipal.fxml");

            if (fxmlLocation == null) {
                throw new IllegalStateException("TelaPrincipal.fxml não encontrado.");
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            Stage stage = (Stage) btnEntrar.getScene().getWindow();
            stage.setTitle("ERP Comercial - Tela Principal");
            stage.setScene(new Scene(root));
            stage.setMaximized(true);

        } catch (IOException | RuntimeException e) {
            System.err.println("[ERRO] Falha ao abrir TelaPrincipal.fxml.");
            e.printStackTrace();

            mostrarAlerta(Alert.AlertType.ERROR,
                    "Erro",
                    "Login realizado, mas não foi possível abrir a tela principal.");
        }
    }

    /**
     * Exibe alertas padronizados.
     */
    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensagem);
        alerta.showAndWait();
    }
}