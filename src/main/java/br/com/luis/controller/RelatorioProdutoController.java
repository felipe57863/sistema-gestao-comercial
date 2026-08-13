package br.com.luis.controller;

import br.com.luis.model.Promocao.TipoDesconto;
import br.com.luis.model.Usuario;
import br.com.luis.service.RelatorioProdutoService;
import br.com.luis.util.CabecalhoUtil;
import br.com.luis.util.NavegacaoUtil;
import br.com.luis.util.SessaoUsuario;
import br.com.luis.viewmodel.FiltroRelatorioEstoqueProduto;
import br.com.luis.viewmodel.FiltroRelatorioPromocaoProduto;
import br.com.luis.viewmodel.ProdutoEstoqueRelatorioView;
import br.com.luis.viewmodel.ProdutoPromocaoRelatorioView;
import br.com.luis.viewmodel.ResultadoRelatorioEstoqueProduto;
import br.com.luis.viewmodel.ResultadoRelatorioPromocaoProduto;
import br.com.luis.viewmodel.SituacaoEstoqueProduto;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Controller das visões de estoque e de produtos em promoção.
 *
 * A classe traduz os filtros visuais para os contratos do relatório e executa
 * uma consulta por vez fora da JavaFX Application Thread. As regras de
 * autorização, classificação, ordenação e totalização permanecem no Service.
 */
public class RelatorioProdutoController {

    private static final String MENSAGEM_ACESSO_NEGADO =
            "Usuário não autorizado a consultar relatórios de produtos.";

    private static final String MENSAGEM_ESTOQUE_VAZIO =
            "Nenhum produto encontrado para os filtros informados.";

    private static final String MENSAGEM_PROMOCOES_VAZIAS =
            "Nenhum produto em promoção foi encontrado para os filtros informados.";

    @FXML private Label lblUsuario;
    @FXML private Label lblDataHora;
    @FXML private Button btnVoltar;
    @FXML private ProgressIndicator progressoRelatorio;
    @FXML private Label lblEstadoConsulta;
    @FXML private TabPane tabPaneRelatorios;
    @FXML private Tab tabEstoque;
    @FXML private Tab tabPromocoes;

    @FXML private TextField txtDescricaoEstoque;
    @FXML private ComboBox<OpcaoFiltro<Boolean>> cbStatusEstoque;
    @FXML private ComboBox<OpcaoFiltro<SituacaoEstoqueProduto>> cbSituacaoEstoque;
    @FXML private Button btnFiltrarEstoque;
    @FXML private TableView<ProdutoEstoqueRelatorioView> tabelaEstoque;
    @FXML private TableColumn<ProdutoEstoqueRelatorioView, Integer> colEstoqueProdutoId;
    @FXML private TableColumn<ProdutoEstoqueRelatorioView, String> colEstoqueDescricao;
    @FXML private TableColumn<ProdutoEstoqueRelatorioView, Integer> colEstoqueAtual;
    @FXML private TableColumn<ProdutoEstoqueRelatorioView, Integer> colEstoqueMinimo;
    @FXML private TableColumn<ProdutoEstoqueRelatorioView, Integer> colEstoqueDiferenca;
    @FXML private TableColumn<ProdutoEstoqueRelatorioView, String> colEstoqueSituacao;
    @FXML private TableColumn<ProdutoEstoqueRelatorioView, String> colEstoqueStatus;
    @FXML private Label lblQuantidadeProdutos;
    @FXML private Label lblQuantidadeAbaixo;
    @FXML private Label lblQuantidadeNoMinimo;
    @FXML private Label lblQuantidadeAcima;

    @FXML private TextField txtDescricaoPromocao;
    @FXML private ComboBox<OpcaoFiltro<Boolean>> cbStatusPromocao;
    @FXML private ComboBox<OpcaoFiltro<TipoDesconto>> cbTipoDesconto;
    @FXML private Button btnFiltrarPromocoes;
    @FXML private TableView<ProdutoPromocaoRelatorioView> tabelaPromocoes;
    @FXML private TableColumn<ProdutoPromocaoRelatorioView, Integer> colPromocaoProdutoId;
    @FXML private TableColumn<ProdutoPromocaoRelatorioView, String> colPromocaoDescricao;
    @FXML private TableColumn<ProdutoPromocaoRelatorioView, BigDecimal> colPromocaoPrecoNormal;
    @FXML private TableColumn<ProdutoPromocaoRelatorioView, String> colPromocaoTipoDesconto;
    @FXML private TableColumn<ProdutoPromocaoRelatorioView, String> colPromocaoValorDesconto;
    @FXML private TableColumn<ProdutoPromocaoRelatorioView, String> colPromocaoStatusProduto;
    @FXML private Label lblQuantidadePromocoes;

    private final RelatorioProdutoService relatorioProdutoService;
    private final ObservableList<ProdutoEstoqueRelatorioView> produtosEstoqueExibidos;
    private final ObservableList<ProdutoPromocaoRelatorioView> promocoesExibidas;
    private final NumberFormat formatadorMoeda;

    private Task<?> tarefaConsultaAtual;
    private boolean promocoesConsultadas;
    private boolean telaAtiva;

    /**
     * Inicializa as dependências sem acesso antecipado à interface ou ao banco.
     */
    public RelatorioProdutoController() {
        this.relatorioProdutoService = new RelatorioProdutoService();
        this.produtosEstoqueExibidos = FXCollections.observableArrayList();
        this.promocoesExibidas = FXCollections.observableArrayList();
        this.formatadorMoeda = NumberFormat.getCurrencyInstance(
                new Locale("pt", "BR")
        );
        this.tarefaConsultaAtual = null;
        this.promocoesConsultadas = false;
        this.telaAtiva = true;
    }

    /**
     * Configura a tela e agenda a consulta inicial da aba Estoque.
     */
    @FXML
    public void initialize() {
        CabecalhoUtil.configurarUsuarioEDataHora(
                lblUsuario,
                lblDataHora
        );

        configurarCombos();
        configurarTabelaEstoque();
        configurarTabelaPromocoes();
        configurarEstadoVisualInicial();
        configurarCarregamentoPromocoes();

        Platform.runLater(this::iniciarConsultaInicial);
    }

    /**
     * Preenche os filtros e aplica os padrões visuais aprovados.
     */
    private void configurarCombos() {
        cbStatusEstoque.getItems().setAll(
                new OpcaoFiltro<>("Ativos", Boolean.TRUE),
                new OpcaoFiltro<>("Inativos", Boolean.FALSE),
                new OpcaoFiltro<>("Todos", null)
        );

        cbSituacaoEstoque.getItems().setAll(
                new OpcaoFiltro<>("Todas", null),
                new OpcaoFiltro<>(
                        "Abaixo do mínimo",
                        SituacaoEstoqueProduto.ABAIXO_DO_MINIMO
                ),
                new OpcaoFiltro<>(
                        "No mínimo",
                        SituacaoEstoqueProduto.NO_MINIMO
                ),
                new OpcaoFiltro<>(
                        "Acima do mínimo",
                        SituacaoEstoqueProduto.ACIMA_DO_MINIMO
                )
        );

        cbStatusPromocao.getItems().setAll(
                new OpcaoFiltro<>("Ativos", Boolean.TRUE),
                new OpcaoFiltro<>("Inativos", Boolean.FALSE),
                new OpcaoFiltro<>("Todos", null)
        );

        cbTipoDesconto.getItems().setAll(
                new OpcaoFiltro<>("Todos", null),
                new OpcaoFiltro<>("Percentual", TipoDesconto.PERCENTUAL),
                new OpcaoFiltro<>("Valor fixo", TipoDesconto.VALOR_FIXO)
        );

        cbStatusEstoque.getSelectionModel().selectFirst();
        cbSituacaoEstoque.getSelectionModel().selectFirst();
        cbStatusPromocao.getSelectionModel().selectFirst();
        cbTipoDesconto.getSelectionModel().selectFirst();
        tabPaneRelatorios.getSelectionModel().select(tabEstoque);
    }

    /**
     * Configura as colunas da visão de estoque sem recalcular dados do Service.
     */
    private void configurarTabelaEstoque() {
        colEstoqueProdutoId.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getProdutoId()
                )
        );

        colEstoqueDescricao.setCellValueFactory(
                celula -> new SimpleStringProperty(
                        celula.getValue().getDescricao()
                )
        );

        colEstoqueAtual.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getEstoqueAtual()
                )
        );

        colEstoqueMinimo.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getEstoqueMinimo()
                )
        );

        colEstoqueDiferenca.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getDiferenca()
                )
        );

        colEstoqueSituacao.setCellValueFactory(
                celula -> new SimpleStringProperty(
                        celula.getValue().getSituacao().getDescricao()
                )
        );

        colEstoqueStatus.setCellValueFactory(
                celula -> new SimpleStringProperty(
                        formatarStatus(celula.getValue().isAtivo())
                )
        );

        tabelaEstoque.setItems(produtosEstoqueExibidos);
        tabelaEstoque.setPlaceholder(
                criarPlaceholder("Relatório de estoque ainda não carregado.")
        );
    }

    /**
     * Configura as colunas da visão de promoções e apenas formata seus valores.
     */
    private void configurarTabelaPromocoes() {
        colPromocaoProdutoId.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getProdutoId()
                )
        );

        colPromocaoDescricao.setCellValueFactory(
                celula -> new SimpleStringProperty(
                        celula.getValue().getDescricao()
                )
        );

        colPromocaoPrecoNormal.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getPrecoNormal()
                )
        );

        colPromocaoPrecoNormal.setCellFactory(
                coluna -> new TableCell<>() {
                    @Override
                    protected void updateItem(
                            BigDecimal valor,
                            boolean empty
                    ) {
                        super.updateItem(valor, empty);
                        setText(empty ? null : formatarMoeda(valor));
                        setStyle("-fx-alignment: CENTER-RIGHT;");
                    }
                }
        );

        colPromocaoTipoDesconto.setCellValueFactory(
                celula -> new SimpleStringProperty(
                        formatarTipoDesconto(
                                celula.getValue().getTipoDesconto()
                        )
                )
        );

        colPromocaoValorDesconto.setCellValueFactory(
                celula -> new SimpleStringProperty(
                        formatarDesconto(
                                celula.getValue().getTipoDesconto(),
                                celula.getValue().getValorDesconto()
                        )
                )
        );

        colPromocaoStatusProduto.setCellValueFactory(
                celula -> new SimpleStringProperty(
                        formatarStatus(celula.getValue().isProdutoAtivo())
                )
        );

        tabelaPromocoes.setItems(promocoesExibidas);
        tabelaPromocoes.setPlaceholder(
                criarPlaceholder("Relatório de promoções ainda não carregado.")
        );
    }

    /**
     * Define os totalizadores e o indicador antes da primeira consulta.
     */
    private void configurarEstadoVisualInicial() {
        lblQuantidadeProdutos.setText("0");
        lblQuantidadeAbaixo.setText("0");
        lblQuantidadeNoMinimo.setText("0");
        lblQuantidadeAcima.setText("0");
        lblQuantidadePromocoes.setText("0");
        lblEstadoConsulta.setText("Relatório pronto para consulta.");

        progressoRelatorio.setVisible(false);
        progressoRelatorio.setManaged(false);
        btnFiltrarEstoque.setDisable(true);
        btnFiltrarPromocoes.setDisable(true);
    }

    /**
     * Consulta promoções somente na primeira seleção de sua aba.
     */
    private void configurarCarregamentoPromocoes() {
        tabPromocoes.selectedProperty().addListener(
                (observable, selecionadaAntes, selecionadaAgora) -> {
                    if (selecionadaAgora
                            && !promocoesConsultadas
                            && telaAtiva
                            && tarefaConsultaAtual == null) {

                        consultarPromocoesPelosFiltrosAtuais();
                    }
                }
        );
    }

    /**
     * Valida a sessão e inicia a consulta padrão de estoque.
     */
    private void iniciarConsultaInicial() {
        if (!telaAtiva) {
            return;
        }

        consultarEstoquePelosFiltrosAtuais();
    }

    /**
     * Obtém o ID do administrador da sessão para a proteção visual.
     */
    private Integer obterUsuarioIdAutorizadoVisualmente() {
        Usuario usuarioLogado =
                SessaoUsuario.getInstance().getUsuarioLogado();

        if (usuarioLogado == null) {
            throw new SecurityException(MENSAGEM_ACESSO_NEGADO);
        }

        Integer usuarioId = usuarioLogado.getIdUsuario();

        if (usuarioId == null
                || usuarioId <= 0
                || !"ADMIN".equals(usuarioLogado.getPerfil())) {

            throw new SecurityException(MENSAGEM_ACESSO_NEGADO);
        }

        return usuarioId;
    }

    /**
     * Fotografa os filtros visuais da aba Estoque.
     */
    private FiltroRelatorioEstoqueProduto montarFiltroEstoque() {
        return new FiltroRelatorioEstoqueProduto(
                txtDescricaoEstoque.getText(),
                obterValorSelecionado(cbStatusEstoque),
                obterValorSelecionado(cbSituacaoEstoque)
        );
    }

    /**
     * Fotografa os filtros visuais da aba Em promoção.
     */
    private FiltroRelatorioPromocaoProduto montarFiltroPromocao() {
        return new FiltroRelatorioPromocaoProduto(
                txtDescricaoPromocao.getText(),
                obterValorSelecionado(cbStatusPromocao),
                obterValorSelecionado(cbTipoDesconto)
        );
    }

    /**
     * Extrai o valor opcional associado a uma opção visual.
     */
    private <T> T obterValorSelecionado(
            ComboBox<OpcaoFiltro<T>> comboBox
    ) {
        OpcaoFiltro<T> opcao = comboBox.getValue();
        return opcao == null ? null : opcao.getValor();
    }

    /**
     * Valida a sessão e inicia a consulta de estoque com uma fotografia dos filtros.
     */
    private void consultarEstoquePelosFiltrosAtuais() {
        if (!telaAtiva || tarefaConsultaAtual != null) {
            return;
        }

        try {
            Integer usuarioId = obterUsuarioIdAutorizadoVisualmente();
            FiltroRelatorioEstoqueProduto filtro = montarFiltroEstoque();
            iniciarConsultaEstoque(filtro, usuarioId);

        } catch (SecurityException e) {
            tratarAcessoNegado();

        } catch (IllegalArgumentException e) {
            tratarFiltroInvalido(e);
        }
    }

    /**
     * Valida a sessão e inicia a consulta de promoções com filtros fotografados.
     */
    private void consultarPromocoesPelosFiltrosAtuais() {
        if (!telaAtiva || tarefaConsultaAtual != null) {
            return;
        }

        try {
            Integer usuarioId = obterUsuarioIdAutorizadoVisualmente();
            FiltroRelatorioPromocaoProduto filtro = montarFiltroPromocao();
            iniciarConsultaPromocoes(filtro, usuarioId);

        } catch (SecurityException e) {
            tratarAcessoNegado();

        } catch (IllegalArgumentException e) {
            tratarFiltroInvalido(e);
        }
    }

    /**
     * Executa a consulta de estoque fora da JavaFX Application Thread.
     */
    private void iniciarConsultaEstoque(
            FiltroRelatorioEstoqueProduto filtro,
            Integer usuarioId
    ) {
        Task<ResultadoRelatorioEstoqueProduto> tarefa = new Task<>() {
            @Override
            protected ResultadoRelatorioEstoqueProduto call() {
                return relatorioProdutoService.consultarEstoque(
                        filtro,
                        usuarioId
                );
            }
        };

        tarefaConsultaAtual = tarefa;
        configurarEstadoConsulta(true, "Consultando estoque de produtos...");

        tarefa.setOnSucceeded(event -> {
            if (!tarefaEhAtual(tarefa)) {
                return;
            }

            try {
                aplicarResultadoEstoque(tarefa.getValue());
                finalizarConsulta(tarefa);

            } catch (RuntimeException e) {
                tratarFalhaConsulta(tarefa, e);
            }
        });

        configurarHandlersComuns(tarefa);
        iniciarThread(tarefa, "relatorio-produtos-estoque");
    }

    /**
     * Executa a consulta de promoções fora da JavaFX Application Thread.
     */
    private void iniciarConsultaPromocoes(
            FiltroRelatorioPromocaoProduto filtro,
            Integer usuarioId
    ) {
        Task<ResultadoRelatorioPromocaoProduto> tarefa = new Task<>() {
            @Override
            protected ResultadoRelatorioPromocaoProduto call() {
                return relatorioProdutoService.consultarPromocoes(
                        filtro,
                        usuarioId
                );
            }
        };

        tarefaConsultaAtual = tarefa;
        configurarEstadoConsulta(true, "Consultando produtos em promoção...");

        tarefa.setOnSucceeded(event -> {
            if (!tarefaEhAtual(tarefa)) {
                return;
            }

            try {
                aplicarResultadoPromocoes(tarefa.getValue());
                promocoesConsultadas = true;
                finalizarConsulta(tarefa);

            } catch (RuntimeException e) {
                tratarFalhaConsulta(tarefa, e);
            }
        });

        configurarHandlersComuns(tarefa);
        iniciarThread(tarefa, "relatorio-produtos-promocoes");
    }

    /**
     * Configura falha e cancelamento comuns às duas consultas.
     */
    private void configurarHandlersComuns(Task<?> tarefa) {
        tarefa.setOnFailed(event -> {
            if (!tarefaEhAtual(tarefa)) {
                return;
            }

            Throwable causa = tarefa.getException();

            if (localizarSecurityException(causa) != null) {
                tratarAcessoNegado();
                return;
            }

            tratarFalhaConsulta(tarefa, causa);
        });

        tarefa.setOnCancelled(event -> {
            if (tarefaConsultaAtual != tarefa) {
                return;
            }

            finalizarConsulta(tarefa);
            lblEstadoConsulta.setText("Consulta cancelada.");
        });
    }

    /**
     * Inicia uma Task em thread daemon dedicada.
     */
    private void iniciarThread(Task<?> tarefa, String nome) {
        Thread thread = new Thread(tarefa, nome);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Confirma que um handler ainda pertence à consulta ativa.
     */
    private boolean tarefaEhAtual(Task<?> tarefa) {
        return telaAtiva
                && tarefaConsultaAtual == tarefa
                && !tarefa.isCancelled();
    }

    /**
     * Finaliza e libera os controles da consulta atual.
     */
    private void finalizarConsulta(Task<?> tarefa) {
        if (tarefaConsultaAtual != tarefa) {
            return;
        }

        tarefaConsultaAtual = null;
        configurarEstadoConsulta(false, null);
    }

    /**
     * Cancela a consulta ativa sem permitir atualizações posteriores na tela.
     */
    private void cancelarConsultaAtual() {
        Task<?> tarefaAnterior = tarefaConsultaAtual;
        tarefaConsultaAtual = null;

        if (tarefaAnterior != null && !tarefaAnterior.isDone()) {
            tarefaAnterior.cancel(true);
        }

        configurarEstadoConsulta(false, null);
    }

    /**
     * Bloqueia ou libera os controles para impedir consultas concorrentes.
     */
    private void configurarEstadoConsulta(
            boolean consultando,
            String mensagem
    ) {
        boolean bloquear = consultando || !telaAtiva;

        tabPaneRelatorios.setDisable(bloquear);
        txtDescricaoEstoque.setDisable(bloquear);
        cbStatusEstoque.setDisable(bloquear);
        cbSituacaoEstoque.setDisable(bloquear);
        btnFiltrarEstoque.setDisable(bloquear);
        txtDescricaoPromocao.setDisable(bloquear);
        cbStatusPromocao.setDisable(bloquear);
        cbTipoDesconto.setDisable(bloquear);
        btnFiltrarPromocoes.setDisable(bloquear);
        btnVoltar.setDisable(false);

        boolean mostrarProgresso = consultando && telaAtiva;
        progressoRelatorio.setVisible(mostrarProgresso);
        progressoRelatorio.setManaged(mostrarProgresso);

        if (mostrarProgresso && mensagem != null) {
            lblEstadoConsulta.setText(mensagem);
        }
    }

    /**
     * Aplica linhas e totalizadores consolidados da visão de estoque.
     */
    private void aplicarResultadoEstoque(
            ResultadoRelatorioEstoqueProduto resultado
    ) {
        if (resultado == null) {
            throw new IllegalStateException(
                    "O Service não retornou o resultado do relatório de estoque."
            );
        }

        List<ProdutoEstoqueRelatorioView> produtos =
                List.copyOf(resultado.getProdutos());

        produtosEstoqueExibidos.setAll(produtos);
        lblQuantidadeProdutos.setText(
                Integer.toString(resultado.getQuantidadeProdutos())
        );
        lblQuantidadeAbaixo.setText(
                Integer.toString(resultado.getQuantidadeAbaixoDoMinimo())
        );
        lblQuantidadeNoMinimo.setText(
                Integer.toString(resultado.getQuantidadeNoMinimo())
        );
        lblQuantidadeAcima.setText(
                Integer.toString(resultado.getQuantidadeAcimaDoMinimo())
        );

        tabelaEstoque.setPlaceholder(
                criarPlaceholder(MENSAGEM_ESTOQUE_VAZIO)
        );
        lblEstadoConsulta.setText(
                produtos.isEmpty()
                        ? "Consulta de estoque concluída."
                        : "Relatório de estoque atualizado com sucesso."
        );
    }

    /**
     * Aplica linhas e totalizador consolidado da visão de promoções.
     */
    private void aplicarResultadoPromocoes(
            ResultadoRelatorioPromocaoProduto resultado
    ) {
        if (resultado == null) {
            throw new IllegalStateException(
                    "O Service não retornou o resultado do relatório de promoções."
            );
        }

        List<ProdutoPromocaoRelatorioView> promocoes =
                List.copyOf(resultado.getPromocoes());

        promocoesExibidas.setAll(promocoes);
        lblQuantidadePromocoes.setText(
                Integer.toString(resultado.getQuantidadePromocoes())
        );
        tabelaPromocoes.setPlaceholder(
                criarPlaceholder(MENSAGEM_PROMOCOES_VAZIAS)
        );
        lblEstadoConsulta.setText(
                promocoes.isEmpty()
                        ? "Consulta de promoções concluída."
                        : "Relatório de promoções atualizado com sucesso."
        );
    }

    /**
     * Trata filtros inválidos antes de iniciar a Task.
     */
    private void tratarFiltroInvalido(IllegalArgumentException e) {
        String mensagem = e.getMessage() == null || e.getMessage().isBlank()
                ? "Verifique os filtros informados."
                : e.getMessage();

        lblEstadoConsulta.setText("Filtros inválidos.");
        mostrarAlerta(
                Alert.AlertType.WARNING,
                "Filtros inválidos",
                mensagem
        );
    }

    /**
     * Registra a causa técnica e mantém o último resultado válido visível.
     */
    private void tratarFalhaConsulta(
            Task<?> tarefa,
            Throwable causa
    ) {
        if (!tarefaEhAtual(tarefa)) {
            return;
        }

        Throwable causaEfetiva = causa != null
                ? causa
                : new IllegalStateException(
                        "A falha da consulta não informou uma causa."
                );

        System.err.println(
                "[ERRO] Falha ao atualizar o relatório de produtos."
        );
        causaEfetiva.printStackTrace();

        finalizarConsulta(tarefa);
        lblEstadoConsulta.setText(
                "Não foi possível atualizar o relatório de produtos."
        );

        mostrarAlerta(
                Alert.AlertType.ERROR,
                "Erro",
                "Não foi possível atualizar o relatório de produtos.\n"
                        + "Os dados já exibidos foram mantidos."
        );
    }

    /**
     * Localiza uma SecurityException na cadeia de causas.
     */
    private SecurityException localizarSecurityException(Throwable causa) {
        Throwable causaAtual = causa;

        while (causaAtual != null) {
            if (causaAtual instanceof SecurityException securityException) {
                return securityException;
            }

            causaAtual = causaAtual.getCause();
        }

        return null;
    }

    /**
     * Limpa dados protegidos e retorna à Tela Principal em acesso negado.
     */
    private void tratarAcessoNegado() {
        telaAtiva = false;
        cancelarConsultaAtual();
        produtosEstoqueExibidos.clear();
        promocoesExibidas.clear();
        lblQuantidadeProdutos.setText("0");
        lblQuantidadeAbaixo.setText("0");
        lblQuantidadeNoMinimo.setText("0");
        lblQuantidadeAcima.setText("0");
        lblQuantidadePromocoes.setText("0");
        tabelaEstoque.setPlaceholder(criarPlaceholder("Acesso negado."));
        tabelaPromocoes.setPlaceholder(criarPlaceholder("Acesso negado."));
        lblEstadoConsulta.setText(MENSAGEM_ACESSO_NEGADO);

        mostrarAlerta(
                Alert.AlertType.WARNING,
                "Acesso negado",
                MENSAGEM_ACESSO_NEGADO
        );

        abrirTelaPrincipal(false);
    }

    /**
     * Volta para a Tela Principal usando o mesmo Stage.
     */
    private void abrirTelaPrincipal(boolean reativarEmFalha) {
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

            if (reativarEmFalha) {
                telaAtiva = true;
                configurarEstadoConsulta(false, null);
                lblEstadoConsulta.setText(
                        "Não foi possível retornar à Tela Principal."
                );
            }

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro",
                    "Não foi possível retornar para a Tela Principal."
            );
        }
    }

    /**
     * Executa a consulta solicitada na aba Estoque.
     */
    @FXML
    private void onFiltrarEstoque() {
        consultarEstoquePelosFiltrosAtuais();
    }

    /**
     * Executa a consulta solicitada na aba Em promoção.
     */
    @FXML
    private void onFiltrarPromocoes() {
        consultarPromocoesPelosFiltrosAtuais();
    }

    /**
     * Cancela a Task ativa e retorna à Tela Principal.
     */
    @FXML
    private void onVoltar() {
        boolean reativarEmFalha = telaAtiva;
        telaAtiva = false;
        cancelarConsultaAtual();
        abrirTelaPrincipal(reativarEmFalha);
    }

    /**
     * Cria um placeholder textual reutilizável nas tabelas.
     */
    private Label criarPlaceholder(String mensagem) {
        Label placeholder = new Label(mensagem);
        placeholder.setWrapText(true);
        return placeholder;
    }

    private String formatarStatus(boolean ativo) {
        return ativo ? "Ativo" : "Inativo";
    }

    private String formatarTipoDesconto(TipoDesconto tipoDesconto) {
        if (tipoDesconto == null) {
            return "—";
        }

        return switch (tipoDesconto) {
            case PERCENTUAL -> "Percentual";
            case VALOR_FIXO -> "Valor fixo";
        };
    }

    /**
     * Formata o desconto sem modificar nem recalcular o valor recebido.
     */
    private String formatarDesconto(
            TipoDesconto tipoDesconto,
            BigDecimal valorDesconto
    ) {
        if (tipoDesconto == null || valorDesconto == null) {
            return "—";
        }

        if (tipoDesconto == TipoDesconto.PERCENTUAL) {
            return valorDesconto
                    .stripTrailingZeros()
                    .toPlainString()
                    .replace('.', ',')
                    + "%";
        }

        return formatarMoeda(valorDesconto);
    }

    /**
     * Formata valores monetários no padrão brasileiro.
     */
    private String formatarMoeda(BigDecimal valor) {
        if (valor == null) {
            return "—";
        }

        return formatadorMoeda
                .format(valor)
                .replace('\u00A0', ' ');
    }

    /**
     * Exibe alertas padronizados sem expor detalhes técnicos ao usuário.
     */
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

    /**
     * Associa um rótulo visual a um valor de filtro opcional.
     */
    private static final class OpcaoFiltro<T> {

        private final String rotulo;
        private final T valor;

        private OpcaoFiltro(String rotulo, T valor) {
            if (rotulo == null || rotulo.isBlank()) {
                throw new IllegalArgumentException(
                        "Rótulo da opção de filtro é obrigatório."
                );
            }

            this.rotulo = rotulo.trim();
            this.valor = valor;
        }

        private T getValor() {
            return valor;
        }

        @Override
        public String toString() {
            return rotulo;
        }
    }
}
