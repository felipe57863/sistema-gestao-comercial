package br.com.luis.controller;

import br.com.luis.model.Usuario;
import br.com.luis.service.GestaoUsuarioService;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Controller do modal de redefinição administrativa de senha.
 *
 * Recebe administrador e alvo explicitamente, coleta as senhas somente durante
 * a confirmação e não acessa DAO ou SessaoUsuario.
 */
public class RedefinirSenhaUsuarioController {

    private final GestaoUsuarioService gestaoUsuarioService;

    private Usuario administradorLogado;
    private Usuario usuarioAlvo;

    @FXML private Label lblLogin;
    @FXML private Label lblPerfil;
    @FXML private Label lblStatus;
    @FXML private PasswordField txtSenhaTemporaria;
    @FXML private PasswordField txtConfirmacao;
    @FXML private Button btnConfirmar;
    @FXML private Button btnCancelar;

    public RedefinirSenhaUsuarioController() {
        this.gestaoUsuarioService = new GestaoUsuarioService();
    }

    @FXML
    public void initialize() {
        btnConfirmar.setDisable(true);
        txtSenhaTemporaria.requestFocus();
    }

    /**
     * Define o contexto completo antes da exibição do modal.
     */
    public void definirContexto(
            Usuario administrador,
            Usuario alvo
    ) {

        validarAdministrador(administrador);

        if (alvo == null) {
            throw new IllegalArgumentException(
                    "Usuário alvo é obrigatório."
            );
        }

        if (alvo.getIdUsuario() == null || alvo.getIdUsuario() <= 0) {
            throw new IllegalStateException(
                    "Usuário alvo não possui um ID válido."
            );
        }

        if (administrador.getIdUsuario().equals(alvo.getIdUsuario())) {
            throw new IllegalStateException(
                    "Use a opção Alterar Senha para modificar a própria senha."
            );
        }

        this.administradorLogado = administrador;
        this.usuarioAlvo = alvo;

        lblLogin.setText(alvo.getLogin());
        lblPerfil.setText(alvo.getPerfil());
        lblStatus.setText(alvo.getStatus());
        btnConfirmar.setDisable(false);
    }

    @FXML
    private void confirmar() {
        try {
            btnConfirmar.setDisable(true);

            if (administradorLogado == null || usuarioAlvo == null) {
                throw new IllegalStateException(
                        "O contexto da redefinição não foi informado."
                );
            }

            gestaoUsuarioService.redefinirSenhaAdministrativamente(
                    administradorLogado,
                    usuarioAlvo,
                    txtSenhaTemporaria.getText(),
                    txtConfirmacao.getText()
            );

            limparCampos();

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Senha redefinida",
                    "Senha temporária definida. A troca será obrigatória "
                            + "no próximo acesso do usuário."
            );

            fecharModal();

        } catch (RuntimeException e) {
            limparCampos();
            btnConfirmar.setDisable(false);

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Não foi possível redefinir a senha",
                    e.getMessage() == null || e.getMessage().isBlank()
                            ? "Ocorreu uma falha inesperada."
                            : e.getMessage()
            );
        }
    }

    @FXML
    private void cancelar() {
        limparCampos();
        fecharModal();
    }

    private void validarAdministrador(Usuario administrador) {
        if (administrador == null) {
            throw new IllegalArgumentException(
                    "Administrador logado é obrigatório."
            );
        }

        if (administrador.getIdUsuario() == null
                || administrador.getIdUsuario() <= 0) {
            throw new IllegalStateException(
                    "Administrador logado não possui um ID válido."
            );
        }

        if (!"ADMIN".equals(administrador.getPerfil())
                || !"ATIVO".equals(administrador.getStatus())
                || administrador.isTrocaSenhaObrigatoria()) {
            throw new IllegalStateException(
                    "O usuário atual não pode redefinir senhas administrativamente."
            );
        }
    }

    private void limparCampos() {
        txtSenhaTemporaria.clear();
        txtConfirmacao.clear();
        txtSenhaTemporaria.requestFocus();
    }

    private void fecharModal() {
        Scene sceneAtual = btnCancelar.getScene();

        if (sceneAtual == null) {
            throw new IllegalStateException(
                    "Scene da redefinição de senha não encontrada."
            );
        }

        Window windowAtual = sceneAtual.getWindow();

        if (!(windowAtual instanceof Stage stage)) {
            throw new IllegalStateException(
                    "Janela da redefinição de senha não encontrada."
            );
        }

        stage.close();
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
