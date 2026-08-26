package br.com.luis.controller;

import br.com.luis.model.Produto;
import br.com.luis.model.Promocao;
import br.com.luis.service.EntradaEstoqueService;
import br.com.luis.service.ProdutoService;
import br.com.luis.service.PromocaoService;
import br.com.luis.util.CabecalhoUtil;
import br.com.luis.util.NavegacaoUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller da tela JavaFX de gestão de produtos.
 *
 * Coordena os componentes visuais e o preenchimento do formulário, delegando as
 * regras de produto e promoção aos respectivos Services. A coordenação entre os
 * dois módulos ocorre na interface, mas cada operação é tratada separadamente
 * pelo seu Service. O Controller não acessa DAOs diretamente e usa Task nas
 * consultas da tabela para evitar o bloqueio da interface.
 */
public class ProdutoController implements Initializable {

    private final ProdutoService produtoService = new ProdutoService();
    private final PromocaoService promocaoService = new PromocaoService();
    private final EntradaEstoqueService entradaEstoqueService = new EntradaEstoqueService();

    private final NumberFormat formatoMoeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    // MAPEAMENTO DO FXML (Esquerda: Formulário)

    @FXML private TextField txtDescricao;
    @FXML private TextField txtPreco;
    @FXML private TextField txtUltimoPrecoCompra;
    @FXML private Label lblEstoque;
    @FXML private Spinner<Integer> spnEstoque;
    @FXML private Spinner<Integer> spnEstoqueMinimo;
    @FXML private ComboBox<String> cbStatus;

    // Promoção
    @FXML private CheckBox chkPromocao;
    @FXML private RadioButton rbPercentual;
    @FXML private RadioButton rbFixo;
    @FXML private TextField txtValorDesconto;
    @FXML private Label lblStatusPromocao;
    @FXML private Label lblPrefixoDesconto;
    @FXML private Label lblSufixoDesconto;

    @FXML private Label lblUsuario;
    @FXML private Label lblDataHora;

    private ToggleGroup tgTipoDesconto;

    // Botões
    @FXML private Button btnVoltar;
    @FXML private Button btnSalvar;
    @FXML private Button btnCancelar;
    @FXML private Button btnLimpar;
    @FXML private Button btnNovo;

    // MAPEAMENTO DO FXML (Direita: Lista)

    @FXML private TextField txtBusca;
    @FXML private Button btnFiltrar;

    @FXML private TableView<Produto> tabelaProdutos;
    @FXML private TableColumn<Produto, Integer> colId;
    @FXML private TableColumn<Produto, String> colDescricao;
    @FXML private TableColumn<Produto, String> colPreco;
    @FXML private TableColumn<Produto, String> colUltimoPrecoCompra;
    @FXML private TableColumn<Produto, Integer> colEstoque;
    @FXML private TableColumn<Produto, Integer> colEstoqueMinimo;
    @FXML private TableColumn<Produto, String> colPromocao;
    @FXML private TableColumn<Produto, String> colStatus;

    @FXML private Label lblTotalProdutos;

    private final ObservableList<Produto> obsProdutos = FXCollections.observableArrayList();
    private final Map<Integer, String> promocaoPorProduto = new HashMap<>();
    private final Map<Integer, BigDecimal> ultimosPrecosCompra = new HashMap<>();

    // Controle de edição
    private Produto produtoSelecionado;
    private Task<DadosProdutosTela> tarefaCarregamentoProdutosAtual;

    // Guarda a promoção ativa carregada ao selecionar um produto na tabela
    private Promocao promocaoAtivaProdutoSelecionado;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        CabecalhoUtil.configurarUsuarioEDataHora(
                lblUsuario,
                lblDataHora
        );
        configurarComponentes();
        configurarComportamentoPromocao();
        configurarDirtyState();
        configurarColunas();
        configurarTabelaListener();
        atualizarEstadoBotaoSalvar();
        carregarTabela();
    }

    // CONFIGURAÇÕES INICIAIS DA TELA

    private void configurarComponentes() {

        // Status visual da tela: internamente será convertido para boolean
        cbStatus.setItems(FXCollections.observableArrayList("Ativo", "Inativo"));
        cbStatus.setValue("Ativo");

        // Configuração dos Spinners
        spnEstoque.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10000, 0));
        spnEstoqueMinimo.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10000, 0));
        txtUltimoPrecoCompra.setEditable(false);
        txtUltimoPrecoCompra.setFocusTraversable(false);
        txtUltimoPrecoCompra.setText("—");
        atualizarModoEstoque();

        // Tipo de desconto
        tgTipoDesconto = new ToggleGroup();
        rbPercentual.setToggleGroup(tgTipoDesconto);
        rbFixo.setToggleGroup(tgTipoDesconto);
        rbPercentual.setSelected(true);

        // Status informativo da promoção
        lblStatusPromocao.setText("Sem promoção");

        // Permite filtrar pressionando Enter no campo de busca
        txtBusca.setOnAction(event -> acaoFiltrar());

        atualizarUnidadeDesconto();
    }

    private void configurarComportamentoPromocao() {

        travarCamposPromocao(true);

        chkPromocao.selectedProperty().addListener((obs, antigo, novo) -> {
            travarCamposPromocao(!novo);

            if (novo) {
                lblStatusPromocao.setText("Ativa");
            } else {
                txtValorDesconto.clear();
                rbPercentual.setSelected(true);
                atualizarUnidadeDesconto();
                lblStatusPromocao.setText("Sem promoção");
            }
        });

        rbPercentual.selectedProperty().addListener((obs, antigo, novo) -> {
            if (novo) {
                atualizarUnidadeDesconto();
            }
        });

        rbFixo.selectedProperty().addListener((obs, antigo, novo) -> {
            if (novo) {
                atualizarUnidadeDesconto();
            }
        });
    }

    /**
     * Mantém o botão de atualização sincronizado com a comparação do estado
     * persistível atual e a fotografia carregada do produto selecionado.
     */
    private void configurarDirtyState() {
        txtDescricao.textProperty().addListener((obs, antigo, novo) -> atualizarEstadoBotaoSalvar());
        txtPreco.textProperty().addListener((obs, antigo, novo) -> atualizarEstadoBotaoSalvar());
        spnEstoqueMinimo.valueProperty().addListener((obs, antigo, novo) -> atualizarEstadoBotaoSalvar());
        cbStatus.valueProperty().addListener((obs, antigo, novo) -> atualizarEstadoBotaoSalvar());
        chkPromocao.selectedProperty().addListener((obs, antigo, novo) -> atualizarEstadoBotaoSalvar());
        rbPercentual.selectedProperty().addListener((obs, antigo, novo) -> atualizarEstadoBotaoSalvar());
        rbFixo.selectedProperty().addListener((obs, antigo, novo) -> atualizarEstadoBotaoSalvar());
        txtValorDesconto.textProperty().addListener((obs, antigo, novo) -> atualizarEstadoBotaoSalvar());
    }

    private void travarCamposPromocao(boolean travar) {
        rbPercentual.setDisable(travar);
        rbFixo.setDisable(travar);
        txtValorDesconto.setDisable(travar);
        lblPrefixoDesconto.setDisable(travar);
        lblSufixoDesconto.setDisable(travar);
    }

    private void atualizarUnidadeDesconto() {

        if (rbFixo.isSelected()) {
            lblPrefixoDesconto.setText("R$");
            lblSufixoDesconto.setText("");
            txtValorDesconto.setPromptText("Ex: 50,00");
            return;
        }

        lblPrefixoDesconto.setText("");
        lblSufixoDesconto.setText("%");
        txtValorDesconto.setPromptText("Ex: 10");
    }

    private void configurarColunas() {

        colId.setCellValueFactory(new PropertyValueFactory<>("idProduto"));
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));

        colPreco.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarMoeda(cellData.getValue().getPreco()))
        );

        colUltimoPrecoCompra.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        formatarUltimoPrecoCompra(cellData.getValue().getIdProduto())
                )
        );

        colEstoque.setCellValueFactory(new PropertyValueFactory<>("quantidadeEstoque"));
        colEstoqueMinimo.setCellValueFactory(new PropertyValueFactory<>("estoqueMinimo"));

        // Não consulta o banco dentro da célula.
        // Usa informações carregadas previamente em background.
        colPromocao.setCellValueFactory(cellData -> {
            Produto produto = cellData.getValue();

            if (produto.getIdProduto() == null) {
                return new SimpleStringProperty("Não");
            }

            return new SimpleStringProperty(
                    promocaoPorProduto.getOrDefault(produto.getIdProduto(), "Não")
            );
        });

        colStatus.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().isAtivo() ? "Ativo" : "Inativo"
                )
        );
    }

    private void configurarTabelaListener() {

        tabelaProdutos.getSelectionModel().selectedItemProperty().addListener(
                (obs, antigo, novo) -> {
                    if (novo != null) {
                        preencherFormulario(novo);
                        btnSalvar.setText("Atualizar");
                    }

                    atualizarEstadoBotaoSalvar();
                }
        );
    }

    /**
     * Recarrega a tabela usando o texto atualmente informado no campo de pesquisa.
     */
    private void carregarTabela() {
        carregarTabela(txtBusca == null ? "" : txtBusca.getText());
    }

    /**
     * Consulta produtos, últimos preços de compra e promoções em uma Task executada
     * fora da thread da interface. Após o sucesso, publica as fotografias nos
     * componentes JavaFX; em falha, restaura os controles e exibe uma mensagem
     * adequada.
     *
     * @param termoBusca descrição usada como filtro, vazia para listar todos.
     */
    private void carregarTabela(String termoBusca) {

        String termo = termoBusca == null ? "" : termoBusca.trim();

        Task<DadosProdutosTela> tarefaAnterior =
                tarefaCarregamentoProdutosAtual;

        tarefaCarregamentoProdutosAtual = null;

        if (tarefaAnterior != null) {
            tarefaAnterior.cancel();
        }

        Task<DadosProdutosTela> task = new Task<>() {
            @Override
            protected DadosProdutosTela call() {

                List<Produto> produtos;

                if (termo.isBlank()) {
                    produtos = produtoService.listarTodos();
                } else {
                    produtos = produtoService.buscarPorDescricao(termo);
                }

                Map<Integer, BigDecimal> precosCompra =
                        entradaEstoqueService.buscarUltimosPrecosCompra();
                Map<Integer, String> promocoes = new HashMap<>();

                for (Produto produto : produtos) {
                    if (produto.getIdProduto() == null) {
                        continue;
                    }

                    Promocao promocao = promocaoService.buscarPromocaoAtivaPorProduto(produto);
                    promocoes.put(produto.getIdProduto(), formatarPromocao(promocao));
                }

                return new DadosProdutosTela(
                        produtos,
                        precosCompra,
                        promocoes,
                        termo
                );
            }
        };

        tarefaCarregamentoProdutosAtual = task;
        tabelaProdutos.setPlaceholder(new Label("Carregando produtos..."));
        btnFiltrar.setDisable(true);

        task.setOnSucceeded(event -> {
            if (tarefaCarregamentoProdutosAtual != task) {
                return;
            }

            DadosProdutosTela dados = task.getValue();

            promocaoPorProduto.clear();
            promocaoPorProduto.putAll(dados.promocoes());

            ultimosPrecosCompra.clear();
            ultimosPrecosCompra.putAll(dados.ultimosPrecosCompra());

            obsProdutos.setAll(dados.produtos());
            tabelaProdutos.setItems(obsProdutos);

            lblTotalProdutos.setText("Total: " + obsProdutos.size() + " produto"
                    + (obsProdutos.size() == 1 ? "" : "s"));

            if (obsProdutos.isEmpty()) {
                if (dados.termoBusca().isBlank()) {
                    tabelaProdutos.setPlaceholder(new Label("Nenhum produto cadastrado."));
                } else {
                    tabelaProdutos.setPlaceholder(new Label("Nenhum produto encontrado."));
                }
            }

            tarefaCarregamentoProdutosAtual = null;
            btnFiltrar.setDisable(false);
            atualizarEstadoBotaoSalvar();
        });

        task.setOnFailed(event -> {
            if (tarefaCarregamentoProdutosAtual != task) {
                return;
            }

            tarefaCarregamentoProdutosAtual = null;

            System.err.println("[ERRO] Falha ao carregar produtos.");
            task.getException().printStackTrace();

            tabelaProdutos.setPlaceholder(new Label("Não foi possível carregar os produtos."));
            btnFiltrar.setDisable(false);
            atualizarEstadoBotaoSalvar();

            mostrarErroAmigavel("Não foi possível carregar os produtos.");
        });

        task.setOnCancelled(event -> {
            if (tarefaCarregamentoProdutosAtual != task) {
                return;
            }

            tarefaCarregamentoProdutosAtual = null;
            btnFiltrar.setDisable(false);
            atualizarEstadoBotaoSalvar();
        });

        Thread thread = new Thread(task, "carregar-produtos");
        thread.setDaemon(true);
        thread.start();
    }

    private String formatarPromocao(Promocao promocao) {

        if (promocao == null) {
            return "Não";
        }

        if (promocao.getTipoDesconto() == Promocao.TipoDesconto.PERCENTUAL) {
            return "Sim - " + formatarPercentual(promocao.getValorDesconto()) + "%";
        }

        return "Sim - " + formatarMoeda(promocao.getValorDesconto());
    }

    // AÇÕES DOS BOTÕES

    @FXML
    private void onVoltar() {

        if (!confirmarSaidaComAlteracoesNaoSalvas()) {
            return;
        }

        invalidarCarregamentoProdutos();

        try {
            NavegacaoUtil.abrirTela(
                    btnVoltar,
                    "/br/com/luis/view/TelaPrincipal.fxml",
                    "Tela Principal"
            );

        } catch (IOException | RuntimeException e) {
            System.err.println("[ERRO] Falha ao voltar para a Tela Principal.");
            e.printStackTrace();

            mostrarErroAmigavel("Não foi possível retornar para a Tela Principal.");
        }
    }

    /**
     * Confirma a saída quando existem dados de produto ou promoção não salvos.
     *
     * Não altera o formulário, não salva dados e não executa navegação.
     */
    private boolean confirmarSaidaComAlteracoesNaoSalvas() {

        if (!existemAlteracoesProdutoNaoSalvas()) {
            return true;
        }

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Alterações não salvas");
        alerta.setHeaderText(null);
        alerta.setContentText(
                "Existem dados de produto ou promoção ainda não salvos.\n"
                        + "Ao voltar, essas alterações serão descartadas.\n"
                        + "Deseja realmente sair desta tela?"
        );

        ButtonType botaoSair = new ButtonType(
                "Sair sem salvar",
                ButtonBar.ButtonData.OK_DONE
        );

        ButtonType botaoContinuar = new ButtonType(
                "Continuar editando",
                ButtonBar.ButtonData.CANCEL_CLOSE
        );

        alerta.getButtonTypes().setAll(
                botaoSair,
                botaoContinuar
        );

        return alerta.showAndWait().orElse(botaoContinuar) == botaoSair;
    }

    /**
     * Verifica o formulário de acordo com o contexto de cadastro ou edição.
     */
    private boolean existemAlteracoesProdutoNaoSalvas() {
        if (produtoSelecionado == null) {
            return formularioNovoProdutoFoiAlterado();
        }

        return formularioEdicaoProdutoFoiAlterado();
    }

    /**
     * Verifica se o formulário de um novo produto saiu do estado inicial.
     */
    private boolean formularioNovoProdutoFoiAlterado() {
        return !normalizarTextoComparacao(txtDescricao.getText()).isEmpty()
                || !normalizarTextoComparacao(txtPreco.getText()).isEmpty()
                || valoresInteirosDiferentes(spnEstoque.getValue(), 0)
                || valoresInteirosDiferentes(spnEstoqueMinimo.getValue(), 0)
                || !"Ativo".equals(cbStatus.getValue())
                || chkPromocao.isSelected();
    }

    /**
     * Compara os dados atuais do formulário com o produto selecionado.
     */
    private boolean formularioEdicaoProdutoFoiAlterado() {
        if (!normalizarTextoComparacao(txtDescricao.getText()).equals(
                normalizarTextoComparacao(produtoSelecionado.getDescricao())
        )) {
            return true;
        }

        if (valorDecimalFoiAlterado(txtPreco.getText(), produtoSelecionado.getPreco())) {
            return true;
        }


        if (valoresInteirosDiferentes(
                spnEstoqueMinimo.getValue(),
                produtoSelecionado.getEstoqueMinimo()
        )) {
            return true;
        }

        if (obterProdutoAtivoSelecionado() != produtoSelecionado.isAtivo()) {
            return true;
        }

        return promocaoFormularioFoiAlterada();
    }

    /**
     * Compara o estado atual da promoção com a promoção carregada na seleção.
     */
    private boolean promocaoFormularioFoiAlterada() {
        if (promocaoAtivaProdutoSelecionado == null) {
            return chkPromocao.isSelected();
        }

        if (!chkPromocao.isSelected()) {
            return true;
        }

        Promocao.TipoDesconto tipoAtual = obterTipoPromocaoParaComparacao();

        if (tipoAtual != promocaoAtivaProdutoSelecionado.getTipoDesconto()) {
            return true;
        }

        return valorDecimalFoiAlterado(
                txtValorDesconto.getText(),
                promocaoAtivaProdutoSelecionado.getValorDesconto()
        );
    }

    /**
     * Normaliza textos para comparação sem espaços externos.
     */
    private String normalizarTextoComparacao(String texto) {
        return texto == null ? "" : texto.trim();
    }

    /**
     * Compara dois valores inteiros com tratamento seguro de nulos.
     */
    private boolean valoresInteirosDiferentes(Integer valorAtual, Integer valorOriginal) {
        if (valorAtual == null) {
            return valorOriginal != null;
        }

        return !valorAtual.equals(valorOriginal);
    }

    /**
     * Compara numericamente um valor decimal do formulário com o valor original.
     * Campo vazio ou inválido durante a edição representa alteração.
     */
    private boolean valorDecimalFoiAlterado(String textoAtual, BigDecimal valorOriginal) {
        try {
            BigDecimal valorAtual = converterDecimal(
                    textoAtual,
                    "Valor obrigatório para comparação."
            );

            return valorOriginal == null
                    || valorAtual.compareTo(valorOriginal) != 0;

        } catch (NumberFormatException e) {
            return true;
        }
    }

    /**
     * Obtém o tipo de promoção atualmente selecionado, sem validar o formulário.
     */
    private Promocao.TipoDesconto obterTipoPromocaoParaComparacao() {
        if (rbPercentual.isSelected()) {
            return Promocao.TipoDesconto.PERCENTUAL;
        }

        if (rbFixo.isSelected()) {
            return Promocao.TipoDesconto.VALOR_FIXO;
        }

        return null;
    }

    /**
     * Lê e converte os campos visuais e delega o cadastro ou a atualização ao
     * ProdutoService. Após a persistência do produto, trata separadamente a
     * promoção pelo PromocaoService, recarrega a tabela e limpa o formulário.
     * Produto e promoção não são apresentados como uma única transação.
     */
    @FXML
    public void acaoSalvar() {
        if (produtoSelecionado != null && !formularioEdicaoProdutoFoiAlterado()) {
            atualizarEstadoBotaoSalvar();
            return;
        }

        try {

            boolean estavaEditando = produtoSelecionado != null;
            Promocao promocaoAnterior = promocaoAtivaProdutoSelecionado;

            String descricao = txtDescricao.getText();
            BigDecimal preco = converterDecimal(txtPreco.getText(), "Preço é obrigatório.");

            int estoque = produtoSelecionado == null
                    ? spnEstoque.getValue()
                    : produtoSelecionado.getQuantidadeEstoque();
            int estoqueMin = spnEstoqueMinimo.getValue();

            boolean ativo = produtoSelecionado == null || obterProdutoAtivoSelecionado();

            Produto produto;

            if (!estavaEditando) {

                // NOVO
                produto = new Produto(null, descricao, preco, estoque, estoqueMin, true);
                produtoService.cadastrar(produto);

                System.out.println("[LOG] Produto cadastrado com sucesso!");

            } else {

                // EDIÇÃO
                produto = new Produto(
                        produtoSelecionado.getIdProduto(),
                        descricao,
                        preco,
                        estoque,
                        estoqueMin,
                        ativo
                );

                produtoService.atualizar(produto);

                System.out.println("[LOG] Produto atualizado com sucesso!");
            }

            tratarPromocaoAposSalvar(produto, estavaEditando, promocaoAnterior);

            carregarTabela();
            prepararNovoCadastro();

            mostrarInformacao(
                    estavaEditando
                            ? "Produto atualizado com sucesso."
                            : "Produto cadastrado com sucesso."
            );

        } catch (NumberFormatException e) {

            mostrarAviso(e.getMessage());

        } catch (IllegalArgumentException e) {

            mostrarAviso(e.getMessage());

        } catch (RuntimeException e) {

            System.err.println("[ERRO] Falha ao salvar produto.");
            e.printStackTrace();

            mostrarErroAmigavel("Não foi possível salvar o produto. Verifique os dados e tente novamente.");
        }
    }

    /**
     * Coordena a promoção depois que o produto foi salvo.
     *
     * Quando a promoção está selecionada, cria uma nova versão para produto novo,
     * ausência de promoção anterior, alteração do desconto ou mudança de preço.
     * Quando a opção é removida durante uma edição, solicita ao PromocaoService a
     * inativação da promoção ativa. As validações e transações permanecem no Service.
     */
    private void tratarPromocaoAposSalvar(Produto produto, boolean estavaEditando, Promocao promocaoAnterior) {

        if (chkPromocao.isSelected()) {

            Promocao novaPromocao = montarPromocao(produto);

            boolean deveCadastrarNovaPromocao = !estavaEditando
                    || promocaoAnterior == null
                    || promocaoFoiAlterada(promocaoAnterior, novaPromocao)
                    || precoFoiAlterado(produto);

            if (deveCadastrarNovaPromocao) {
                promocaoService.cadastrarPromocaoNova(novaPromocao);
                System.out.println("[LOG] Promoção cadastrada para o produto: " + produto.getDescricao());
            }

            return;
        }

        if (estavaEditando && promocaoAnterior != null) {
            promocaoService.inativarPromocaoAtivaDoProduto(produto);
            System.out.println("[LOG] Promoção removida do produto: " + produto.getDescricao());
        }
    }

    private boolean promocaoFoiAlterada(Promocao promocaoAnterior, Promocao novaPromocao) {

        if (promocaoAnterior.getTipoDesconto() != novaPromocao.getTipoDesconto()) {
            return true;
        }

        return promocaoAnterior.getValorDesconto().compareTo(novaPromocao.getValorDesconto()) != 0;
    }

    private boolean precoFoiAlterado(Produto produtoAtualizado) {

        if (produtoSelecionado == null || produtoSelecionado.getPreco() == null) {
            return false;
        }

        return produtoSelecionado.getPreco().compareTo(produtoAtualizado.getPreco()) != 0;
    }

    private Promocao montarPromocao(Produto produto) {

        if (!rbPercentual.isSelected() && !rbFixo.isSelected()) {
            throw new IllegalArgumentException("Selecione o tipo de desconto da promoção.");
        }

        BigDecimal valorDesconto = converterDecimal(
                txtValorDesconto.getText(),
                "Valor do desconto é obrigatório."
        );

        Promocao.TipoDesconto tipoDesconto = rbPercentual.isSelected()
                ? Promocao.TipoDesconto.PERCENTUAL
                : Promocao.TipoDesconto.VALOR_FIXO;

        return new Promocao(
                null,
                tipoDesconto,
                valorDesconto,
                true,
                produto
        );
    }

    private BigDecimal converterDecimal(String texto, String mensagemVazio) {

        if (texto == null || texto.isBlank()) {
            throw new NumberFormatException(mensagemVazio);
        }

        String normalizado = texto.trim()
                .replace("R$", "")
                .replace("%", "")
                .replace(" ", "");

        if (normalizado.contains(",")) {
            normalizado = normalizado
                    .replace(".", "")
                    .replace(",", ".");
        }

        return new BigDecimal(normalizado);
    }

    private boolean obterProdutoAtivoSelecionado() {
        return !"Inativo".equals(cbStatus.getValue());
    }

    @FXML
    public void acaoNovo() {
        prepararNovoCadastro();
    }

    @FXML
    public void acaoLimpar() {
        prepararNovoCadastro();
    }

    @FXML
    public void acaoCancelar() {
        prepararNovoCadastro();
        txtBusca.clear();
        carregarTabela("");
    }

    @FXML
    public void acaoFiltrar() {
        carregarTabela(txtBusca.getText());
    }

    // MÉTODOS AUXILIARES

    private void preencherFormulario(Produto produto) {

        this.produtoSelecionado = produto;

        txtDescricao.setText(produto.getDescricao());
        txtPreco.setText(produto.getPreco().toString().replace(".", ","));
        spnEstoque.getValueFactory().setValue(produto.getQuantidadeEstoque());
        spnEstoqueMinimo.getValueFactory().setValue(produto.getEstoqueMinimo());
        atualizarModoEstoque();

        cbStatus.setValue(produto.isAtivo() ? "Ativo" : "Inativo");

        carregarPromocaoAtivaNoFormulario(produto);
        atualizarEstadoBotaoSalvar();
    }

    /**
     * Consulta a promoção ativa pelo PromocaoService e prepara o formulário para
     * apresentar seus dados. Quando não há promoção, limpa a seção; em falha,
     * restaura o estado visual seguro e informa o usuário.
     */
    private void carregarPromocaoAtivaNoFormulario(Produto produto) {

        try {
            promocaoAtivaProdutoSelecionado = promocaoService.buscarPromocaoAtivaPorProduto(produto);

            if (promocaoAtivaProdutoSelecionado == null) {
                limparCamposPromocao();
                return;
            }

            chkPromocao.setSelected(true);
            travarCamposPromocao(false);

            if (promocaoAtivaProdutoSelecionado.getTipoDesconto() == Promocao.TipoDesconto.PERCENTUAL) {
                rbPercentual.setSelected(true);
            } else {
                rbFixo.setSelected(true);
            }

            atualizarUnidadeDesconto();

            txtValorDesconto.setText(
                    formatarValorDescontoParaCampo(promocaoAtivaProdutoSelecionado)
            );

            lblStatusPromocao.setText("Ativa");

        } catch (RuntimeException e) {
            System.err.println("[ERRO] Falha ao carregar promoção ativa do produto.");
            e.printStackTrace();

            promocaoAtivaProdutoSelecionado = null;
            limparCamposPromocao();

            mostrarErroAmigavel("Não foi possível carregar os dados da promoção do produto.");
        }
    }

    private void prepararNovoCadastro() {

        produtoSelecionado = null;
        promocaoAtivaProdutoSelecionado = null;

        limparSomenteCamposFormulario();
        atualizarModoEstoque();

        tabelaProdutos.getSelectionModel().clearSelection();

        btnSalvar.setText("Salvar");
        atualizarEstadoBotaoSalvar();
        txtDescricao.requestFocus();
    }

    /**
     * Mantém o saldo inicial editável somente durante um novo cadastro.
     * Produtos persistidos exibem o estoque atual apenas para consulta.
     */
    private void atualizarModoEstoque() {
        boolean produtoExistente = produtoSelecionado != null;

        lblEstoque.setText(
                produtoExistente ? "Estoque Atual" : "Quantidade Inicial:*"
        );
        spnEstoque.setDisable(produtoExistente);
        txtUltimoPrecoCompra.setText(
                produtoExistente
                        ? formatarUltimoPrecoCompraParaCampo(produtoSelecionado.getIdProduto())
                        : "—"
        );
    }

    private void atualizarEstadoBotaoSalvar() {
        if (btnSalvar == null) {
            return;
        }

        btnSalvar.setDisable(
                produtoSelecionado != null
                        && !formularioEdicaoProdutoFoiAlterado()
        );
    }

    private void invalidarCarregamentoProdutos() {
        Task<DadosProdutosTela> taskAtual =
                tarefaCarregamentoProdutosAtual;

        tarefaCarregamentoProdutosAtual = null;

        if (taskAtual != null) {
            taskAtual.cancel();
        }

        btnFiltrar.setDisable(false);
        atualizarEstadoBotaoSalvar();
    }

    private void limparSomenteCamposFormulario() {

        txtDescricao.clear();
        txtPreco.clear();
        txtUltimoPrecoCompra.setText("—");

        spnEstoque.getValueFactory().setValue(0);
        spnEstoqueMinimo.getValueFactory().setValue(0);

        cbStatus.setValue("Ativo");

        limparCamposPromocao();

        txtDescricao.requestFocus();
    }

    private void limparCamposPromocao() {
        chkPromocao.setSelected(false);
        rbPercentual.setSelected(true);
        txtValorDesconto.clear();
        lblStatusPromocao.setText("Sem promoção");
        atualizarUnidadeDesconto();
        travarCamposPromocao(true);
    }

    private String formatarMoeda(BigDecimal valor) {

        if (valor == null) {
            return "R$ 0,00";
        }

        return formatoMoeda.format(valor).replace('\u00A0', ' ');
    }

    private String formatarUltimoPrecoCompra(Integer produtoId) {

        if (produtoId == null) {
            return "—";
        }

        BigDecimal ultimoPrecoCompra = ultimosPrecosCompra.get(produtoId);

        return ultimoPrecoCompra == null
                ? "—"
                : formatarMoeda(ultimoPrecoCompra);
    }

    private String formatarUltimoPrecoCompraParaCampo(Integer produtoId) {

        if (produtoId == null) {
            return "—";
        }

        BigDecimal ultimoPrecoCompra = ultimosPrecosCompra.get(produtoId);

        return ultimoPrecoCompra == null
                ? "—"
                : ultimoPrecoCompra
                        .setScale(2, RoundingMode.HALF_UP)
                        .toPlainString()
                        .replace('.', ',');
    }

    private String formatarPercentual(BigDecimal valor) {

        if (valor == null) {
            return "0";
        }

        return valor.stripTrailingZeros()
                .toPlainString()
                .replace(".", ",");
    }

    private String formatarValorDescontoParaCampo(Promocao promocao) {

        if (promocao.getTipoDesconto() == Promocao.TipoDesconto.PERCENTUAL) {
            return formatarPercentual(promocao.getValorDesconto());
        }

        return promocao.getValorDesconto()
                .setScale(2, RoundingMode.HALF_UP)
                .toString()
                .replace(".", ",");
    }

    private void mostrarInformacao(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informação");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void mostrarAviso(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Aviso");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void mostrarErroAmigavel(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private record DadosProdutosTela(
            List<Produto> produtos,
            Map<Integer, BigDecimal> ultimosPrecosCompra,
            Map<Integer, String> promocoes,
            String termoBusca
    ) {
    }
}
