package br.com.luis.controller;

import br.com.luis.model.Usuario;
import br.com.luis.service.GestaoUsuarioService;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Controller do modal de edição cadastral de usuário.
 */
public class EditarUsuarioController {

    private final GestaoUsuarioService gestaoUsuarioService;

    private Integer administradorId;
    private Integer usuarioAlvoId;
    private String nomeOriginal;
    private String loginOriginal;
    private String perfilOriginal;
    private Usuario usuarioAtualizado;
    private boolean edicaoConcluida;

    @FXML private TextField txtNome;
    @FXML private TextField txtLogin;
    @FXML private ComboBox<String> cmbPerfil;
    @FXML private Label lblStatus;
    @FXML private Label lblTrocaObrigatoria;
    @FXML private Button btnCancelar;
    @FXML private Button btnSalvar;

    public EditarUsuarioController() {
        this.gestaoUsuarioService = new GestaoUsuarioService();
    }

    @FXML
    public void initialize() {
        cmbPerfil.getItems().setAll("ADMIN", "VENDEDOR");
    }

    /**
     * Recebe o executor e copia o snapshot sem modificar o objeto selecionado.
     */
    public void definirContexto(
            Integer administradorId,
            Usuario usuarioAlvo
    ) {

        if (administradorId == null || administradorId <= 0) {
            throw new IllegalArgumentException(
                    "Administrador executor não possui um ID válido."
            );
        }

        if (usuarioAlvo == null
                || usuarioAlvo.getIdUsuario() == null
                || usuarioAlvo.getIdUsuario() <= 0) {
            throw new IllegalArgumentException(
                    "Usuário alvo da edição é obrigatório."
            );
        }

        this.administradorId = administradorId;
        this.usuarioAlvoId = usuarioAlvo.getIdUsuario();
        this.nomeOriginal = usuarioAlvo.getNome();
        this.loginOriginal = usuarioAlvo.getLogin();
        this.perfilOriginal = usuarioAlvo.getPerfil();
        this.usuarioAtualizado = null;
        this.edicaoConcluida = false;

        txtNome.setText(nomeOriginal);
        txtLogin.setText(loginOriginal);
        cmbPerfil.getSelectionModel().select(perfilOriginal);
        lblStatus.setText(usuarioAlvo.getStatus());
        lblTrocaObrigatoria.setText(
                usuarioAlvo.isTrocaSenhaObrigatoria() ? "Sim" : "Não"
        );

        boolean autoedicao = administradorId.equals(usuarioAlvoId);
        cmbPerfil.setDisable(autoedicao);
        btnSalvar.setDisable(false);
        txtNome.requestFocus();
    }

    @FXML
    private void salvar() {
        if (administradorId == null || usuarioAlvoId == null) {
            mostrarErro("O contexto da edição não foi informado.");
            return;
        }

        btnSalvar.setDisable(true);

        try {
            Usuario atualizado = gestaoUsuarioService.editarUsuario(
                    administradorId,
                    usuarioAlvoId,
                    nomeOriginal,
                    loginOriginal,
                    perfilOriginal,
                    txtNome.getText(),
                    txtLogin.getText(),
                    cmbPerfil.getValue()
            );

            usuarioAtualizado = atualizado;
            edicaoConcluida = true;
            fecharModal();

        } catch (RuntimeException e) {
            mostrarErro(mensagemErro(e));

        } finally {
            btnSalvar.setDisable(false);
        }
    }

    @FXML
    private void cancelar() {
        fecharModal();
    }

    public boolean isEdicaoConcluida() {
        return edicaoConcluida;
    }

    public Usuario getUsuarioAtualizado() {
        return usuarioAtualizado;
    }

    private void fecharModal() {
        Scene scene = btnCancelar.getScene();

        if (scene == null) {
            return;
        }

        Window window = scene.getWindow();

        if (window instanceof Stage stage) {
            stage.close();
        }
    }

    private String mensagemErro(Throwable e) {
        return e.getMessage() == null || e.getMessage().isBlank()
                ? "Ocorreu uma falha inesperada."
                : e.getMessage();
    }

    private void mostrarErro(String mensagem) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Não foi possível editar o usuário");
        alerta.setHeaderText(null);
        alerta.setContentText(mensagem);
        alerta.showAndWait();
    }
}
