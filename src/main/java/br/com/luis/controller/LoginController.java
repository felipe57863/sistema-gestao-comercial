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
 * Responsável apenas por interação com a UI.
 */
public class LoginController {

    @FXML private TextField txtLogin;
    @FXML private PasswordField txtSenha;
    @FXML private Button btnEntrar;

    private AuthService authService;

    /**
     * Inicialização da tela.
     */
    @FXML
    public void initialize() {
        this.authService = new AuthService();

        // UX: foco inicial no campo login
        txtLogin.requestFocus();
    }

    /**
     * Ação do botão "Entrar".
     */
    @FXML
    public void fazerLogin(ActionEvent event) {

        String login = txtLogin.getText();
        String senha = txtSenha.getText();

        try {
            // Evita múltiplos cliques
            btnEntrar.setDisable(true);

            // Autenticação via Service
            Usuario usuarioAutenticado = authService.autenticar(login, senha);

            // Armazena na sessão
            SessaoUsuario.getInstance().setUsuarioLogado(usuarioAutenticado);

            // Feedback ao usuário
            mostrarAlerta(Alert.AlertType.INFORMATION,
                    "Sucesso",
                    "Bem-vindo(a), " + usuarioAutenticado.getNome() + "!");

            // Redireciona para a tela principal temporária da Fase 3
            abrirTelaPrincipalTemporaria();

        } catch (RuntimeException e) {

            // Limpa senha por segurança
            txtSenha.clear();

            mostrarAlerta(Alert.AlertType.ERROR,
                    "Erro de Acesso",
                    e.getMessage());

        } finally {
            btnEntrar.setDisable(false);
        }
    }

    /**
     * Abre a tela principal temporária usada para navegação da Fase 3.
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