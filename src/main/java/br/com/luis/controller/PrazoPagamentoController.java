package br.com.luis.controller;

import br.com.luis.model.PrazoPagamento;
import br.com.luis.service.PrazoPagamentoService;
import br.com.luis.util.CabecalhoUtil;
import br.com.luis.util.NavegacaoUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.util.List;

/**
 * Controller da tela administrativa de prazos de pagamento.
 *
 * Coordena os componentes visuais e delega cadastro, atualização, inativação,
 * reativação e listagem ao PrazoPagamentoService, sem acessar o DAO diretamente.
 */
public class PrazoPagamentoController {

    private final PrazoPagamentoService prazoPagamentoService = new PrazoPagamentoService();
    private final ObservableList<PrazoPagamento> prazos = FXCollections.observableArrayList();

    // --- CABEÇALHO ---
    @FXML private Button btnVoltar;
    @FXML private Label lblUsuario;
    @FXML private Label lblDataHora;

    // --- FORMULÁRIO ---
    @FXML private TextField txtDescricao;
    @FXML private TextField txtQuantidadeDias;
    @FXML private Button btnSalvar;

    // --- LISTAGEM E AÇÕES DE STATUS ---
    @FXML private TableView<PrazoPagamento> tabelaPrazos;
    @FXML private TableColumn<PrazoPagamento, Integer> colId;
    @FXML private TableColumn<PrazoPagamento, String> colDescricao;
    @FXML private TableColumn<PrazoPagamento, Integer> colDias;
    @FXML private TableColumn<PrazoPagamento, String> colStatus;
    @FXML private Button btnAtivar;
    @FXML private Button btnDesativar;
    @FXML private Label lblTotalPrazos;

    private PrazoPagamento prazoSelecionado;

    @FXML
    public void initialize() {
        configurarCabecalho();
        configurarTabela();
        prepararNovoCadastro();
        carregarTabelaInicial();
    }

    private void configurarCabecalho() {
        CabecalhoUtil.configurarUsuarioEDataHora(
                lblUsuario,
                lblDataHora
        );
    }

    private void configurarTabela() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idPrazo"));
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colDias.setCellValueFactory(new PropertyValueFactory<>("quantidadeDias"));
        colStatus.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().isAtivo() ? "Ativo" : "Inativo"
                )
        );

        tabelaPrazos.setItems(prazos);
        tabelaPrazos.setPlaceholder(new Label("Nenhum prazo de pagamento cadastrado."));

        tabelaPrazos.getSelectionModel().selectedItemProperty().addListener(
                (observable, anterior, atual) -> {
                    if (atual == null) {
                        prazoSelecionado = null;
                        limparCamposFormulario();
                        btnSalvar.setText("Salvar");
                        atualizarBotoesStatus();
                        return;
                    }

                    preencherFormulario(atual);
                }
        );
    }

    /**
     * Carrega a listagem inicial de forma síncrona por ser uma consulta local e pequena.
     */
    private void carregarTabelaInicial() {
        try {
            carregarTabela();

        } catch (RuntimeException e) {
            System.err.println("[ERRO] Falha ao carregar prazos de pagamento.");
            e.printStackTrace();

            tabelaPrazos.setPlaceholder(new Label("Não foi possível carregar os prazos de pagamento."));
            mostrarErro("Não foi possível carregar os prazos de pagamento.");
        }
    }

    /**
     * Consulta todos os prazos, ativos e inativos, por meio do Service.
     */
    private void carregarTabela() {
        List<PrazoPagamento> prazosCarregados = prazoPagamentoService.listarTodos();

        prazos.setAll(prazosCarregados);
        tabelaPrazos.setPlaceholder(new Label("Nenhum prazo de pagamento cadastrado."));
        atualizarContador();
    }

    private void atualizarContador() {
        int total = prazos.size();
        lblTotalPrazos.setText("Total: " + total + " prazo" + (total == 1 ? "" : "s"));
    }

    @FXML
    private void acaoSalvar() {
        try {
            boolean cadastrando = prazoSelecionado == null;
            PrazoPagamento prazo = montarPrazoFormulario(cadastrando);

            if (cadastrando) {
                prazoPagamentoService.cadastrar(prazo);
                concluirOperacao("Prazo de pagamento cadastrado com sucesso.");
            } else {
                prazoPagamentoService.atualizar(prazo);
                concluirOperacao("Prazo de pagamento atualizado com sucesso.");
            }

        } catch (IllegalArgumentException e) {
            mostrarAviso(e.getMessage());

        } catch (RuntimeException e) {
            tratarFalhaOperacao(
                    "Falha ao salvar prazo de pagamento.",
                    "Não foi possível salvar o prazo de pagamento.",
                    e
            );
        }
    }

    /**
     * Prepara uma nova instância para que o objeto exibido na tabela não seja
     * alterado antes de o Service concluir a operação.
     */
    private PrazoPagamento montarPrazoFormulario(boolean cadastrando) {
        String descricao = obterDescricaoFormulario();
        Integer quantidadeDias = converterQuantidadeDias(txtQuantidadeDias.getText());

        if (cadastrando) {
            PrazoPagamento novoPrazo = new PrazoPagamento();
            novoPrazo.setDescricao(descricao);
            novoPrazo.setQuantidadeDias(quantidadeDias);

            return novoPrazo;
        }

        return new PrazoPagamento(
                prazoSelecionado.getIdPrazo(),
                descricao,
                quantidadeDias,
                prazoSelecionado.isAtivo()
        );
    }

    private String obterDescricaoFormulario() {
        String descricao = txtDescricao.getText();

        if (descricao == null || descricao.isBlank()) {
            txtDescricao.requestFocus();
            throw new IllegalArgumentException("Descrição do prazo é obrigatória.");
        }

        return descricao.trim();
    }

    private Integer converterQuantidadeDias(String texto) {
        if (texto == null || texto.isBlank()) {
            txtQuantidadeDias.requestFocus();
            throw new IllegalArgumentException("Quantidade de dias é obrigatória.");
        }

        try {
            int quantidadeDias = Integer.parseInt(texto.trim());

            if (quantidadeDias <= 0) {
                txtQuantidadeDias.requestFocus();
                throw new IllegalArgumentException("Quantidade de dias deve ser maior que zero.");
            }

            return quantidadeDias;

        } catch (NumberFormatException e) {
            txtQuantidadeDias.requestFocus();
            throw new IllegalArgumentException("Quantidade de dias deve ser um número inteiro.");
        }
    }

    @FXML
    private void acaoNovo() {
        prepararNovoCadastro();
    }

    @FXML
    private void acaoCancelar() {
        prepararNovoCadastro();
    }

    @FXML
    private void acaoAtivar() {
        PrazoPagamento selecionado = obterPrazoSelecionado();

        if (selecionado == null) {
            return;
        }

        if (selecionado.isAtivo()) {
            mostrarAviso("O prazo selecionado já está ativo.");
            return;
        }

        try {
            PrazoPagamento prazoAtivado = new PrazoPagamento(
                    selecionado.getIdPrazo(),
                    selecionado.getDescricao(),
                    selecionado.getQuantidadeDias(),
                    true
            );

            prazoPagamentoService.atualizar(prazoAtivado);
            concluirOperacao("Prazo de pagamento ativado com sucesso.");

        } catch (IllegalArgumentException e) {
            mostrarAviso(e.getMessage());

        } catch (RuntimeException e) {
            tratarFalhaOperacao(
                    "Falha ao ativar prazo de pagamento.",
                    "Não foi possível ativar o prazo de pagamento.",
                    e
            );
        }
    }

    @FXML
    private void acaoDesativar() {
        PrazoPagamento selecionado = obterPrazoSelecionado();

        if (selecionado == null) {
            return;
        }

        if (!selecionado.isAtivo()) {
            mostrarAviso("O prazo selecionado já está inativo.");
            return;
        }

        if (!confirmarDesativacao(selecionado)) {
            return;
        }

        try {
            prazoPagamentoService.inativar(selecionado.getIdPrazo());
            concluirOperacao("Prazo de pagamento desativado com sucesso.");

        } catch (IllegalArgumentException e) {
            mostrarAviso(e.getMessage());

        } catch (RuntimeException e) {
            tratarFalhaOperacao(
                    "Falha ao desativar prazo de pagamento.",
                    "Não foi possível desativar o prazo de pagamento.",
                    e
            );
        }
    }

    private PrazoPagamento obterPrazoSelecionado() {
        PrazoPagamento selecionado = tabelaPrazos.getSelectionModel().getSelectedItem();

        if (selecionado == null) {
            mostrarAviso("Selecione um prazo de pagamento na tabela.");
        }

        return selecionado;
    }

    private boolean confirmarDesativacao(PrazoPagamento prazo) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Confirmar desativação");
        alerta.setHeaderText(null);
        alerta.setContentText(
                "Deseja realmente desativar o prazo \"" + prazo.getDescricao() + "\"?"
        );

        ButtonType botaoDesativar = new ButtonType(
                "Desativar",
                ButtonBar.ButtonData.OK_DONE
        );
        ButtonType botaoCancelar = new ButtonType(
                "Cancelar",
                ButtonBar.ButtonData.CANCEL_CLOSE
        );

        alerta.getButtonTypes().setAll(botaoDesativar, botaoCancelar);

        return alerta.showAndWait().orElse(botaoCancelar) == botaoDesativar;
    }

    /**
     * Atualiza a listagem e normaliza o formulário somente após sucesso no Service.
     */
    private void concluirOperacao(String mensagemSucesso) {
        try {
            carregarTabela();
            prepararNovoCadastro();
            mostrarInformacao(mensagemSucesso);

        } catch (RuntimeException e) {
            System.err.println("[ERRO] Operação concluída, mas a listagem não foi atualizada.");
            e.printStackTrace();

            prepararNovoCadastro();
            mostrarErro("A operação foi concluída, mas não foi possível atualizar a listagem.");
        }
    }

    private void preencherFormulario(PrazoPagamento prazo) {
        prazoSelecionado = prazo;

        txtDescricao.setText(prazo.getDescricao());
        txtQuantidadeDias.setText(
                prazo.getQuantidadeDias() == null
                        ? ""
                        : prazo.getQuantidadeDias().toString()
        );

        btnSalvar.setText("Atualizar");
        atualizarBotoesStatus();
    }

    private void prepararNovoCadastro() {
        prazoSelecionado = null;
        tabelaPrazos.getSelectionModel().clearSelection();

        limparCamposFormulario();
        btnSalvar.setText("Salvar");
        atualizarBotoesStatus();

        txtDescricao.requestFocus();
    }

    private void limparCamposFormulario() {
        txtDescricao.clear();
        txtQuantidadeDias.clear();
    }

    private void atualizarBotoesStatus() {
        boolean semSelecao = prazoSelecionado == null;

        btnAtivar.setDisable(semSelecao || prazoSelecionado.isAtivo());
        btnDesativar.setDisable(semSelecao || !prazoSelecionado.isAtivo());
    }

    @FXML
    private void onVoltar() {
        if (!confirmarSaidaComAlteracoesNaoSalvas()) {
            return;
        }

        try {
            NavegacaoUtil.abrirTela(
                    btnVoltar,
                    "/br/com/luis/view/TelaPrincipal.fxml",
                    "Tela Principal"
            );

        } catch (IOException | RuntimeException e) {
            System.err.println("[ERRO] Falha ao voltar para a Tela Principal.");
            e.printStackTrace();

            mostrarErro("Não foi possível retornar para a Tela Principal.");
        }
    }

    private boolean confirmarSaidaComAlteracoesNaoSalvas() {
        if (!existemAlteracoesPrazoNaoSalvas()) {
            return true;
        }

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Alterações não salvas");
        alerta.setHeaderText(null);
        alerta.setContentText(
                "Existem dados de prazo ainda não salvos.\n"
                        + "Ao voltar, essas alterações serão descartadas.\n"
                        + "Deseja realmente sair desta tela?"
        );

        ButtonType botaoSair = new ButtonType(
                "Sair sem salvar",
                ButtonBar.ButtonData.OK_DONE
        );
        ButtonType botaoContinuar = new ButtonType(
                "Continuar editando",
                ButtonBar.ButtonData.CANCEL_CLOSE
        );

        alerta.getButtonTypes().setAll(
                botaoSair,
                botaoContinuar
        );

        return alerta.showAndWait().orElse(botaoContinuar) == botaoSair;
    }

    private boolean existemAlteracoesPrazoNaoSalvas() {
        String descricaoAtual = normalizarTextoComparacao(
                txtDescricao.getText()
        );
        String quantidadeDiasAtual = normalizarTextoComparacao(
                txtQuantidadeDias.getText()
        );

        if (prazoSelecionado == null) {
            return !descricaoAtual.isEmpty()
                    || !quantidadeDiasAtual.isEmpty();
        }

        if (!descricaoAtual.equals(
                normalizarTextoComparacao(
                        prazoSelecionado.getDescricao()
                )
        )) {
            return true;
        }

        try {
            Integer quantidadeDiasOriginal =
                    prazoSelecionado.getQuantidadeDias();
            int quantidadeDiasInformada =
                    Integer.parseInt(quantidadeDiasAtual);

            return quantidadeDiasAtual.isEmpty()
                    || quantidadeDiasOriginal == null
                    || quantidadeDiasInformada
                    != quantidadeDiasOriginal;

        } catch (NumberFormatException e) {
            return true;
        }
    }

    private String normalizarTextoComparacao(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private void tratarFalhaOperacao(
            String mensagemConsole,
            String mensagemPadrao,
            RuntimeException e
    ) {
        System.err.println("[ERRO] " + mensagemConsole);
        e.printStackTrace();

        String mensagem = e.getMessage();

        mostrarErro(
                mensagem == null || mensagem.isBlank()
                        ? mensagemPadrao
                        : mensagem
        );
    }

    private void mostrarInformacao(String mensagem) {
        mostrarAlerta(Alert.AlertType.INFORMATION, "Informação", mensagem);
    }

    private void mostrarAviso(String mensagem) {
        mostrarAlerta(Alert.AlertType.WARNING, "Aviso", mensagem);
    }

    private void mostrarErro(String mensagem) {
        mostrarAlerta(Alert.AlertType.ERROR, "Erro", mensagem);
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensagem);
        alerta.showAndWait();
    }
}
