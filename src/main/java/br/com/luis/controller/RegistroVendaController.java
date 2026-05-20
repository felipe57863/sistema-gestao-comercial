package br.com.luis.controller;

import br.com.luis.model.ItemVenda;
import br.com.luis.model.Produto;
import br.com.luis.model.Venda;
import br.com.luis.model.TipoDescontoGlobal;
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
import javafx.scene.control.Alert;

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
        configurarCamposDescontoGlobal();
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

    /**
     * Evento do botão Adicionar Produto.
     *
     * Nesta primeira versão da tela, o campo txtBuscaProduto é tratado
     * temporariamente como ID do produto.
     *
     * O Controller apenas captura os dados da tela, chama o VendaService
     * e atualiza a interface.
     *
     * Não salva venda, não baixa estoque e não executa financeiro.
     */
    @FXML
    private void onAdicionarProduto() {

        try {
            Integer idProduto = obterIdProdutoInformado();
            Integer quantidade = obterQuantidadeInformada();

            vendaService.adicionarItemAoCarrinho(vendaAtual, idProduto, quantidade);

            atualizarTabelaCarrinho();
            atualizarResumoVenda();
            limparCamposProduto();

        } catch (IllegalArgumentException e) {
            exibirErro(e.getMessage());

        } catch (RuntimeException e) {
            exibirErro("Não foi possível adicionar o produto ao carrinho.");
            System.err.println("[ERRO] Falha inesperada ao adicionar produto ao carrinho.");
            e.printStackTrace();
        }
    }

    /**
     * Obtém e valida o ID do produto informado na tela.
     *
     * Nesta fase, txtBuscaProduto representa temporariamente o ID do produto.
     */
    private Integer obterIdProdutoInformado() {

        String textoProduto = txtBuscaProduto.getText();

        if (textoProduto == null || textoProduto.isBlank()) {
            throw new IllegalArgumentException("Informe o ID do produto.");
        }

        try {
            Integer idProduto = Integer.parseInt(textoProduto.trim());

            if (idProduto <= 0) {
                throw new IllegalArgumentException("O ID do produto deve ser maior que zero.");
            }

            return idProduto;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("O ID do produto deve ser um número válido.");
        }
    }

    /**
     * Obtém e valida a quantidade informada no Spinner.
     *
     * Como o Spinner é editável, o valor digitado manualmente também é tratado.
     */
    private Integer obterQuantidadeInformada() {

        String textoQuantidade = spnQuantidadeProduto.getEditor().getText();

        if (textoQuantidade == null || textoQuantidade.isBlank()) {
            throw new IllegalArgumentException("Informe uma quantidade válida.");
        }

        try {
            Integer quantidade = Integer.parseInt(textoQuantidade.trim());

            if (quantidade <= 0) {
                throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
            }

            spnQuantidadeProduto.getValueFactory().setValue(quantidade);

            return quantidade;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("A quantidade deve ser um número válido.");
        }
    }

    /**
     * Limpa os campos da área de produto após adicionar item ao carrinho.
     */
    private void limparCamposProduto() {
        txtBuscaProduto.clear();

        if (spnQuantidadeProduto.getValueFactory() != null) {
            spnQuantidadeProduto.getValueFactory().setValue(1);
        }

        txtBuscaProduto.requestFocus();
    }

    /**
     * Exibe uma mensagem de erro amigável para o usuário.
     */
    private void exibirErro(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    /**
     * Evento do botão Remover Produto.
     *
     * Remove do carrinho o item selecionado na TableView.
     *
     * O Controller apenas identifica o item selecionado, chama o VendaService
     * e atualiza a interface.
     *
     * Não salva venda, não baixa estoque e não executa financeiro.
     */
    @FXML
    private void onRemoverProduto() {

        try {
            ItemCarrinhoView itemSelecionado = tblItensVenda
                    .getSelectionModel()
                    .getSelectedItem();

            if (itemSelecionado == null) {
                throw new IllegalArgumentException("Selecione um produto para remover.");
            }

            ItemVenda itemVenda = itemSelecionado.getItemVenda();

            vendaService.removerItemDoCarrinho(vendaAtual, itemVenda);

            atualizarTabelaCarrinho();
            atualizarResumoVenda();

        } catch (IllegalArgumentException e) {
            exibirErro(e.getMessage());

        } catch (RuntimeException e) {
            exibirErro("Não foi possível remover o produto do carrinho.");
            System.err.println("[ERRO] Falha inesperada ao remover produto do carrinho.");
            e.printStackTrace();
        }
    }

    /**
     * Evento do botão Limpar Venda.
     *
     * Limpa todos os itens do carrinho em memória.
     *
     * O Controller apenas solicita a limpeza ao VendaService
     * e atualiza a interface.
     *
     * Não salva venda, não baixa estoque e não executa financeiro.
     */
    @FXML
    private void onLimparVenda() {

        try {
            vendaService.limparCarrinho(vendaAtual);

            atualizarTabelaCarrinho();
            atualizarResumoVenda();
            limparCamposProduto();

        } catch (IllegalArgumentException e) {
            exibirErro(e.getMessage());

        } catch (RuntimeException e) {
            exibirErro("Não foi possível limpar a venda.");
            System.err.println("[ERRO] Falha inesperada ao limpar venda.");
            e.printStackTrace();
        }
    }

    /**
     * Configura o comportamento visual dos campos de desconto global.
     *
     * Quando "Valor R$" estiver selecionado, habilita apenas o campo de valor.
     * Quando "Percentual %" estiver selecionado, habilita apenas o campo percentual.
     */
    private void configurarCamposDescontoGlobal() {

        txtDescontoValor.setDisable(true);
        txtDescontoPercentual.setDisable(true);

        rbDescontoValor.selectedProperty().addListener((observable, valorAntigo, selecionado) -> {
            if (selecionado) {
                txtDescontoValor.setDisable(false);
                txtDescontoPercentual.setDisable(true);
            }
        });

        rbDescontoPercentual.selectedProperty().addListener((observable, valorAntigo, selecionado) -> {
            if (selecionado) {
                txtDescontoPercentual.setDisable(false);
                txtDescontoValor.setDisable(true);
            }
        });
    }

    /**
     * Evento do botão Aplicar Desconto.
     *
     * O Controller apenas identifica o tipo de desconto, lê o valor informado,
     * chama o VendaService e atualiza a interface.
     *
     * Não calcula desconto global, não salva venda, não baixa estoque
     * e não executa financeiro.
     */
    @FXML
    private void onAplicarDesconto() {

        try {
            TipoDescontoGlobal tipoDescontoGlobal = obterTipoDescontoGlobalSelecionado();
            BigDecimal valorDesconto = obterValorDescontoInformado(tipoDescontoGlobal);

            vendaService.aplicarDescontoGlobal(
                    vendaAtual,
                    tipoDescontoGlobal,
                    valorDesconto
            );

            atualizarTabelaCarrinho();
            atualizarResumoVenda();

        } catch (IllegalArgumentException e) {
            exibirErro(e.getMessage());

        } catch (RuntimeException e) {
            exibirErro("Não foi possível aplicar o desconto.");
            System.err.println("[ERRO] Falha inesperada ao aplicar desconto global.");
            e.printStackTrace();
        }
    }

    /**
     * Identifica qual tipo de desconto global foi selecionado na tela.
     */
    private TipoDescontoGlobal obterTipoDescontoGlobalSelecionado() {

        if (rbDescontoValor.isSelected()) {
            return TipoDescontoGlobal.VALOR_FIXO;
        }

        if (rbDescontoPercentual.isSelected()) {
            return TipoDescontoGlobal.PERCENTUAL;
        }

        throw new IllegalArgumentException("Selecione o tipo de desconto.");
    }

    /**
     * Obtém o valor de desconto informado de acordo com o tipo selecionado.
     */
    private BigDecimal obterValorDescontoInformado(TipoDescontoGlobal tipoDescontoGlobal) {

        String textoValor;

        if (tipoDescontoGlobal == TipoDescontoGlobal.VALOR_FIXO) {
            textoValor = txtDescontoValor.getText();
        } else {
            textoValor = txtDescontoPercentual.getText();
        }

        return converterTextoParaBigDecimal(textoValor);
    }

    /**
     * Converte texto digitado na tela para BigDecimal.
     *
     * Aceita vírgula decimal brasileira.
     * Exemplo:
     * "10,50" vira "10.50".
     */
    private BigDecimal converterTextoParaBigDecimal(String textoValor) {

        if (textoValor == null || textoValor.isBlank()) {
            throw new IllegalArgumentException("Informe o valor do desconto.");
        }

        String textoNormalizado = textoValor
                .trim()
                .replace(",", ".");

        try {
            BigDecimal valor = new BigDecimal(textoNormalizado);

            if (valor.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("O desconto não pode ser negativo.");
            }

            return valor;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Informe um valor de desconto válido.");
        }
    }
}