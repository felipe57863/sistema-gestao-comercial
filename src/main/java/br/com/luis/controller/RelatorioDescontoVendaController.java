package br.com.luis.controller;

import br.com.luis.model.TipoVenda;
import br.com.luis.model.Usuario;
import br.com.luis.service.RelatorioDescontoVendaService;
import br.com.luis.util.CabecalhoUtil;
import br.com.luis.util.NavegacaoUtil;
import br.com.luis.util.SessaoUsuario;
import br.com.luis.viewmodel.FiltroRelatorioDescontoVenda;
import br.com.luis.viewmodel.ResultadoRelatorioDescontoVenda;
import br.com.luis.viewmodel.VendaDescontoRelatorioView;
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
 * Controller do relatório de descontos concedidos.
 *
 * Fotografa filtros visuais, executa uma consulta por vez por meio do Service e
 * apenas formata os valores já consolidados. Não acessa DAO, Connection ou SQL e
 * não reconstrói descontos históricos.
 */
public class RelatorioDescontoVendaController {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter FORMATO_DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final String MENSAGEM_ACESSO_NEGADO =
            "Usuário não autorizado a consultar o relatório de descontos concedidos.";

    private static final String MENSAGEM_RESULTADO_VAZIO =
            "Nenhuma venda com desconto foi encontrada para os filtros informados.";

    @FXML private Label lblUsuario;
    @FXML private Label lblDataHora;
    @FXML private Button btnVoltar;

    @FXML private DatePicker dpDataInicial;
    @FXML private DatePicker dpDataFinal;
    @FXML private ComboBox<OpcaoFiltro<TipoVenda>> cbTipoVenda;
    @FXML private Button btnFiltrar;
    @FXML private ProgressIndicator progressoRelatorio;
    @FXML private Label lblEstadoConsulta;

    @FXML private TableView<VendaDescontoRelatorioView> tabelaVendas;
    @FXML private TableColumn<VendaDescontoRelatorioView, Integer> colVendaId;
    @FXML private TableColumn<VendaDescontoRelatorioView, LocalDateTime> colDataHoraVenda;
    @FXML private TableColumn<VendaDescontoRelatorioView, String> colCliente;
    @FXML private TableColumn<VendaDescontoRelatorioView, TipoVenda> colTipoVenda;
    @FXML private TableColumn<VendaDescontoRelatorioView, BigDecimal> colValorBruto;
    @FXML private TableColumn<VendaDescontoRelatorioView, BigDecimal> colDescontoPromocional;
    @FXML private TableColumn<VendaDescontoRelatorioView, BigDecimal> colDescontoGlobal;
    @FXML private TableColumn<VendaDescontoRelatorioView, BigDecimal> colDescontoTotal;
    @FXML private TableColumn<VendaDescontoRelatorioView, BigDecimal> colValorLiquido;

    @FXML private Label lblQuantidadeVendas;
    @FXML private Label lblTotalDescontoPromocional;
    @FXML private Label lblTotalDescontoGlobal;
    @FXML private Label lblTotalDescontos;

    private final RelatorioDescontoVendaService relatorioService;
    private final ObservableList<VendaDescontoRelatorioView> vendasExibidas;
    private final NumberFormat formatadorMoeda;

    private Task<ResultadoRelatorioDescontoVenda> tarefaConsultaAtual;
    private boolean telaAtiva;

    /**
     * Inicializa dependências sem consultar o banco antes de carregar o FXML.
     */
    public RelatorioDescontoVendaController() {
        this.relatorioService = new RelatorioDescontoVendaService();
        this.vendasExibidas = FXCollections.observableArrayList();
        this.formatadorMoeda = NumberFormat.getCurrencyInstance(
                new Locale("pt", "BR")
        );
        this.tarefaConsultaAtual = null;
        this.telaAtiva = true;
    }

    /**
     * Configura a tela e agenda a consulta automática do mês atual.
     */
    @FXML
    public void initialize() {
        CabecalhoUtil.configurarUsuarioEDataHora(lblUsuario, lblDataHora);
        configurarDatePickers();
        definirPeriodoInicial();
        configurarComboTipoVenda();
        configurarTabela();
        configurarEstadoVisualInicial();

        Platform.runLater(this::consultarPelosFiltrosAtuais);
    }

    private void configurarDatePickers() {
        StringConverter<LocalDate> conversorData = criarConversorData();

        dpDataInicial.setConverter(conversorData);
        dpDataFinal.setConverter(conversorData);
        dpDataInicial.setPromptText("dd/MM/yyyy");
        dpDataFinal.setPromptText("dd/MM/yyyy");
    }

    private StringConverter<LocalDate> criarConversorData() {
        return new StringConverter<>() {
            @Override
            public String toString(LocalDate data) {
                return data == null ? "" : data.format(FORMATO_DATA);
            }

            @Override
            public LocalDate fromString(String texto) {
                if (texto == null || texto.isBlank()) {
                    return null;
                }

                try {
                    return LocalDate.parse(texto.trim(), FORMATO_DATA);

                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException(
                            "Data inválida. Utilize o formato dd/MM/yyyy.",
                            e
                    );
                }
            }
        };
    }

    private void definirPeriodoInicial() {
        LocalDate hoje = LocalDate.now();
        dpDataInicial.setValue(hoje.withDayOfMonth(1));
        dpDataFinal.setValue(hoje);
    }

    private void configurarComboTipoVenda() {
        cbTipoVenda.getItems().setAll(
                new OpcaoFiltro<>("Todas", null),
                new OpcaoFiltro<>("À vista", TipoVenda.A_VISTA),
                new OpcaoFiltro<>("A prazo", TipoVenda.A_PRAZO)
        );

        cbTipoVenda.getSelectionModel().selectFirst();
    }

    /**
     * Configura as nove colunas sem executar cálculos financeiros.
     */
    private void configurarTabela() {
        colVendaId.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getVendaId()
                )
        );

        colDataHoraVenda.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getDataHora()
                )
        );

        colCliente.setCellValueFactory(
                celula -> new SimpleStringProperty(
                        celula.getValue().getCliente()
                )
        );

        colTipoVenda.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getTipoVenda()
                )
        );

        colValorBruto.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getValorBruto()
                )
        );
        colDescontoPromocional.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getDescontoPromocional()
                )
        );
        colDescontoGlobal.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getDescontoGlobal()
                )
        );
        colDescontoTotal.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getDescontoTotal()
                )
        );
        colValorLiquido.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getValorLiquido()
                )
        );

        configurarColunaDataHora();
        configurarColunaTipoVenda();
        configurarColunaMonetaria(colValorBruto);
        configurarColunaMonetaria(colDescontoPromocional);
        configurarColunaMonetaria(colDescontoGlobal);
        configurarColunaMonetaria(colDescontoTotal);
        configurarColunaMonetaria(colValorLiquido);

        tabelaVendas.setItems(vendasExibidas);
        tabelaVendas.setPlaceholder(
                criarPlaceholder("Relatório de descontos ainda não carregado.")
        );
    }

    private void configurarColunaDataHora() {
        colDataHoraVenda.setCellFactory(
                coluna -> new TableCell<>() {
                    @Override
                    protected void updateItem(
                            LocalDateTime dataHora,
                            boolean empty
                    ) {
                        super.updateItem(dataHora, empty);
                        setText(
                                empty || dataHora == null
                                        ? null
                                        : dataHora.format(FORMATO_DATA_HORA)
                        );
                    }
                }
        );
    }

    private void configurarColunaTipoVenda() {
        colTipoVenda.setCellFactory(
                coluna -> new TableCell<>() {
                    @Override
                    protected void updateItem(TipoVenda tipoVenda, boolean empty) {
                        super.updateItem(tipoVenda, empty);
                        setText(empty ? null : formatarTipoVenda(tipoVenda));
                    }
                }
        );
    }

    private void configurarColunaMonetaria(
            TableColumn<VendaDescontoRelatorioView, BigDecimal> coluna
    ) {
        coluna.setCellFactory(
                colunaTabela -> new TableCell<>() {
                    @Override
                    protected void updateItem(BigDecimal valor, boolean empty) {
                        super.updateItem(valor, empty);
                        setText(empty ? null : formatarMoeda(valor));
                        setStyle("-fx-alignment: CENTER-RIGHT;");
                    }
                }
        );
    }

    private void configurarEstadoVisualInicial() {
        lblQuantidadeVendas.setText("0");
        lblTotalDescontoPromocional.setText("R$ 0,00");
        lblTotalDescontoGlobal.setText("R$ 0,00");
        lblTotalDescontos.setText("R$ 0,00");
        lblEstadoConsulta.setText("Relatório pronto para consulta.");

        progressoRelatorio.setVisible(false);
        progressoRelatorio.setManaged(false);
        btnFiltrar.setDisable(true);
    }

    private FiltroRelatorioDescontoVenda montarFiltro() {
        LocalDate dataInicial = confirmarTextoDatePicker(dpDataInicial);
        LocalDate dataFinal = confirmarTextoDatePicker(dpDataFinal);
        TipoVenda tipoVenda = obterTipoVendaSelecionado();

        return new FiltroRelatorioDescontoVenda(
                dataInicial,
                dataFinal,
                tipoVenda
        );
    }

    private LocalDate confirmarTextoDatePicker(DatePicker datePicker) {
        String texto = datePicker.getEditor().getText();
        LocalDate data = datePicker.getConverter().fromString(texto);
        datePicker.setValue(data);
        return data;
    }

    private TipoVenda obterTipoVendaSelecionado() {
        OpcaoFiltro<TipoVenda> opcao = cbTipoVenda.getValue();
        return opcao == null ? null : opcao.getValor();
    }

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

    private void consultarPelosFiltrosAtuais() {
        if (!telaAtiva || tarefaConsultaAtual != null) {
            return;
        }

        try {
            Integer usuarioId = obterUsuarioIdAutorizadoVisualmente();
            FiltroRelatorioDescontoVenda filtro = montarFiltro();
            iniciarConsulta(filtro, usuarioId);

        } catch (SecurityException e) {
            tratarAcessoNegado();

        } catch (IllegalArgumentException e) {
            tratarFiltroInvalido(e);
        }
    }

    /**
     * Executa a consulta fora da JavaFX Application Thread.
     */
    private void iniciarConsulta(
            FiltroRelatorioDescontoVenda filtro,
            Integer usuarioId
    ) {
        Task<ResultadoRelatorioDescontoVenda> tarefa = new Task<>() {
            @Override
            protected ResultadoRelatorioDescontoVenda call() {
                return relatorioService.consultar(filtro, usuarioId);
            }
        };

        tarefaConsultaAtual = tarefa;
        configurarEstadoConsulta(true);

        tarefa.setOnSucceeded(event -> {
            if (!tarefaEhAtual(tarefa)) {
                return;
            }

            try {
                aplicarResultado(tarefa.getValue());
                finalizarConsulta(tarefa);

            } catch (RuntimeException e) {
                tratarFalhaConsulta(tarefa, e);
            }
        });

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

        Thread thread = new Thread(tarefa, "relatorio-descontos-vendas");
        thread.setDaemon(true);
        thread.start();
    }

    private boolean tarefaEhAtual(
            Task<ResultadoRelatorioDescontoVenda> tarefa
    ) {
        return telaAtiva
                && tarefaConsultaAtual == tarefa
                && !tarefa.isCancelled();
    }

    private void finalizarConsulta(
            Task<ResultadoRelatorioDescontoVenda> tarefa
    ) {
        if (tarefaConsultaAtual != tarefa) {
            return;
        }

        tarefaConsultaAtual = null;
        configurarEstadoConsulta(false);
    }

    private void cancelarConsultaAtual() {
        Task<ResultadoRelatorioDescontoVenda> tarefaAnterior =
                tarefaConsultaAtual;

        tarefaConsultaAtual = null;

        if (tarefaAnterior != null && !tarefaAnterior.isDone()) {
            tarefaAnterior.cancel(true);
        }

        configurarEstadoConsulta(false);
    }

    private void configurarEstadoConsulta(boolean consultando) {
        boolean bloquear = consultando || !telaAtiva;

        dpDataInicial.setDisable(bloquear);
        dpDataFinal.setDisable(bloquear);
        cbTipoVenda.setDisable(bloquear);
        btnFiltrar.setDisable(bloquear);
        btnVoltar.setDisable(false);

        boolean mostrarProgresso = consultando && telaAtiva;
        progressoRelatorio.setVisible(mostrarProgresso);
        progressoRelatorio.setManaged(mostrarProgresso);

        if (mostrarProgresso) {
            lblEstadoConsulta.setText("Consultando descontos concedidos...");
        }
    }

    /**
     * Aplica linhas e cards exclusivamente a partir do resultado consolidado.
     */
    private void aplicarResultado(ResultadoRelatorioDescontoVenda resultado) {
        if (resultado == null) {
            throw new IllegalStateException(
                    "O Service não retornou o resultado do relatório de descontos."
            );
        }

        List<VendaDescontoRelatorioView> vendas =
                List.copyOf(resultado.getVendas());

        String quantidadeVendas =
                Integer.toString(resultado.getQuantidadeVendas());
        String totalPromocional =
                formatarMoeda(resultado.getTotalDescontoPromocional());
        String totalGlobal =
                formatarMoeda(resultado.getTotalDescontoGlobal());
        String totalDescontos =
                formatarMoeda(resultado.getTotalDescontos());

        vendasExibidas.setAll(vendas);
        lblQuantidadeVendas.setText(quantidadeVendas);
        lblTotalDescontoPromocional.setText(totalPromocional);
        lblTotalDescontoGlobal.setText(totalGlobal);
        lblTotalDescontos.setText(totalDescontos);
        tabelaVendas.setPlaceholder(criarPlaceholder(MENSAGEM_RESULTADO_VAZIO));

        lblEstadoConsulta.setText(
                vendas.isEmpty()
                        ? "Consulta concluída."
                        : "Relatório atualizado com sucesso."
        );
    }

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
     * Mantém o último resultado válido quando uma atualização falha.
     */
    private void tratarFalhaConsulta(
            Task<ResultadoRelatorioDescontoVenda> tarefa,
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
                "[ERRO] Falha ao atualizar o relatório de descontos concedidos."
        );
        causaEfetiva.printStackTrace();

        finalizarConsulta(tarefa);
        lblEstadoConsulta.setText(
                "Não foi possível atualizar o relatório de descontos."
        );

        mostrarAlerta(
                Alert.AlertType.ERROR,
                "Erro",
                "Não foi possível atualizar o relatório de descontos.\n"
                        + "Os dados já exibidos foram mantidos."
        );
    }

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

    private void tratarAcessoNegado() {
        telaAtiva = false;
        cancelarConsultaAtual();
        vendasExibidas.clear();
        lblQuantidadeVendas.setText("0");
        lblTotalDescontoPromocional.setText("R$ 0,00");
        lblTotalDescontoGlobal.setText("R$ 0,00");
        lblTotalDescontos.setText("R$ 0,00");
        tabelaVendas.setPlaceholder(criarPlaceholder("Acesso negado."));
        lblEstadoConsulta.setText(MENSAGEM_ACESSO_NEGADO);

        mostrarAlerta(
                Alert.AlertType.WARNING,
                "Acesso negado",
                MENSAGEM_ACESSO_NEGADO
        );

        abrirTelaPrincipal(false);
    }

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
                configurarEstadoConsulta(false);
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

    @FXML
    private void onFiltrar() {
        consultarPelosFiltrosAtuais();
    }

    @FXML
    private void onVoltar() {
        boolean reativarEmFalha = telaAtiva;
        telaAtiva = false;
        cancelarConsultaAtual();
        abrirTelaPrincipal(reativarEmFalha);
    }

    private Label criarPlaceholder(String mensagem) {
        Label placeholder = new Label(mensagem);
        placeholder.setWrapText(true);
        return placeholder;
    }

    private String formatarTipoVenda(TipoVenda tipoVenda) {
        if (tipoVenda == null) {
            return "—";
        }

        return switch (tipoVenda) {
            case A_VISTA -> "À vista";
            case A_PRAZO -> "A prazo";
        };
    }

    private String formatarMoeda(BigDecimal valor) {
        if (valor == null) {
            return "—";
        }

        return formatadorMoeda.format(valor).replace('\u00A0', ' ');
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

    /**
     * Associa um rótulo visual ao tipo opcional de venda.
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
