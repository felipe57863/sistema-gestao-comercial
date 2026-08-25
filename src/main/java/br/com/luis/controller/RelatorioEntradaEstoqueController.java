package br.com.luis.controller;

import br.com.luis.model.Produto;
import br.com.luis.model.Usuario;
import br.com.luis.service.GestaoUsuarioService;
import br.com.luis.service.ProdutoService;
import br.com.luis.service.RelatorioEntradaEstoqueService;
import br.com.luis.util.CabecalhoUtil;
import br.com.luis.util.NavegacaoUtil;
import br.com.luis.util.SessaoUsuario;
import br.com.luis.viewmodel.EntradaEstoqueRelatorioView;
import br.com.luis.viewmodel.FiltroRelatorioEntradaEstoque;
import br.com.luis.viewmodel.ItemEntradaEstoqueRelatorioView;
import br.com.luis.viewmodel.ResultadoRelatorioEntradaEstoque;
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
import javafx.scene.control.TextFormatter;
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
 * Controller do relatório histórico e somente leitura de entradas de estoque.
 */
public class RelatorioEntradaEstoqueController {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final String MENSAGEM_ACESSO_NEGADO =
            "Usuário não autorizado a consultar o relatório de entradas de estoque.";
    private static final String MENSAGEM_RESULTADO_VAZIO =
            "Nenhuma entrada de estoque foi encontrada para os filtros informados.";

    @FXML private Label lblUsuario;
    @FXML private Label lblDataHora;
    @FXML private Button btnVoltar;
    @FXML private DatePicker dpDataInicial;
    @FXML private DatePicker dpDataFinal;
    @FXML private TextField txtNumeroEntrada;
    @FXML private ComboBox<OpcaoFiltro<Usuario>> cbResponsavel;
    @FXML private ComboBox<OpcaoFiltro<Produto>> cbProduto;
    @FXML private TextField txtReferencia;
    @FXML private Button btnFiltrar;
    @FXML private Button btnLimparFiltros;
    @FXML private ProgressIndicator progressoRelatorio;
    @FXML private Label lblEstadoConsulta;
    @FXML private Label lblFiltroAplicado;

    @FXML private TableView<EntradaEstoqueRelatorioView> tabelaEntradas;
    @FXML private TableColumn<EntradaEstoqueRelatorioView, Integer> colNumero;
    @FXML private TableColumn<EntradaEstoqueRelatorioView, LocalDateTime> colDataHoraEntrada;
    @FXML private TableColumn<EntradaEstoqueRelatorioView, String> colResponsavel;
    @FXML private TableColumn<EntradaEstoqueRelatorioView, String> colReferencia;
    @FXML private TableColumn<EntradaEstoqueRelatorioView, Integer> colProdutos;
    @FXML private TableColumn<EntradaEstoqueRelatorioView, Integer> colUnidades;
    @FXML private TableColumn<EntradaEstoqueRelatorioView, BigDecimal> colValorTotal;

    @FXML private Label lblQuantidadeEntradas;
    @FXML private Label lblTotalUnidades;
    @FXML private Label lblValorTotal;
    @FXML private Label lblObservacao;
    @FXML private TableView<ItemEntradaEstoqueRelatorioView> tabelaItens;
    @FXML private TableColumn<ItemEntradaEstoqueRelatorioView, String> colProdutoDetalhe;
    @FXML private TableColumn<ItemEntradaEstoqueRelatorioView, Integer> colQuantidadeDetalhe;
    @FXML private TableColumn<ItemEntradaEstoqueRelatorioView, BigDecimal> colPrecoCompraDetalhe;
    @FXML private TableColumn<ItemEntradaEstoqueRelatorioView, BigDecimal> colSubtotalDetalhe;

    private final ObservableList<EntradaEstoqueRelatorioView> entradasExibidas =
            FXCollections.observableArrayList();
    private final ObservableList<ItemEntradaEstoqueRelatorioView> itensExibidos =
            FXCollections.observableArrayList();
    private final RelatorioEntradaEstoqueService relatorioService =
            new RelatorioEntradaEstoqueService();
    private final GestaoUsuarioService gestaoUsuarioService =
            new GestaoUsuarioService();
    private final ProdutoService produtoService = new ProdutoService();

    private Task<OpcoesConsulta> taskOpcoesAtual;
    private Task<ResultadoRelatorioEntradaEstoque> taskConsultaAtual;
    private Task<List<ItemEntradaEstoqueRelatorioView>> taskDetalheAtual;
    private long tokenConsultaAtual;
    private long tokenDetalheAtual;
    private ResultadoRelatorioEntradaEstoque ultimoResultadoValido;
    private boolean telaAtiva = true;

    @FXML
    public void initialize() {
        CabecalhoUtil.configurarUsuarioEDataHora(lblUsuario, lblDataHora);
        configurarDatePickers();
        configurarNumeroEntrada();
        configurarTabelas();
        definirPeriodoInicial();
        configurarEstadoInicial();
        Platform.runLater(this::iniciarFluxoInicial);
    }

    private void configurarDatePickers() {
        StringConverter<LocalDate> converter = new StringConverter<>() {
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
                            "Data inválida. Utilize o formato dd/MM/yyyy.", e
                    );
                }
            }
        };
        dpDataInicial.setConverter(converter);
        dpDataFinal.setConverter(converter);
        dpDataInicial.setPromptText("dd/MM/yyyy");
        dpDataFinal.setPromptText("dd/MM/yyyy");
    }

    private void configurarNumeroEntrada() {
        txtNumeroEntrada.setTextFormatter(
                new TextFormatter<>(mudanca ->
                        mudanca.getControlNewText().matches("[0-9]*")
                                ? mudanca : null)
        );
    }

    private void definirPeriodoInicial() {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = hoje.withDayOfMonth(hoje.lengthOfMonth());

        dpDataInicial.setValue(inicioMes);
        dpDataFinal.setValue(fimMes);
        dpDataInicial.getEditor().setText(inicioMes.format(FORMATO_DATA));
        dpDataFinal.getEditor().setText(fimMes.format(FORMATO_DATA));
    }

    private void configurarTabelas() {
        colNumero.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getIdEntrada()));
        colDataHoraEntrada.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getDataHora()));
        colResponsavel.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getNomeUsuario()));
        colReferencia.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getReferencia()));
        colProdutos.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(
                        c.getValue().getQuantidadeProdutosDistintos()));
        colUnidades.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getTotalUnidades()));
        colValorTotal.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getValorTotal()));

        colDataHoraEntrada.setCellFactory(coluna -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime valor, boolean vazio) {
                super.updateItem(valor, vazio);
                setText(vazio || valor == null
                        ? null : valor.format(FORMATO_DATA_HORA));
            }
        });
        colReferencia.setCellFactory(coluna -> new TableCell<>() {
            @Override
            protected void updateItem(String valor, boolean vazio) {
                super.updateItem(valor, vazio);
                setText(vazio ? null : valor == null ? "-" : valor);
            }
        });
        configurarColunaMonetaria(colValorTotal);

        colProdutoDetalhe.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(
                        c.getValue().getDescricaoProduto()));
        colQuantidadeDetalhe.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(
                        c.getValue().getQuantidadeRecebida()));
        colPrecoCompraDetalhe.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(
                        c.getValue().getPrecoCompraUnitario()));
        colSubtotalDetalhe.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getSubtotal()));
        configurarColunaMonetaria(colPrecoCompraDetalhe);
        configurarColunaMonetaria(colSubtotalDetalhe);

        tabelaEntradas.setItems(entradasExibidas);
        tabelaItens.setItems(itensExibidos);
        tabelaEntradas.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, atual) ->
                        selecionarEntrada(atual));
    }

    private <T> void configurarColunaMonetaria(
            TableColumn<T, BigDecimal> coluna
    ) {
        coluna.setCellFactory(ignorada -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal valor, boolean vazio) {
                super.updateItem(valor, vazio);
                setText(vazio || valor == null ? null : formatarMoeda(valor));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });
    }

    private void configurarEstadoInicial() {
        cbResponsavel.getItems().setAll(new OpcaoFiltro<>("Todos", null));
        cbProduto.getItems().setAll(new OpcaoFiltro<>("Todos", null));
        cbResponsavel.getSelectionModel().selectFirst();
        cbProduto.getSelectionModel().selectFirst();
        lblQuantidadeEntradas.setText("0 entradas");
        lblTotalUnidades.setText("0 unidades");
        lblValorTotal.setText("R$ 0,00");
        lblObservacao.setText(
                "Selecione uma entrada para visualizar a observação.");
        lblFiltroAplicado.setText("");
        lblEstadoConsulta.setText("Carregando opções de filtro...");
        tabelaEntradas.setPlaceholder(
                criarPlaceholder("Relatório ainda não carregado."));
        tabelaItens.setPlaceholder(
                criarPlaceholder("Selecione uma entrada para visualizar os itens."));
        atualizarEstadoControles();
    }

    private void iniciarFluxoInicial() {
        if (!telaAtiva) {
            return;
        }
        try {
            iniciarCarregamentoOpcoes(
                    obterAdministradorAutorizadoVisualmente());
        } catch (SecurityException e) {
            tratarAcessoNegado();
        }
    }

    private Usuario obterAdministradorAutorizadoVisualmente() {
        Usuario usuario = SessaoUsuario.getInstance().getUsuarioLogado();
        boolean autorizado = usuario != null
                && usuario.getIdUsuario() != null
                && usuario.getIdUsuario() > 0
                && "ADMIN".equals(usuario.getPerfil())
                && "ATIVO".equals(usuario.getStatus())
                && !usuario.isTrocaSenhaObrigatoria();

        if (!autorizado) {
            throw new SecurityException(MENSAGEM_ACESSO_NEGADO);
        }
        return usuario;
    }
    private void iniciarCarregamentoOpcoes(Usuario administrador) {
        invalidarTaskOpcoes();
        Task<OpcoesConsulta> task = new Task<>() {
            @Override
            protected OpcoesConsulta call() {
                List<Usuario> usuarios =
                        gestaoUsuarioService.listarUsuarios(administrador);
                List<Produto> produtos = produtoService.listarTodos();
                if (usuarios == null || produtos == null) {
                    throw new IllegalStateException(
                            "O carregamento de opções retornou lista nula.");
                }
                return new OpcoesConsulta(usuarios, produtos);
            }
        };

        taskOpcoesAtual = task;
        atualizarEstadoControles();

        task.setOnSucceeded(event -> {
            if (!telaAtiva || taskOpcoesAtual != task || task.isCancelled()) {
                return;
            }
            taskOpcoesAtual = null;
            try {
                aplicarOpcoes(task.getValue());
                Usuario usuario = obterAdministradorAutorizadoVisualmente();
                iniciarConsulta(montarFiltro(), usuario.getIdUsuario());
            } catch (SecurityException e) {
                tratarAcessoNegado();
            } catch (IllegalArgumentException e) {
                tratarFiltroInvalido(e);
            } catch (RuntimeException e) {
                tratarFalhaOpcoes(e);
            }
        });

        task.setOnFailed(event -> {
            if (!telaAtiva || taskOpcoesAtual != task) {
                return;
            }
            taskOpcoesAtual = null;
            Throwable causa = task.getException();
            if (localizarSecurityException(causa) != null) {
                tratarAcessoNegado();
                return;
            }
            tratarFalhaOpcoes(causa);
        });

        task.setOnCancelled(event -> {
            if (taskOpcoesAtual == task) {
                taskOpcoesAtual = null;
                atualizarEstadoControles();
            }
        });
        iniciarThread(task, "relatorio-entradas-opcoes");
    }

    private void tratarFalhaOpcoes(Throwable causa) {
        registrarFalha(
                "Falha ao carregar opções do relatório de entradas.", causa);
        atualizarEstadoControles();
        lblEstadoConsulta.setText(
                "Não foi possível carregar as opções de filtro.");
        mostrarAlerta(
                Alert.AlertType.ERROR,
                "Erro",
                "Não foi possível carregar usuários e produtos para o relatório."
        );
    }

    private void aplicarOpcoes(OpcoesConsulta opcoes) {
        if (opcoes == null) {
            throw new IllegalStateException(
                    "Opções do relatório não foram retornadas.");
        }

        ObservableList<OpcaoFiltro<Usuario>> usuarios =
                FXCollections.observableArrayList();
        usuarios.add(new OpcaoFiltro<>("Todos", null));
        for (Usuario usuario : opcoes.usuarios()) {
            if (usuario == null || usuario.getIdUsuario() == null
                    || usuario.getIdUsuario() <= 0) {
                throw new IllegalStateException(
                        "Usuário inválido no carregamento de opções.");
            }
            String sufixo = "INATIVO".equals(usuario.getStatus())
                    ? " (Inativo)" : "";
            usuarios.add(new OpcaoFiltro<>(
                    usuario.getIdUsuario() + " - "
                            + usuario.getNome() + sufixo,
                    usuario
            ));
        }

        ObservableList<OpcaoFiltro<Produto>> produtos =
                FXCollections.observableArrayList();
        produtos.add(new OpcaoFiltro<>("Todos", null));
        for (Produto produto : opcoes.produtos()) {
            if (produto == null || produto.getIdProduto() == null
                    || produto.getIdProduto() <= 0) {
                throw new IllegalStateException(
                        "Produto inválido no carregamento de opções.");
            }
            String sufixo = produto.isAtivo() ? "" : " (Inativo)";
            produtos.add(new OpcaoFiltro<>(
                    produto.getIdProduto() + " - "
                            + produto.getDescricao() + sufixo,
                    produto
            ));
        }

        cbResponsavel.setItems(usuarios);
        cbProduto.setItems(produtos);
        cbResponsavel.getSelectionModel().selectFirst();
        cbProduto.getSelectionModel().selectFirst();
        atualizarEstadoControles();
    }

    private FiltroRelatorioEntradaEstoque montarFiltro() {
        LocalDate dataInicial = confirmarTextoDatePicker(dpDataInicial);
        LocalDate dataFinal = confirmarTextoDatePicker(dpDataFinal);
        Integer entradaId = converterEntradaId(
                txtNumeroEntrada.getText());
        Usuario responsavel = obterValorSelecionado(cbResponsavel);
        Produto produto = obterValorSelecionado(cbProduto);

        return new FiltroRelatorioEntradaEstoque(
                dataInicial,
                dataFinal,
                entradaId,
                responsavel == null ? null : responsavel.getIdUsuario(),
                produto == null ? null : produto.getIdProduto(),
                txtReferencia.getText()
        );
    }

    private LocalDate confirmarTextoDatePicker(DatePicker datePicker) {
        String texto = datePicker.getEditor().getText();
        LocalDate data = datePicker.getConverter().fromString(texto);
        datePicker.setValue(data);
        return data;
    }

    private Integer converterEntradaId(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            int id = Integer.parseInt(texto.trim());
            if (id <= 0) {
                throw new IllegalArgumentException(
                        "Número da entrada deve ser maior que zero.");
            }
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Número da entrada deve ser um inteiro válido.", e);
        }
    }

    private <T> T obterValorSelecionado(ComboBox<OpcaoFiltro<T>> combo) {
        OpcaoFiltro<T> opcao = combo.getValue();
        return opcao == null ? null : opcao.valor();
    }

    private void iniciarConsulta(
            FiltroRelatorioEntradaEstoque filtro,
            Integer usuarioId
    ) {
        if (!telaAtiva) {
            return;
        }
        invalidarConsultaAtual();
        long token = tokenConsultaAtual;

        Task<ResultadoRelatorioEntradaEstoque> task = new Task<>() {
            @Override
            protected ResultadoRelatorioEntradaEstoque call() {
                return relatorioService.consultarRelatorio(filtro, usuarioId);
            }
        };

        taskConsultaAtual = task;
        lblEstadoConsulta.setText("Consultando entradas de estoque...");
        atualizarEstadoControles();

        task.setOnSucceeded(event -> {
            if (!consultaEhAtual(task, token)) {
                return;
            }
            ResultadoRelatorioEntradaEstoque resultado = task.getValue();
            if (resultado == null) {
                tratarFalhaConsulta(task, token,
                        new IllegalStateException(
                                "O Service não retornou o resultado do relatório."));
                return;
            }
            try {
                aplicarResultado(resultado);
                ultimoResultadoValido = resultado;
            } catch (RuntimeException e) {
                tratarFalhaConsulta(task, token, e);
                return;
            }
            finalizarConsulta(task, token);
        });

        task.setOnFailed(event -> {
            if (!consultaEhAtual(task, token)) {
                return;
            }
            Throwable causa = task.getException();
            if (localizarSecurityException(causa) != null) {
                tratarAcessoNegado();
                return;
            }
            tratarFalhaConsulta(task, token, causa);
        });

        task.setOnCancelled(event -> {
            if (consultaEhAtual(task, token, false)) {
                finalizarConsulta(task, token);
                lblEstadoConsulta.setText("Consulta cancelada.");
            }
        });

        iniciarThread(task, "relatorio-entradas-consulta-" + token);
    }

    private boolean consultaEhAtual(
            Task<ResultadoRelatorioEntradaEstoque> task,
            long token
    ) {
        return consultaEhAtual(task, token, true);
    }

    private boolean consultaEhAtual(
            Task<ResultadoRelatorioEntradaEstoque> task,
            long token,
            boolean exigirNaoCancelada
    ) {
        return telaAtiva
                && taskConsultaAtual == task
                && tokenConsultaAtual == token
                && (!exigirNaoCancelada || !task.isCancelled());
    }

    private void invalidarConsultaAtual() {
        tokenConsultaAtual++;
        Task<ResultadoRelatorioEntradaEstoque> anterior = taskConsultaAtual;
        taskConsultaAtual = null;
        if (anterior != null && !anterior.isDone()) {
            anterior.cancel(true);
        }
    }

    private void finalizarConsulta(
            Task<ResultadoRelatorioEntradaEstoque> task,
            long token
    ) {
        if (!consultaEhAtual(task, token, false)) {
            return;
        }
        taskConsultaAtual = null;
        atualizarEstadoControles();
    }

    private void aplicarResultado(ResultadoRelatorioEntradaEstoque resultado) {
        List<EntradaEstoqueRelatorioView> entradas =
                List.copyOf(resultado.getEntradas());

        invalidarDetalheAtual();
        itensExibidos.clear();
        tabelaEntradas.getSelectionModel().clearSelection();
        lblObservacao.setText(
                "Selecione uma entrada para visualizar a observação.");

        entradasExibidas.setAll(entradas);
        lblQuantidadeEntradas.setText(formatarQuantidade(
                resultado.getQuantidadeEntradas(), "entrada", "entradas"));
        lblTotalUnidades.setText(formatarQuantidade(
                resultado.getTotalUnidades(), "unidade", "unidades"));
        lblValorTotal.setText(formatarMoeda(resultado.getValorTotal()));
        lblFiltroAplicado.setText(
                formatarFiltroAplicado(resultado.getFiltro()));

        if (entradas.isEmpty()) {
            tabelaEntradas.setPlaceholder(
                    criarPlaceholder(MENSAGEM_RESULTADO_VAZIO));
            tabelaItens.setPlaceholder(
                    criarPlaceholder("Nenhuma entrada selecionada."));
            lblEstadoConsulta.setText(MENSAGEM_RESULTADO_VAZIO);
        } else {
            tabelaEntradas.setPlaceholder(
                    criarPlaceholder("Nenhuma entrada para exibir."));
            lblEstadoConsulta.setText(
                    "Relatório atualizado com sucesso.");
        }
    }
    private void selecionarEntrada(EntradaEstoqueRelatorioView entrada) {
        invalidarDetalheAtual();
        itensExibidos.clear();

        if (!telaAtiva || entrada == null) {
            lblObservacao.setText(
                    "Selecione uma entrada para visualizar a observação.");
            tabelaItens.setPlaceholder(criarPlaceholder(
                    "Selecione uma entrada para visualizar os itens."));
            atualizarEstadoControles();
            return;
        }

        lblObservacao.setText(entrada.getObservacao() == null
                ? "Sem observação." : entrada.getObservacao());

        Integer usuarioId;
        try {
            usuarioId = obterAdministradorAutorizadoVisualmente()
                    .getIdUsuario();
        } catch (SecurityException e) {
            tratarAcessoNegado();
            return;
        }

        long token = tokenDetalheAtual;
        Task<List<ItemEntradaEstoqueRelatorioView>> task = new Task<>() {
            @Override
            protected List<ItemEntradaEstoqueRelatorioView> call() {
                return relatorioService.consultarItensEntrada(
                        entrada.getIdEntrada(), usuarioId);
            }
        };

        taskDetalheAtual = task;
        tabelaItens.setPlaceholder(
                criarPlaceholder("Carregando itens da entrada..."));
        lblEstadoConsulta.setText("Carregando itens da entrada...");
        atualizarEstadoControles();

        task.setOnSucceeded(event -> {
            if (!detalheEhAtual(task, token, entrada)) {
                return;
            }
            List<ItemEntradaEstoqueRelatorioView> itens = task.getValue();
            if (itens == null || itens.isEmpty()) {
                tratarFalhaDetalhe(task, token, entrada,
                        new IllegalStateException(
                                "O Service não retornou itens para a entrada."));
                return;
            }
            itensExibidos.setAll(List.copyOf(itens));
            taskDetalheAtual = null;
            lblEstadoConsulta.setText("Itens da entrada carregados.");
            atualizarEstadoControles();
        });

        task.setOnFailed(event -> {
            if (!detalheEhAtual(task, token, entrada)) {
                return;
            }
            Throwable causa = task.getException();
            if (localizarSecurityException(causa) != null) {
                tratarAcessoNegado();
                return;
            }
            tratarFalhaDetalhe(task, token, entrada, causa);
        });

        task.setOnCancelled(event -> {
            if (detalheEhAtual(task, token, entrada, false)) {
                taskDetalheAtual = null;
                atualizarEstadoControles();
            }
        });

        iniciarThread(task, "relatorio-entradas-detalhe-" + token);
    }

    private boolean detalheEhAtual(
            Task<List<ItemEntradaEstoqueRelatorioView>> task,
            long token,
            EntradaEstoqueRelatorioView entrada
    ) {
        return detalheEhAtual(task, token, entrada, true);
    }

    private boolean detalheEhAtual(
            Task<List<ItemEntradaEstoqueRelatorioView>> task,
            long token,
            EntradaEstoqueRelatorioView entrada,
            boolean exigirNaoCancelada
    ) {
        return telaAtiva
                && taskDetalheAtual == task
                && tokenDetalheAtual == token
                && (!exigirNaoCancelada || !task.isCancelled())
                && tabelaEntradas.getSelectionModel()
                .getSelectedItem() == entrada;
    }

    private void invalidarDetalheAtual() {
        tokenDetalheAtual++;
        Task<List<ItemEntradaEstoqueRelatorioView>> anterior =
                taskDetalheAtual;
        taskDetalheAtual = null;
        if (anterior != null && !anterior.isDone()) {
            anterior.cancel(true);
        }
    }

    private void tratarFalhaConsulta(
            Task<ResultadoRelatorioEntradaEstoque> task,
            long token,
            Throwable causa
    ) {
        if (!consultaEhAtual(task, token)) {
            return;
        }
        registrarFalha(
                "Falha ao atualizar o relatório de entradas de estoque.", causa);
        finalizarConsulta(task, token);

        String complemento = ultimoResultadoValido == null
                ? "Nenhum resultado válido foi carregado."
                : "Os dados anteriormente exibidos foram mantidos.";
        lblEstadoConsulta.setText(
                "Não foi possível atualizar o relatório. " + complemento);
        mostrarAlerta(
                Alert.AlertType.ERROR,
                "Erro",
                "Não foi possível atualizar o relatório de entradas de estoque.\n"
                        + complemento
        );
    }

    private void tratarFalhaDetalhe(
            Task<List<ItemEntradaEstoqueRelatorioView>> task,
            long token,
            EntradaEstoqueRelatorioView entrada,
            Throwable causa
    ) {
        if (!detalheEhAtual(task, token, entrada)) {
            return;
        }
        registrarFalha(
                "Falha ao carregar itens da entrada de estoque.", causa);
        taskDetalheAtual = null;
        itensExibidos.clear();
        tabelaItens.setPlaceholder(criarPlaceholder(
                "Não foi possível carregar os itens desta entrada."));
        lblEstadoConsulta.setText(
                "Não foi possível carregar os itens da entrada selecionada.");
        atualizarEstadoControles();
        mostrarAlerta(
                Alert.AlertType.ERROR,
                "Erro",
                "Não foi possível carregar os itens da entrada selecionada."
        );
    }

    private void tratarFiltroInvalido(IllegalArgumentException e) {
        atualizarEstadoControles();
        lblEstadoConsulta.setText(
                "Filtros inválidos. Corrija os dados informados.");
        mostrarAlerta(
                Alert.AlertType.WARNING,
                "Filtros inválidos",
                e.getMessage() == null || e.getMessage().isBlank()
                        ? "Verifique os filtros informados." : e.getMessage()
        );
    }

    private void tratarAcessoNegado() {
        telaAtiva = false;
        invalidarTodasAsTasks();
        entradasExibidas.clear();
        itensExibidos.clear();
        ultimoResultadoValido = null;
        lblEstadoConsulta.setText(MENSAGEM_ACESSO_NEGADO);
        atualizarEstadoControles();
        mostrarAlerta(
                Alert.AlertType.WARNING,
                "Acesso negado",
                MENSAGEM_ACESSO_NEGADO
        );
        abrirTelaPrincipal(false);
    }

    private void atualizarEstadoControles() {
        boolean opcoes = taskOpcoesAtual != null
                && !taskOpcoesAtual.isDone();
        boolean consulta = taskConsultaAtual != null
                && !taskConsultaAtual.isDone();
        boolean detalhe = taskDetalheAtual != null
                && !taskDetalheAtual.isDone();
        boolean bloquear = !telaAtiva || opcoes || consulta;

        dpDataInicial.setDisable(bloquear);
        dpDataFinal.setDisable(bloquear);
        txtNumeroEntrada.setDisable(bloquear);
        cbResponsavel.setDisable(bloquear);
        cbProduto.setDisable(bloquear);
        txtReferencia.setDisable(bloquear);
        btnFiltrar.setDisable(bloquear);
        btnLimparFiltros.setDisable(bloquear);
        btnVoltar.setDisable(false);

        boolean mostrar = telaAtiva && (opcoes || consulta || detalhe);
        progressoRelatorio.setVisible(mostrar);
        progressoRelatorio.setManaged(mostrar);
    }

    private void invalidarTaskOpcoes() {
        Task<OpcoesConsulta> anterior = taskOpcoesAtual;
        taskOpcoesAtual = null;
        if (anterior != null && !anterior.isDone()) {
            anterior.cancel(true);
        }
    }

    private void invalidarTodasAsTasks() {
        invalidarTaskOpcoes();
        invalidarConsultaAtual();
        invalidarDetalheAtual();
    }

    @FXML
    private void onFiltrar() {
        if (!telaAtiva) {
            return;
        }
        try {
            Integer usuarioId = obterAdministradorAutorizadoVisualmente()
                    .getIdUsuario();
            iniciarConsulta(montarFiltro(), usuarioId);
        } catch (SecurityException e) {
            tratarAcessoNegado();
        } catch (IllegalArgumentException e) {
            tratarFiltroInvalido(e);
        }
    }

    @FXML
    private void onLimparFiltros() {
        if (!telaAtiva) {
            return;
        }
        definirPeriodoInicial();
        txtNumeroEntrada.clear();
        cbResponsavel.getSelectionModel().selectFirst();
        cbProduto.getSelectionModel().selectFirst();
        txtReferencia.clear();
        lblEstadoConsulta.setText(
                "Filtros restaurados. Clique em Filtrar para aplicá-los.");
    }

    @FXML
    private void onVoltar() {
        boolean reativarEmFalha = telaAtiva;
        telaAtiva = false;
        invalidarTodasAsTasks();
        atualizarEstadoControles();
        abrirTelaPrincipal(reativarEmFalha);
    }

    private void abrirTelaPrincipal(boolean reativarEmFalha) {
        try {
            NavegacaoUtil.abrirTela(
                    btnVoltar,
                    "/br/com/luis/view/TelaPrincipal.fxml",
                    "Tela Principal"
            );
        } catch (IOException | RuntimeException e) {
            registrarFalha("Falha ao voltar para a Tela Principal.", e);
            if (reativarEmFalha) {
                telaAtiva = true;
                atualizarEstadoControles();
            }
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro",
                    "Não foi possível retornar para a Tela Principal."
            );
        }
    }

    private String formatarFiltroAplicado(
            FiltroRelatorioEntradaEstoque filtro
    ) {
        return "Dados exibidos: "
                + filtro.getDataInicial().format(FORMATO_DATA)
                + " a " + filtro.getDataFinal().format(FORMATO_DATA)
                + " | Entrada: " + valorOuTodos(filtro.getEntradaId())
                + " | Responsável: " + valorOuTodos(filtro.getUsuarioId())
                + " | Produto: " + valorOuTodos(filtro.getProdutoId())
                + " | Referência: "
                + (filtro.getReferencia() == null
                ? "Todas" : filtro.getReferencia());
    }

    private String valorOuTodos(Integer valor) {
        return valor == null ? "Todos" : valor.toString();
    }

    private String formatarQuantidade(
            int quantidade,
            String singular,
            String plural
    ) {
        return quantidade + " " + (quantidade == 1 ? singular : plural);
    }

    private String formatarMoeda(BigDecimal valor) {
        BigDecimal seguro = valor == null ? BigDecimal.ZERO : valor;
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR"))
                .format(seguro).replace(' ', ' ');
    }

    private Label criarPlaceholder(String texto) {
        Label label = new Label(texto);
        label.setWrapText(true);
        return label;
    }

    private void iniciarThread(Task<?> task, String nome) {
        Thread thread = new Thread(task, nome);
        thread.setDaemon(true);
        thread.start();
    }

    private SecurityException localizarSecurityException(Throwable causa) {
        Throwable atual = causa;
        while (atual != null) {
            if (atual instanceof SecurityException securityException) {
                return securityException;
            }
            atual = atual.getCause();
        }
        return null;
    }

    private void registrarFalha(String mensagem, Throwable causa) {
        System.err.println("[ERRO] " + mensagem);
        if (causa != null) {
            causa.printStackTrace();
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

    private record OpcaoFiltro<T>(String rotulo, T valor) {
        private OpcaoFiltro {
            if (rotulo == null || rotulo.isBlank()) {
                throw new IllegalArgumentException(
                        "Rótulo da opção de filtro é obrigatório.");
            }
            rotulo = rotulo.trim();
        }

        @Override
        public String toString() {
            return rotulo;
        }
    }

    private record OpcoesConsulta(
            List<Usuario> usuarios,
            List<Produto> produtos
    ) {
        private OpcoesConsulta {
            usuarios = List.copyOf(usuarios);
            produtos = List.copyOf(produtos);
        }
    }
}
