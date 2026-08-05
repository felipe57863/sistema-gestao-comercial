package br.com.luis.controller;

import br.com.luis.model.Usuario;
import br.com.luis.service.TrocaSenhaService;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Controller da alteração voluntária de senha do usuário já logado.
 *
 * Coleta os valores somente durante a ação, delega as regras ao Service e fecha
 * exclusivamente a janela modal. A sessão existente não é criada, substituída
 * ou encerrada por este Controller.
 */
public class AlterarSenhaController {

    private final TrocaSenhaService trocaSenhaService;

    private Usuario usuarioLogado;

    @FXML private Label lblUsuario;
    @FXML private PasswordField txtSenhaAtual;
    @FXML private PasswordField txtNovaSenha;
    @FXML private PasswordField txtConfirmacaoSenha;
    @FXML private Button btnAlterarSenha;
    @FXML private Button btnCancelar;

    public AlterarSenhaController() {
        this.trocaSenhaService = new TrocaSenhaService();
    }

    @FXML
    public void initialize() {
        txtSenhaAtual.requestFocus();
    }

    /**
     * Recebe explicitamente o usuário mantido pela sessão normal.
     *
     * @param usuario usuário já logado e liberado para acesso ao ERP.
     */
    public void definirUsuarioLogado(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException(
                    "Usuário logado é obrigatório."
            );
        }

        if (usuario.getIdUsuario() == null || usuario.getIdUsuario() <= 0) {
            throw new IllegalStateException(
                    "Usuário logado não possui um ID válido."
            );
        }

        if (usuario.isTrocaSenhaObrigatoria()) {
            throw new IllegalStateException(
                    "A troca obrigatória ainda está pendente para este usuário."
            );
        }

        this.usuarioLogado = usuario;
        lblUsuario.setText(usuario.getLogin());
    }

    /**
     * Solicita ao Service a alteração da senha sem modificar a sessão atual.
     */
    @FXML
    private void alterarSenha() {
        String senhaAtual = txtSenhaAtual.getText();
        String novaSenha = txtNovaSenha.getText();
        String confirmacao = txtConfirmacaoSenha.getText();
        boolean alteracaoPersistida = false;

        try {
            btnAlterarSenha.setDisable(true);

            if (usuarioLogado == null) {
                throw new IllegalStateException(
                        "Usuário logado não foi informado."
                );
            }

            trocaSenhaService.alterarSenhaVoluntariamente(
                    usuarioLogado,
                    senhaAtual,
                    novaSenha,
                    confirmacao
            );

            alteracaoPersistida = true;
            limparCamposSenha();
            concluirInterfaceAposSucesso();

        } catch (RuntimeException e) {
            limparCamposSenha();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Não foi possível alterar a senha",
                    e.getMessage()
            );

        } finally {
            btnAlterarSenha.setDisable(alteracaoPersistida);
        }
    }

    /**
     * Fecha apenas a janela modal, sem alterar usuário, banco ou sessão.
     */
    @FXML
    private void cancelar() {
        try {
            fecharJanelaModal();
        } catch (RuntimeException e) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro",
                    "Não foi possível fechar a janela Alterar Senha."
            );
        }
    }

    /**
     * A persistência já foi confirmada neste ponto. Falhas exclusivamente
     * visuais não desfazem a nova senha nem encerram a sessão existente.
     */
    private void concluirInterfaceAposSucesso() {
        RuntimeException falhaVisual = null;

        try {
            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Senha alterada",
                    "A senha foi alterada com sucesso."
            );
        } catch (RuntimeException e) {
            falhaVisual = e;
            registrarFalhaVisual(e);
        }

        try {
            fecharJanelaModal();
        } catch (RuntimeException e) {
            if (falhaVisual != null) {
                e.addSuppressed(falhaVisual);
            }

            registrarFalhaVisual(e);

            try {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Senha alterada",
                        "A senha foi alterada, mas não foi possível fechar "
                                + "a janela. A sessão permanece ativa."
                );
            } catch (RuntimeException alertaErro) {
                e.addSuppressed(alertaErro);
                registrarFalhaVisual(alertaErro);
            }
        }
    }

    private void registrarFalhaVisual(RuntimeException e) {
        System.err.println(
                "[ERRO] Falha visual após a alteração voluntária de senha."
        );
        e.printStackTrace();
    }

    private void limparCamposSenha() {
        txtSenhaAtual.clear();
        txtNovaSenha.clear();
        txtConfirmacaoSenha.clear();
        txtSenhaAtual.requestFocus();
    }

    private void fecharJanelaModal() {
        Scene sceneAtual = btnCancelar.getScene();

        if (sceneAtual == null) {
            throw new IllegalStateException(
                    "Scene da alteração de senha não encontrada."
            );
        }

        Window windowAtual = sceneAtual.getWindow();

        if (!(windowAtual instanceof Stage stage)) {
            throw new IllegalStateException(
                    "Janela da alteração de senha não encontrada."
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
