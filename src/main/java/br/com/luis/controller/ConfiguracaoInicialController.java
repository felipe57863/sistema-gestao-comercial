package br.com.luis.controller;

import br.com.luis.service.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Controller da configuração segura do primeiro administrador.
 *
 * Coleta somente senha e confirmação, delega todas as regras ao AuthService e
 * abre o Login após a persistência. Não acessa DAO, Connection, BCrypt ou
 * SessaoUsuario.
 */
public class ConfiguracaoInicialController {

    @FXML private Label lblNomeAdministrador;
    @FXML private Label lblLoginAdministrador;
    @FXML private PasswordField txtNovaSenha;
    @FXML private PasswordField txtConfirmacaoSenha;
    @FXML private Button btnConfigurarAdministrador;

    private AuthService authService;

    @FXML
    public void initialize() {
        authService = new AuthService();

        lblNomeAdministrador.setText(
                authService.obterNomeAdministradorInicial()
        );
        lblLoginAdministrador.setText(
                authService.obterLoginAdministradorInicial()
        );

        txtNovaSenha.requestFocus();
    }

    /**
     * Solicita o provisionamento e abre o Login sem criar sessão automática.
     */
    @FXML
    private void configurarAdministrador() {
        String senha = txtNovaSenha.getText();
        String confirmacaoSenha = txtConfirmacaoSenha.getText();
        boolean configuracaoConcluida = false;

        try {
            btnConfigurarAdministrador.setDisable(true);

            authService.configurarAdministradorInicial(
                    senha,
                    confirmacaoSenha
            );

            configuracaoConcluida = true;
            limparCamposSenha();

            if (!abrirLogin()) {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Configuração concluída",
                        "O administrador foi configurado, mas não foi possível "
                                + "abrir o Login. Reinicie a aplicação."
                );
            }

        } catch (RuntimeException e) {
            limparCamposSenha();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Não foi possível configurar o administrador",
                    mensagemSegura(e)
            );

        } finally {
            btnConfigurarAdministrador.setDisable(configuracaoConcluida);
        }
    }

    /**
     * Carrega o Login no mesmo Stage somente após o cadastro confirmado.
     */
    private boolean abrirLogin() {
        try {
            URL fxmlLocation = getClass().getResource(
                    "/br/com/luis/view/Login.fxml"
            );

            if (fxmlLocation == null) {
                throw new IllegalStateException("Login.fxml não encontrado.");
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            Scene sceneAtual = btnConfigurarAdministrador.getScene();

            if (sceneAtual == null
                    || !(sceneAtual.getWindow() instanceof Stage stage)) {
                throw new IllegalStateException(
                        "Stage atual da configuração inicial não encontrado."
                );
            }

            stage.setMaximized(false);
            stage.setTitle("ERP Comercial - Login");
            stage.setScene(new Scene(root));

            Platform.runLater(() -> stage.setMaximized(true));

            return true;

        } catch (IOException | RuntimeException e) {
            System.err.println(
                    "[ERRO] Falha ao abrir Login.fxml após a configuração inicial."
            );
            return false;
        }
    }

    private void limparCamposSenha() {
        txtNovaSenha.clear();
        txtConfirmacaoSenha.clear();
        txtNovaSenha.requestFocus();
    }

    private String mensagemSegura(RuntimeException e) {
        String mensagem = e.getMessage();

        if (mensagem == null || mensagem.isBlank()) {
            return "Ocorreu uma falha inesperada durante a configuração inicial.";
        }

        return mensagem;
    }

    private void mostrarAlerta(
            Alert.AlertType tipo,
            String titulo,
            String mensagem
    ) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensagem);
        alerta.showAndWait();
    }
}
