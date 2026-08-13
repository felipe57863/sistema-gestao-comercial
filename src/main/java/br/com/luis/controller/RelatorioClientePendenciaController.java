package br.com.luis.controller;

import br.com.luis.model.Cliente.StatusCliente;
import br.com.luis.model.Usuario;
import br.com.luis.service.RelatorioClientePendenciaService;
import br.com.luis.util.CabecalhoUtil;
import br.com.luis.util.NavegacaoUtil;
import br.com.luis.util.SessaoUsuario;
import br.com.luis.viewmodel.ClientePendenciaRelatorioView;
import br.com.luis.viewmodel.FiltroRelatorioClientePendencia;
import br.com.luis.viewmodel.ResultadoRelatorioClientePendencia;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Controller do relatório de clientes com pendências financeiras.
 *
 * Traduz filtros visuais para contratos imutáveis e executa uma consulta por
 * vez fora da JavaFX Application Thread. Autorização, agregação, ordenação e
 * totalização permanecem no Service e no DAO.
 */
public class RelatorioClientePendenciaController {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String MENSAGEM_ACESSO_NEGADO =
            "Usuário não autorizado a consultar clientes com pendências financeiras.";

    private static final String MENSAGEM_RESULTADO_VAZIO =
            "Nenhum cliente com pendências foi encontrado para os filtros informados.";

    @FXML private Label lblUsuario;
    @FXML private Label lblDataHora;
    @FXML private Button btnVoltar;
    @FXML private ProgressIndicator progressoRelatorio;
    @FXML private Label lblEstadoConsulta;
    @FXML private Label lblPosicao;

    @FXML private TextField txtCliente;
    @FXML private ComboBox<OpcaoFiltro<StatusCliente>> cbStatusCliente;
    @FXML private ComboBox<OpcaoFiltro<Boolean>> cbPendencia;
    @FXML private Button btnFiltrar;

    @FXML private TableView<ClientePendenciaRelatorioView> tabelaClientes;
    @FXML private TableColumn<ClientePendenciaRelatorioView, Integer> colClienteId;
    @FXML private TableColumn<ClientePendenciaRelatorioView, String> colClienteNome;
    @FXML private TableColumn<ClientePendenciaRelatorioView, String> colDocumento;
    @FXML private TableColumn<ClientePendenciaRelatorioView, String> colStatusCliente;
    @FXML private TableColumn<ClientePendenciaRelatorioView, Integer> colContasPendentes;
    @FXML private TableColumn<ClientePendenciaRelatorioView, BigDecimal> colValorPendente;
    @FXML private TableColumn<ClientePendenciaRelatorioView, Integer> colContasVencidas;
    @FXML private TableColumn<ClientePendenciaRelatorioView, BigDecimal> colValorVencido;

    @FXML private Label lblQuantidadeClientes;
    @FXML private Label lblValorTotalPendente;
    @FXML private Label lblClientesComVencidas;
    @FXML private Label lblValorTotalVencido;

    private final RelatorioClientePendenciaService relatorioService;
    private final ObservableList<ClientePendenciaRelatorioView> clientesExibidos;
    private final NumberFormat formatadorMoeda;

    private Task<ResultadoRelatorioClientePendencia> tarefaConsultaAtual;
    private boolean telaAtiva;

    /**
     * Inicializa dependências sem consultar o banco antes do carregamento do FXML.
     */
    public RelatorioClientePendenciaController() {
        this.relatorioService = new RelatorioClientePendenciaService();
        this.clientesExibidos = FXCollections.observableArrayList();
        this.formatadorMoeda = NumberFormat.getCurrencyInstance(
                new Locale("pt", "BR")
        );
        this.tarefaConsultaAtual = null;
        this.telaAtiva = true;
    }

    /**
     * Configura a tela e agenda a consulta automática com Todos/Todas.
     */
    @FXML
    public void initialize() {
        CabecalhoUtil.configurarUsuarioEDataHora(
                lblUsuario,
                lblDataHora
        );

        configurarCombos();
        configurarTabela();
        configurarEstadoVisualInicial();

        Platform.runLater(this::consultarPelosFiltrosAtuais);
    }

    private void configurarCombos() {
        cbStatusCliente.getItems().setAll(
                new OpcaoFiltro<>("Todos", null),
                new OpcaoFiltro<>("Ativos", StatusCliente.ATIVO),
                new OpcaoFiltro<>("Bloqueados", StatusCliente.BLOQUEADO)
        );

        cbPendencia.getItems().setAll(
                new OpcaoFiltro<>("Todas", null),
                new OpcaoFiltro<>("Com vencidas", Boolean.TRUE),
                new OpcaoFiltro<>("Sem vencidas", Boolean.FALSE)
        );

        cbStatusCliente.getSelectionModel().selectFirst();
        cbPendencia.getSelectionModel().selectFirst();
    }

    /**
     * Configura as oito colunas sem recalcular dados consolidados.
     */
    private void configurarTabela() {
        colClienteId.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getClienteId()
                )
        );

        colClienteNome.setCellValueFactory(
                celula -> new SimpleStringProperty(
                        celula.getValue().getNome()
                )
        );

        colDocumento.setCellValueFactory(
                celula -> new SimpleStringProperty(
                        celula.getValue().getDocumento()
                )
        );

        colStatusCliente.setCellValueFactory(
                celula -> new SimpleStringProperty(
                        formatarStatus(celula.getValue().getStatusCliente())
                )
        );

        colContasPendentes.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getQuantidadeContasPendentes()
                )
        );

        colValorPendente.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getValorPendente()
                )
        );

        colContasVencidas.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getQuantidadeContasVencidas()
                )
        );

        colValorVencido.setCellValueFactory(
                celula -> new ReadOnlyObjectWrapper<>(
                        celula.getValue().getValorVencido()
                )
        );

        configurarColunaMonetaria(colValorPendente);
        configurarColunaMonetaria(colValorVencido);

        tabelaClientes.setItems(clientesExibidos);
        tabelaClientes.setPlaceholder(
                criarPlaceholder("Relatório de pendências ainda não carregado.")
        );
    }

    private void configurarColunaMonetaria(
            TableColumn<ClientePendenciaRelatorioView, BigDecimal> coluna
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
        lblQuantidadeClientes.setText("0");
        lblValorTotalPendente.setText("R$ 0,00");
        lblClientesComVencidas.setText("0");
        lblValorTotalVencido.setText("R$ 0,00");
        lblPosicao.setText("Posição em: —");
        lblEstadoConsulta.setText("Relatório pronto para consulta.");

        progressoRelatorio.setVisible(false);
        progressoRelatorio.setManaged(false);
        btnFiltrar.setDisable(true);
    }

    private FiltroRelatorioClientePendencia montarFiltro() {
        return new FiltroRelatorioClientePendencia(
                txtCliente.getText(),
                obterValorSelecionado(cbStatusCliente),
                obterValorSelecionado(cbPendencia)
        );
    }

    private <T> T obterValorSelecionado(ComboBox<OpcaoFiltro<T>> comboBox) {
        OpcaoFiltro<T> opcao = comboBox.getValue();
        return opcao == null ? null : opcao.getValor();
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

    private void consultarPelosFiltrosAtuais() {
        if (!telaAtiva || tarefaConsultaAtual != null) {
            return;
        }

        try {
            Integer usuarioId = obterUsuarioIdAutorizadoVisualmente();
            FiltroRelatorioClientePendencia filtro = montarFiltro();
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
            FiltroRelatorioClientePendencia filtro,
            Integer usuarioId
    ) {
        Task<ResultadoRelatorioClientePendencia> tarefa = new Task<>() {
            @Override
            protected ResultadoRelatorioClientePendencia call() {
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

        Thread thread = new Thread(
                tarefa,
                "relatorio-clientes-pendencias"
        );
        thread.setDaemon(true);
        thread.start();
    }

    private boolean tarefaEhAtual(
            Task<ResultadoRelatorioClientePendencia> tarefa
    ) {
        return telaAtiva
                && tarefaConsultaAtual == tarefa
                && !tarefa.isCancelled();
    }

    private void finalizarConsulta(
            Task<ResultadoRelatorioClientePendencia> tarefa
    ) {
        if (tarefaConsultaAtual != tarefa) {
            return;
        }

        tarefaConsultaAtual = null;
        configurarEstadoConsulta(false);
    }

    private void cancelarConsultaAtual() {
        Task<ResultadoRelatorioClientePendencia> tarefaAnterior =
                tarefaConsultaAtual;

        tarefaConsultaAtual = null;

        if (tarefaAnterior != null && !tarefaAnterior.isDone()) {
            tarefaAnterior.cancel(true);
        }

        configurarEstadoConsulta(false);
    }

    /**
     * Impede consultas simultâneas e mantém Voltar acessível.
     */
    private void configurarEstadoConsulta(boolean consultando) {
        boolean bloquear = consultando || !telaAtiva;

        txtCliente.setDisable(bloquear);
        cbStatusCliente.setDisable(bloquear);
        cbPendencia.setDisable(bloquear);
        btnFiltrar.setDisable(bloquear);
        btnVoltar.setDisable(false);

        boolean mostrarProgresso = consultando && telaAtiva;
        progressoRelatorio.setVisible(mostrarProgresso);
        progressoRelatorio.setManaged(mostrarProgresso);

        if (mostrarProgresso) {
            lblEstadoConsulta.setText(
                    "Consultando clientes com pendências..."
            );
        }
    }

    /**
     * Aplica linhas, data e cards exclusivamente a partir do resultado.
     */
    private void aplicarResultado(
            ResultadoRelatorioClientePendencia resultado
    ) {
        if (resultado == null) {
            throw new IllegalStateException(
                    "O Service não retornou o resultado do relatório de pendências."
            );
        }

        List<ClientePendenciaRelatorioView> clientes =
                List.copyOf(resultado.getClientes());

        LocalDate dataReferencia = resultado.getDataReferencia();

        if (dataReferencia == null) {
            throw new IllegalStateException(
                    "O resultado não informou a data de referência."
            );
        }

        String quantidadeClientes =
                Integer.toString(resultado.getQuantidadeClientes());

        String valorTotalPendente =
                formatarMoeda(resultado.getValorTotalPendente());

        String clientesComVencidas =
                Integer.toString(resultado.getQuantidadeClientesComVencidas());

        String valorTotalVencido =
                formatarMoeda(resultado.getValorTotalVencido());

        String posicao =
                "Posição em: " + dataReferencia.format(FORMATO_DATA);

        clientesExibidos.setAll(clientes);
        lblQuantidadeClientes.setText(quantidadeClientes);
        lblValorTotalPendente.setText(valorTotalPendente);
        lblClientesComVencidas.setText(clientesComVencidas);
        lblValorTotalVencido.setText(valorTotalVencido);
        lblPosicao.setText(posicao);

        tabelaClientes.setPlaceholder(
                criarPlaceholder(MENSAGEM_RESULTADO_VAZIO)
        );

        lblEstadoConsulta.setText(
                clientes.isEmpty()
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
     * Mantém o último resultado válido quando a atualização falha.
     */
    private void tratarFalhaConsulta(
            Task<ResultadoRelatorioClientePendencia> tarefa,
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
                "[ERRO] Falha ao atualizar clientes com pendências."
        );
        causaEfetiva.printStackTrace();

        finalizarConsulta(tarefa);
        lblEstadoConsulta.setText(
                "Não foi possível atualizar o relatório de pendências."
        );

        mostrarAlerta(
                Alert.AlertType.ERROR,
                "Erro",
                "Não foi possível atualizar o relatório de pendências.\n"
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
        clientesExibidos.clear();
        lblQuantidadeClientes.setText("0");
        lblValorTotalPendente.setText("R$ 0,00");
        lblClientesComVencidas.setText("0");
        lblValorTotalVencido.setText("R$ 0,00");
        lblPosicao.setText("Posição em: —");
        tabelaClientes.setPlaceholder(criarPlaceholder("Acesso negado."));
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

    private String formatarStatus(StatusCliente statusCliente) {
        if (statusCliente == null) {
            return "—";
        }

        return switch (statusCliente) {
            case ATIVO -> "Ativo";
            case BLOQUEADO -> "Bloqueado";
        };
    }

    private String formatarMoeda(BigDecimal valor) {
        if (valor == null) {
            return "—";
        }

        return formatadorMoeda
                .format(valor)
                .replace('\u00A0', ' ');
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
     * Associa um rótulo visual a um valor opcional de filtro.
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
