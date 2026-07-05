package br.com.luis.controller;

import br.com.luis.model.ItemVenda;
import br.com.luis.model.Produto;
import br.com.luis.model.TipoDescontoGlobal;
import br.com.luis.model.Venda;
import br.com.luis.model.Usuario;
import br.com.luis.model.TipoVenda;
import br.com.luis.util.SessaoUsuario;
import br.com.luis.service.ProdutoService;
import br.com.luis.service.VendaService;
import br.com.luis.viewmodel.ItemCarrinhoView;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
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

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Controller da tela Registro de Venda.
 *
 * Nesta fase, a tela representa apenas o carrinho em memória.
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
    private static final int QUANTIDADE_INICIAL_ITEM = 1;

    private final VendaService vendaService;
    private final ProdutoService produtoService;
    private final ObservableList<ItemCarrinhoView> itensCarrinhoView;

    private Venda vendaAtual;

    @FXML private Label lblUsuarioLogado;
    @FXML private Label lblDataHora;

    @FXML private TextField txtBuscaProduto;

    @FXML private TableView<ItemCarrinhoView> tblItensVenda;
    @FXML private TableColumn<ItemCarrinhoView, String> colProduto;
    @FXML private TableColumn<ItemCarrinhoView, String> colPreco;
    @FXML private TableColumn<ItemCarrinhoView, String> colPromocao;
    @FXML private TableColumn<ItemCarrinhoView, Integer> colQuantidade;
    @FXML private TableColumn<ItemCarrinhoView, String> colSubtotal;

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
     * @implNote Nesta fase, usa usuário temporário apenas para manter a venda em memória.
     * A ligação definitiva com SessaoUsuario será ajustada em etapa futura.
     */
    private void inicializarVenda() {
        this.vendaAtual = new Venda(obterUsuarioIdAtual());
    }

    /**
     * Obtém o ID do usuário atualmente logado.
     *
     * Enquanto a integração completa da sessão não é finalizada,
     * mantém USUARIO_TEMPORARIO_ID apenas como fallback de segurança.
     */
    private Integer obterUsuarioIdAtual() {

        Usuario usuarioLogado = SessaoUsuario.getInstance().getUsuarioLogado();

        if (usuarioLogado == null) {
            return USUARIO_TEMPORARIO_ID;
        }

        if (usuarioLogado.getIdUsuario() == null || usuarioLogado.getIdUsuario() <= 0) {
            return USUARIO_TEMPORARIO_ID;
        }

        return usuarioLogado.getIdUsuario();
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
     * Adiciona o produto ao carrinho chamando o VendaService.
     *
     * Após sucesso, atualiza tabela, resumo e limpa os campos da área de produto.
     */
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
     * Nesta versão, o campo txtBuscaProduto aceita:
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

    /**
     * Obtém e valida o texto informado no campo de busca de produto.
     */
    private String obterTextoBuscaProduto() {

        String textoBusca = txtBuscaProduto.getText();

        if (textoBusca == null || textoBusca.isBlank()) {
            throw new IllegalArgumentException("Informe o ID ou a descrição do produto.");
        }

        return textoBusca.trim();
    }

    /**
     * Verifica se o texto digitado contém apenas números.
     */
    private boolean textoEhNumero(String texto) {
        return texto != null && texto.matches("\\d+");
    }

    /**
     * Converte o texto numérico informado para ID de produto.
     */
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

    /**
     * Limpa os campos da área de produto após adicionar item ao carrinho.
     */
    private void limparCamposProduto() {
        txtBuscaProduto.clear();
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
     * Não salva venda, não baixa estoque e não executa financeiro.
     */
    @FXML
    private void onLimparVenda() {

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
                txtDescontoValor.setDisable(false);

                txtDescontoPercentual.clear();
                txtDescontoPercentual.setDisable(true);

                txtDescontoValor.requestFocus();
            }
        });

        rbDescontoPercentual.selectedProperty().addListener((observable, valorAntigo, selecionado) -> {
            if (selecionado) {
                txtDescontoPercentual.setDisable(false);

                txtDescontoValor.clear();
                txtDescontoValor.setDisable(true);

                txtDescontoPercentual.requestFocus();
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

    /**
     * Evento do botão Cancelar Venda.
     *
     * Nesta fase, a venda ainda não é persistida no banco.
     * Portanto, cancelar significa descartar a venda em memória atual
     * e retornar a tela ao estado inicial.
     *
     * Não salva venda, não baixa estoque, não gera financeiro
     * e não executa estorno.
     */
    @FXML
    private void onCancelarVenda() {

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
     *
     * Nesta fase, o tipo de venda é apenas visual/preparatório.
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
     * Nesta fase, cliente ainda não possui regra completa de venda a prazo.
     */
    private void limparAreaCliente() {

        txtBuscaCliente.clear();
        lblNomeCliente.setText("-");
        lblStatusCliente.setText("-");
        lblLimiteDisponivel.setText("R$ 0,00");
    }

    /**
     * Evento do botão Finalizar Venda.
     *
     * Nesta fase, a venda ainda não deve ser finalizada.
     * Este botão existe apenas para indicar o fluxo futuro da Fase 5.
     *
     * Não salva venda, não baixa estoque, não abre pagamento,
     * não gera financeiro e não persiste dados no banco.
     */
    @FXML
    private void onFinalizarVenda() {
        exibirInformacao(
                "Finalização de Venda",
                "A finalização da venda será implementada na Fase 5."
        );
    }

    /**
     * Evento do botão Selecionar Cliente.
     *
     * Nesta fase, a seleção real de cliente ainda não será implementada.
     * A área de cliente permanece apenas visual/preparatória para a Fase 5.
     *
     * Não busca cliente no banco, não valida limite, não valida prazo
     * e não executa regras financeiras.
     */
    @FXML
    private void onSelecionarCliente() {
        exibirInformacao(
                "Seleção de Cliente",
                "A seleção de cliente será implementada na Fase 5."
        );
    }

    /**
     * Exibe uma mensagem informativa amigável para o usuário.
     */
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
     * Quando houver produtos encontrados, exibe uma TableView com os produtos
     * separados em colunas e permite selecionar uma linha.
     *
     * Quando não houver produtos encontrados, exibe a mensagem dentro da própria
     * área da TableView usando placeholder, sem abrir Alert separado.
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
                colunaId,
                colunaDescricao,
                colunaPreco,
                colunaEstoque
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
}