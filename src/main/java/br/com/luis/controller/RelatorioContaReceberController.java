package br.com.luis.controller;

import br.com.luis.model.Usuario;
import br.com.luis.service.RelatorioContaReceberService;
import br.com.luis.util.CabecalhoUtil;
import br.com.luis.util.NavegacaoUtil;
import br.com.luis.util.SessaoUsuario;
import br.com.luis.viewmodel.ContaReceberRelatorioView;
import br.com.luis.viewmodel.FiltroRelatorioContaReceber;
import br.com.luis.viewmodel.ResultadoRelatorioContaReceber;
import br.com.luis.viewmodel.SituacaoRelatorioContaReceber;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
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
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

/**
 * Controller do relatório de contas a receber.
 *
 * Configura a interface, monta filtros tipados e executa as consultas por meio
 * de Task, mantendo o JavaFX Application Thread livre. A autorização visual é
 * verificada pela sessão e a autorização definitiva permanece no Service.
 *
 * O Controller não acessa DAO, não abre Connection, não classifica vencimentos
 * e não calcula os totais do relatório.
 */
public class RelatorioContaReceberController {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String MENSAGEM_ACESSO_NEGADO =
            "Usuário não autorizado a consultar o relatório de contas a receber.";

    private static final String MENSAGEM_RESULTADO_VAZIO =
            "Nenhuma conta a receber foi encontrada para os filtros informados.";

    @FXML private Label lblUsuario;
    @FXML private Label lblDataHora;
    @FXML private Button btnVoltar;

    @FXML private DatePicker dpDataInicial;
    @FXML private DatePicker dpDataFinal;
    @FXML private TextField txtCliente;

    @FXML
    private ComboBox<OpcaoFiltro<SituacaoRelatorioContaReceber>>
            cbSituacao;

    @FXML private ProgressIndicator progressoRelatorio;
    @FXML private Label lblEstadoConsulta;
    @FXML private Label lblFiltroAplicado;
    @FXML private Button btnLimparFiltros;
    @FXML private Button btnFiltrar;

    @FXML
    private TableView<ContaReceberRelatorioView>
            tabelaContasReceber;

    @FXML
    private TableColumn<ContaReceberRelatorioView, Integer>
            colContaId;

    @FXML
    private TableColumn<ContaReceberRelatorioView, Integer>
            colVendaId;

    @FXML
    private TableColumn<ContaReceberRelatorioView, String>
            colCliente;

    @FXML
    private TableColumn<ContaReceberRelatorioView, BigDecimal>
            colValor;

    @FXML
    private TableColumn<ContaReceberRelatorioView, LocalDate>
            colVencimento;

    @FXML
    private TableColumn<
            ContaReceberRelatorioView,
            SituacaoRelatorioContaReceber
            > colSituacao;

    @FXML private Label lblQuantidadeContas;
    @FXML private Label lblValorListado;
    @FXML private Label lblValorPendente;
    @FXML private Label lblValorVencido;

    private final ObservableList<ContaReceberRelatorioView>
            contasExibidas;

    private final RelatorioContaReceberService
            relatorioContaReceberService;

    private Task<ResultadoRelatorioContaReceber>
            taskConsultaAtual;

    private long tokenConsultaAtual;

    private ResultadoRelatorioContaReceber
            ultimoResultadoValido;

    private boolean telaAtiva;

    /**
     * Inicializa o Service, a lista observável e o estado de controle das
     * consultas.
     */
    public RelatorioContaReceberController() {

        this.relatorioContaReceberService =
                new RelatorioContaReceberService();

        this.contasExibidas =
                FXCollections.observableArrayList();

        this.taskConsultaAtual = null;
        this.tokenConsultaAtual = 0L;
        this.ultimoResultadoValido = null;
        this.telaAtiva = true;
    }

    /**
     * Inicializa a estrutura visual e agenda a consulta automática para depois
     * do carregamento completo do FXML.
     */
    @FXML
    public void initialize() {

        configurarCabecalho();
        configurarDatePickers();
        definirPeriodoInicial();
        configurarComboSituacao();
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
     * Define o mês atual inteiro como período inicial de vencimento.
     */
    private void definirPeriodoInicial() {

        LocalDate hoje = LocalDate.now();

        dpDataInicial.setValue(
                hoje.withDayOfMonth(1)
        );

        dpDataFinal.setValue(
                hoje.withDayOfMonth(
                        hoje.lengthOfMonth()
                )
        );
    }

    /**
     * Preenche o filtro de situação e seleciona a opção Todas.
     */
    private void configurarComboSituacao() {

        cbSituacao.getItems().setAll(
                new OpcaoFiltro<>("Todas", null),
                new OpcaoFiltro<>(
                        "A vencer",
                        SituacaoRelatorioContaReceber.A_VENCER
                ),
                new OpcaoFiltro<>(
                        "Vencidas",
                        SituacaoRelatorioContaReceber.VENCIDA
                ),
                new OpcaoFiltro<>(
                        "Pagas",
                        SituacaoRelatorioContaReceber.PAGA
                ),
                new OpcaoFiltro<>(
                        "Canceladas",
                        SituacaoRelatorioContaReceber.CANCELADA
                )
        );

        cbSituacao
                .getSelectionModel()
                .selectFirst();
    }

    /**
     * Configura as propriedades e formatações da tabela.
     */
    private void configurarTabela() {

        colContaId.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getIdConta()
                )
        );

        colVendaId.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getVendaId()
                )
        );

        colCliente.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getNomeCliente()
                )
        );

        colValor.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getValor()
                )
        );

        colVencimento.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getDataVencimento()
                )
        );

        colSituacao.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getSituacao()
                )
        );

        configurarColunaValor();
        configurarColunaVencimento();
        configurarColunaSituacao();

        tabelaContasReceber.setItems(
                contasExibidas
        );

        Label placeholder =
                new Label(
                        "Relatório de contas a receber ainda não carregado."
                );

        placeholder.setWrapText(true);
        tabelaContasReceber.setPlaceholder(placeholder);
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
     * Formata a data de vencimento.
     */
    private void configurarColunaVencimento() {

        colVencimento.setCellFactory(
                coluna -> new TableCell<>() {
                    @Override
                    protected void updateItem(
                            LocalDate vencimento,
                            boolean empty
                    ) {
                        super.updateItem(vencimento, empty);

                        setText(
                                empty
                                        ? null
                                        : formatarData(vencimento)
                        );
                    }
                }
        );
    }

    /**
     * Formata a situação calculada do relatório.
     */
    private void configurarColunaSituacao() {

        colSituacao.setCellFactory(
                coluna -> new TableCell<>() {
                    @Override
                    protected void updateItem(
                            SituacaoRelatorioContaReceber situacao,
                            boolean empty
                    ) {
                        super.updateItem(situacao, empty);

                        setText(
                                empty
                                        ? null
                                        : formatarSituacao(situacao)
                        );
                    }
                }
        );
    }

    /**
     * Define os textos e estados visuais anteriores à primeira consulta.
     */
    private void configurarEstadoVisualInicial() {

        lblQuantidadeContas.setText("0 contas");
        lblValorListado.setText("R$ 0,00");
        lblValorPendente.setText("R$ 0,00");
        lblValorVencido.setText("R$ 0,00");

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

            FiltroRelatorioContaReceber filtro =
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
     * Confirma os textos dos DatePickers e constrói o filtro oficial.
     */
    private FiltroRelatorioContaReceber montarFiltro() {

        LocalDate dataInicial =
                confirmarTextoDatePicker(
                        dpDataInicial
                );

        LocalDate dataFinal =
                confirmarTextoDatePicker(
                        dpDataFinal
                );

        String clienteTexto =
                txtCliente.getText();

        SituacaoRelatorioContaReceber situacao =
                obterValorOpcaoSelecionada(
                        cbSituacao
                );

        return new FiltroRelatorioContaReceber(
                dataInicial,
                dataFinal,
                clienteTexto,
                situacao
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
            FiltroRelatorioContaReceber filtro,
            Integer usuarioId
    ) {

        if (!telaAtiva) {
            return;
        }

        invalidarConsultaAtual();

        long tokenDaConsulta =
                tokenConsultaAtual;

        Task<ResultadoRelatorioContaReceber>
                novaTask = new Task<>() {

            @Override
            protected ResultadoRelatorioContaReceber call() {
                return relatorioContaReceberService
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
                "relatorio-contas-receber-consulta-"
                        + tokenDaConsulta
        );

        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Configura os handlers da consulta com proteção por referência e token.
     */
    private void configurarHandlersConsulta(
            Task<ResultadoRelatorioContaReceber> task,
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

            ResultadoRelatorioContaReceber resultado =
                    task.getValue();

            if (resultado == null) {
                tratarFalhaTecnica(
                        task,
                        tokenDaConsulta,
                        new IllegalStateException(
                                "O Service não retornou o resultado "
                                        + "do relatório de contas a receber."
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
            Task<ResultadoRelatorioContaReceber> task,
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

        Task<ResultadoRelatorioContaReceber>
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
            Task<ResultadoRelatorioContaReceber> task,
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
        txtCliente.setDisable(bloquearConsultas);
        cbSituacao.setDisable(bloquearConsultas);

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
                    "Consultando contas a receber..."
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
            ResultadoRelatorioContaReceber resultado
    ) {

        if (resultado == null) {
            throw new IllegalArgumentException(
                    "Resultado do relatório não pode ser nulo."
            );
        }

        FiltroRelatorioContaReceber filtroAplicado =
                resultado.getFiltroAplicado();

        if (filtroAplicado == null) {
            throw new IllegalStateException(
                    "Filtro aplicado não retornado pelo resultado."
            );
        }

        LocalDate dataReferencia =
                resultado.getDataReferencia();

        if (dataReferencia == null) {
            throw new IllegalStateException(
                    "Data de referência não retornada pelo resultado."
            );
        }

        List<ContaReceberRelatorioView> novasContas =
                List.copyOf(
                        resultado.getContas()
                );

        int quantidade =
                resultado.getQuantidadeContas();

        String textoQuantidade =
                formatarQuantidadeContas(
                        quantidade
                );

        String textoValorListado =
                formatarValor(
                        resultado.getValorListado()
                );

        String textoValorPendente =
                formatarValor(
                        resultado.getValorPendente()
                );

        String textoValorVencido =
                formatarValor(
                        resultado.getValorVencido()
                );

        String textoFiltroAplicado =
                formatarFiltroAplicado(
                        filtroAplicado,
                        dataReferencia
                );

        boolean resultadoVazio =
                novasContas.isEmpty();

        String textoEstado =
                resultadoVazio
                        ? "Consulta concluída."
                        : "Relatório atualizado com sucesso.";

        Label placeholder =
                new Label(
                        MENSAGEM_RESULTADO_VAZIO
                );

        placeholder.setWrapText(true);

        contasExibidas.setAll(
                novasContas
        );

        lblQuantidadeContas.setText(
                textoQuantidade
        );

        lblValorListado.setText(
                textoValorListado
        );

        lblValorPendente.setText(
                textoValorPendente
        );

        lblValorVencido.setText(
                textoValorVencido
        );

        lblFiltroAplicado.setText(
                textoFiltroAplicado
        );

        lblEstadoConsulta.setText(
                textoEstado
        );

        tabelaContasReceber.setPlaceholder(
                placeholder
        );
    }

    /**
     * Monta o texto do período, filtros e data de referência efetivamente usados.
     */
    private String formatarFiltroAplicado(
            FiltroRelatorioContaReceber filtro,
            LocalDate dataReferencia
    ) {

        String cliente =
                filtro.getClienteTexto() == null
                        ? "Todos"
                        : filtro.getClienteTexto();

        return "Dados exibidos: "
                + filtro.getDataInicial()
                        .format(FORMATO_DATA)
                + " a "
                + filtro.getDataFinal()
                        .format(FORMATO_DATA)
                + " | Cliente: "
                + cliente
                + " | Situação: "
                + formatarSituacaoFiltro(
                        filtro.getSituacao()
                )
                + " | Referência: "
                + dataReferencia.format(FORMATO_DATA);
    }

    /**
     * Formata a quantidade de contas com singular ou plural.
     */
    private String formatarQuantidadeContas(
            int quantidade
    ) {

        return quantidade
                + (quantidade == 1
                ? " conta"
                : " contas");
    }

    /**
     * Formata a situação aplicada ao filtro.
     */
    private String formatarSituacaoFiltro(
            SituacaoRelatorioContaReceber situacao
    ) {

        if (situacao == null) {
            return "Todas";
        }

        return formatarSituacao(situacao);
    }

    /**
     * Trata uma falha de consulta sem alterar o último resultado válido.
     */
    private void tratarFalhaTecnica(
            Task<ResultadoRelatorioContaReceber> task,
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
                "[ERRO] Falha ao atualizar o relatório de contas a receber."
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
                "Não foi possível atualizar o relatório de contas a receber.\n"
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
     * Remove todos os dados protegidos da tela e da memória do Controller.
     */
    private void limparDadosProtegidos() {

        contasExibidas.clear();

        lblQuantidadeContas.setText("0 contas");
        lblValorListado.setText("R$ 0,00");
        lblValorPendente.setText("R$ 0,00");
        lblValorVencido.setText("R$ 0,00");

        lblFiltroAplicado.setText("");
        lblEstadoConsulta.setText(
                MENSAGEM_ACESSO_NEGADO
        );

        Label placeholder =
                new Label("Acesso negado.");

        tabelaContasReceber.setPlaceholder(
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
        limparDadosProtegidos();

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
     * Restaura os filtros visuais sem iniciar nova consulta e sem alterar o
     * último resultado exibido.
     */
    @FXML
    private void onLimparFiltros() {

        if (!telaAtiva) {
            return;
        }

        definirPeriodoInicial();
        txtCliente.clear();

        cbSituacao
                .getSelectionModel()
                .selectFirst();

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

            FiltroRelatorioContaReceber filtro =
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
     * Formata uma data para apresentação.
     */
    private String formatarData(
            LocalDate data
    ) {

        if (data == null) {
            return "—";
        }

        return data.format(FORMATO_DATA);
    }

    /**
     * Formata a situação gerencial da conta.
     */
    private String formatarSituacao(
            SituacaoRelatorioContaReceber situacao
    ) {

        if (situacao == null) {
            return "—";
        }

        return switch (situacao) {
            case A_VENCER -> "A vencer";
            case VENCIDA -> "Vencida";
            case PAGA -> "Paga";
            case CANCELADA -> "Cancelada";
        };
    }

    /**
     * Formata um valor monetário no padrão brasileiro.
     */
    private String formatarValor(
            BigDecimal valor
    ) {

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
