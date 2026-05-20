package br.com.luis.controller;

import br.com.luis.model.ItemVenda;
import br.com.luis.model.Produto;
import br.com.luis.model.Venda;
import br.com.luis.service.ProdutoService;
import br.com.luis.service.VendaService;
import br.com.luis.viewmodel.ItemCarrinhoView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Controller inicial da tela Registro de Venda.
 *
 * Nesta etapa, a classe prepara a estrutura necessária para a futura ligação
 * com o RegistroVenda.fxml, mas ainda não executa ações de venda.
 *
 * A tela representa apenas o carrinho em memória.
 *
 * Não implementa:
 * - finalização da venda;
 * - pagamento;
 * - venda a prazo;
 * - baixa de estoque;
 * - persistência completa da venda;
 * - financeiro.
 */
public class RegistroVendaController {

    private static final int USUARIO_TEMPORARIO_ID = 1;

    private final VendaService vendaService;
    private final ProdutoService produtoService;
    private final ObservableList<ItemCarrinhoView> itensCarrinhoView;

    private Venda vendaAtual;

    @FXML private Label lblTituloTela;
    @FXML private Label lblUsuarioLogado;
    @FXML private Label lblDataHora;

    @FXML private TextField txtBuscaProduto;
    @FXML private Spinner<Integer> spnQuantidadeProduto;
    @FXML private Button btnAdicionarProduto;

    @FXML private TableView<ItemCarrinhoView> tblItensVenda;
    @FXML private TableColumn<ItemCarrinhoView, String> colProduto;
    @FXML private TableColumn<ItemCarrinhoView, String> colPreco;
    @FXML private TableColumn<ItemCarrinhoView, String> colPromocao;
    @FXML private TableColumn<ItemCarrinhoView, Integer> colQuantidade;
    @FXML private TableColumn<ItemCarrinhoView, String> colSubtotal;

    @FXML private Button btnRemoverProduto;
    @FXML private Button btnLimparVenda;

    @FXML private TextField txtBuscaCliente;
    @FXML private Button btnSelecionarCliente;
    @FXML private Label lblNomeCliente;
    @FXML private Label lblStatusCliente;
    @FXML private Label lblLimiteDisponivel;

    @FXML private Label lblSubtotalVenda;
    @FXML private Label lblDescontoGlobal;
    @FXML private Label lblTotalVenda;

    @FXML private RadioButton rbDescontoValor;
    @FXML private RadioButton rbDescontoPercentual;
    @FXML private TextField txtDescontoValor;
    @FXML private TextField txtDescontoPercentual;
    @FXML private Button btnAplicarDesconto;

    @FXML private RadioButton rbVendaAVista;
    @FXML private RadioButton rbVendaAPrazo;

    @FXML private Button btnFinalizarVenda;
    @FXML private Button btnCancelarVenda;

    /**
     * Construtor do Controller.
     *
     * Inicializa os Services e a lista observável usada pela TableView.
     */
    public RegistroVendaController() {
        this.vendaService = new VendaService();
        this.produtoService = new ProdutoService();
        this.itensCarrinhoView = FXCollections.observableArrayList();
    }

    /**
     * Inicialização automática do JavaFX após o carregamento do FXML.
     *
     * Este método será executado apenas quando o FXML for ligado a este Controller.
     */
    @FXML
    public void initialize() {
        inicializarVenda();
        configurarCabecalho();
        configurarSpinnerQuantidade();
        configurarTabela();
        atualizarTabelaCarrinho();
        atualizarResumoVenda();
    }

    /**
     * Inicializa uma nova venda em memória.
     *
     * @implNote Nesta fase, usa usuário temporário apenas para manter a venda em memória.
     * A ligação definitiva com SessaoUsuario será ajustada em etapa futura.
     */
    private void inicializarVenda() {
        this.vendaAtual = new Venda(USUARIO_TEMPORARIO_ID);
    }

    /**
     * Configura informações visuais iniciais do cabeçalho.
     */
    private void configurarCabecalho() {
        if (lblUsuarioLogado != null) {
            lblUsuarioLogado.setText("Usuário: Vendedor |");
        }

        if (lblDataHora != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            lblDataHora.setText(LocalDateTime.now().format(formatter));
        }
    }

    /**
     * Configura o Spinner de quantidade do produto.
     *
     * Quantidade mínima: 1.
     * Quantidade máxima: 999.
     * Valor inicial: 1.
     */
    private void configurarSpinnerQuantidade() {
        if (spnQuantidadeProduto == null) {
            return;
        }

        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1);

        spnQuantidadeProduto.setValueFactory(valueFactory);
        spnQuantidadeProduto.setEditable(true);
    }

    /**
     * Configura as colunas da TableView do carrinho.
     *
     * A tabela usa ItemCarrinhoView, pois a entidade ItemVenda não contém
     * todos os dados formatados necessários para exibição amigável.
     */
    private void configurarTabela() {
        if (tblItensVenda == null) {
            return;
        }

        colProduto.setCellValueFactory(new PropertyValueFactory<>("nomeProduto"));
        colPreco.setCellValueFactory(new PropertyValueFactory<>("precoFormatado"));
        colPromocao.setCellValueFactory(new PropertyValueFactory<>("promocaoFormatada"));
        colQuantidade.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotalFormatado"));

        tblItensVenda.setItems(itensCarrinhoView);
    }

    /**
     * Atualiza a TableView com base nos itens atuais da venda em memória.
     */
    private void atualizarTabelaCarrinho() {
        itensCarrinhoView.clear();

        if (vendaAtual == null || vendaAtual.getItens() == null) {
            return;
        }

        for (ItemVenda itemVenda : vendaAtual.getItens()) {
            itensCarrinhoView.add(converterParaItemCarrinhoView(itemVenda));
        }
    }

    /**
     * Converte um ItemVenda em ItemCarrinhoView para exibição na TableView.
     *
     * Esta conversão é responsabilidade de apresentação, não regra de negócio.
     */
    private ItemCarrinhoView converterParaItemCarrinhoView(ItemVenda itemVenda) {
        Produto produto = produtoService.buscarPorId(itemVenda.getProdutoId());

        return new ItemCarrinhoView(
                itemVenda.getProdutoId(),
                produto.getDescricao(),
                formatarMoeda(itemVenda.getPrecoUnitario()),
                formatarPromocao(itemVenda),
                itemVenda.getQuantidade(),
                formatarMoeda(itemVenda.getSubtotal()),
                itemVenda
        );
    }

    /**
     * Atualiza os labels do resumo da venda.
     */
    private void atualizarResumoVenda() {
        if (vendaAtual == null) {
            return;
        }

        BigDecimal descontoGlobal = vendaAtual.getValorDescontoGlobal();
        BigDecimal total = vendaAtual.getValorTotal();

        BigDecimal subtotal = total.add(descontoGlobal);

        if (lblSubtotalVenda != null) {
            lblSubtotalVenda.setText(formatarMoeda(subtotal));
        }

        if (lblDescontoGlobal != null) {
            lblDescontoGlobal.setText(formatarMoeda(descontoGlobal));
        }

        if (lblTotalVenda != null) {
            lblTotalVenda.setText(formatarMoeda(total));
        }
    }

    /**
     * Formata valores monetários para exibição em padrão brasileiro.
     */
    private String formatarMoeda(BigDecimal valor) {
        BigDecimal valorSeguro = valor != null ? valor : BigDecimal.ZERO;

        NumberFormat formatoMoeda = NumberFormat.getCurrencyInstance(
                new Locale("pt", "BR")
        );

        return formatoMoeda.format(valorSeguro);
    }

    /**
     * Formata a informação de promoção para a coluna da tabela.
     */
    private String formatarPromocao(ItemVenda itemVenda) {
        BigDecimal descontoPromocional = itemVenda.getDescontoPromocional();

        if (descontoPromocional != null && descontoPromocional.compareTo(BigDecimal.ZERO) > 0) {
            return "Sim - " + formatarMoeda(descontoPromocional);
        }

        return "Não";
    }
}