package br.com.luis.controller;

import br.com.luis.model.Usuario;
import br.com.luis.service.TrocaSenhaService;
import br.com.luis.util.NavegacaoUtil;
import br.com.luis.util.SessaoUsuario;
import javafx.event.ActionEvent;
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
 * Controller da tela restrita usada para concluir a troca obrigatória de senha.
 *
 * O usuário recebido já possui credenciais válidas, mas ainda não pode acessar
 * o ERP normalmente. Por isso a referência permanece somente neste Controller
 * e entra na SessaoUsuario apenas depois da persistência concluída com sucesso.
 */
public class TrocaSenhaObrigatoriaController {

    private final TrocaSenhaService trocaSenhaService;

    private Usuario usuarioAutenticado;

    @FXML private Label lblUsuario;
    @FXML private PasswordField txtNovaSenha;
    @FXML private PasswordField txtConfirmacaoSenha;
    @FXML private Button btnConfirmar;
    @FXML private Button btnVoltar;

    public TrocaSenhaObrigatoriaController() {
        this.trocaSenhaService = new TrocaSenhaService();
    }

    @FXML
    public void initialize() {
        txtNovaSenha.requestFocus();
    }

    /**
     * Recebe o usuário autenticado antes de a tela restrita ser exibida.
     *
     * @param usuario usuário ativo com troca obrigatória pendente.
     */
    public void definirUsuarioAutenticado(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException(
                    "Usuário autenticado é obrigatório."
            );
        }

        if (usuario.getIdUsuario() == null || usuario.getIdUsuario() <= 0) {
            throw new IllegalStateException(
                    "Usuário autenticado não possui um ID válido."
            );
        }

        if (!usuario.isTrocaSenhaObrigatoria()) {
            throw new IllegalStateException(
                    "O usuário não possui troca obrigatória pendente."
            );
        }

        if (SessaoUsuario.getInstance().isUsuarioLogado()) {
            throw new IllegalStateException(
                    "A sessão normal deve permanecer vazia antes da troca."
            );
        }

        this.usuarioAutenticado = usuario;
        lblUsuario.setText(usuario.getLogin());
    }

    /**
     * Valida e persiste a nova senha antes de liberar a sessão normal.
     */
    @FXML
    private void confirmarTrocaSenha(ActionEvent event) {
        String novaSenha = txtNovaSenha.getText();
        String confirmacao = txtConfirmacaoSenha.getText();
        boolean trocaConcluida = false;

        try {
            btnConfirmar.setDisable(true);

            if (usuarioAutenticado == null) {
                throw new IllegalStateException(
                        "Usuário autenticado não foi informado para a troca."
                );
            }

            trocaSenhaService.concluirTrocaSenhaObrigatoria(
                    usuarioAutenticado,
                    novaSenha,
                    confirmacao
            );
            trocaConcluida = true;

            if (usuarioAutenticado.isTrocaSenhaObrigatoria()) {
                throw new IllegalStateException(
                        "A troca foi persistida, mas o usuário não foi atualizado."
                );
            }

            // A sessão normal nasce somente após o Service confirmar o commit.
            SessaoUsuario.getInstance().setUsuarioLogado(usuarioAutenticado);

            if (!abrirTelaPrincipal(usuarioAutenticado.getNome())) {
                limparSessaoNormal();
                limparCamposSenha();

                // A senha já foi persistida e não deve ser desfeita por uma
                // falha exclusivamente visual. O retorno ao Login permanece ativo.
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Erro",
                        "A nova senha foi salva, mas não foi possível abrir "
                                + "a Tela Principal. Retorne ao Login e entre "
                                + "com a nova senha."
                );

                return;
            }

        } catch (RuntimeException e) {
            limparSessaoNormal();
            limparCamposSenha();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Não foi possível concluir a troca",
                    e.getMessage()
            );

        } finally {
            // Depois de uma troca persistida, evita nova tentativa na mesma tela
            // caso a navegação tenha falhado.
            btnConfirmar.setDisable(trocaConcluida);
        }
    }

    /**
     * Retorna ao Login sem alterar senha ou indicador.
     */
    @FXML
    private void voltarAoLogin(ActionEvent event) {
        try {
            URL fxmlLocation = getClass().getResource(
                    "/br/com/luis/view/Login.fxml"
            );

            if (fxmlLocation == null) {
                throw new IllegalStateException("Login.fxml não encontrado.");
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();
            Stage stage = obterStageAtual();

            limparSessaoNormal();

            stage.setMaximized(false);
            stage.setScene(new Scene(root));
            stage.setTitle("ERP Comercial - Login");
            stage.sizeToScene();
            stage.centerOnScreen();

            usuarioAutenticado = null;

        } catch (IOException | RuntimeException e) {
            System.err.println("[ERRO] Falha ao retornar para Login.fxml.");
            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro",
                    "Não foi possível retornar à tela de Login."
            );
        }
    }

    /**
     * Prepara e abre a Tela Principal reutilizando o Stage atual.
     */
    private boolean abrirTelaPrincipal(String nomeUsuario) {
        Scene cenaTrocaSenha = btnConfirmar.getScene();
        Stage stage = null;

        try {
            URL fxmlLocation = getClass().getResource(
                    "/br/com/luis/view/TelaPrincipal.fxml"
            );

            if (fxmlLocation == null) {
                throw new IllegalStateException(
                        "TelaPrincipal.fxml não encontrado."
                );
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();
            stage = obterStageAtual();

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Senha atualizada",
                    "Nova senha definida com sucesso. Bem-vindo(a), "
                            + nomeUsuario + "!"
            );

            stage.setTitle("ERP Comercial - Tela Principal");
            NavegacaoUtil.configurarTelaFuncional(
                    stage,
                    new Scene(root)
            );

            return true;

        } catch (IOException | RuntimeException e) {
            System.err.println(
                    "[ERRO] Falha ao abrir TelaPrincipal.fxml após a troca de senha."
            );
            e.printStackTrace();

            if (stage != null && cenaTrocaSenha != null) {
                try {
                    stage.setMaximized(false);
                    stage.setScene(cenaTrocaSenha);
                    stage.setTitle("ERP Comercial - Troca Obrigatória de Senha");
                    stage.sizeToScene();
                    stage.centerOnScreen();
                } catch (RuntimeException restauracaoErro) {
                    e.addSuppressed(restauracaoErro);
                }
            }

            return false;
        }
    }

    private Stage obterStageAtual() {
        Scene sceneAtual = btnConfirmar.getScene();

        if (sceneAtual == null
                || !(sceneAtual.getWindow() instanceof Stage stage)) {
            throw new IllegalStateException(
                    "Stage atual da troca de senha não encontrado."
            );
        }

        return stage;
    }

    private void limparCamposSenha() {
        txtNovaSenha.clear();
        txtConfirmacaoSenha.clear();
        txtNovaSenha.requestFocus();
    }

    private void limparSessaoNormal() {
        SessaoUsuario sessaoUsuario = SessaoUsuario.getInstance();
        sessaoUsuario.fazerLogout();

        if (sessaoUsuario.isUsuarioLogado()) {
            throw new IllegalStateException(
                    "Não foi possível manter a sessão normal vazia."
            );
        }
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
