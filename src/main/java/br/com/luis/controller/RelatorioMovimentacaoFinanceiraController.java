package br.com.luis.controller;

import br.com.luis.model.FormaPagamento;
import br.com.luis.model.OrigemMovimentacaoFinanceira;
import br.com.luis.model.TipoMovimentacaoFinanceira;
import br.com.luis.model.Usuario;
import br.com.luis.service.RelatorioMovimentacaoFinanceiraService;
import br.com.luis.util.CabecalhoUtil;
import br.com.luis.util.NavegacaoUtil;
import br.com.luis.util.SessaoUsuario;
import br.com.luis.viewmodel.FiltroRelatorioMovimentacaoFinanceira;
import br.com.luis.viewmodel.MovimentacaoFinanceiraRelatorioView;
import br.com.luis.viewmodel.ResultadoRelatorioMovimentacaoFinanceira;
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
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.StringConverter;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

/**
 * Controller do relatório de movimentações financeiras.
 *
 * Configura a interface, monta filtros tipados e executa as consultas por meio
 * de Task, mantendo o JavaFX Application Thread livre. A autorização visual é
 * verificada pela sessão e a autorização definitiva permanece no Service.
 *
 * O Controller não acessa DAO, não abre Connection e não calcula os totais do
 * relatório.
 */
public class RelatorioMovimentacaoFinanceiraController {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter FORMATO_DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final String MENSAGEM_ACESSO_NEGADO =
            "Usuário não autorizado a consultar o relatório financeiro.";

    private static final String MENSAGEM_RESULTADO_VAZIO =
            "Nenhuma movimentação financeira foi encontrada "
                    + "para os filtros informados.";

    @FXML private Label lblUsuario;
    @FXML private Label lblDataHora;
    @FXML private Button btnVoltar;

    @FXML private DatePicker dpDataInicial;
    @FXML private DatePicker dpDataFinal;

    @FXML
    private ComboBox<OpcaoFiltro<TipoMovimentacaoFinanceira>>
            cbTipoMovimentacao;

    @FXML
    private ComboBox<OpcaoFiltro<OrigemMovimentacaoFinanceira>>
            cbOrigemMovimentacao;

    @FXML
    private ComboBox<OpcaoFiltro<FormaPagamento>>
            cbFormaPagamento;

    @FXML private ProgressIndicator progressoRelatorio;
    @FXML private Label lblEstadoConsulta;
    @FXML private Label lblFiltroAplicado;
    @FXML private Button btnLimparFiltros;
    @FXML private Button btnFiltrar;

    @FXML
    private TableView<MovimentacaoFinanceiraRelatorioView>
            tabelaMovimentacoes;

    @FXML
    private TableColumn<MovimentacaoFinanceiraRelatorioView, Integer>
            colMovimentacaoId;

    @FXML
    private TableColumn<MovimentacaoFinanceiraRelatorioView, LocalDateTime>
            colDataHora;

    @FXML
    private TableColumn<
            MovimentacaoFinanceiraRelatorioView,
            TipoMovimentacaoFinanceira
            > colTipo;

    @FXML
    private TableColumn<
            MovimentacaoFinanceiraRelatorioView,
            OrigemMovimentacaoFinanceira
            > colOrigem;

    @FXML
    private TableColumn<
            MovimentacaoFinanceiraRelatorioView,
            FormaPagamento
            > colFormaPagamento;

    @FXML
    private TableColumn<MovimentacaoFinanceiraRelatorioView, BigDecimal>
            colValor;

    @FXML
    private TableColumn<MovimentacaoFinanceiraRelatorioView, Integer>
            colVendaId;

    @FXML
    private TableColumn<MovimentacaoFinanceiraRelatorioView, Integer>
            colContaReceberId;

    @FXML
    private TableColumn<MovimentacaoFinanceiraRelatorioView, String>
            colResponsavel;

    @FXML private Label lblQuantidadeMovimentacoes;
    @FXML private Label lblTotalEntradas;
    @FXML private Label lblTotalSaidas;
    @FXML private Label lblResultadoLiquido;

    private final ObservableList<MovimentacaoFinanceiraRelatorioView>
            movimentacoesExibidas;

    private final RelatorioMovimentacaoFinanceiraService
            relatorioMovimentacaoFinanceiraService;

    private Task<ResultadoRelatorioMovimentacaoFinanceira>
            taskConsultaAtual;

    private long tokenConsultaAtual;

    private ResultadoRelatorioMovimentacaoFinanceira
            ultimoResultadoValido;

    private boolean telaAtiva;

    /**
     * Inicializa o Service, a lista observável e o estado de controle das
     * consultas.
     */
    public RelatorioMovimentacaoFinanceiraController() {

        this.relatorioMovimentacaoFinanceiraService =
                new RelatorioMovimentacaoFinanceiraService();

        this.movimentacoesExibidas =
                FXCollections.observableArrayList();

        this.taskConsultaAtual = null;
        this.tokenConsultaAtual = 0L;
        this.ultimoResultadoValido = null;
        this.telaAtiva = true;
    }

    /**
     * Inicializa a estrutura visual e agenda o fluxo inicial para depois do
     * carregamento completo do FXML.
     */
    @FXML
    public void initialize() {

        configurarCabecalho();
        configurarDatePickers();
        definirPeriodoInicial();
        configurarCombos();
        configurarTabela();
        configurarEstadoVisualInicial();

        Platform.runLater(this::iniciarFluxoInicial);
    }

    /**
     * Configura o usuário e o relógio do cabeçalho padrão.
     */
    private void configurarCabecalho() {
        CabecalhoUtil.configurarUsuarioEDataHora(
                lblUsuario,
                lblDataHora
        );
    }

    /**
     * Configura a apresentação das datas no padrão brasileiro.
     */
    private void configurarDatePickers() {

        StringConverter<LocalDate> conversorData =
                criarConversorData();

        dpDataInicial.setConverter(conversorData);
        dpDataFinal.setConverter(conversorData);

        dpDataInicial.setPromptText("dd/MM/yyyy");
        dpDataFinal.setPromptText("dd/MM/yyyy");
    }

    /**
     * Cria o conversor que mantém LocalDate internamente.
     */
    private StringConverter<LocalDate> criarConversorData() {

        return new StringConverter<>() {
            @Override
            public String toString(LocalDate data) {

                if (data == null) {
                    return "";
                }

                return FORMATO_DATA.format(data);
            }

            @Override
            public LocalDate fromString(String texto) {

                if (texto == null || texto.isBlank()) {
                    return null;
                }

                try {
                    return LocalDate.parse(
                            texto.trim(),
                            FORMATO_DATA
                    );

                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException(
                            "Data inválida. Utilize o formato dd/MM/yyyy.",
                            e
                    );
                }
            }
        };
    }

    /**
     * Define o primeiro dia do mês atual e a data atual.
     */
    private void definirPeriodoInicial() {

        LocalDate hoje = LocalDate.now();

        dpDataInicial.setValue(
                hoje.withDayOfMonth(1)
        );

        dpDataFinal.setValue(hoje);
    }

    /**
     * Preenche os filtros tipados e seleciona as opções de todos os registros.
     */
    private void configurarCombos() {

        cbTipoMovimentacao.getItems().setAll(
                new OpcaoFiltro<>("Todos", null),
                new OpcaoFiltro<>(
                        "Entrada",
                        TipoMovimentacaoFinanceira.ENTRADA
                ),
                new OpcaoFiltro<>(
                        "Saída",
                        TipoMovimentacaoFinanceira.SAIDA
                )
        );

        cbOrigemMovimentacao.getItems().setAll(
                new OpcaoFiltro<>("Todas", null),
                new OpcaoFiltro<>(
                        "Venda à vista",
                        OrigemMovimentacaoFinanceira.VENDA_A_VISTA
                ),
                new OpcaoFiltro<>(
                        "Recebimento de conta",
                        OrigemMovimentacaoFinanceira.RECEBIMENTO_CONTA
                ),
                new OpcaoFiltro<>(
                        "Estorno de venda à vista",
                        OrigemMovimentacaoFinanceira
                                .ESTORNO_VENDA_A_VISTA
                ),
                new OpcaoFiltro<>(
                        "Estorno de recebimento de conta",
                        OrigemMovimentacaoFinanceira
                                .ESTORNO_RECEBIMENTO_CONTA
                )
        );

        cbFormaPagamento.getItems().setAll(
                new OpcaoFiltro<>("Todas", null),
                new OpcaoFiltro<>(
                        "Dinheiro",
                        FormaPagamento.DINHEIRO
                ),
                new OpcaoFiltro<>(
                        "PIX",
                        FormaPagamento.PIX
                ),
                new OpcaoFiltro<>(
                        "Cartão",
                        FormaPagamento.CARTAO
                )
        );

        selecionarPrimeirasOpcoesDosCombos();
    }

    /**
     * Seleciona a opção Todos ou Todas de cada filtro.
     */
    private void selecionarPrimeirasOpcoesDosCombos() {

        cbTipoMovimentacao
                .getSelectionModel()
                .selectFirst();

        cbOrigemMovimentacao
                .getSelectionModel()
                .selectFirst();

        cbFormaPagamento
                .getSelectionModel()
                .selectFirst();
    }

    /**
     * Configura as propriedades e formatações da tabela.
     */
    private void configurarTabela() {

        colMovimentacaoId.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue()
                                .getMovimentacaoFinanceiraId()
                )
        );

        colDataHora.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getDataHora()
                )
        );

        colTipo.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getTipo()
                )
        );

        colOrigem.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getOrigem()
                )
        );

        colFormaPagamento.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue()
                                .getFormaPagamento()
                )
        );

        colValor.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getValor()
                )
        );

        colVendaId.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getVendaId()
                )
        );

        colContaReceberId.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue()
                                .getContaReceberId()
                )
        );

        colResponsavel.setCellValueFactory(
                celula -> new SimpleStringProperty(
                        formatarResponsavel(
                                celula.getValue()
                                        .getNomeResponsavel(),
                                celula.getValue()
                                        .getUsuarioId()
                        )
                )
        );

        configurarColunaDataHora();
        configurarColunaTipo();
        configurarColunaOrigem();
        configurarColunaFormaPagamento();
        configurarColunaValor();
        configurarColunaContaReceber();

        tabelaMovimentacoes.setItems(
                movimentacoesExibidas
        );

        Label placeholder =
                new Label(
                        "Relatório financeiro ainda não carregado."
                );

        placeholder.setWrapText(true);
        tabelaMovimentacoes.setPlaceholder(placeholder);
    }

    /**
     * Formata a coluna de data e hora.
     */
    private void configurarColunaDataHora() {

        colDataHora.setCellFactory(
                coluna -> new TableCell<>() {
                    @Override
                    protected void updateItem(
                            LocalDateTime dataHora,
                            boolean empty
                    ) {
                        super.updateItem(dataHora, empty);

                        setText(
                                empty
                                        ? null
                                        : formatarDataHora(dataHora)
                        );
                    }
                }
        );
    }

    /**
     * Formata a coluna de tipo.
     */
    private void configurarColunaTipo() {

        colTipo.setCellFactory(
                coluna -> new TableCell<>() {
                    @Override
                    protected void updateItem(
                            TipoMovimentacaoFinanceira tipo,
                            boolean empty
                    ) {
                        super.updateItem(tipo, empty);

                        setText(
                                empty
                                        ? null
                                        : formatarTipo(tipo)
                        );
                    }
                }
        );
    }

    /**
     * Formata a coluna de origem.
     */
    private void configurarColunaOrigem() {

        colOrigem.setCellFactory(
                coluna -> new TableCell<>() {
                    @Override
                    protected void updateItem(
                            OrigemMovimentacaoFinanceira origem,
                            boolean empty
                    ) {
                        super.updateItem(origem, empty);

                        setText(
                                empty
                                        ? null
                                        : formatarOrigem(origem)
                        );
                    }
                }
        );
    }

    /**
     * Formata a coluna de forma de pagamento.
     */
    private void configurarColunaFormaPagamento() {

        colFormaPagamento.setCellFactory(
                coluna -> new TableCell<>() {
                    @Override
                    protected void updateItem(
                            FormaPagamento formaPagamento,
                            boolean empty
                    ) {
                        super.updateItem(
                                formaPagamento,
                                empty
                        );

                        setText(
                                empty
                                        ? null
                                        : formatarFormaPagamento(
                                                formaPagamento
                                        )
                        );
                    }
                }
        );
    }

    /**
     * Formata e alinha à direita a coluna monetária.
     */
    private void configurarColunaValor() {

        colValor.setCellFactory(
                coluna -> new TableCell<>() {
                    @Override
                    protected void updateItem(
                            BigDecimal valor,
                            boolean empty
                    ) {
                        super.updateItem(valor, empty);

                        setText(
                                empty
                                        ? null
                                        : formatarValor(valor)
                        );

                        setStyle(
                                "-fx-alignment: CENTER-RIGHT;"
                        );
                    }
                }
        );
    }

    /**
     * Exibe travessão quando a movimentação não possui conta vinculada.
     */
    private void configurarColunaContaReceber() {

        colContaReceberId.setCellFactory(
                coluna -> new TableCell<>() {
                    @Override
                    protected void updateItem(
                            Integer contaReceberId,
                            boolean empty
                    ) {
                        super.updateItem(
                                contaReceberId,
                                empty
                        );

                        setText(
                                empty
                                        ? null
                                        : formatarConta(
                                                contaReceberId
                                        )
                        );
                    }
                }
        );
    }

    /**
     * Define os textos e estados visuais anteriores à primeira consulta.
     */
    private void configurarEstadoVisualInicial() {

        lblQuantidadeMovimentacoes.setText(
                "0 movimentações"
        );

        lblTotalEntradas.setText("R$ 0,00");
        lblTotalSaidas.setText("R$ 0,00");
        lblResultadoLiquido.setText("R$ 0,00");

        lblEstadoConsulta.setText(
                "Relatório pronto para consulta."
        );

        lblFiltroAplicado.setText("");

        progressoRelatorio.setVisible(false);
        progressoRelatorio.setManaged(false);

        btnFiltrar.setDisable(true);
    }

    /**
     * Valida visualmente a sessão e inicia a consulta automática com os filtros
     * padrão definidos durante a inicialização.
     */
    private void iniciarFluxoInicial() {

        if (!telaAtiva) {
            return;
        }

        try {
            Integer usuarioId =
                    obterUsuarioIdAutorizadoVisualmente();

            FiltroRelatorioMovimentacaoFinanceira filtro =
                    montarFiltro();

            iniciarConsulta(filtro, usuarioId);

        } catch (SecurityException e) {
            tratarAcessoNegado();

        } catch (IllegalArgumentException e) {
            tratarFiltroInvalido(e);
        }
    }

    /**
     * Obtém o ID do administrador mantido na sessão atual.
     *
     * Essa verificação é somente visual. O Service reconsulta o usuário no
     * banco antes de autorizar o relatório.
     *
     * @return ID positivo do administrador atual.
     * @throws SecurityException quando a sessão não representa um ADMIN válido.
     */
    private Integer obterUsuarioIdAutorizadoVisualmente() {

        Usuario usuarioLogado =
                SessaoUsuario
                        .getInstance()
                        .getUsuarioLogado();

        if (usuarioLogado == null) {
            throw new SecurityException(
                    MENSAGEM_ACESSO_NEGADO
            );
        }

        Integer usuarioId =
                usuarioLogado.getIdUsuario();

        if (usuarioId == null
                || usuarioId <= 0
                || !"ADMIN".equals(
                        usuarioLogado.getPerfil()
                )) {

            throw new SecurityException(
                    MENSAGEM_ACESSO_NEGADO
            );
        }

        return usuarioId;
    }

    /**
     * Confirma os textos digitados nos DatePickers e constrói o filtro oficial.
     *
     * As regras definitivas de período e forma de pagamento permanecem no
     * construtor de FiltroRelatorioMovimentacaoFinanceira.
     */
    private FiltroRelatorioMovimentacaoFinanceira montarFiltro() {

        LocalDate dataInicial =
                confirmarTextoDatePicker(
                        dpDataInicial
                );

        LocalDate dataFinal =
                confirmarTextoDatePicker(
                        dpDataFinal
                );

        TipoMovimentacaoFinanceira tipo =
                obterValorOpcaoSelecionada(
                        cbTipoMovimentacao
                );

        OrigemMovimentacaoFinanceira origem =
                obterValorOpcaoSelecionada(
                        cbOrigemMovimentacao
                );

        FormaPagamento formaPagamento =
                obterValorOpcaoSelecionada(
                        cbFormaPagamento
                );

        return new FiltroRelatorioMovimentacaoFinanceira(
                dataInicial,
                dataFinal,
                tipo,
                origem,
                formaPagamento
        );
    }

    /**
     * Converte o texto atualmente exibido no editor e reaplica o LocalDate
     * correspondente ao controle.
     */
    private LocalDate confirmarTextoDatePicker(
            DatePicker datePicker
    ) {

        String texto =
                datePicker
                        .getEditor()
                        .getText();

        LocalDate dataConvertida =
                datePicker
                        .getConverter()
                        .fromString(texto);

        datePicker.setValue(dataConvertida);

        return dataConvertida;
    }

    /**
     * Obtém o valor tipado associado à opção selecionada.
     *
     * Uma seleção ausente é interpretada como o filtro Todos ou Todas.
     */
    private <T> T obterValorOpcaoSelecionada(
            ComboBox<OpcaoFiltro<T>> comboBox
    ) {

        OpcaoFiltro<T> opcao =
                comboBox.getValue();

        if (opcao == null) {
            return null;
        }

        return opcao.getValor();
    }

    /**
     * Inicia uma nova consulta e invalida qualquer Task anterior.
     */
    private void iniciarConsulta(
            FiltroRelatorioMovimentacaoFinanceira filtro,
            Integer usuarioId
    ) {

        if (!telaAtiva) {
            return;
        }

        invalidarConsultaAtual();

        long tokenDaConsulta =
                tokenConsultaAtual;

        Task<ResultadoRelatorioMovimentacaoFinanceira>
                novaTask = new Task<>() {

            @Override
            protected ResultadoRelatorioMovimentacaoFinanceira call() {
                return relatorioMovimentacaoFinanceiraService
                        .consultarRelatorio(
                                filtro,
                                usuarioId
                        );
            }
        };

        taskConsultaAtual = novaTask;

        configurarHandlersConsulta(
                novaTask,
                tokenDaConsulta
        );

        configurarEstadoConsulta(true);

        Thread thread = new Thread(
                novaTask,
                "relatorio-financeiro-consulta-"
                        + tokenDaConsulta
        );

        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Configura os handlers da consulta com proteção por referência e token.
     */
    private void configurarHandlersConsulta(
            Task<ResultadoRelatorioMovimentacaoFinanceira> task,
            long tokenDaConsulta
    ) {

        task.setOnSucceeded(event -> {

            if (!consultaEhAtual(
                    task,
                    tokenDaConsulta,
                    true
            )) {
                return;
            }

            ResultadoRelatorioMovimentacaoFinanceira resultado =
                    task.getValue();

            if (resultado == null) {
                tratarFalhaTecnica(
                        task,
                        tokenDaConsulta,
                        new IllegalStateException(
                                "O Service não retornou o resultado "
                                        + "do relatório financeiro."
                        )
                );

                return;
            }

            try {
                aplicarResultado(resultado);
                ultimoResultadoValido = resultado;

            } catch (RuntimeException e) {
                tratarFalhaTecnica(
                        task,
                        tokenDaConsulta,
                        e
                );

                return;
            }

            finalizarConsultaAtual(
                    task,
                    tokenDaConsulta
            );
        });

        task.setOnFailed(event -> {

            if (!consultaEhAtual(
                    task,
                    tokenDaConsulta,
                    true
            )) {
                return;
            }

            Throwable causa =
                    task.getException();

            if (localizarSecurityException(causa)
                    != null) {

                tratarAcessoNegado();
                return;
            }

            tratarFalhaTecnica(
                    task,
                    tokenDaConsulta,
                    causa
            );
        });

        task.setOnCancelled(event -> {

            if (!consultaEhAtual(
                    task,
                    tokenDaConsulta,
                    false
            )) {
                return;
            }

            finalizarConsultaAtual(
                    task,
                    tokenDaConsulta
            );

            lblEstadoConsulta.setText(
                    "Consulta cancelada."
            );
        });
    }

    /**
     * Confirma que um handler ainda pertence à consulta atual.
     */
    private boolean consultaEhAtual(
            Task<ResultadoRelatorioMovimentacaoFinanceira> task,
            long tokenDaConsulta,
            boolean exigirNaoCancelada
    ) {

        if (!telaAtiva
                || taskConsultaAtual != task
                || tokenConsultaAtual != tokenDaConsulta) {

            return false;
        }

        return !exigirNaoCancelada
                || !task.isCancelled();
    }

    /**
     * Invalida a identidade atual antes de cancelar a Task correspondente.
     */
    private void invalidarConsultaAtual() {

        tokenConsultaAtual++;

        Task<ResultadoRelatorioMovimentacaoFinanceira>
                taskAnterior = taskConsultaAtual;

        taskConsultaAtual = null;

        if (taskAnterior != null
                && !taskAnterior.isDone()) {

            taskAnterior.cancel(true);
        }
    }

    /**
     * Finaliza uma consulta somente quando sua identidade ainda é a atual.
     */
    private void finalizarConsultaAtual(
            Task<ResultadoRelatorioMovimentacaoFinanceira> task,
            long tokenDaConsulta
    ) {

        if (!consultaEhAtual(
                task,
                tokenDaConsulta,
                false
        )) {
            return;
        }

        taskConsultaAtual = null;
        configurarEstadoConsulta(false);
    }

    /**
     * Bloqueia ou libera os controles vinculados à consulta.
     */
    private void configurarEstadoConsulta(
            boolean carregando
    ) {

        boolean bloquearConsultas =
                carregando || !telaAtiva;

        dpDataInicial.setDisable(bloquearConsultas);
        dpDataFinal.setDisable(bloquearConsultas);

        cbTipoMovimentacao.setDisable(
                bloquearConsultas
        );

        cbOrigemMovimentacao.setDisable(
                bloquearConsultas
        );

        cbFormaPagamento.setDisable(
                bloquearConsultas
        );

        btnLimparFiltros.setDisable(
                bloquearConsultas
        );

        btnFiltrar.setDisable(
                bloquearConsultas
        );

        btnVoltar.setDisable(false);

        boolean mostrarProgresso =
                carregando && telaAtiva;

        progressoRelatorio.setVisible(
                mostrarProgresso
        );

        progressoRelatorio.setManaged(
                mostrarProgresso
        );

        if (mostrarProgresso) {
            lblEstadoConsulta.setText(
                    "Consultando movimentações financeiras..."
            );
        }
    }

    /**
     * Aplica uma fotografia completa do relatório.
     *
     * Todos os textos são preparados antes da alteração dos componentes, e os
     * totais são obtidos exclusivamente do resultado consolidado pelo Service.
     */
    private void aplicarResultado(
            ResultadoRelatorioMovimentacaoFinanceira resultado
    ) {

        if (resultado == null) {
            throw new IllegalArgumentException(
                    "Resultado do relatório não pode ser nulo."
            );
        }

        FiltroRelatorioMovimentacaoFinanceira filtroAplicado =
                resultado.getFiltroAplicado();

        if (filtroAplicado == null) {
            throw new IllegalStateException(
                    "Filtro aplicado não retornado pelo resultado."
            );
        }

        List<MovimentacaoFinanceiraRelatorioView>
                novasMovimentacoes =
                List.copyOf(
                        resultado.getMovimentacoes()
                );

        int quantidade =
                resultado.getQuantidadeMovimentacoes();

        String textoQuantidade =
                formatarQuantidadeMovimentacoes(
                        quantidade
                );

        String textoEntradas =
                formatarValor(
                        resultado.getTotalEntradas()
                );

        String textoSaidas =
                formatarValor(
                        resultado.getTotalSaidas()
                );

        String textoLiquido =
                formatarValor(
                        resultado.getResultadoLiquido()
                );

        String textoFiltroAplicado =
                formatarFiltroAplicado(
                        filtroAplicado
                );

        boolean resultadoVazio =
                novasMovimentacoes.isEmpty();

        String textoEstado =
                resultadoVazio
                        ? MENSAGEM_RESULTADO_VAZIO
                        : "Relatório atualizado com sucesso.";

        Label placeholder =
                new Label(
                        MENSAGEM_RESULTADO_VAZIO
                );

        placeholder.setWrapText(true);

        movimentacoesExibidas.setAll(
                novasMovimentacoes
        );

        lblQuantidadeMovimentacoes.setText(
                textoQuantidade
        );

        lblTotalEntradas.setText(
                textoEntradas
        );

        lblTotalSaidas.setText(
                textoSaidas
        );

        lblResultadoLiquido.setText(
                textoLiquido
        );

        lblFiltroAplicado.setText(
                textoFiltroAplicado
        );

        lblEstadoConsulta.setText(
                textoEstado
        );

        tabelaMovimentacoes.setPlaceholder(
                placeholder
        );
    }

    /**
     * Monta o texto do período e dos filtros efetivamente aplicados.
     */
    private String formatarFiltroAplicado(
            FiltroRelatorioMovimentacaoFinanceira filtro
    ) {

        return "Dados exibidos: "
                + filtro.getDataInicial()
                        .format(FORMATO_DATA)
                + " a "
                + filtro.getDataFinal()
                        .format(FORMATO_DATA)
                + " | Tipo: "
                + formatarTipoFiltro(
                        filtro.getTipo()
                )
                + " | Origem: "
                + formatarOrigemFiltro(
                        filtro.getOrigem()
                )
                + " | Pagamento: "
                + formatarFormaPagamentoFiltro(
                        filtro.getFormaPagamento()
                );
    }

    /**
     * Formata a quantidade de movimentações com singular ou plural.
     */
    private String formatarQuantidadeMovimentacoes(
            int quantidade
    ) {

        return quantidade
                + (quantidade == 1
                ? " movimentação"
                : " movimentações");
    }

    /**
     * Formata o tipo aplicado ao filtro.
     */
    private String formatarTipoFiltro(
            TipoMovimentacaoFinanceira tipo
    ) {

        if (tipo == null) {
            return "Todos";
        }

        return formatarTipo(tipo);
    }

    /**
     * Formata a origem aplicada ao filtro.
     */
    private String formatarOrigemFiltro(
            OrigemMovimentacaoFinanceira origem
    ) {

        if (origem == null) {
            return "Todas";
        }

        return formatarOrigem(origem);
    }

    /**
     * Formata a forma de pagamento aplicada ao filtro.
     */
    private String formatarFormaPagamentoFiltro(
            FormaPagamento formaPagamento
    ) {

        if (formaPagamento == null) {
            return "Todas";
        }

        return formatarFormaPagamento(
                formaPagamento
        );
    }

    /**
     * Trata uma falha de consulta sem alterar o último resultado válido.
     */
    private void tratarFalhaTecnica(
            Task<ResultadoRelatorioMovimentacaoFinanceira> task,
            long tokenDaConsulta,
            Throwable causa
    ) {

        if (!consultaEhAtual(
                task,
                tokenDaConsulta,
                true
        )) {
            return;
        }

        Throwable causaEfetiva =
                causa != null
                        ? causa
                        : new IllegalStateException(
                                "A falha da consulta não informou uma causa."
                        );

        System.err.println(
                "[ERRO] Falha ao atualizar o relatório financeiro."
        );

        causaEfetiva.printStackTrace();

        finalizarConsultaAtual(
                task,
                tokenDaConsulta
        );

        String complemento =
                ultimoResultadoValido != null
                        ? "Os dados exibidos correspondem à última "
                        + "consulta concluída com sucesso."
                        : "Nenhum resultado válido foi carregado.";

        String mensagem =
                "Não foi possível atualizar o relatório financeiro.\n"
                        + complemento;

        lblEstadoConsulta.setText(
                mensagem.replace('\n', ' ')
        );

        mostrarAlerta(
                Alert.AlertType.ERROR,
                "Erro",
                mensagem
        );
    }

    /**
     * Localiza uma SecurityException na causa recebida ou em sua cadeia.
     */
    private SecurityException localizarSecurityException(
            Throwable causa
    ) {

        Throwable causaAtual =
                causa;

        while (causaAtual != null) {

            if (causaAtual
                    instanceof SecurityException securityException) {

                return securityException;
            }

            causaAtual =
                    causaAtual.getCause();
        }

        return null;
    }

    /**
     * Remove todos os dados financeiros da tela e da memória do Controller.
     */
    private void limparDadosFinanceirosProtegidos() {

        movimentacoesExibidas.clear();

        lblQuantidadeMovimentacoes.setText(
                "0 movimentações"
        );

        lblTotalEntradas.setText("R$ 0,00");
        lblTotalSaidas.setText("R$ 0,00");
        lblResultadoLiquido.setText("R$ 0,00");

        lblFiltroAplicado.setText("");
        lblEstadoConsulta.setText(
                MENSAGEM_ACESSO_NEGADO
        );

        Label placeholder =
                new Label("Acesso negado.");

        tabelaMovimentacoes.setPlaceholder(
                placeholder
        );

        ultimoResultadoValido = null;
    }

    /**
     * Trata a negação visual ou definitiva de acesso ao relatório.
     */
    private void tratarAcessoNegado() {

        telaAtiva = false;

        invalidarConsultaAtual();
        configurarEstadoConsulta(false);
        limparDadosFinanceirosProtegidos();

        mostrarAlerta(
                Alert.AlertType.WARNING,
                "Acesso negado",
                MENSAGEM_ACESSO_NEGADO
        );

        abrirTelaPrincipal(false);
    }

    /**
     * Trata filtros inválidos sem alterar o último resultado aplicado.
     */
    private void tratarFiltroInvalido(
            IllegalArgumentException e
    ) {

        if (!telaAtiva) {
            return;
        }

        configurarEstadoConsulta(false);

        String mensagem =
                e.getMessage() == null
                        || e.getMessage().isBlank()
                        ? "Verifique os filtros informados."
                        : e.getMessage();

        lblEstadoConsulta.setText(
                "Filtros inválidos. Corrija os dados informados."
        );

        mostrarAlerta(
                Alert.AlertType.WARNING,
                "Filtros inválidos",
                mensagem
        );
    }

    /**
     * Abre a Tela Principal e controla a possibilidade de reativar esta tela
     * quando a navegação normal falha.
     */
    private void abrirTelaPrincipal(
            boolean reativarEmFalha
    ) {

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
                configurarEstadoConsulta(false);

                lblEstadoConsulta.setText(
                        "Não foi possível retornar à Tela Principal."
                );

            } else {
                configurarEstadoConsulta(false);
            }

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro",
                    "Não foi possível retornar para a Tela Principal."
            );
        }
    }

    /**
     * Restaura os filtros visuais sem iniciar consulta e sem alterar o último
     * resultado exibido.
     */
    @FXML
    private void onLimparFiltros() {

        if (!telaAtiva) {
            return;
        }

        definirPeriodoInicial();
        selecionarPrimeirasOpcoesDosCombos();

        lblEstadoConsulta.setText(
                "Filtros restaurados. Clique em Filtrar para aplicá-los."
        );
    }

    /**
     * Valida a sessão e os filtros antes de iniciar uma consulta assíncrona.
     */
    @FXML
    private void onFiltrar() {

        if (!telaAtiva) {
            return;
        }

        try {
            Integer usuarioId =
                    obterUsuarioIdAutorizadoVisualmente();

            FiltroRelatorioMovimentacaoFinanceira filtro =
                    montarFiltro();

            iniciarConsulta(filtro, usuarioId);

        } catch (SecurityException e) {
            tratarAcessoNegado();

        } catch (IllegalArgumentException e) {
            tratarFiltroInvalido(e);
        }
    }

    /**
     * Retorna para a Tela Principal usando o padrão centralizado de navegação.
     */
    @FXML
    private void onVoltar() {

        boolean reativarEmFalha =
                telaAtiva;

        telaAtiva = false;

        invalidarConsultaAtual();
        configurarEstadoConsulta(false);

        abrirTelaPrincipal(
                reativarEmFalha
        );
    }

    /**
     * Formata data e hora para apresentação na tabela.
     */
    private String formatarDataHora(
            LocalDateTime dataHora
    ) {

        if (dataHora == null) {
            return "—";
        }

        return dataHora.format(FORMATO_DATA_HORA);
    }

    /**
     * Formata o tipo da movimentação.
     */
    private String formatarTipo(
            TipoMovimentacaoFinanceira tipo
    ) {

        if (tipo == null) {
            return "—";
        }

        return switch (tipo) {
            case ENTRADA -> "Entrada";
            case SAIDA -> "Saída";
        };
    }

    /**
     * Formata a origem financeira.
     */
    private String formatarOrigem(
            OrigemMovimentacaoFinanceira origem
    ) {

        if (origem == null) {
            return "—";
        }

        return switch (origem) {
            case VENDA_A_VISTA ->
                    "Venda à vista";

            case RECEBIMENTO_CONTA ->
                    "Recebimento de conta";

            case ESTORNO_VENDA_A_VISTA ->
                    "Estorno de venda à vista";

            case ESTORNO_RECEBIMENTO_CONTA ->
                    "Estorno de recebimento de conta";
        };
    }

    /**
     * Formata a forma de pagamento.
     */
    private String formatarFormaPagamento(
            FormaPagamento formaPagamento
    ) {

        if (formaPagamento == null) {
            return "—";
        }

        return switch (formaPagamento) {
            case DINHEIRO -> "Dinheiro";
            case PIX -> "PIX";
            case CARTAO -> "Cartão";
            case A_PRAZO -> "A prazo";
        };
    }

    /**
     * Formata um valor monetário no padrão brasileiro.
     */
    private String formatarValor(BigDecimal valor) {

        BigDecimal valorSeguro =
                valor != null
                        ? valor
                        : BigDecimal.ZERO;

        NumberFormat formatoMoeda =
                NumberFormat.getCurrencyInstance(
                        new Locale("pt", "BR")
                );

        return formatoMoeda
                .format(valorSeguro)
                .replace('\u00A0', ' ');
    }

    /**
     * Formata o identificador opcional da conta.
     */
    private String formatarConta(
            Integer contaReceberId
    ) {

        if (contaReceberId == null
                || contaReceberId <= 0) {

            return "—";
        }

        return contaReceberId.toString();
    }

    /**
     * Combina o nome atual e o identificador estável do responsável.
     */
    private String formatarResponsavel(
            String nomeResponsavel,
            Integer usuarioId
    ) {

        String nomeFormatado =
                nomeResponsavel == null
                        || nomeResponsavel.isBlank()
                        ? "—"
                        : nomeResponsavel.trim();

        if (usuarioId == null || usuarioId <= 0) {
            return nomeFormatado;
        }

        return nomeFormatado
                + " (ID "
                + usuarioId
                + ")";
    }

    /**
     * Exibe alertas padronizados.
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
     * Representa uma opção visual associada a um valor tipado opcional.
     *
     * @param <T> tipo do enum associado ao filtro.
     */
    private static final class OpcaoFiltro<T> {

        private final String rotulo;
        private final T valor;

        private OpcaoFiltro(
                String rotulo,
                T valor
        ) {

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
