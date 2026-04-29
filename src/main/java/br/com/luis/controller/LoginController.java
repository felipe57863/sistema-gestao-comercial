package br.com.luis.controller;

import br.com.luis.model.Usuario;
import br.com.luis.service.AuthService;
import br.com.luis.util.SessaoUsuario;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

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

            // TODO: Redirecionar para tela principal (Dashboard)

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