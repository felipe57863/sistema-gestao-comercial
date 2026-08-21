package br.com.luis.controller;

import br.com.luis.model.Cliente;
import br.com.luis.model.ItemVenda;
import br.com.luis.model.Produto;
import br.com.luis.model.TipoDescontoGlobal;
import br.com.luis.model.Venda;
import br.com.luis.model.Usuario;
import br.com.luis.model.TipoVenda;
import br.com.luis.model.FormaPagamento;
import br.com.luis.model.PrazoPagamento;
import br.com.luis.util.CabecalhoUtil;
import br.com.luis.util.NavegacaoUtil;
import br.com.luis.util.SessaoUsuario;
import br.com.luis.service.ClienteService;
import br.com.luis.service.NotaVendaService;
import br.com.luis.service.ProdutoService;
import br.com.luis.service.VendaService;
import br.com.luis.service.PrazoPagamentoService;
import br.com.luis.util.TipoViaNotaVendaPdf;
import br.com.luis.viewmodel.ItemCarrinhoView;
import br.com.luis.viewmodel.ResultadoFinalizacaoVenda;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.event.ActionEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Map;

/**
 * Controller da tela Registro de Venda.
 *
 * Mantém a venda atual e seu carrinho em memória enquanto o usuário interage
 * com a tela. Coordena a busca e a seleção de produtos, clientes e prazos,
 * recebe os dados visuais de desconto e pagamento e apresenta os resultados.
 *
 * A finalização à vista ou a prazo é delegada ao VendaService, responsável
 * pelas validações de negócio, persistência, baixa de estoque e geração da
 * movimentação financeira ou da conta a receber. O Controller não acessa DAO
 * diretamente e limpa a tela somente após a finalização bem-sucedida.
 */
public class RegistroVendaController {

    private static final int QUANTIDADE_INICIAL_ITEM = 1;

    private final VendaService vendaService;
    private final ProdutoService produtoService;
    private final ClienteService clienteService;
    private final PrazoPagamentoService prazoPagamentoService;
    private final NotaVendaService notaVendaService;
    private final ObservableList<ItemCarrinhoView> itensCarrinhoView;

    private Venda vendaAtual;
    private Cliente clienteSelecionado;

    @FXML private Button btnVoltar;
    @FXML private Label lblUsuarioLogado;
    @FXML private Label lblDataHora;

    @FXML private TextField txtBuscaProduto;

    @FXML private TableView<ItemCarrinhoView> tblItensVenda;
    @FXML private TableColumn<ItemCarrinhoView, String> colProduto;
    @FXML private TableColumn<ItemCarrinhoView, String> colPreco;
    @FXML private TableColumn<ItemCarrinhoView, String> colPromocao;
    @FXML private TableColumn<ItemCarrinhoView, Integer> colQuantidade;
    @FXML private TableColumn<ItemCarrinhoView, String> colSubtotal;
    @FXML private Button btnRemoverProduto;
    @FXML private Button btnLimparVenda;
    @FXML private Button btnAplicarDesconto;
    @FXML private Button btnFinalizarVenda;

    @FXML private TextField txtBuscaCliente;
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

    @FXML private RadioButton rbVendaAVista;
    @FXML private RadioButton rbVendaAPrazo;

    @FXML private ToggleGroup tgTipoDescontoGlobal;
    @FXML private ToggleGroup tgTipoVenda;

    /**
     * Construtor do Controller.
     *
     * Inicializa os Services e a lista observável usada pela TableView.
     */
    public RegistroVendaController() {
        this.vendaService = new VendaService();
        this.produtoService = new ProdutoService();
        this.clienteService = new ClienteService();
        this.prazoPagamentoService = new PrazoPagamentoService();
        this.notaVendaService = new NotaVendaService();
        this.itensCarrinhoView = FXCollections.observableArrayList();
    }

    /**
     * Inicialização automática do JavaFX após o carregamento do FXML.
     */
    @FXML
    public void initialize() {
        inicializarVenda();
        configurarCabecalho();
        configurarTabela();
        configurarCamposDescontoGlobal();
        atualizarTabelaCarrinho();
        atualizarResumoVenda();
    }

    /**
     * Inicializa uma nova venda em memória.
     *
     * Vincula a nova venda ao usuário válido obtido da SessaoUsuario.
     * Uma sessão inválida interrompe a inicialização.
     */
    private void inicializarVenda() {
        this.vendaAtual = new Venda(obterUsuarioIdAtual());
    }

    /**
     * Obtém o ID do usuário atualmente logado.
     *
     * Consulta a SessaoUsuario e retorna somente um ID real e válido.
     * Lança IllegalStateException quando a sessão ou o ID forem inválidos.
     */
    private Integer obterUsuarioIdAtual() {

        Usuario usuarioLogado = SessaoUsuario.getInstance().getUsuarioLogado();

        if (usuarioLogado == null) {
            throw new IllegalStateException(
                    "Não foi possível identificar o usuário logado. Entre novamente no sistema."
            );
        }

        Integer usuarioId = usuarioLogado.getIdUsuario();

        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalStateException(
                    "Não foi possível identificar o usuário logado. Entre novamente no sistema."
            );
        }

        return usuarioId;
    }

    /**
     * Configura informações visuais iniciais do cabeçalho.
     */
    private void configurarCabecalho() {
        CabecalhoUtil.configurarUsuarioEDataHora(
                lblUsuarioLogado,
                lblDataHora
        );
    }

    /**
     * Retorna para a Tela Principal usando o mesmo Stage atual.
     * Solicita confirmação somente quando o carrinho possui itens.
     */
    @FXML
    private void onVoltar() {

        if (!confirmarSaidaComVendaEmAndamento()) {
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

            exibirErro("Não foi possível retornar para a Tela Principal.");
        }
    }

    /**
     * Confirma a saída quando existe uma venda com itens no carrinho.
     *
     * Não altera a venda atual e não executa navegação ou regras de negócio.
     */
    private boolean confirmarSaidaComVendaEmAndamento() {

        if (!itensCarrinhoView.isEmpty()) {
            Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
            alerta.setTitle("Venda em andamento");
            alerta.setHeaderText(null);
            alerta.setContentText(
                    "Existe uma venda ainda não finalizada.\n"
                            + "Ao voltar, os itens do carrinho serão descartados.\n"
                            + "Deseja realmente sair desta tela?"
            );

            ButtonType botaoSair = new ButtonType(
                    "Sair da venda",
                    ButtonBar.ButtonData.OK_DONE
            );

            ButtonType botaoContinuar = new ButtonType(
                    "Continuar na venda",
                    ButtonBar.ButtonData.CANCEL_CLOSE
            );

            alerta.getButtonTypes().setAll(
                    botaoSair,
                    botaoContinuar
            );

            Optional<ButtonType> resultado = alerta.showAndWait();

            return resultado.isPresent()
                    && resultado.get() == botaoSair;
        }

        return true;
    }

    /**
     * Configura as colunas da TableView do carrinho.
     *
     * A tabela usa ItemCarrinhoView, pois a entidade ItemVenda não contém
     * todos os dados formatados necessários para exibição amigável.
     *
     * A coluna Quantidade é editável para permitir que o vendedor altere
     * diretamente a quantidade final do item no carrinho.
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

        tblItensVenda.setEditable(true);
        colQuantidade.setEditable(true);
        colQuantidade.setCellFactory(coluna -> criarCelulaQuantidadeEditavel());

        tblItensVenda.setItems(itensCarrinhoView);
        tblItensVenda.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, atual) ->
                        atualizarEstadoAcoesVenda()
                );
    }

    /**
     * Cria uma célula editável para a coluna Quantidade.
     *
     * A célula usa TextField com TextFormatter para aceitar apenas dígitos.
     * A validação de estoque e regras de negócio continuam no VendaService.
     */
    private TableCell<ItemCarrinhoView, Integer> criarCelulaQuantidadeEditavel() {

        return new TableCell<>() {

            private TextField textField;
            private boolean finalizandoEdicao;

            @Override
            public void startEdit() {
                if (isEmpty()) {
                    return;
                }

                super.startEdit();

                criarTextFieldSeNecessario();

                Integer quantidadeAtual = getItem();
                textField.setText(quantidadeAtual != null ? quantidadeAtual.toString() : "");

                setText(null);
                setGraphic(textField);

                textField.selectAll();
                textField.requestFocus();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();

                Integer quantidadeAtual = getItem();
                setText(quantidadeAtual != null ? quantidadeAtual.toString() : "");
                setGraphic(null);
            }

            @Override
            protected void updateItem(Integer quantidade, boolean vazio) {
                super.updateItem(quantidade, vazio);

                if (vazio) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                if (isEditing()) {
                    criarTextFieldSeNecessario();
                    textField.setText(quantidade != null ? quantidade.toString() : "");
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(quantidade != null ? quantidade.toString() : "");
                    setGraphic(null);
                }
            }

            private void criarTextFieldSeNecessario() {
                if (textField != null) {
                    return;
                }

                textField = new TextField();

                TextFormatter<String> textFormatter = new TextFormatter<>(change -> {
                    String novoTexto = change.getControlNewText();

                    if (novoTexto.matches("\\d*")) {
                        return change;
                    }

                    return null;
                });

                textField.setTextFormatter(textFormatter);

                textField.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ENTER) {
                        confirmarEdicao();
                        event.consume();
                    }

                    if (event.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                        event.consume();
                    }
                });

                textField.focusedProperty().addListener((observable, estavaFocado, estaFocado) -> {
                    if (!estaFocado && isEditing()) {
                        confirmarEdicao();
                    }
                });
            }

            private void confirmarEdicao() {
                if (finalizandoEdicao) {
                    return;
                }

                finalizandoEdicao = true;

                try {
                    confirmarEdicaoQuantidade(textField.getText(), this);
                } finally {
                    finalizandoEdicao = false;
                }
            }
        };
    }

    /**
     * Confirma a edição da quantidade digitada na célula da TableView.
     *
     * O Controller valida apenas a entrada básica da tela.
     * A regra de estoque, promoção, subtotal e desconto global fica no VendaService.
     */
    private void confirmarEdicaoQuantidade(
            String textoQuantidade,
            TableCell<ItemCarrinhoView, Integer> celula
    ) {

        ItemCarrinhoView itemCarrinhoView = celula.getTableRow().getItem();

        try {
            if (itemCarrinhoView == null || itemCarrinhoView.getItemVenda() == null) {
                throw new IllegalArgumentException("Item inválido para atualizar quantidade.");
            }

            Integer novaQuantidade = converterTextoQuantidadeEditada(textoQuantidade);

            vendaService.atualizarQuantidadeItemCarrinho(
                    vendaAtual,
                    itemCarrinhoView.getItemVenda(),
                    novaQuantidade
            );

            celula.commitEdit(novaQuantidade);

            atualizarTabelaCarrinho();
            atualizarResumoVenda();
            limparCamposDescontoGlobal();

        } catch (IllegalArgumentException e) {
            exibirErro(e.getMessage());

            celula.cancelEdit();
            atualizarTabelaCarrinho();
            atualizarResumoVenda();

        } catch (RuntimeException e) {
            exibirErro("Não foi possível atualizar a quantidade do item.");
            System.err.println("[ERRO] Falha inesperada ao atualizar quantidade do item.");
            e.printStackTrace();

            celula.cancelEdit();
            atualizarTabelaCarrinho();
            atualizarResumoVenda();
        }
    }

    /**
     * Converte e valida a quantidade digitada na célula editável.
     */
    private Integer converterTextoQuantidadeEditada(String textoQuantidade) {

        if (textoQuantidade == null || textoQuantidade.isBlank()) {
            throw new IllegalArgumentException("Informe uma quantidade inteira maior que zero.");
        }

        try {
            Integer quantidade = Integer.parseInt(textoQuantidade.trim());

            if (quantidade <= 0) {
                throw new IllegalArgumentException("Informe uma quantidade inteira maior que zero.");
            }

            return quantidade;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Informe uma quantidade inteira maior que zero.");
        }
    }

    /**
     * Atualiza a TableView com base nos itens atuais da venda em memória.
     */
    private void atualizarTabelaCarrinho() {
        itensCarrinhoView.clear();

        if (vendaAtual == null || vendaAtual.getItens() == null) {
            atualizarEstadoAcoesVenda();
            return;
        }

        for (ItemVenda itemVenda : vendaAtual.getItens()) {
            itensCarrinhoView.add(converterParaItemCarrinhoView(itemVenda));
        }

        atualizarEstadoAcoesVenda();
    }

    private void atualizarEstadoAcoesVenda() {
        boolean carrinhoVazio = itensCarrinhoView.isEmpty();
        boolean semSelecao = tblItensVenda
                .getSelectionModel()
                .getSelectedItem() == null;

        btnRemoverProduto.setDisable(carrinhoVazio || semSelecao);
        btnLimparVenda.setDisable(carrinhoVazio);
        btnFinalizarVenda.setDisable(carrinhoVazio);

        atualizarEstadoAreaDescontoGlobal();
    }

    /**
     * Atualiza exclusivamente o estado visual da área de desconto global.
     *
     * A elegibilidade dos itens é consultada no VendaService. Quando não existe
     * item elegível, toda a área é desabilitada, limpa e fica sem seleção.
     */
    private void atualizarEstadoAreaDescontoGlobal() {
        boolean possuiItemElegivel = vendaService
                .possuiItemElegivelParaDescontoGlobal(vendaAtual);

        rbDescontoValor.setDisable(!possuiItemElegivel);
        rbDescontoPercentual.setDisable(!possuiItemElegivel);
        btnAplicarDesconto.setDisable(!possuiItemElegivel);

        if (!possuiItemElegivel) {
            limparCamposDescontoGlobal();
            return;
        }

        txtDescontoValor.setDisable(!rbDescontoValor.isSelected());
        txtDescontoPercentual.setDisable(!rbDescontoPercentual.isSelected());
    }

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

        BigDecimal subtotal = vendaService.calcularSubtotalBruto(vendaAtual);
        BigDecimal descontoTotal = vendaService.calcularDescontoTotal(vendaAtual);
        BigDecimal total = vendaAtual.getValorTotal();

        if (lblSubtotalVenda != null) {
            lblSubtotalVenda.setText(formatarMoeda(subtotal));
        }

        if (lblDescontoGlobal != null) {
            lblDescontoGlobal.setText(formatarMoeda(descontoTotal));
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

    private void adicionarProdutoAoCarrinho(Integer idProduto, Integer quantidade) {

        vendaService.adicionarItemAoCarrinho(vendaAtual, idProduto, quantidade);

        atualizarTabelaCarrinho();
        atualizarResumoVenda();
        limparCamposDescontoGlobal();
        limparCamposProduto();
    }

    /**
     * Evento do botão Adicionar Produto.
     *
     * O campo txtBuscaProduto aceita:
     * - apenas números: trata como ID do produto;
     * - texto: busca produtos por descrição e abre uma caixa de seleção.
     *
     * Ao adicionar um produto, a quantidade inicial é sempre 1.
     * A quantidade final deve ser editada diretamente na coluna Quantidade
     * da TableView.
     *
     * O Controller apenas captura os dados da tela, chama o VendaService
     * e atualiza a interface.
     *
     * Não valida estoque, não valida produto ativo, não calcula promoção
     * e não calcula subtotal. Essas regras continuam no VendaService.
     */
    @FXML
    private void onAdicionarProduto() {

        try {
            String textoBusca = obterTextoBuscaProduto();
            Integer quantidade = QUANTIDADE_INICIAL_ITEM;

            if (textoEhNumero(textoBusca)) {
                Integer idProduto = converterTextoParaIdProduto(textoBusca);
                adicionarProdutoAoCarrinho(idProduto, quantidade);
                return;
            }

            Optional<Produto> produtoSelecionado = abrirDialogSelecaoProduto(textoBusca);

            if (produtoSelecionado.isEmpty()) {
                return;
            }

            adicionarProdutoAoCarrinho(
                    produtoSelecionado.get().getIdProduto(),
                    quantidade
            );

        } catch (IllegalArgumentException e) {
            exibirErro(e.getMessage());

        } catch (RuntimeException e) {
            exibirErro("Não foi possível adicionar o produto ao carrinho.");
            System.err.println("[ERRO] Falha inesperada ao adicionar produto ao carrinho.");
            e.printStackTrace();
        }
    }

    private String obterTextoBuscaProduto() {

        String textoBusca = txtBuscaProduto.getText();

        if (textoBusca == null || textoBusca.isBlank()) {
            throw new IllegalArgumentException("Informe o ID ou a descrição do produto.");
        }

        return textoBusca.trim();
    }

    private boolean textoEhNumero(String texto) {
        return texto != null && texto.matches("\\d+");
    }

    private Integer converterTextoParaIdProduto(String textoProduto) {

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

    private void limparCamposProduto() {
        txtBuscaProduto.clear();
        txtBuscaProduto.requestFocus();
    }

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
     * A remoção atua somente sobre a venda atual ainda não finalizada. Não
     * persiste alterações, não baixa estoque e não executa operações financeiras.
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
            limparCamposDescontoGlobal();

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
     * A limpeza atua somente sobre a venda atual ainda não finalizada. Não
     * persiste alterações, não baixa estoque e não executa operações financeiras.
     */
    @FXML
    private void onLimparVenda() {
        if (!confirmarLimpezaVenda()) {
            return;
        }

        try {
            vendaService.limparCarrinho(vendaAtual);

            atualizarTabelaCarrinho();
            atualizarResumoVenda();
            limparCamposProduto();
            limparCamposDescontoGlobal();

        } catch (IllegalArgumentException e) {
            exibirErro(e.getMessage());

        } catch (RuntimeException e) {
            exibirErro("Não foi possível limpar a venda.");
            System.err.println("[ERRO] Falha inesperada ao limpar venda.");
            e.printStackTrace();
        }
    }

    private boolean confirmarLimpezaVenda() {
        if (itensCarrinhoView.isEmpty()) {
            return true;
        }

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Limpar venda");
        alerta.setHeaderText(null);
        alerta.setContentText(
                "Todos os itens e dados da venda atual serão descartados.\n"
                        + "Deseja realmente limpar a venda?"
        );

        ButtonType botaoLimpar = new ButtonType(
                "Limpar venda",
                ButtonBar.ButtonData.OK_DONE
        );
        ButtonType botaoContinuar = new ButtonType(
                "Continuar na venda",
                ButtonBar.ButtonData.CANCEL_CLOSE
        );

        alerta.getButtonTypes().setAll(
                botaoLimpar,
                botaoContinuar
        );

        return alerta.showAndWait().orElse(botaoContinuar) == botaoLimpar;
    }

    /**
     * Configura o comportamento visual dos campos de desconto global.
     *
     * Quando "Valor R$" estiver selecionado, habilita apenas o campo de valor
     * e limpa o campo de percentual.
     *
     * Quando "Percentual %" estiver selecionado, habilita apenas o campo percentual
     * e limpa o campo de valor.
     */
    private void configurarCamposDescontoGlobal() {

        txtDescontoValor.setDisable(true);
        txtDescontoPercentual.setDisable(true);

        rbDescontoValor.selectedProperty().addListener((observable, valorAntigo, selecionado) -> {
            if (selecionado) {
                boolean possuiItemElegivel = vendaService
                        .possuiItemElegivelParaDescontoGlobal(vendaAtual);

                txtDescontoValor.setDisable(!possuiItemElegivel);

                txtDescontoPercentual.clear();
                txtDescontoPercentual.setDisable(true);

                if (possuiItemElegivel) {
                    txtDescontoValor.requestFocus();
                }
            }
        });

        rbDescontoPercentual.selectedProperty().addListener((observable, valorAntigo, selecionado) -> {
            if (selecionado) {
                boolean possuiItemElegivel = vendaService
                        .possuiItemElegivelParaDescontoGlobal(vendaAtual);

                txtDescontoPercentual.setDisable(!possuiItemElegivel);

                txtDescontoValor.clear();
                txtDescontoValor.setDisable(true);

                if (possuiItemElegivel) {
                    txtDescontoPercentual.requestFocus();
                }
            }
        });
    }

    /**
     * Evento do botão Aplicar Desconto.
     *
     * O Controller apenas identifica o tipo de desconto, lê o valor informado,
     * chama o VendaService e atualiza a interface.
     *
     * O cálculo e a distribuição do desconto pertencem ao VendaService e afetam
     * somente a venda atual em memória até que ela seja finalizada.
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

    /**
     * Evento do botão Cancelar Venda.
     *
     * Descarta a venda atual ainda não finalizada e retorna a tela ao estado
     * inicial, criando uma nova venda em memória.
     *
     * Não persiste a venda descartada, não baixa estoque, não gera financeiro
     * e não representa estorno de uma venda já persistida.
     */
    @FXML
    private void onCancelarVenda() {
        if (!confirmarCancelamentoVenda()) {
            return;
        }

        try {
            inicializarVenda();

            atualizarTabelaCarrinho();
            atualizarResumoVenda();

            limparCamposProduto();
            limparCamposDescontoGlobal();
            limparSelecaoTipoVenda();
            limparAreaCliente();

        } catch (RuntimeException e) {
            exibirErro("Não foi possível cancelar a venda.");
            System.err.println("[ERRO] Falha inesperada ao cancelar venda.");
            e.printStackTrace();
        }
    }

    private boolean confirmarCancelamentoVenda() {
        if (itensCarrinhoView.isEmpty()) {
            return true;
        }

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Cancelar venda");
        alerta.setHeaderText(null);
        alerta.setContentText(
                "A venda atual ainda não foi finalizada.\n"
                        + "Todos os dados da venda serão descartados.\n"
                        + "Deseja realmente cancelar?"
        );

        ButtonType botaoCancelar = new ButtonType(
                "Cancelar venda",
                ButtonBar.ButtonData.OK_DONE
        );
        ButtonType botaoContinuar = new ButtonType(
                "Continuar na venda",
                ButtonBar.ButtonData.CANCEL_CLOSE
        );

        alerta.getButtonTypes().setAll(
                botaoCancelar,
                botaoContinuar
        );

        return alerta.showAndWait().orElse(botaoContinuar) == botaoCancelar;
    }

    /**
     * Limpa os campos da área de desconto global e retorna os campos
     * ao estado inicial.
     */
    private void limparCamposDescontoGlobal() {

        txtDescontoValor.clear();
        txtDescontoPercentual.clear();

        if (tgTipoDescontoGlobal != null) {
            tgTipoDescontoGlobal.selectToggle(null);
        } else {
            rbDescontoValor.setSelected(false);
            rbDescontoPercentual.setSelected(false);
        }

        txtDescontoValor.setDisable(true);
        txtDescontoPercentual.setDisable(true);
    }

    /**
     * Obtém o tipo de venda selecionado na tela.
     *
     * O Controller apenas identifica a seleção visual.
     * As regras de finalização continuam no VendaService.
     */
    private TipoVenda obterTipoVendaSelecionado() {

        if (rbVendaAVista.isSelected()) {
            return TipoVenda.A_VISTA;
        }

        if (rbVendaAPrazo.isSelected()) {
            return TipoVenda.A_PRAZO;
        }

        throw new IllegalArgumentException("Selecione o tipo de venda.");
    }

    /**
     * Limpa a seleção do tipo de venda.
     * Esta operação altera somente o estado visual dos RadioButtons.
     */
    private void limparSelecaoTipoVenda() {

        if (tgTipoVenda != null) {
            tgTipoVenda.selectToggle(null);
        } else {
            rbVendaAVista.setSelected(false);
            rbVendaAPrazo.setSelected(false);
        }
    }

    /**
     * Limpa a área visual de cliente.
     *
     * Remove o cliente selecionado e restaura os campos de busca, nome, status
     * e limite disponível ao estado inicial.
     */
    private void limparAreaCliente() {

        clienteSelecionado = null;

        txtBuscaCliente.clear();
        lblNomeCliente.setText("-");
        lblStatusCliente.setText("-");
        lblLimiteDisponivel.setText("R$ 0,00");
    }

    /**
     * Atualiza a área visual de cliente com base no cliente selecionado.
     *
     * Solicita ao VendaService o cálculo do limite de crédito disponível e apenas
     * formata e exibe os dados. O Controller não calcula o limite nem acessa DAO.
     */
    private void atualizarAreaClienteSelecionado() {

        if (clienteSelecionado == null) {
            limparAreaCliente();
            return;
        }

        BigDecimal limiteDisponivel = vendaService.calcularLimiteCreditoDisponivel(
                clienteSelecionado.getIdCliente()
        );

        lblNomeCliente.setText(
                clienteSelecionado.getNome() != null
                        ? clienteSelecionado.getNome()
                        : "-"
        );

        if (clienteSelecionado.getStatus() != null) {
            lblStatusCliente.setText(clienteSelecionado.getStatus().name());
        } else {
            lblStatusCliente.setText("-");
        }

        lblLimiteDisponivel.setText(formatarMoeda(limiteDisponivel));
    }

    /**
     * Solicita os dados de pagamento para uma venda à vista.
     *
     * O Dialog permite selecionar DINHEIRO, PIX ou CARTAO. Para DINHEIRO, exibe
     * o campo de valor recebido e valida visualmente seu preenchimento, formato
     * e valor positivo. As validações de negócio, inclusive a suficiência do
     * valor recebido, permanecem no VendaService.
     *
     * Se o usuário cancelar ou fechar a janela, retorna Optional.empty() e a
     * venda atual permanece inalterada. O troco é calculado pelo VendaService,
     * apresentado após o sucesso e não é persistido no banco de dados.
     */
    private Optional<DadosPagamentoAVista> solicitarDadosPagamentoAVista() {

        Dialog<DadosPagamentoAVista> dialog = new Dialog<>();
        dialog.setTitle("Pagamento à vista");
        dialog.setHeaderText("Informe a forma de pagamento da venda à vista.");

        ButtonType botaoConfirmar = new ButtonType(
                "Confirmar",
                ButtonBar.ButtonData.OK_DONE
        );

        dialog.getDialogPane().getButtonTypes().addAll(
                botaoConfirmar,
                ButtonType.CANCEL
        );

        RadioButton rbPagamentoDinheiro = new RadioButton("Dinheiro");
        RadioButton rbPagamentoPix = new RadioButton("PIX");
        RadioButton rbPagamentoCartao = new RadioButton("Cartão");

        ToggleGroup tgFormaPagamento = new ToggleGroup();
        rbPagamentoDinheiro.setToggleGroup(tgFormaPagamento);
        rbPagamentoPix.setToggleGroup(tgFormaPagamento);
        rbPagamentoCartao.setToggleGroup(tgFormaPagamento);

        Label lblValorRecebido = new Label("Valor recebido:");
        TextField txtValorRecebido = new TextField();
        txtValorRecebido.setPromptText("Ex.: 100,00");

        Label lblMensagemErro = new Label();
        lblMensagemErro.setStyle("-fx-text-fill: red;");

        lblValorRecebido.setVisible(false);
        lblValorRecebido.setManaged(false);
        txtValorRecebido.setVisible(false);
        txtValorRecebido.setManaged(false);

        tgFormaPagamento.selectedToggleProperty().addListener((observable, formaAnterior, formaAtual) -> {
            boolean pagamentoEmDinheiro = formaAtual == rbPagamentoDinheiro;

            lblValorRecebido.setVisible(pagamentoEmDinheiro);
            lblValorRecebido.setManaged(pagamentoEmDinheiro);

            txtValorRecebido.setVisible(pagamentoEmDinheiro);
            txtValorRecebido.setManaged(pagamentoEmDinheiro);

            if (pagamentoEmDinheiro) {
                txtValorRecebido.requestFocus();
            } else {
                txtValorRecebido.clear();
            }

            lblMensagemErro.setText("");
        });

        VBox conteudo = new VBox(
                8,
                rbPagamentoDinheiro,
                rbPagamentoPix,
                rbPagamentoCartao,
                lblValorRecebido,
                txtValorRecebido,
                lblMensagemErro
        );

        dialog.getDialogPane().setContent(conteudo);

        final DadosPagamentoAVista[] dadosPagamentoSelecionados = new DadosPagamentoAVista[1];

        Node botaoConfirmarNode = dialog.getDialogPane().lookupButton(botaoConfirmar);

        botaoConfirmarNode.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                lblMensagemErro.setText("");

                FormaPagamento formaPagamento;

                if (rbPagamentoDinheiro.isSelected()) {
                    formaPagamento = FormaPagamento.DINHEIRO;
                } else if (rbPagamentoPix.isSelected()) {
                    formaPagamento = FormaPagamento.PIX;
                } else if (rbPagamentoCartao.isSelected()) {
                    formaPagamento = FormaPagamento.CARTAO;
                } else {
                    throw new IllegalArgumentException("Selecione a forma de pagamento.");
                }

                BigDecimal valorRecebido = null;

                if (formaPagamento == FormaPagamento.DINHEIRO) {
                    String textoValorRecebido = txtValorRecebido.getText();

                    if (textoValorRecebido == null || textoValorRecebido.isBlank()) {
                        throw new IllegalArgumentException("Informe o valor recebido.");
                    }

                    String textoNormalizado = textoValorRecebido
                            .trim()
                            .replace(",", ".");

                    try {
                        valorRecebido = new BigDecimal(textoNormalizado);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Informe um valor recebido válido.");
                    }

                    if (valorRecebido.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("Valor recebido deve ser maior que zero.");
                    }
                }

                dadosPagamentoSelecionados[0] = new DadosPagamentoAVista(
                        formaPagamento,
                        valorRecebido
                );

            } catch (IllegalArgumentException e) {
                lblMensagemErro.setText(e.getMessage());
                event.consume();
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType == botaoConfirmar) {
                return dadosPagamentoSelecionados[0];
            }

            return null;
        });

        return dialog.showAndWait();
    }

    /**
     * Abre um Dialog para seleção do prazo efetivo da venda a prazo.
     *
     * Lista os prazos ativos obtidos pelo PrazoPagamentoService e permite a
     * seleção visual de um deles. Apenas retorna o prazo escolhido e não altera
     * o estado da tela. As validações definitivas do prazo pertencem ao VendaService.
     * Se o Dialog for cancelado ou fechado, retorna Optional.empty().
     */
    private Optional<PrazoPagamento> abrirDialogSelecaoPrazoPagamento() {

        List<PrazoPagamento> prazos = prazoPagamentoService.listarAtivos();

        List<PrazoPagamento> prazosAtivos = prazos != null
                ? prazos.stream()
                  .filter(prazoPagamento -> prazoPagamento != null)
                  .toList()
                : List.of();

        Dialog<PrazoPagamento> dialog = new Dialog<>();
        dialog.setTitle("Selecionar Prazo");
        dialog.setHeaderText("Selecione o prazo efetivo da venda a prazo.");

        TableView<PrazoPagamento> tableViewPrazos = new TableView<>(
                FXCollections.observableArrayList(prazosAtivos)
        );

        tableViewPrazos.setPrefWidth(520);
        tableViewPrazos.setPrefHeight(260);

        Label placeholder = new Label("Nenhum prazo ativo encontrado.");
        placeholder.setWrapText(true);
        tableViewPrazos.setPlaceholder(placeholder);

        TableColumn<PrazoPagamento, String> colunaDescricao = new TableColumn<>("Descrição");
        colunaDescricao.setPrefWidth(320);
        colunaDescricao.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getDescricao() != null
                                ? cellData.getValue().getDescricao()
                                : "-"
                )
        );

        TableColumn<PrazoPagamento, String> colunaDias = new TableColumn<>("Dias");
        colunaDias.setPrefWidth(140);
        colunaDias.setCellValueFactory(cellData -> {
            Integer quantidadeDias = cellData.getValue().getQuantidadeDias();

            return new SimpleStringProperty(
                    quantidadeDias != null
                            ? quantidadeDias.toString()
                            : "-"
            );
        });

        tableViewPrazos.getColumns().addAll(
                List.of(
                        colunaDescricao,
                        colunaDias
                )
        );

        dialog.getDialogPane().setContent(tableViewPrazos);

        boolean encontrouPrazos = !prazosAtivos.isEmpty();

        if (encontrouPrazos) {
            ButtonType botaoSelecionar = new ButtonType(
                    "Selecionar",
                    ButtonBar.ButtonData.OK_DONE
            );

            dialog.getDialogPane().getButtonTypes().addAll(
                    botaoSelecionar,
                    ButtonType.CANCEL
            );

            Node botaoSelecionarNode = dialog.getDialogPane().lookupButton(botaoSelecionar);
            botaoSelecionarNode.setDisable(true);

            tableViewPrazos.getSelectionModel()
                    .selectedItemProperty()
                    .addListener((observable, prazoAnterior, prazoAtual) ->
                            botaoSelecionarNode.setDisable(prazoAtual == null)
                    );

            dialog.setResultConverter(buttonType -> {
                if (buttonType == botaoSelecionar) {
                    return tableViewPrazos.getSelectionModel().getSelectedItem();
                }

                return null;
            });

        } else {
            ButtonType botaoFechar = new ButtonType(
                    "Fechar",
                    ButtonBar.ButtonData.CANCEL_CLOSE
            );

            dialog.getDialogPane().getButtonTypes().add(botaoFechar);
            dialog.setResultConverter(buttonType -> null);
        }

        return dialog.showAndWait();
    }

    /**
     * Finaliza uma venda a prazo usando cliente selecionado e prazo escolhido no Dialog.
     *
     * O Controller verifica a presença dos IDs selecionados e delega ao
     * VendaService as validações de cliente, prazo máximo, limite e estoque,
     * além da persistência da venda, itens e conta a receber.
     *
     * Se a seleção do prazo for cancelada, mantém a venda atual. Após sucesso,
     * apresenta o resultado e limpa a tela; erros são propagados ao evento de
     * finalização, que exibe a mensagem sem descartar o carrinho.
     */
    private ResultadoFinalizacaoVenda finalizarVendaAPrazo() {

        if (clienteSelecionado == null) {
            throw new IllegalArgumentException("Selecione um cliente para a venda a prazo.");
        }

        if (clienteSelecionado.getIdCliente() == null || clienteSelecionado.getIdCliente() <= 0) {
            throw new IllegalArgumentException("Cliente selecionado inválido.");
        }

        Optional<PrazoPagamento> prazoSelecionadoOptional = abrirDialogSelecaoPrazoPagamento();

        if (prazoSelecionadoOptional.isEmpty()) {
            return null;
        }

        PrazoPagamento prazoSelecionado = prazoSelecionadoOptional.get();

        if (prazoSelecionado.getIdPrazo() == null || prazoSelecionado.getIdPrazo() <= 0) {
            throw new IllegalArgumentException("Prazo de pagamento selecionado inválido.");
        }

        return vendaService.finalizarVenda(
                vendaAtual,
                TipoVenda.A_PRAZO,
                FormaPagamento.A_PRAZO,
                null,
                clienteSelecionado.getIdCliente(),
                prazoSelecionado.getIdPrazo(),
                obterUsuarioIdAtual()
        );
    }

    /**
     * Evento do botão Finalizar Venda.
     *
     * Identifica o tipo selecionado e executa o fluxo real de finalização.
     * Para venda à vista, solicita a forma de pagamento e o valor recebido
     * quando necessário. Para venda a prazo, utiliza o cliente selecionado e
     * solicita o prazo efetivo.
     *
     * Em ambos os fluxos, chama VendaService.finalizarVenda(...), responsável
     * pelas validações de negócio e pela persistência transacional. A tela é
     * limpa somente após sucesso. Cancelamentos de Dialog e erros preservam a
     * venda e o carrinho atuais para correção ou nova tentativa.
     */
    @FXML
    private void onFinalizarVenda() {

        TipoVenda tipoVenda;
        ResultadoFinalizacaoVenda resultado;

        try {
            tipoVenda = obterTipoVendaSelecionado();

            if (tipoVenda == TipoVenda.A_PRAZO) {
                resultado = finalizarVendaAPrazo();

                if (resultado == null) {
                    return;
                }
            } else {
                Optional<DadosPagamentoAVista> dadosPagamentoOptional =
                        solicitarDadosPagamentoAVista();

                if (dadosPagamentoOptional.isEmpty()) {
                    return;
                }

                DadosPagamentoAVista dadosPagamento =
                        dadosPagamentoOptional.get();

                resultado = vendaService.finalizarVenda(
                        vendaAtual,
                        TipoVenda.A_VISTA,
                        dadosPagamento.getFormaPagamento(),
                        dadosPagamento.getValorRecebido(),
                        null,
                        null,
                        obterUsuarioIdAtual()
                );
            }

        } catch (IllegalArgumentException | IllegalStateException e) {
            exibirErro(e.getMessage());
            return;

        } catch (Exception e) {
            e.printStackTrace();
            exibirErro("Erro inesperado ao finalizar venda.");
            return;
        }

        processarEtapaDocumentalAposVenda(
                resultado,
                tipoVenda
        );
    }

    /**
     * Exibe o resultado da finalização de uma venda à vista.
     *
     * Apresenta ID, total, forma de pagamento e, quando positivo, o troco
     * calculado pelo VendaService. O troco exibido não é persistido no banco.
     */
    private boolean exibirResultadoFinalizacaoAVista(
            ResultadoFinalizacaoVenda resultado
    ) {

        if (resultado == null) {
            exibirInformacao(
                    "Venda finalizada",
                    "Venda finalizada com sucesso."
            );
            return false;
        }

        StringBuilder mensagem = new StringBuilder();

        mensagem.append("Venda finalizada com sucesso.")
                .append("\n\n")
                .append("Venda: ")
                .append(resultado.getVendaId())
                .append("\n")
                .append("Nota de Venda: ")
                .append(formatarNumeroNota(resultado.getNotaVendaId()))
                .append("\n")
                .append("Total: ")
                .append(formatarMoeda(resultado.getValorTotal()))
                .append("\n")
                .append("Forma de pagamento: ")
                .append(resultado.getFormaPagamento());

        if (resultado.getTroco() != null
                && resultado.getTroco().compareTo(BigDecimal.ZERO) > 0) {
            mensagem.append("\n")
                    .append("Troco: ")
                    .append(formatarMoeda(resultado.getTroco()));
        }

        return oferecerSalvamentoNotaOriginal(
                "Venda finalizada",
                mensagem.toString()
        );
    }

    /**
     * Exibe o resultado da finalização de uma venda a prazo.
     *
     * Apresenta ID, total, status, vencimento e, quando disponível, o ID da
     * conta a receber gerada pelo VendaService.
     */
    private boolean exibirResultadoFinalizacaoAPrazo(
            ResultadoFinalizacaoVenda resultado
    ) {

        if (resultado == null) {
            exibirInformacao(
                    "Venda a prazo finalizada",
                    "Venda a prazo finalizada com sucesso."
            );
            return false;
        }

        StringBuilder mensagem = new StringBuilder();

        mensagem.append("Venda a prazo finalizada com sucesso.")
                .append("\n\n")
                .append("Venda: ")
                .append(resultado.getVendaId())
                .append("\n")
                .append("Nota de Venda: ")
                .append(formatarNumeroNota(resultado.getNotaVendaId()))
                .append("\n")
                .append("Total: ")
                .append(formatarMoeda(resultado.getValorTotal()))
                .append("\n")
                .append("Status: ")
                .append(resultado.getStatusVenda() != null
                        ? resultado.getStatusVenda()
                        : "-")
                .append("\n")
                .append("Data de vencimento: ")
                .append(resultado.getDataVencimento() != null
                        ? resultado.getDataVencimento()
                        : "-");

        if (resultado.getContaReceberId() != null) {
            mensagem.append("\n")
                    .append("Conta a receber: ")
                    .append(resultado.getContaReceberId());
        }

        return oferecerSalvamentoNotaOriginal(
                "Venda a prazo finalizada",
                mensagem.toString()
        );
    }

    /**
     * Executa somente a etapa documental depois que a venda comercial já foi
     * concluída pelo VendaService.
     *
     * Cancelamentos e falhas de PDF nunca retornam ao tratamento de erro da
     * finalização comercial. A tela da venda concluída é sempre limpa ao final.
     */
    private void processarEtapaDocumentalAposVenda(
            ResultadoFinalizacaoVenda resultado,
            TipoVenda tipoVenda
    ) {
        try {
            boolean salvarNota;

            if (tipoVenda == TipoVenda.A_PRAZO) {
                salvarNota =
                        exibirResultadoFinalizacaoAPrazo(resultado);
            } else {
                salvarNota =
                        exibirResultadoFinalizacaoAVista(resultado);
            }

            if (salvarNota) {
                gerarNotaOriginal(resultado);
            }

        } catch (IllegalArgumentException | IllegalStateException e) {
            System.err.println(
                    "[ERRO] Venda concluída, mas houve falha na etapa documental."
            );
            e.printStackTrace();

            exibirFalhaDocumental(
                    e.getMessage()
            );

        } catch (RuntimeException e) {
            System.err.println(
                    "[ERRO] Venda concluída, mas houve falha inesperada na etapa documental."
            );
            e.printStackTrace();

            exibirFalhaDocumental(
                    "Não foi possível concluir a geração do PDF da Nota de Venda."
            );

        } finally {
            limparTelaAposFinalizacao();
        }
    }

    /**
     * Exibe o resultado da venda já concluída e oferece o salvamento do PDF.
     *
     * Fechar pelo X ou escolher "Agora não" retorna false e não representa erro.
     */
    private boolean oferecerSalvamentoNotaOriginal(
            String titulo,
            String mensagem
    ) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensagem);

        ButtonType botaoSalvar = new ButtonType(
                "Salvar Nota em PDF",
                ButtonBar.ButtonData.OK_DONE
        );

        ButtonType botaoAgoraNao = new ButtonType(
                "Agora não",
                ButtonBar.ButtonData.CANCEL_CLOSE
        );

        alerta.getButtonTypes().setAll(
                botaoSalvar,
                botaoAgoraNao
        );

        Optional<ButtonType> resposta = alerta.showAndWait();

        return resposta.isPresent()
                && resposta.get() == botaoSalvar;
    }

    /**
     * Abre o FileChooser e, quando houver destino escolhido, solicita ao Service
     * a geração da via ORIGINAL. Cancelar o FileChooser não representa erro.
     */
    private void gerarNotaOriginal(
            ResultadoFinalizacaoVenda resultado
    ) {
        if (resultado == null) {
            throw new IllegalStateException(
                    "Resultado da venda não possui dados para gerar a Nota."
            );
        }

        Integer notaVendaId = resultado.getNotaVendaId();

        String nomeSugerido =
                notaVendaService.sugerirNomeArquivo(
                        notaVendaId,
                        TipoViaNotaVendaPdf.ORIGINAL
                );

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Salvar Nota de Venda");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Arquivo PDF (*.pdf)",
                        "*.pdf"
                )
        );
        fileChooser.setInitialFileName(nomeSugerido);

        File arquivoSelecionado = fileChooser.showSaveDialog(
                btnVoltar.getScene().getWindow()
        );

        if (arquivoSelecionado == null) {
            return;
        }

        Path destino = arquivoSelecionado.toPath();

        notaVendaService.gerarPdfPorNotaId(
                notaVendaId,
                TipoViaNotaVendaPdf.ORIGINAL,
                destino
        );

        exibirInformacao(
                "Nota de Venda",
                "PDF da Nota de Venda salvo com sucesso."
        );
    }

    /**
     * Exibe falha exclusivamente documental após uma venda já concluída.
     */
    private void exibirFalhaDocumental(String detalhe) {
        String mensagemDetalhe =
                detalhe != null && !detalhe.isBlank()
                        ? detalhe
                        : "Não foi possível gerar o PDF da Nota de Venda.";

        exibirErro(
                "Venda finalizada com sucesso, mas não foi possível gerar "
                        + "o PDF da Nota de Venda.\n\n"
                        + mensagemDetalhe
        );
    }

    private String formatarNumeroNota(Integer notaVendaId) {
        if (notaVendaId == null || notaVendaId <= 0) {
            return "—";
        }

        return String.format("%06d", notaVendaId);
    }

    /**
     * Limpa a tela após uma venda ser finalizada com sucesso.
     *
     * Este método só deve ser chamado depois que o VendaService finalizar
     * a venda sem erro. Cria uma nova venda em memória e restaura carrinho,
     * campos, seleções e área do cliente. Não é chamado quando ocorre erro.
     */
    private void limparTelaAposFinalizacao() {

        inicializarVenda();

        atualizarTabelaCarrinho();
        atualizarResumoVenda();

        limparCamposProduto();
        limparCamposDescontoGlobal();
        limparSelecaoTipoVenda();
        limparAreaCliente();
    }

    /**
     * Filtra clientes em memória usando nome ou documento.
     *
     * Este método não acessa banco e não aplica regra de negócio.
     * Ele apenas apoia a seleção visual de cliente no Controller.
     */
    private List<Cliente> filtrarClientesPorTermo(
            List<Cliente> clientes,
            String termoBusca
    ) {

        if (clientes == null) {
            return List.of();
        }

        if (termoBusca == null || termoBusca.isBlank()) {
            return clientes.stream()
                    .filter(cliente -> cliente != null)
                    .toList();
        }

        String termoNormalizado = termoBusca
                .trim()
                .toLowerCase(Locale.ROOT);

        return clientes.stream()
                .filter(cliente -> {
                    if (cliente == null) {
                        return false;
                    }

                    String nome = cliente.getNome() != null
                            ? cliente.getNome().toLowerCase(Locale.ROOT)
                            : "";

                    String documento = cliente.getDocumento() != null
                            ? cliente.getDocumento().toLowerCase(Locale.ROOT)
                            : "";

                    return nome.contains(termoNormalizado)
                            || documento.contains(termoNormalizado);
                })
                .toList();
    }

    /**
     * Abre um Dialog para seleção real de cliente.
     *
     * Obtém os clientes pelo ClienteService, filtra nome ou documento em memória
     * e solicita ao VendaService os limites disponíveis para exibição. O Controller
     * não acessa DAO nem calcula limite de crédito.
     *
     * O método apenas retorna o cliente escolhido; não altera clienteSelecionado
     * nem limpa a área de cliente. Cancelar ou fechar retorna Optional.empty().
     */
    private Optional<Cliente> abrirDialogSelecaoCliente(
            String termoBusca
    ) {

        List<Cliente> clientes = clienteService.listarTodos();
        List<Cliente> clientesEncontrados = filtrarClientesPorTermo(
                clientes,
                termoBusca
        );

        Map<Integer, BigDecimal> limitesDisponiveisPorCliente =
                vendaService.calcularLimitesCreditoDisponiveis(clientesEncontrados);

        Dialog<Cliente> dialog = new Dialog<>();
        dialog.setTitle("Selecionar Cliente");

        String termoCabecalho = termoBusca != null && !termoBusca.isBlank()
                ? termoBusca.trim()
                : "todos os clientes";

        dialog.setHeaderText("Resultado da busca por: " + termoCabecalho);

        TableView<Cliente> tableViewClientes = new TableView<>(
                FXCollections.observableArrayList(clientesEncontrados)
        );

        tableViewClientes.setPrefWidth(960);
        tableViewClientes.setPrefHeight(300);

        Label placeholder = new Label("Nenhum cliente encontrado para esta busca.");
        placeholder.setWrapText(true);
        tableViewClientes.setPlaceholder(placeholder);

        TableColumn<Cliente, Integer> colunaId = new TableColumn<>("ID");
        colunaId.setPrefWidth(80);
        colunaId.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(
                        cellData.getValue().getIdCliente() != null
                                ? cellData.getValue().getIdCliente()
                                : 0
                ).asObject()
        );

        TableColumn<Cliente, String> colunaNome = new TableColumn<>("Nome");
        colunaNome.setPrefWidth(260);
        colunaNome.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getNome() != null
                                ? cellData.getValue().getNome()
                                : "-"
                )
        );

        TableColumn<Cliente, String> colunaDocumento = new TableColumn<>("Documento");
        colunaDocumento.setPrefWidth(180);
        colunaDocumento.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getDocumento() != null
                                ? cellData.getValue().getDocumento()
                                : "-"
                )
        );

        TableColumn<Cliente, String> colunaStatus = new TableColumn<>("Status");
        colunaStatus.setPrefWidth(120);
        colunaStatus.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getStatus() != null
                                ? cellData.getValue().getStatus().name()
                                : "-"
                )
        );

        TableColumn<Cliente, String> colunaLimiteTotal = new TableColumn<>("Limite total");
        colunaLimiteTotal.setPrefWidth(140);
        colunaLimiteTotal.setCellValueFactory(cellData -> {
            BigDecimal limiteTotal = cellData.getValue().getLimiteCredito() != null
                    ? cellData.getValue().getLimiteCredito()
                    : BigDecimal.ZERO;

            return new SimpleStringProperty(formatarMoeda(limiteTotal));
        });

        TableColumn<Cliente, String> colunaLimiteDisponivel = new TableColumn<>("Limite disponível");
        colunaLimiteDisponivel.setPrefWidth(160);
        colunaLimiteDisponivel.setCellValueFactory(cellData -> {
            Cliente cliente = cellData.getValue();

            if (cliente == null || cliente.getIdCliente() == null) {
                return new SimpleStringProperty("-");
            }

            BigDecimal limiteDisponivel = limitesDisponiveisPorCliente.get(
                    cliente.getIdCliente()
            );

            if (limiteDisponivel == null) {
                return new SimpleStringProperty("-");
            }

            return new SimpleStringProperty(formatarMoeda(limiteDisponivel));
        });

        tableViewClientes.getColumns().addAll(
                List.of(
                        colunaId,
                        colunaNome,
                        colunaDocumento,
                        colunaStatus,
                        colunaLimiteTotal,
                        colunaLimiteDisponivel
                )
        );

        dialog.getDialogPane().setContent(tableViewClientes);

        boolean encontrouClientes = !clientesEncontrados.isEmpty();

        if (encontrouClientes) {
            ButtonType botaoSelecionar = new ButtonType(
                    "Selecionar",
                    ButtonBar.ButtonData.OK_DONE
            );

            dialog.getDialogPane().getButtonTypes().addAll(
                    botaoSelecionar,
                    ButtonType.CANCEL
            );

            Node botaoSelecionarNode = dialog.getDialogPane().lookupButton(botaoSelecionar);
            botaoSelecionarNode.setDisable(true);

            tableViewClientes.getSelectionModel()
                    .selectedItemProperty()
                    .addListener((observable, clienteAnterior, clienteAtual) ->
                            botaoSelecionarNode.setDisable(clienteAtual == null)
                    );

            dialog.setResultConverter(buttonType -> {
                if (buttonType == botaoSelecionar) {
                    return tableViewClientes.getSelectionModel().getSelectedItem();
                }

                return null;
            });

        } else {
            ButtonType botaoFechar = new ButtonType(
                    "Fechar",
                    ButtonBar.ButtonData.CANCEL_CLOSE
            );

            dialog.getDialogPane().getButtonTypes().add(botaoFechar);
            dialog.setResultConverter(buttonType -> null);
        }

        return dialog.showAndWait();
    }

    /**
     * Evento do botão Selecionar Cliente.
     *
     * Abre o Dialog de seleção, mantém o cliente escolhido em memória e atualiza
     * nome, status e limite de crédito disponível na tela.
     *
     * A listagem é obtida pelo ClienteService e o limite disponível pelo
     * VendaService; o Controller não acessa DAO diretamente. Cancelar o Dialog
     * preserva a seleção anterior. Se a atualização falhar, o cliente anterior
     * é restaurado.
     */
    @FXML
    private void onSelecionarCliente() {

        try {
            String termoBusca = txtBuscaCliente.getText();

            if (termoBusca == null) {
                termoBusca = "";
            }

            Optional<Cliente> clienteSelecionadoOptional = abrirDialogSelecaoCliente(termoBusca);

            if (clienteSelecionadoOptional.isEmpty()) {
                return;
            }

            Cliente clienteAnterior = clienteSelecionado;

            try {
                clienteSelecionado = clienteSelecionadoOptional.get();
                atualizarAreaClienteSelecionado();

            } catch (RuntimeException e) {
                clienteSelecionado = clienteAnterior;
                throw e;
            }

        } catch (IllegalArgumentException e) {
            exibirErro(e.getMessage());

        } catch (RuntimeException e) {
            exibirErro("Não foi possível selecionar o cliente.");
            System.err.println("[ERRO] Falha inesperada ao selecionar cliente.");
            e.printStackTrace();
        }
    }

    private void exibirInformacao(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    /**
     * Abre a caixa de seleção de produto quando a busca é feita por descrição.
     *
     * Obtém os resultados pelo ProdutoService e limita-se a montar e controlar
     * o Dialog; as regras de estoque e venda permanecem no VendaService.
     *
     * Quando houver produtos encontrados, exibe uma TableView com os produtos
     * separados em colunas e permite selecionar uma linha.
     *
     * Quando não houver produtos encontrados, exibe a mensagem dentro da própria
     * área da TableView usando placeholder, sem abrir Alert separado.
     * Cancelar ou fechar o Dialog retorna Optional.empty().
     */
    private Optional<Produto> abrirDialogSelecaoProduto(String termoBusca) {

        List<Produto> produtosEncontrados = produtoService.buscarPorDescricao(termoBusca);

        Dialog<Produto> dialog = new Dialog<>();
        dialog.setTitle("Selecionar Produto");
        dialog.setHeaderText("Resultado da busca por: " + termoBusca);

        TableView<Produto> tableViewProdutos = new TableView<>(
                FXCollections.observableArrayList(
                        produtosEncontrados != null ? produtosEncontrados : List.of()
                )
        );

        tableViewProdutos.setPrefWidth(720);
        tableViewProdutos.setPrefHeight(280);

        Label placeholder = new Label("Nenhum produto encontrado para esta busca.");
        placeholder.setWrapText(true);
        tableViewProdutos.setPlaceholder(placeholder);

        TableColumn<Produto, Integer> colunaId = new TableColumn<>("ID");
        colunaId.setPrefWidth(80);
        colunaId.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(
                        cellData.getValue().getIdProduto() != null
                                ? cellData.getValue().getIdProduto()
                                : 0
                ).asObject()
        );

        TableColumn<Produto, String> colunaDescricao = new TableColumn<>("Produto");
        colunaDescricao.setPrefWidth(330);
        colunaDescricao.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getDescricao() != null
                                ? cellData.getValue().getDescricao()
                                : "-"
                )
        );

        TableColumn<Produto, String> colunaPreco = new TableColumn<>("Preço");
        colunaPreco.setPrefWidth(150);
        colunaPreco.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarMoeda(cellData.getValue().getPreco()))
        );

        TableColumn<Produto, Integer> colunaEstoque = new TableColumn<>("Estoque");
        colunaEstoque.setPrefWidth(120);
        colunaEstoque.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(
                        cellData.getValue().getQuantidadeEstoque() != null
                                ? cellData.getValue().getQuantidadeEstoque()
                                : 0
                ).asObject()
        );

        tableViewProdutos.getColumns().addAll(
                List.of(
                        colunaId,
                        colunaDescricao,
                        colunaPreco,
                        colunaEstoque
                )
        );

        dialog.getDialogPane().setContent(tableViewProdutos);

        boolean encontrouProdutos = produtosEncontrados != null && !produtosEncontrados.isEmpty();

        if (encontrouProdutos) {
            ButtonType botaoSelecionar = new ButtonType(
                    "Selecionar",
                    ButtonBar.ButtonData.OK_DONE
            );

            dialog.getDialogPane().getButtonTypes().addAll(botaoSelecionar, ButtonType.CANCEL);

            Node botaoSelecionarNode = dialog.getDialogPane().lookupButton(botaoSelecionar);
            botaoSelecionarNode.setDisable(true);

            tableViewProdutos.getSelectionModel()
                    .selectedItemProperty()
                    .addListener((observable, produtoAnterior, produtoAtual) ->
                            botaoSelecionarNode.setDisable(produtoAtual == null)
                    );

            dialog.setResultConverter(buttonType -> {
                if (buttonType == botaoSelecionar) {
                    return tableViewProdutos.getSelectionModel().getSelectedItem();
                }

                return null;
            });

        } else {
            ButtonType botaoFechar = new ButtonType(
                    "Fechar",
                    ButtonBar.ButtonData.CANCEL_CLOSE
            );

            dialog.getDialogPane().getButtonTypes().add(botaoFechar);
            dialog.setResultConverter(buttonType -> null);
        }

        return dialog.showAndWait();
    }

    /**
     * Estrutura auxiliar interna para transportar os dados de pagamento
     * da venda à vista dentro do RegistroVendaController.
     *
     * Esta classe não é model, não é viewmodel e não deve sair deste Controller.
     */
    private static class DadosPagamentoAVista {

        private final FormaPagamento formaPagamento;
        private final BigDecimal valorRecebido;

        private DadosPagamentoAVista(
                FormaPagamento formaPagamento,
                BigDecimal valorRecebido
        ) {
            this.formaPagamento = formaPagamento;
            this.valorRecebido = valorRecebido;
        }

        private FormaPagamento getFormaPagamento() {
            return formaPagamento;
        }

        private BigDecimal getValorRecebido() {
            return valorRecebido;
        }
    }
}
