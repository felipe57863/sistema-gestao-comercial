package br.com.luis.controller;

import br.com.luis.model.FormaPagamento;
import br.com.luis.model.OrigemMovimentacaoFinanceira;
import br.com.luis.model.StatusContaReceber;
import br.com.luis.model.StatusVenda;
import br.com.luis.model.TipoMovimentacaoFinanceira;
import br.com.luis.model.TipoVenda;
import br.com.luis.model.Usuario;
import br.com.luis.service.EstornoVendaService;
import br.com.luis.service.HistoricoVendaService;
import br.com.luis.util.CabecalhoUtil;
import br.com.luis.util.NavegacaoUtil;
import br.com.luis.util.SessaoUsuario;
import br.com.luis.viewmodel.FiltroHistoricoVenda;
import br.com.luis.viewmodel.ItemVendaHistoricoView;
import br.com.luis.viewmodel.ResultadoEstornoVenda;
import br.com.luis.viewmodel.VendaHistoricoDetalheView;
import br.com.luis.viewmodel.VendaHistoricoListagemView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Controller da tela de Histórico de Vendas.
 *
 * Configura os filtros, a listagem, o painel de detalhes e o fluxo visual
 * de estorno total da venda selecionada.
 *
 * O Controller não acessa DAO, não abre Connection e não contém as regras
 * transacionais do estorno. As consultas são delegadas ao
 * HistoricoVendaService e o estorno é delegado ao EstornoVendaService.
 */
public class HistoricoVendasController {

    private static final int LIMITE_MOTIVO_ESTORNO = 500;

    @FXML private Label lblUsuario;
    @FXML private Label lblDataHora;

    @FXML private DatePicker dpDataInicial;
    @FXML private DatePicker dpDataFinal;
    @FXML private TextField txtClienteDocumento;
    @FXML private TextField txtVendaId;
    @FXML private ComboBox<TipoVenda> cbTipoVenda;
    @FXML private ComboBox<StatusVenda> cbStatusVenda;

    @FXML private Button btnVoltar;
    @FXML private Button btnLimparFiltros;
    @FXML private Button btnFiltrar;
    @FXML private Button btnAtualizar;
    @FXML private Button btnEstornarVenda;

    @FXML private TableView<VendaHistoricoListagemView> tabelaVendas;

    @FXML
    private TableColumn<VendaHistoricoListagemView, Integer>
            colVendaId;

    @FXML
    private TableColumn<VendaHistoricoListagemView, LocalDateTime>
            colDataHora;

    @FXML
    private TableColumn<VendaHistoricoListagemView, String>
            colCliente;

    @FXML
    private TableColumn<VendaHistoricoListagemView, TipoVenda>
            colTipoVenda;

    @FXML
    private TableColumn<VendaHistoricoListagemView, StatusVenda>
            colStatusVenda;

    @FXML
    private TableColumn<VendaHistoricoListagemView, BigDecimal>
            colValorTotal;

    @FXML
    private TableColumn<VendaHistoricoListagemView, Integer>
            colQuantidadeItens;

    @FXML
    private TableColumn<VendaHistoricoListagemView, String>
            colResumoFinanceiro;

    @FXML private Label lblTotalVendas;

    @FXML private Label lblDetalheVendaId;
    @FXML private Label lblDetalheDataHora;
    @FXML private Label lblDetalheTipoVenda;
    @FXML private Label lblDetalheStatusVenda;
    @FXML private Label lblDetalheFormaPagamento;
    @FXML private Label lblDetalheValorTotal;

    @FXML private Label lblDetalheCliente;
    @FXML private Label lblDetalheDocumento;
    @FXML private Label lblDetalheUsuarioVenda;

    @FXML private Label lblDetalheContaId;
    @FXML private Label lblDetalheStatusConta;
    @FXML private Label lblDetalheValorConta;
    @FXML private Label lblDetalheMovimentacaoOriginal;
    @FXML private Label lblDetalheMovimentacaoSaida;

    @FXML private Label lblDetalheAuditoriaId;
    @FXML private Label lblDetalheUsuarioEstorno;
    @FXML private Label lblDetalheDataHoraEstorno;
    @FXML private Label lblDetalheStatusVendaAnterior;
    @FXML private Label lblDetalheStatusContaAnterior;
    @FXML private Label lblDetalheMotivoEstorno;

    @FXML private TableView<ItemVendaHistoricoView> tabelaItensVenda;

    @FXML
    private TableColumn<ItemVendaHistoricoView, Integer>
            colItemProdutoId;

    @FXML
    private TableColumn<ItemVendaHistoricoView, String>
            colItemDescricao;

    @FXML
    private TableColumn<ItemVendaHistoricoView, Integer>
            colItemQuantidade;

    @FXML
    private TableColumn<ItemVendaHistoricoView, BigDecimal>
            colItemPrecoUnitario;

    @FXML
    private TableColumn<ItemVendaHistoricoView, BigDecimal>
            colItemSubtotal;

    private final HistoricoVendaService historicoVendaService;
    private final EstornoVendaService estornoVendaService;

    private final ObservableList<VendaHistoricoListagemView>
            vendasExibidas;

    private final ObservableList<ItemVendaHistoricoView>
            itensVendaExibidos;

    /**
     * Inicializa Services e listas observáveis.
     *
     * Não acessa banco de dados.
     */
    public HistoricoVendasController() {
        this.historicoVendaService =
                new HistoricoVendaService();

        this.estornoVendaService =
                new EstornoVendaService();

        this.vendasExibidas =
                FXCollections.observableArrayList();

        this.itensVendaExibidos =
                FXCollections.observableArrayList();
    }

    /**
     * Inicializa a interface.
     */
    @FXML
    public void initialize() {

        CabecalhoUtil.configurarUsuarioEDataHora(
                lblUsuario,
                lblDataHora
        );

        configurarFiltros();
        configurarTabelaVendas();
        configurarTabelaItens();
        configurarSelecaoTabela();
        limparPainelDetalhes();
        definirPeriodoInicial();
        carregarHistorico();
    }

    /**
     * Configura os campos de filtro.
     */
    private void configurarFiltros() {

        cbTipoVenda.getItems().setAll(
                TipoVenda.values()
        );

        cbTipoVenda.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(
                            TipoVenda tipoVenda
                    ) {
                        return formatarTipoVenda(tipoVenda);
                    }

                    @Override
                    public TipoVenda fromString(String texto) {
                        return null;
                    }
                }
        );

        cbStatusVenda.getItems().setAll(
                StatusVenda.values()
        );

        cbStatusVenda.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(
                            StatusVenda statusVenda
                    ) {
                        return formatarStatusVenda(statusVenda);
                    }

                    @Override
                    public StatusVenda fromString(String texto) {
                        return null;
                    }
                }
        );

        txtVendaId.setTextFormatter(
                new TextFormatter<>(alteracao -> {

                    String novoTexto =
                            alteracao.getControlNewText();

                    if (novoTexto.matches("\\d*")) {
                        return alteracao;
                    }

                    return null;
                })
        );
    }

    /**
     * Define o período inicial como o mês atual.
     */
    private void definirPeriodoInicial() {

        LocalDate hoje = LocalDate.now();

        dpDataInicial.setValue(
                hoje.withDayOfMonth(1)
        );

        dpDataFinal.setValue(hoje);
    }

    /**
     * Configura a tabela principal do histórico.
     */
    private void configurarTabelaVendas() {

        colVendaId.setCellValueFactory(
                new PropertyValueFactory<>("vendaId")
        );

        colDataHora.setCellValueFactory(
                new PropertyValueFactory<>("dataHora")
        );

        colCliente.setCellValueFactory(
                new PropertyValueFactory<>("nomeCliente")
        );

        colTipoVenda.setCellValueFactory(
                new PropertyValueFactory<>("tipoVenda")
        );

        colStatusVenda.setCellValueFactory(
                new PropertyValueFactory<>("statusVenda")
        );

        colValorTotal.setCellValueFactory(
                new PropertyValueFactory<>("valorTotal")
        );

        colQuantidadeItens.setCellValueFactory(
                new PropertyValueFactory<>("quantidadeItens")
        );

        colResumoFinanceiro.setCellValueFactory(
                new PropertyValueFactory<>("resumoFinanceiro")
        );

        configurarColunaDataHora();
        configurarColunaTipoVenda();
        configurarColunaStatusVenda();
        configurarColunaValorTotal();

        tabelaVendas.setItems(vendasExibidas);
    }

    /**
     * Configura a tabela de itens históricos.
     */
    private void configurarTabelaItens() {

        colItemProdutoId.setCellValueFactory(
                new PropertyValueFactory<>("produtoId")
        );

        colItemDescricao.setCellValueFactory(
                new PropertyValueFactory<>("descricaoProduto")
        );

        colItemQuantidade.setCellValueFactory(
                new PropertyValueFactory<>("quantidade")
        );

        colItemPrecoUnitario.setCellValueFactory(
                new PropertyValueFactory<>("precoUnitario")
        );

        colItemSubtotal.setCellValueFactory(
                new PropertyValueFactory<>("subtotal")
        );

        configurarColunaMonetariaItem(
                colItemPrecoUnitario
        );

        configurarColunaMonetariaItem(
                colItemSubtotal
        );

        tabelaItensVenda.setItems(
                itensVendaExibidos
        );
    }

    /**
     * Formata a coluna Data/Hora.
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

                        if (empty || dataHora == null) {
                            setText(null);
                            return;
                        }

                        setText(formatarDataHora(dataHora));
                    }
                }
        );
    }

    /**
     * Formata a coluna Tipo.
     */
    private void configurarColunaTipoVenda() {

        colTipoVenda.setCellFactory(
                coluna -> new TableCell<>() {
                    @Override
                    protected void updateItem(
                            TipoVenda tipoVenda,
                            boolean empty
                    ) {
                        super.updateItem(tipoVenda, empty);

                        if (empty || tipoVenda == null) {
                            setText(null);
                            return;
                        }

                        setText(formatarTipoVenda(tipoVenda));
                    }
                }
        );
    }

    /**
     * Formata a coluna Status.
     */
    private void configurarColunaStatusVenda() {

        colStatusVenda.setCellFactory(
                coluna -> new TableCell<>() {
                    @Override
                    protected void updateItem(
                            StatusVenda statusVenda,
                            boolean empty
                    ) {
                        super.updateItem(statusVenda, empty);

                        if (empty || statusVenda == null) {
                            setText(null);
                            return;
                        }

                        setText(formatarStatusVenda(statusVenda));
                    }
                }
        );
    }

    /**
     * Formata a coluna Valor Total.
     */
    private void configurarColunaValorTotal() {

        colValorTotal.setCellFactory(
                coluna -> new TableCell<>() {
                    @Override
                    protected void updateItem(
                            BigDecimal valor,
                            boolean empty
                    ) {
                        super.updateItem(valor, empty);

                        if (empty || valor == null) {
                            setText(null);
                            return;
                        }

                        setText(formatarValor(valor));
                    }
                }
        );
    }

    /**
     * Formata uma coluna monetária da tabela de itens.
     */
    private void configurarColunaMonetariaItem(
            TableColumn<ItemVendaHistoricoView, BigDecimal> coluna
    ) {

        coluna.setCellFactory(
                colunaAtual -> new TableCell<>() {
                    @Override
                    protected void updateItem(
                            BigDecimal valor,
                            boolean empty
                    ) {
                        super.updateItem(valor, empty);

                        if (empty || valor == null) {
                            setText(null);
                            return;
                        }

                        setText(formatarValor(valor));
                    }
                }
        );
    }

    /**
     * Configura a seleção da tabela.
     */
    private void configurarSelecaoTabela() {

        tabelaVendas
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (
                                observable,
                                vendaAnterior,
                                vendaAtual
                        ) -> {

                            if (vendaAtual == null) {
                                limparPainelDetalhes();
                                return;
                            }

                            carregarDetalhesVenda(
                                    vendaAtual.getVendaId()
                            );
                        }
                );
    }

    /**
     * Cria o filtro a partir dos campos da interface.
     */
    private FiltroHistoricoVenda montarFiltro() {

        FiltroHistoricoVenda filtro =
                new FiltroHistoricoVenda();

        filtro.setDataInicial(
                dpDataInicial.getValue()
        );

        filtro.setDataFinal(
                dpDataFinal.getValue()
        );

        filtro.setClienteOuDocumento(
                txtClienteDocumento.getText()
        );

        filtro.setVendaId(
                converterVendaId(
                        txtVendaId.getText()
                )
        );

        filtro.setTipoVenda(
                cbTipoVenda.getValue()
        );

        filtro.setStatusVenda(
                cbStatusVenda.getValue()
        );

        filtro.validar();

        return filtro;
    }

    /**
     * Converte o ID digitado.
     */
    private Integer converterVendaId(String texto) {

        if (texto == null || texto.isBlank()) {
            return null;
        }

        try {
            int vendaId =
                    Integer.parseInt(texto.trim());

            if (vendaId <= 0) {
                throw new IllegalArgumentException(
                        "ID da venda deve ser maior que zero."
                );
            }

            return vendaId;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "ID da venda inválido.",
                    e
            );
        }
    }

    /**
     * Carrega a listagem conforme os filtros atuais.
     */
    private void carregarHistorico() {

        try {
            bloquearAcoesConsulta(true);

            tabelaVendas
                    .getSelectionModel()
                    .clearSelection();

            limparPainelDetalhes();

            FiltroHistoricoVenda filtro =
                    montarFiltro();

            List<VendaHistoricoListagemView> vendas =
                    historicoVendaService.listarHistorico(
                            filtro
                    );

            if (vendas == null) {
                vendasExibidas.clear();
            } else {
                vendasExibidas.setAll(vendas);
            }

            atualizarContadorVendas();

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            vendasExibidas.clear();
            atualizarContadorVendas();
            limparPainelDetalhes();

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Não foi possível consultar",
                    obterMensagemSegura(
                            e,
                            "Não foi possível consultar o histórico."
                    )
            );

        } catch (RuntimeException e) {

            vendasExibidas.clear();
            atualizarContadorVendas();
            limparPainelDetalhes();

            System.err.println(
                    "[ERRO] Falha ao carregar histórico de vendas."
            );
            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro",
                    "Não foi possível carregar o histórico de vendas."
            );

        } finally {
            bloquearAcoesConsulta(false);
        }
    }

    /**
     * Carrega os detalhes da venda selecionada.
     */
    private void carregarDetalhesVenda(Integer vendaId) {

        try {
            btnEstornarVenda.setDisable(true);

            VendaHistoricoDetalheView detalhe =
                    historicoVendaService
                            .buscarDetalhesVenda(vendaId);

            if (detalhe == null) {
                throw new IllegalStateException(
                        "Detalhes da venda não retornados pelo Service."
                );
            }

            preencherPainelDetalhes(detalhe);

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            limparPainelDetalhes();

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Não foi possível carregar os detalhes",
                    obterMensagemSegura(
                            e,
                            "Não foi possível carregar os detalhes da venda."
                    )
            );

        } catch (RuntimeException e) {

            limparPainelDetalhes();

            System.err.println(
                    "[ERRO] Falha ao carregar detalhes da venda "
                            + vendaId
                            + "."
            );
            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro",
                    "Não foi possível carregar os detalhes da venda."
            );
        }
    }

    /**
     * Preenche o painel lateral.
     */
    private void preencherPainelDetalhes(
            VendaHistoricoDetalheView detalhe
    ) {

        lblDetalheVendaId.setText(
                formatarId(detalhe.getVendaId())
        );

        lblDetalheDataHora.setText(
                formatarDataHora(detalhe.getDataHora())
        );

        lblDetalheTipoVenda.setText(
                formatarTipoVenda(detalhe.getTipoVenda())
        );

        lblDetalheStatusVenda.setText(
                formatarStatusVenda(
                        detalhe.getStatusVenda()
                )
        );

        lblDetalheFormaPagamento.setText(
                formatarFormaPagamento(
                        detalhe.getFormaPagamento()
                )
        );

        lblDetalheValorTotal.setText(
                formatarValor(detalhe.getValorTotal())
        );

        lblDetalheCliente.setText(
                formatarPessoaComId(
                        detalhe.getClienteId(),
                        detalhe.getNomeCliente()
                )
        );

        lblDetalheDocumento.setText(
                formatarTexto(
                        detalhe.getDocumentoCliente()
                )
        );

        lblDetalheUsuarioVenda.setText(
                formatarPessoaComId(
                        detalhe.getUsuarioVendaId(),
                        detalhe.getNomeUsuarioVenda()
                )
        );

        lblDetalheContaId.setText(
                formatarId(
                        detalhe.getContaReceberId()
                )
        );

        lblDetalheStatusConta.setText(
                formatarStatusConta(
                        detalhe.getStatusContaReceber()
                )
        );

        lblDetalheValorConta.setText(
                formatarValorOpcional(
                        detalhe.getValorContaReceber()
                )
        );

        lblDetalheMovimentacaoOriginal.setText(
                formatarMovimentacao(
                        detalhe.getMovimentacaoOriginalId(),
                        detalhe.getTipoMovimentacaoOriginal(),
                        detalhe.getOrigemMovimentacaoOriginal(),
                        detalhe.getFormaPagamentoMovimentacaoOriginal(),
                        detalhe.getValorMovimentacaoOriginal(),
                        detalhe.getDataHoraMovimentacaoOriginal()
                )
        );

        lblDetalheMovimentacaoSaida.setText(
                formatarMovimentacao(
                        detalhe.getMovimentacaoSaidaId(),
                        detalhe.getTipoMovimentacaoSaida(),
                        detalhe.getOrigemMovimentacaoSaida(),
                        detalhe.getFormaPagamentoMovimentacaoSaida(),
                        detalhe.getValorMovimentacaoSaida(),
                        detalhe.getDataHoraMovimentacaoSaida()
                )
        );

        lblDetalheAuditoriaId.setText(
                formatarId(detalhe.getAuditoriaId())
        );

        lblDetalheUsuarioEstorno.setText(
                formatarPessoaComId(
                        detalhe.getUsuarioEstornoId(),
                        detalhe.getNomeUsuarioEstorno()
                )
        );

        lblDetalheDataHoraEstorno.setText(
                formatarDataHora(
                        detalhe.getDataHoraEstorno()
                )
        );

        lblDetalheStatusVendaAnterior.setText(
                formatarStatusVenda(
                        detalhe.getStatusVendaAnterior()
                )
        );

        lblDetalheStatusContaAnterior.setText(
                formatarStatusConta(
                        detalhe.getStatusContaReceberAnterior()
                )
        );

        lblDetalheMotivoEstorno.setText(
                formatarTexto(
                        detalhe.getMotivoEstorno()
                )
        );

        List<ItemVendaHistoricoView> itens =
                detalhe.getItens();

        if (itens == null) {
            itensVendaExibidos.clear();
        } else {
            itensVendaExibidos.setAll(itens);
        }

        btnEstornarVenda.setDisable(
                detalhe.getStatusVenda()
                        == StatusVenda.ESTORNADA
        );
    }

    /**
     * Limpa o painel lateral.
     */
    private void limparPainelDetalhes() {

        lblDetalheVendaId.setText("—");
        lblDetalheDataHora.setText("—");
        lblDetalheTipoVenda.setText("—");
        lblDetalheStatusVenda.setText("—");
        lblDetalheFormaPagamento.setText("—");
        lblDetalheValorTotal.setText("R$ 0,00");

        lblDetalheCliente.setText("—");
        lblDetalheDocumento.setText("—");
        lblDetalheUsuarioVenda.setText("—");

        lblDetalheContaId.setText("—");
        lblDetalheStatusConta.setText("—");
        lblDetalheValorConta.setText("—");
        lblDetalheMovimentacaoOriginal.setText("—");
        lblDetalheMovimentacaoSaida.setText("—");

        lblDetalheAuditoriaId.setText("—");
        lblDetalheUsuarioEstorno.setText("—");
        lblDetalheDataHoraEstorno.setText("—");
        lblDetalheStatusVendaAnterior.setText("—");
        lblDetalheStatusContaAnterior.setText("—");
        lblDetalheMotivoEstorno.setText("—");

        itensVendaExibidos.clear();
        btnEstornarVenda.setDisable(true);
    }

    /**
     * Ação do botão Filtrar.
     */
    @FXML
    private void onFiltrar() {
        carregarHistorico();
    }

    /**
     * Limpa todos os filtros e consulta novamente.
     */
    @FXML
    private void onLimparFiltros() {

        dpDataInicial.setValue(null);
        dpDataFinal.setValue(null);
        txtClienteDocumento.clear();
        txtVendaId.clear();
        cbTipoVenda.getSelectionModel().clearSelection();
        cbStatusVenda.getSelectionModel().clearSelection();

        carregarHistorico();
    }

    /**
     * Atualiza a consulta mantendo os filtros preenchidos.
     */
    @FXML
    private void onAtualizar() {
        carregarHistorico();
    }

    /**
     * Retorna para a Tela Principal.
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

    /**
     * Executa o fluxo visual do estorno.
     */
    @FXML
    private void onEstornarVenda() {

        VendaHistoricoListagemView vendaSelecionada =
                tabelaVendas
                        .getSelectionModel()
                        .getSelectedItem();

        if (vendaSelecionada == null) {

            limparPainelDetalhes();

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Atenção",
                    "Selecione uma venda para realizar o estorno."
            );

            return;
        }

        if (vendaSelecionada.getStatusVenda()
                == StatusVenda.ESTORNADA) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Atenção",
                    "Esta venda já foi estornada."
            );

            return;
        }

        Optional<String> motivoOptional =
                solicitarMotivoEstorno(
                        vendaSelecionada
                );

        if (motivoOptional.isEmpty()) {
            return;
        }

        String motivo = motivoOptional.get();

        if (!confirmarEstorno(
                vendaSelecionada,
                motivo
        )) {
            return;
        }

        Integer usuarioId;

        try {
            usuarioId = obterUsuarioIdAtual();

        } catch (IllegalStateException e) {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro",
                    e.getMessage()
            );

            return;
        }

        Integer vendaId =
                vendaSelecionada.getVendaId();

        try {
            bloquearAcoesConsulta(true);
            btnEstornarVenda.setDisable(true);

            ResultadoEstornoVenda resultado =
                    estornoVendaService.estornarVenda(
                            vendaId,
                            motivo,
                            usuarioId
                    );

            if (resultado == null) {
                throw new RuntimeException(
                        "Resultado do estorno não retornado pelo Service."
                );
            }

            mostrarResultadoEstorno(resultado);

            carregarHistorico();
            selecionarVendaPorId(vendaId);

        } catch (IllegalArgumentException
                 | IllegalStateException e) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Não foi possível estornar a venda",
                    obterMensagemSegura(
                            e,
                            "Não foi possível realizar o estorno."
                    )
            );

        } catch (RuntimeException e) {

            System.err.println(
                    "[ERRO] Falha ao realizar estorno da venda "
                            + vendaId
                            + "."
            );
            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro",
                    "Não foi possível realizar o estorno da venda."
            );

        } finally {
            bloquearAcoesConsulta(false);
            atualizarEstadoBotaoEstorno();
        }
    }

    /**
     * Solicita o motivo obrigatório do estorno.
     */
    private Optional<String> solicitarMotivoEstorno(
            VendaHistoricoListagemView venda
    ) {

        Dialog<String> dialog = new Dialog<>();

        dialog.setTitle("Estornar Venda");
        dialog.setHeaderText(
                "Informe o motivo do estorno da venda "
                        + formatarId(venda.getVendaId())
                        + "."
        );

        ButtonType botaoConfirmar =
                new ButtonType(
                        "Continuar",
                        ButtonBar.ButtonData.OK_DONE
                );

        dialog.getDialogPane()
                .getButtonTypes()
                .addAll(
                        botaoConfirmar,
                        ButtonType.CANCEL
                );

        Label lblVenda = new Label(
                "Cliente: "
                        + formatarTexto(venda.getNomeCliente())
                        + "\nValor: "
                        + formatarValor(venda.getValorTotal())
        );

        TextArea txtMotivo = new TextArea();
        txtMotivo.setPromptText(
                "Descreva o motivo do estorno."
        );
        txtMotivo.setWrapText(true);
        txtMotivo.setPrefRowCount(5);

        Label lblContador = new Label(
                "0/" + LIMITE_MOTIVO_ESTORNO
        );

        VBox conteudo = new VBox(
                10,
                lblVenda,
                new Label("Motivo obrigatório:"),
                txtMotivo,
                lblContador
        );

        dialog.getDialogPane().setContent(conteudo);

        Node botaoConfirmarNode =
                dialog.getDialogPane()
                        .lookupButton(botaoConfirmar);

        botaoConfirmarNode.setDisable(true);

        txtMotivo.textProperty().addListener(
                (
                        observable,
                        textoAnterior,
                        textoAtual
                ) -> {

                    String textoSeguro =
                            textoAtual != null
                                    ? textoAtual
                                    : "";

                    lblContador.setText(
                            textoSeguro.length()
                                    + "/"
                                    + LIMITE_MOTIVO_ESTORNO
                    );

                    boolean motivoInvalido =
                            textoSeguro.isBlank()
                                    || textoSeguro.trim().length()
                                    > LIMITE_MOTIVO_ESTORNO;

                    botaoConfirmarNode.setDisable(
                            motivoInvalido
                    );
                }
        );

        dialog.setResultConverter(
                buttonType -> {

                    if (buttonType == botaoConfirmar) {
                        return txtMotivo
                                .getText()
                                .trim();
                    }

                    return null;
                }
        );

        return dialog.showAndWait();
    }

    /**
     * Solicita a confirmação final do estorno.
     */
    private boolean confirmarEstorno(
            VendaHistoricoListagemView venda,
            String motivo
    ) {

        Alert alerta =
                new Alert(Alert.AlertType.CONFIRMATION);

        alerta.setTitle("Confirmar Estorno");
        alerta.setHeaderText(
                "Esta operação realizará o estorno total da venda."
        );

        String mensagem =
                "Venda: "
                        + formatarId(venda.getVendaId())
                        + "\nCliente: "
                        + formatarTexto(venda.getNomeCliente())
                        + "\nValor: "
                        + formatarValor(venda.getValorTotal())
                        + "\nMotivo: "
                        + motivo
                        + "\n\nO estoque e os registros financeiros "
                        + "serão tratados pelo sistema.";

        alerta.setContentText(mensagem);

        Optional<ButtonType> resposta =
                alerta.showAndWait();

        return resposta.isPresent()
                && resposta.get() == ButtonType.OK;
    }

    /**
     * Obtém o ID real do usuário logado.
     */
    private Integer obterUsuarioIdAtual() {

        Usuario usuarioLogado =
                SessaoUsuario
                        .getInstance()
                        .getUsuarioLogado();

        if (usuarioLogado == null) {
            throw new IllegalStateException(
                    "Não foi possível identificar o usuário logado. "
                            + "Entre novamente no sistema."
            );
        }

        Integer usuarioId =
                usuarioLogado.getIdUsuario();

        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalStateException(
                    "Não foi possível identificar o usuário logado. "
                            + "Entre novamente no sistema."
            );
        }

        return usuarioId;
    }

    /**
     * Mostra o resultado consolidado do estorno.
     */
    private void mostrarResultadoEstorno(
            ResultadoEstornoVenda resultado
    ) {

        StringBuilder mensagem =
                new StringBuilder();

        mensagem.append("Estorno realizado com sucesso.")
                .append("\n\n")
                .append("Venda: ")
                .append(formatarId(resultado.getVendaId()))
                .append("\n")
                .append("Tipo: ")
                .append(formatarTipoVenda(
                        resultado.getTipoVenda()
                ))
                .append("\n")
                .append("Status final: ")
                .append(formatarStatusVenda(
                        resultado.getStatusVendaFinal()
                ))
                .append("\n")
                .append("Data e hora: ")
                .append(formatarDataHora(
                        resultado.getDataHoraEstorno()
                ))
                .append("\n")
                .append("Auditoria: ")
                .append(formatarId(
                        resultado.getAuditoriaId()
                ))
                .append("\n")
                .append("Produtos restaurados: ")
                .append(
                        resultado
                                .getQuantidadeDeItensRestaurados()
                )
                .append("\n")
                .append("Unidades restauradas: ")
                .append(
                        resultado
                                .getQuantidadeTotalDeUnidadesRestauradas()
                );

        if (resultado.getContaReceberId() != null) {

            mensagem.append("\n")
                    .append("Conta: ")
                    .append(formatarId(
                            resultado.getContaReceberId()
                    ))
                    .append(" — ")
                    .append(formatarStatusConta(
                            resultado
                                    .getStatusContaReceberFinal()
                    ));
        }

        if (resultado
                .getMovimentacaoFinanceiraSaidaId()
                != null) {

            mensagem.append("\n")
                    .append("Saída financeira: ")
                    .append(formatarId(
                            resultado
                                    .getMovimentacaoFinanceiraSaidaId()
                    ))
                    .append(" — ")
                    .append(formatarValor(
                            resultado.getValorSaida()
                    ));
        } else {
            mensagem.append("\n")
                    .append("Saída financeira: não necessária");
        }

        mostrarAlerta(
                Alert.AlertType.INFORMATION,
                "Sucesso",
                mensagem.toString()
        );
    }

    /**
     * Tenta selecionar novamente uma venda após atualização.
     */
    private void selecionarVendaPorId(Integer vendaId) {

        if (vendaId == null) {
            return;
        }

        for (VendaHistoricoListagemView venda
                : vendasExibidas) {

            if (vendaId.equals(venda.getVendaId())) {

                tabelaVendas
                        .getSelectionModel()
                        .select(venda);

                tabelaVendas.scrollTo(venda);
                return;
            }
        }

        limparPainelDetalhes();
    }

    /**
     * Atualiza o botão de estorno conforme a seleção.
     */
    private void atualizarEstadoBotaoEstorno() {

        VendaHistoricoListagemView vendaSelecionada =
                tabelaVendas
                        .getSelectionModel()
                        .getSelectedItem();

        btnEstornarVenda.setDisable(
                vendaSelecionada == null
                        || vendaSelecionada.getStatusVenda()
                        == StatusVenda.ESTORNADA
        );
    }

    /**
     * Bloqueia ou libera ações de consulta.
     */
    private void bloquearAcoesConsulta(boolean bloquear) {

        btnFiltrar.setDisable(bloquear);
        btnLimparFiltros.setDisable(bloquear);
        btnAtualizar.setDisable(bloquear);

        if (bloquear) {
            btnEstornarVenda.setDisable(true);
        }
    }

    /**
     * Atualiza o contador da tabela.
     */
    private void atualizarContadorVendas() {

        int quantidade = vendasExibidas.size();

        if (quantidade == 1) {
            lblTotalVendas.setText(
                    "Total: 1 venda"
            );
            return;
        }

        lblTotalVendas.setText(
                "Total: "
                        + quantidade
                        + " vendas"
        );
    }

    /**
     * Formata uma movimentação financeira.
     */
    private String formatarMovimentacao(
            Integer movimentacaoId,
            TipoMovimentacaoFinanceira tipo,
            OrigemMovimentacaoFinanceira origem,
            FormaPagamento formaPagamento,
            BigDecimal valor,
            LocalDateTime dataHora
    ) {

        if (movimentacaoId == null) {
            return "—";
        }

        return "ID "
                + movimentacaoId
                + " | "
                + formatarTipoMovimentacao(tipo)
                + " | "
                + formatarOrigemMovimentacao(origem)
                + " | "
                + formatarFormaPagamento(formaPagamento)
                + " | "
                + formatarValorOpcional(valor)
                + " | "
                + formatarDataHora(dataHora);
    }

    /**
     * Formata IDs.
     */
    private String formatarId(Integer id) {

        if (id == null || id <= 0) {
            return "—";
        }

        return id.toString();
    }

    /**
     * Formata textos opcionais.
     */
    private String formatarTexto(String texto) {

        if (texto == null || texto.isBlank()) {
            return "—";
        }

        return texto.trim();
    }

    /**
     * Formata pessoa junto ao respectivo ID.
     */
    private String formatarPessoaComId(
            Integer id,
            String nome
    ) {

        String nomeFormatado =
                formatarTexto(nome);

        if (id == null || id <= 0) {
            return nomeFormatado;
        }

        return nomeFormatado
                + " (ID "
                + id
                + ")";
    }

    /**
     * Formata valor obrigatório.
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
     * Formata valor opcional.
     */
    private String formatarValorOpcional(
            BigDecimal valor
    ) {

        if (valor == null) {
            return "—";
        }

        return formatarValor(valor);
    }

    /**
     * Formata data e hora.
     */
    private String formatarDataHora(
            LocalDateTime dataHora
    ) {

        if (dataHora == null) {
            return "—";
        }

        DateTimeFormatter formato =
                DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy HH:mm"
                );

        return dataHora.format(formato);
    }

    /**
     * Formata o tipo da venda.
     */
    private String formatarTipoVenda(
            TipoVenda tipoVenda
    ) {

        if (tipoVenda == null) {
            return "—";
        }

        switch (tipoVenda) {
            case A_VISTA:
                return "À vista";

            case A_PRAZO:
                return "A prazo";

            default:
                return "—";
        }
    }

    /**
     * Formata o status da venda.
     */
    private String formatarStatusVenda(
            StatusVenda statusVenda
    ) {

        if (statusVenda == null) {
            return "—";
        }

        switch (statusVenda) {
            case PAGA:
                return "Paga";

            case PENDENTE:
                return "Pendente";

            case ESTORNADA:
                return "Estornada";

            default:
                return "—";
        }
    }

    /**
     * Formata o status da conta.
     */
    private String formatarStatusConta(
            StatusContaReceber statusConta
    ) {

        if (statusConta == null) {
            return "—";
        }

        switch (statusConta) {
            case PENDENTE:
                return "Pendente";

            case PAGA:
                return "Paga";

            case CANCELADA:
                return "Cancelada";

            default:
                return "—";
        }
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

        switch (formaPagamento) {
            case DINHEIRO:
                return "Dinheiro";

            case PIX:
                return "PIX";

            case CARTAO:
                return "Cartão";

            case A_PRAZO:
                return "A prazo";

            default:
                return "—";
        }
    }

    /**
     * Formata o tipo da movimentação.
     */
    private String formatarTipoMovimentacao(
            TipoMovimentacaoFinanceira tipo
    ) {

        if (tipo == null) {
            return "—";
        }

        switch (tipo) {
            case ENTRADA:
                return "Entrada";

            case SAIDA:
                return "Saída";

            default:
                return "—";
        }
    }

    /**
     * Formata a origem financeira.
     */
    private String formatarOrigemMovimentacao(
            OrigemMovimentacaoFinanceira origem
    ) {

        if (origem == null) {
            return "—";
        }

        switch (origem) {
            case VENDA_A_VISTA:
                return "Venda à vista";

            case RECEBIMENTO_CONTA:
                return "Recebimento de conta";

            case ESTORNO_VENDA_A_VISTA:
                return "Estorno de venda à vista";

            case ESTORNO_RECEBIMENTO_CONTA:
                return "Estorno de recebimento";

            default:
                return "—";
        }
    }

    /**
     * Obtém uma mensagem segura de exceção.
     */
    private String obterMensagemSegura(
            RuntimeException e,
            String mensagemPadrao
    ) {

        if (e.getMessage() == null
                || e.getMessage().isBlank()) {

            return mensagemPadrao;
        }

        return e.getMessage();
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
}