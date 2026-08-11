package br.com.luis.controller;

import br.com.luis.model.Usuario;
import br.com.luis.service.GestaoUsuarioService;
import br.com.luis.util.CabecalhoUtil;
import br.com.luis.util.NavegacaoUtil;
import br.com.luis.util.SessaoUsuario;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Controller da gestão administrativa de usuários.
 *
 * Configura a interface, obtém o administrador da sessão, coleta os campos e
 * delega todas as regras ao Service. Não acessa DAO, Connection ou BCrypt.
 */
public class GestaoUsuariosController {

    private static final String FILTRO_TODOS = "Todos";

    private final GestaoUsuarioService gestaoUsuarioService;
    private final ObservableList<Usuario> usuariosCarregados;

    private Usuario administradorLogado;
    private Stage janelaAlterarSenha;

    @FXML private Button btnVoltar;
    @FXML private Label lblUsuario;
    @FXML private Label lblDataHora;
    @FXML private Button btnAlterarMinhaSenha;

    @FXML private TableView<Usuario> tabelaUsuarios;
    @FXML private TableColumn<Usuario, Integer> colId;
    @FXML private TableColumn<Usuario, String> colNome;
    @FXML private TableColumn<Usuario, String> colLogin;
    @FXML private TableColumn<Usuario, String> colPerfil;
    @FXML private TableColumn<Usuario, String> colStatus;
    @FXML private TableColumn<Usuario, String> colTrocaObrigatoria;

    @FXML private TextField txtPesquisaUsuario;
    @FXML private ComboBox<String> cmbFiltroPerfil;
    @FXML private ComboBox<String> cmbFiltroStatus;
    @FXML private ComboBox<String> cmbFiltroTrocaObrigatoria;
    @FXML private Label lblTotalUsuarios;

    @FXML private Button btnAtualizarLista;
    @FXML private Button btnAlterarStatus;
    @FXML private Button btnRedefinirSenha;

    @FXML private TextField txtNome;
    @FXML private TextField txtLogin;
    @FXML private ComboBox<String> cmbPerfil;
    @FXML private PasswordField txtSenhaTemporaria;
    @FXML private PasswordField txtConfirmacao;
    @FXML private Button btnCadastrar;
    @FXML private Button btnLimpar;

    public GestaoUsuariosController() {
        this.gestaoUsuarioService = new GestaoUsuarioService();
        this.usuariosCarregados = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        CabecalhoUtil.configurarUsuarioEDataHora(
                lblUsuario,
                lblDataHora
        );

        configurarTabela();
        configurarFiltros();

        cmbPerfil.getItems().setAll("ADMIN", "VENDEDOR");
        cmbPerfil.getSelectionModel().select("VENDEDOR");

        configurarBotoesSelecao(null);
        definirAdministradorLogado(
                SessaoUsuario.getInstance().getUsuarioLogado()
        );
        txtNome.requestFocus();
    }

    /**
     * Define o administrador usado pela tela funcional e pelas operações.
     */
    public void definirAdministradorLogado(Usuario administrador) {
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

        if (!"ADMIN".equals(administrador.getPerfil())) {
            throw new IllegalStateException(
                    "A gestão de usuários é exclusiva para administradores."
            );
        }

        if (!"ATIVO".equals(administrador.getStatus())) {
            throw new IllegalStateException(
                    "A gestão de usuários exige um administrador ativo."
            );
        }

        if (administrador.isTrocaSenhaObrigatoria()) {
            throw new IllegalStateException(
                    "Conclua a troca obrigatória antes de gerenciar usuários."
            );
        }

        this.administradorLogado = administrador;
        carregarUsuarios();
    }

    private void configurarTabela() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idUsuario"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colLogin.setCellValueFactory(new PropertyValueFactory<>("login"));
        colPerfil.setCellValueFactory(new PropertyValueFactory<>("perfil"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colTrocaObrigatoria.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().isTrocaSenhaObrigatoria()
                                ? "Sim"
                                : "Não"
                )
        );

        tabelaUsuarios
                .getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, atual) ->
                        configurarBotoesSelecao(atual)
                );
    }

    private void configurarFiltros() {
        cmbFiltroPerfil.getItems().setAll(
                FILTRO_TODOS,
                "ADMIN",
                "VENDEDOR"
        );
        cmbFiltroStatus.getItems().setAll(
                FILTRO_TODOS,
                "ATIVO",
                "INATIVO"
        );
        cmbFiltroTrocaObrigatoria.getItems().setAll(
                FILTRO_TODOS,
                "Sim",
                "Não"
        );

        limparControlesFiltro();
        txtPesquisaUsuario.setOnAction(event -> aplicarFiltros());
    }

    @FXML
    private void atualizarLista() {
        carregarUsuarios();
    }

    private void carregarUsuarios() {
        try {
            if (administradorLogado == null) {
                throw new IllegalStateException(
                        "Administrador logado não foi informado."
                );
            }

            List<Usuario> usuariosAtualizados =
                    gestaoUsuarioService.listarUsuarios(
                            administradorLogado
                    );

            usuariosCarregados.setAll(
                    usuariosAtualizados == null
                            ? List.of()
                            : usuariosAtualizados
            );

            limparControlesFiltro();
            exibirUsuarios(usuariosCarregados);

        } catch (RuntimeException e) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Não foi possível listar usuários",
                    mensagemErro(e)
            );
        }
    }

    @FXML
    private void aplicarFiltros() {
        List<Usuario> usuariosFiltrados = filtrarUsuariosCarregados(
                usuariosCarregados,
                txtPesquisaUsuario.getText(),
                cmbFiltroPerfil.getValue(),
                cmbFiltroStatus.getValue(),
                cmbFiltroTrocaObrigatoria.getValue()
        );

        exibirUsuarios(usuariosFiltrados);
    }

    @FXML
    private void limparFiltros() {
        limparControlesFiltro();
        exibirUsuarios(usuariosCarregados);
    }

    private void limparControlesFiltro() {
        txtPesquisaUsuario.clear();
        cmbFiltroPerfil.getSelectionModel().select(FILTRO_TODOS);
        cmbFiltroStatus.getSelectionModel().select(FILTRO_TODOS);
        cmbFiltroTrocaObrigatoria.getSelectionModel().select(FILTRO_TODOS);
    }

    private void exibirUsuarios(List<Usuario> usuarios) {
        tabelaUsuarios.setItems(
                FXCollections.observableArrayList(usuarios)
        );
        tabelaUsuarios.getSelectionModel().clearSelection();
        lblTotalUsuarios.setText(
                "Total de usuários: " + usuarios.size()
        );
        configurarBotoesSelecao(null);
    }

    /**
     * Filtra somente a lista já mantida pela tela, sem acessar Service ou banco.
     *
     * O método é independente dos controles JavaFX para permitir validação não
     * visual da combinação dos critérios.
     */
    static List<Usuario> filtrarUsuariosCarregados(
            List<Usuario> usuarios,
            String pesquisa,
            String perfil,
            String status,
            String trocaObrigatoria
    ) {

        if (usuarios == null || usuarios.isEmpty()) {
            return List.of();
        }

        String termoPesquisa = normalizarTextoFiltro(pesquisa);

        return usuarios.stream()
                .filter(usuario -> usuario != null)
                .filter(usuario -> termoPesquisa.isEmpty()
                        || contemIgnorandoCaixa(
                                usuario.getNome(),
                                termoPesquisa
                        )
                        || contemIgnorandoCaixa(
                                usuario.getLogin(),
                                termoPesquisa
                        ))
                .filter(usuario -> correspondeOpcao(
                        usuario.getPerfil(),
                        perfil
                ))
                .filter(usuario -> correspondeOpcao(
                        usuario.getStatus(),
                        status
                ))
                .filter(usuario -> correspondeTrocaObrigatoria(
                        usuario.isTrocaSenhaObrigatoria(),
                        trocaObrigatoria
                ))
                .toList();
    }

    private static boolean contemIgnorandoCaixa(
            String texto,
            String termoNormalizado
    ) {
        return texto != null
                && texto.toLowerCase(Locale.ROOT)
                .contains(termoNormalizado);
    }

    private static boolean correspondeOpcao(
            String valorUsuario,
            String filtro
    ) {
        return filtroTodos(filtro)
                || valorUsuario != null
                && valorUsuario.equalsIgnoreCase(filtro.trim());
    }

    private static boolean correspondeTrocaObrigatoria(
            boolean trocaObrigatoria,
            String filtro
    ) {
        if (filtroTodos(filtro)) {
            return true;
        }

        if ("Sim".equalsIgnoreCase(filtro.trim())) {
            return trocaObrigatoria;
        }

        if ("Não".equalsIgnoreCase(filtro.trim())) {
            return !trocaObrigatoria;
        }

        return false;
    }

    private static boolean filtroTodos(String filtro) {
        return filtro == null
                || filtro.isBlank()
                || FILTRO_TODOS.equalsIgnoreCase(filtro.trim());
    }

    private static String normalizarTextoFiltro(String texto) {
        return texto == null
                ? ""
                : texto.trim().toLowerCase(Locale.ROOT);
    }

    @FXML
    private void cadastrarUsuario() {
        try {
            btnCadastrar.setDisable(true);

            gestaoUsuarioService.cadastrarUsuario(
                    administradorLogado,
                    txtNome.getText(),
                    txtLogin.getText(),
                    cmbPerfil.getValue(),
                    txtSenhaTemporaria.getText(),
                    txtConfirmacao.getText()
            );

            limparFormulario();
            carregarUsuarios();

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Usuário cadastrado",
                    "Usuário cadastrado com sucesso. A troca de senha será "
                            + "obrigatória no primeiro acesso."
            );

        } catch (RuntimeException e) {
            limparSenhasCadastro();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Não foi possível cadastrar",
                    mensagemErro(e)
            );

        } finally {
            btnCadastrar.setDisable(false);
        }
    }

    @FXML
    private void alterarStatusSelecionado() {
        Usuario usuarioAlvo = tabelaUsuarios
                .getSelectionModel()
                .getSelectedItem();

        if (usuarioAlvo == null) {
            mostrarSelecaoObrigatoria();
            return;
        }

        String novoStatus = "ATIVO".equals(usuarioAlvo.getStatus())
                ? "INATIVO"
                : "ATIVO";
        String acao = "ATIVO".equals(novoStatus)
                ? "ativar"
                : "inativar";

        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacao.setTitle("Confirmar alteração de status");
        confirmacao.setHeaderText(null);
        confirmacao.setContentText(
                "Deseja " + acao + " o usuário "
                        + usuarioAlvo.getLogin() + "?"
        );

        Optional<ButtonType> resposta = confirmacao.showAndWait();

        if (resposta.isEmpty() || resposta.get() != ButtonType.OK) {
            return;
        }

        try {
            gestaoUsuarioService.alterarStatusUsuario(
                    administradorLogado,
                    usuarioAlvo,
                    novoStatus
            );

            carregarUsuarios();

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Status atualizado",
                    "Status do usuário atualizado com sucesso."
            );

        } catch (RuntimeException e) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Não foi possível alterar o status",
                    mensagemErro(e)
            );
        }
    }

    @FXML
    private void abrirRedefinicaoSenha() {
        Usuario usuarioAlvo = tabelaUsuarios
                .getSelectionModel()
                .getSelectedItem();

        if (usuarioAlvo == null) {
            mostrarSelecaoObrigatoria();
            return;
        }

        if (mesmoUsuario(usuarioAlvo)) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Operação não permitida",
                    "Use a opção Alterar Minha Senha para modificar a própria senha."
            );
            return;
        }

        try {
            URL fxmlLocation = getClass().getResource(
                    "/br/com/luis/view/RedefinirSenhaUsuario.fxml"
            );

            if (fxmlLocation == null) {
                throw new IllegalStateException(
                        "RedefinirSenhaUsuario.fxml não encontrado."
                );
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            RedefinirSenhaUsuarioController controller =
                    loader.getController();
            controller.definirContexto(
                    administradorLogado,
                    usuarioAlvo
            );

            Stage modal = new Stage();
            modal.initOwner(obterStageAtual());
            modal.initModality(Modality.WINDOW_MODAL);
            modal.setTitle("ERP Comercial - Redefinir Senha");
            modal.setScene(new Scene(root));
            modal.setResizable(false);
            modal.showAndWait();

            carregarUsuarios();

        } catch (IOException | RuntimeException e) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro",
                    "Não foi possível abrir a redefinição de senha. "
                            + mensagemErro(e)
            );
        }
    }

    /**
     * Abre o fluxo voluntário já existente para o próprio administrador logado.
     */
    @FXML
    private void abrirAlteracaoMinhaSenha() {
        if (janelaAlterarSenha != null
                && janelaAlterarSenha.isShowing()) {
            janelaAlterarSenha.requestFocus();
            janelaAlterarSenha.toFront();
            return;
        }

        try {
            if (administradorLogado == null) {
                throw new IllegalStateException(
                        "Administrador logado não foi informado."
                );
            }

            URL fxmlLocation = getClass().getResource(
                    "/br/com/luis/view/AlterarSenha.fxml"
            );

            if (fxmlLocation == null) {
                throw new IllegalStateException(
                        "AlterarSenha.fxml não encontrado."
                );
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            AlterarSenhaController controller = loader.getController();
            controller.definirUsuarioLogado(administradorLogado);

            Stage stageAlterarSenha = new Stage();
            stageAlterarSenha.initOwner(obterStageAtual());
            stageAlterarSenha.initModality(Modality.WINDOW_MODAL);
            stageAlterarSenha.setTitle("ERP Comercial - Alterar Senha");
            stageAlterarSenha.setScene(new Scene(root));
            stageAlterarSenha.setResizable(false);

            janelaAlterarSenha = stageAlterarSenha;

            try {
                stageAlterarSenha.showAndWait();
            } finally {
                if (janelaAlterarSenha == stageAlterarSenha) {
                    janelaAlterarSenha = null;
                }
            }

        } catch (IOException | RuntimeException e) {
            System.err.println(
                    "[ERRO] Falha ao abrir a alteração voluntária de senha."
            );
            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro",
                    "Não foi possível abrir a tela Alterar Senha."
            );
        }
    }

    @FXML
    private void limparFormulario() {
        txtNome.clear();
        txtLogin.clear();
        cmbPerfil.getSelectionModel().select("VENDEDOR");
        limparSenhasCadastro();
        txtNome.requestFocus();
    }

    private void limparSenhasCadastro() {
        txtSenhaTemporaria.clear();
        txtConfirmacao.clear();
    }

    private void configurarBotoesSelecao(Usuario selecionado) {
        boolean semSelecao = selecionado == null;
        boolean proprioAdministrador = !semSelecao
                && mesmoUsuario(selecionado);

        btnAlterarStatus.setDisable(
                semSelecao || proprioAdministrador
        );
        btnRedefinirSenha.setDisable(
                semSelecao || proprioAdministrador
        );

        if (semSelecao) {
            btnAlterarStatus.setText("Ativar / Inativar");
        } else if ("ATIVO".equals(selecionado.getStatus())) {
            btnAlterarStatus.setText("Inativar");
        } else {
            btnAlterarStatus.setText("Ativar");
        }
    }

    private boolean mesmoUsuario(Usuario usuario) {
        return administradorLogado != null
                && usuario != null
                && administradorLogado.getIdUsuario().equals(
                        usuario.getIdUsuario()
                );
    }

    private void mostrarSelecaoObrigatoria() {
        mostrarAlerta(
                Alert.AlertType.WARNING,
                "Seleção obrigatória",
                "Selecione um usuário na tabela."
        );
    }

    /**
     * Retorna à Tela Principal reutilizando o mesmo Stage da tela funcional.
     */
    @FXML
    private void onVoltar() {
        try {
            NavegacaoUtil.abrirTela(
                    btnVoltar,
                    "/br/com/luis/view/TelaPrincipal.fxml",
                    "Tela Principal"
            );

        } catch (IOException | RuntimeException e) {
            System.err.println(
                    "[ERRO] Falha ao voltar para a Tela Principal."
            );
            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro",
                    "Não foi possível retornar para a Tela Principal."
            );
        }
    }

    private Stage obterStageAtual() {
        Scene sceneAtual = btnVoltar.getScene();

        if (sceneAtual == null) {
            throw new IllegalStateException(
                    "Scene da gestão de usuários não encontrada."
            );
        }

        Window windowAtual = sceneAtual.getWindow();

        if (!(windowAtual instanceof Stage stage)) {
            throw new IllegalStateException(
                    "Janela da gestão de usuários não encontrada."
            );
        }

        return stage;
    }

    private String mensagemErro(Throwable e) {
        return e.getMessage() == null || e.getMessage().isBlank()
                ? "Ocorreu uma falha inesperada."
                : e.getMessage();
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
