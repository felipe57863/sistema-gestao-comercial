package br.com.luis.controller;

import br.com.luis.model.EntradaEstoque;
import br.com.luis.model.ItemEntradaEstoque;
import br.com.luis.model.Produto;
import br.com.luis.model.Usuario;
import br.com.luis.service.EntradaEstoqueService;
import br.com.luis.service.ProdutoService;
import br.com.luis.util.CabecalhoUtil;
import br.com.luis.util.NavegacaoUtil;
import br.com.luis.util.SessaoUsuario;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Coordena o rascunho visual e a confirmação de uma Entrada de Estoque.
 *
 * Mantém os itens em memória e delega ao Service a autorização, os snapshots,
 * a persistência e a movimentação transacional do estoque.
 */
public class EntradaEstoqueController {

    private static final int QUANTIDADE_INICIAL = 1;
    private static final DateTimeFormatter FORMATADOR_DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final EntradaEstoqueService entradaEstoqueService;
    private final ProdutoService produtoService;
    private final ObservableList<ItemEntradaEstoque> itensRascunho;
    private final Map<Integer, Integer> estoqueAtualPorProduto;
    private final Map<Integer, Produto> produtoPorId;
    private final NumberFormat formatadorMoeda;

    private Task<List<Produto>> tarefaCarregamentoProdutosAtual;
    private Task<EntradaEstoque> tarefaConfirmacaoAtual;
    private ItemEntradaEstoque itemEmEdicao;

    @FXML private Button btnVoltar;
    @FXML private Label lblUsuario;
    @FXML private Label lblDataHora;
    @FXML private Label lblNumeroEntrada;
    @FXML private Label lblResponsavel;
    @FXML private TextField txtReferencia;
    @FXML private TextArea txtObservacao;
    @FXML private ComboBox<Produto> cbProduto;
    @FXML private Label lblEstoqueAtual;
    @FXML private Spinner<Integer> spnQuantidade;
    @FXML private TextField txtPrecoCompra;
    @FXML private Button btnAdicionarAtualizarItem;
    @FXML private Button btnRemoverItem;
    @FXML private Label lblOrientacaoEdicaoItem;
    @FXML private TableView<ItemEntradaEstoque> tabelaItens;
    @FXML private TableColumn<ItemEntradaEstoque, String> colProduto;
    @FXML private TableColumn<ItemEntradaEstoque, String> colEstoqueAtual;
    @FXML private TableColumn<ItemEntradaEstoque, String> colQuantidade;
    @FXML private TableColumn<ItemEntradaEstoque, String> colPrecoCompra;
    @FXML private TableColumn<ItemEntradaEstoque, String> colSubtotal;
    @FXML private Label lblProdutosDistintos;
    @FXML private Label lblTotalUnidades;
    @FXML private Label lblValorTotal;
    @FXML private Label lblEstadoOperacao;
    @FXML private ProgressIndicator progressoOperacao;
    @FXML private Button btnLimparEntrada;
    @FXML private Button btnConfirmarEntrada;

    public EntradaEstoqueController() {
        entradaEstoqueService = new EntradaEstoqueService();
        produtoService = new ProdutoService();
        itensRascunho = FXCollections.observableArrayList();
        estoqueAtualPorProduto = new HashMap<>();
        produtoPorId = new HashMap<>();
        formatadorMoeda = NumberFormat.getCurrencyInstance(
                Locale.forLanguageTag("pt-BR")
        );
    }

    @FXML
    public void initialize() {
        CabecalhoUtil.configurarUsuarioEDataHora(lblUsuario, lblDataHora);
        configurarIdentificacaoVisual();
        spnQuantidade.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        QUANTIDADE_INICIAL, 100000, QUANTIDADE_INICIAL
                )
        );
        configurarComboProdutos();
        configurarTabela();
        configurarListeners();
        atualizarTotalizadores();
        atualizarEstadoControles();
        carregarProdutosAtivos();
    }

    private void configurarIdentificacaoVisual() {
        lblNumeroEntrada.setText("Número: gerado após confirmação");
        Usuario usuario = obterAdministradorAptoDaSessao();
        String nome = usuario != null && usuario.getNome() != null
                && !usuario.getNome().isBlank()
                ? usuario.getNome().trim()
                : "não identificado";
        lblResponsavel.setText("Responsável: " + nome);
    }

    private void configurarComboProdutos() {
        cbProduto.setConverter(new StringConverter<>() {
            @Override
            public String toString(Produto produto) {
                return formatarProduto(produto);
            }

            @Override
            public Produto fromString(String texto) {
                return null;
            }
        });

        cbProduto.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(Produto produto, boolean vazio) {
                super.updateItem(produto, vazio);
                setText(vazio ? null : formatarProduto(produto));
            }
        });

        cbProduto.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Produto produto, boolean vazio) {
                super.updateItem(produto, vazio);
                setText(vazio ? null : formatarProduto(produto));
            }
        });
    }

    private String formatarProduto(Produto produto) {
        if (produto == null || produto.getIdProduto() == null) {
            return "";
        }
        return produto.getIdProduto() + " - " + produto.getDescricao();
    }

    private void configurarTabela() {
        tabelaItens.setItems(itensRascunho);
        colProduto.setCellValueFactory(dados ->
                new SimpleStringProperty(dados.getValue().getDescricaoProduto())
        );
        colEstoqueAtual.setCellValueFactory(dados -> {
            Integer estoque = estoqueAtualPorProduto.get(
                    dados.getValue().getProdutoId()
            );
            return new SimpleStringProperty(
                    estoque != null ? estoque.toString() : "-"
            );
        });
        colQuantidade.setCellValueFactory(dados ->
                new SimpleStringProperty(
                        String.valueOf(dados.getValue().getQuantidadeRecebida())
                )
        );
        colPrecoCompra.setCellValueFactory(dados ->
                new SimpleStringProperty(
                        formatarMoeda(dados.getValue().getPrecoCompraUnitario())
                )
        );
        colSubtotal.setCellValueFactory(dados ->
                new SimpleStringProperty(
                        formatarMoeda(dados.getValue().getSubtotal())
                )
        );
    }

    private void configurarListeners() {
        cbProduto.getSelectionModel()
                .selectedItemProperty()
                .addListener((observavel, anterior, atual) ->
                        atualizarEstoqueAtualVisual(atual)
                );

        tabelaItens.getSelectionModel()
                .selectedItemProperty()
                .addListener((observavel, anterior, atual) -> {
                    if (atual != null) {
                        carregarItemParaEdicao(atual);
                    }
                    atualizarEstadoControles();
                });
    }

    private void atualizarEstoqueAtualVisual(Produto produto) {
        if (produto == null || produto.getQuantidadeEstoque() == null) {
            lblEstoqueAtual.setText("-");
            return;
        }
        lblEstoqueAtual.setText(produto.getQuantidadeEstoque().toString());
    }

    /**
     * Consulta somente produtos ativos fora da JavaFX Application Thread.
     */
    private void carregarProdutosAtivos() {
        invalidarCarregamentoProdutos();

        Task<List<Produto>> novaTarefa = new Task<>() {
            @Override
            protected List<Produto> call() {
                List<Produto> produtos = produtoService.listarAtivos();
                return produtos != null ? produtos : List.of();
            }
        };

        tarefaCarregamentoProdutosAtual = novaTarefa;
        atualizarEstadoControles();

        novaTarefa.setOnSucceeded(evento -> {
            if (tarefaCarregamentoProdutosAtual != novaTarefa) {
                return;
            }
            tarefaCarregamentoProdutosAtual = null;
            publicarProdutosAtivos(novaTarefa.getValue());
            atualizarEstadoControles();
        });

        novaTarefa.setOnFailed(evento -> {
            if (tarefaCarregamentoProdutosAtual != novaTarefa) {
                return;
            }

            tarefaCarregamentoProdutosAtual = null;
            Throwable erro = novaTarefa.getException();
            System.err.println("[ERRO] Falha ao carregar produtos ativos para a Entrada de Estoque.");
            if (erro != null) {
                erro.printStackTrace();
            }
            atualizarEstadoControles();
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro ao carregar produtos",
                    "Não foi possível carregar os produtos ativos. Tente novamente."
            );
        });

        novaTarefa.setOnCancelled(evento -> {
            if (tarefaCarregamentoProdutosAtual == novaTarefa) {
                tarefaCarregamentoProdutosAtual = null;
                atualizarEstadoControles();
            }
        });

        Thread thread = new Thread(
                novaTarefa,
                "entrada-estoque-carregamento-produtos"
        );
        thread.setDaemon(true);
        thread.start();
    }

    private void publicarProdutosAtivos(List<Produto> produtos) {
        Integer produtoSelecionadoId = cbProduto.getValue() != null
                ? cbProduto.getValue().getIdProduto()
                : null;

        produtoPorId.clear();
        estoqueAtualPorProduto.clear();

        List<Produto> produtosAtivos = produtos.stream()
                .filter(produto -> produto != null && produto.isAtivo())
                .toList();

        for (Produto produto : produtosAtivos) {
            Integer produtoId = produto.getIdProduto();
            if (produtoId != null) {
                produtoPorId.put(produtoId, produto);
                estoqueAtualPorProduto.put(
                        produtoId,
                        produto.getQuantidadeEstoque()
                );
            }
        }

        cbProduto.setItems(FXCollections.observableArrayList(produtosAtivos));
        if (produtoSelecionadoId != null) {
            cbProduto.getSelectionModel().select(
                    produtoPorId.get(produtoSelecionadoId)
            );
        }
        tabelaItens.refresh();
    }

    private void invalidarCarregamentoProdutos() {
        Task<List<Produto>> tarefaAnterior = tarefaCarregamentoProdutosAtual;
        tarefaCarregamentoProdutosAtual = null;
        if (tarefaAnterior != null) {
            tarefaAnterior.cancel();
        }
    }

    @FXML
    private void acaoAdicionarAtualizarItem() {
        if (tarefaConfirmacaoAtual != null) {
            return;
        }

        try {
            Produto produto = cbProduto.getValue();
            if (produto == null || produto.getIdProduto() == null
                    || produto.getIdProduto() <= 0) {
                throw new IllegalArgumentException("Selecione um produto ativo.");
            }

            Integer quantidade = spnQuantidade.getValue();
            if (quantidade == null || quantidade <= 0) {
                throw new IllegalArgumentException(
                        "Informe uma quantidade inteira maior que zero."
                );
            }

            BigDecimal precoCompra = converterPrecoCompra(
                    txtPrecoCompra.getText()
            );
            if (produtoJaExisteNoRascunho(
                    produto.getIdProduto(),
                    itemEmEdicao
            )) {
                throw new IllegalArgumentException(
                        "Este produto já foi adicionado à Entrada de Estoque."
                );
            }

            ItemEntradaEstoque novoItem = new ItemEntradaEstoque(
                    null,
                    null,
                    produto.getIdProduto(),
                    produto.getDescricao(),
                    quantidade,
                    precoCompra
            );

            if (itemEmEdicao == null) {
                itensRascunho.add(novoItem);
            } else {
                int indice = itensRascunho.indexOf(itemEmEdicao);
                if (indice < 0) {
                    throw new IllegalStateException(
                            "O item selecionado não pertence mais ao rascunho."
                    );
                }
                itensRascunho.set(indice, novoItem);
            }

            estoqueAtualPorProduto.put(
                    produto.getIdProduto(),
                    produto.getQuantidadeEstoque()
            );
            limparEdicaoItem();
            atualizarTotalizadores();
            atualizarEstadoControles();

        } catch (IllegalArgumentException | IllegalStateException e) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atenção", e.getMessage());
        }
    }

    private boolean produtoJaExisteNoRascunho(
            Integer produtoId,
            ItemEntradaEstoque itemIgnorado
    ) {
        return itensRascunho.stream().anyMatch(item ->
                item != itemIgnorado
                        && produtoId.equals(item.getProdutoId())
        );
    }

    private BigDecimal converterPrecoCompra(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException(
                    "Informe o preço de compra unitário."
            );
        }

        String normalizado = texto
                .trim()
                .replace("R$", "")
                .replace("\u00A0", "")
                .replace(" ", "");

        if (normalizado.contains(",")) {
            normalizado = normalizado
                    .replace(".", "")
                    .replace(",", ".");
        }

        try {
            BigDecimal valor = new BigDecimal(normalizado);
            if (valor.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "O preço de compra deve ser maior que zero."
                );
            }
            return valor;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Informe um preço de compra válido."
            );
        }
    }

    private void carregarItemParaEdicao(ItemEntradaEstoque item) {
        itemEmEdicao = item;
        cbProduto.getSelectionModel().select(
                produtoPorId.get(item.getProdutoId())
        );
        spnQuantidade.getValueFactory().setValue(
                item.getQuantidadeRecebida()
        );
        txtPrecoCompra.setText(
                item.getPrecoCompraUnitario()
                        .toPlainString()
                        .replace(".", ",")
        );
        btnAdicionarAtualizarItem.setText("Atualizar Item");
    }

    @FXML
    private void acaoRemoverItem() {
        if (tarefaConfirmacaoAtual != null) {
            return;
        }

        ItemEntradaEstoque selecionado = tabelaItens
                .getSelectionModel()
                .getSelectedItem();
        if (selecionado == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Atenção",
                    "Selecione um item para remover."
            );
            return;
        }

        itensRascunho.remove(selecionado);
        limparEdicaoItem();
        atualizarTotalizadores();
        atualizarEstadoControles();
    }

    private void limparEdicaoItem() {
        itemEmEdicao = null;
        tabelaItens.getSelectionModel().clearSelection();
        cbProduto.getSelectionModel().clearSelection();
        lblEstoqueAtual.setText("-");
        spnQuantidade.getValueFactory().setValue(QUANTIDADE_INICIAL);
        txtPrecoCompra.clear();
        btnAdicionarAtualizarItem.setText("Adicionar Item");
    }

    private void atualizarTotalizadores() {
        lblProdutosDistintos.setText(String.valueOf(itensRascunho.size()));
        lblTotalUnidades.setText(String.valueOf(
                calcularTotalUnidades(itensRascunho)
        ));
        lblValorTotal.setText(formatarMoeda(
                calcularValorTotal(itensRascunho)
        ));
    }

    private BigDecimal calcularValorTotal(List<ItemEntradaEstoque> itens) {
        return itens.stream()
                .map(ItemEntradaEstoque::getSubtotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int calcularTotalUnidades(List<ItemEntradaEstoque> itens) {
        return itens.stream()
                .map(ItemEntradaEstoque::getQuantidadeRecebida)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private String formatarMoeda(BigDecimal valor) {
        BigDecimal valorSeguro = valor != null ? valor : BigDecimal.ZERO;
        return formatadorMoeda.format(valorSeguro).replace('\u00A0', ' ');
    }

    @FXML
    private void acaoLimparEntrada() {
        if (tarefaConfirmacaoAtual != null) {
            return;
        }
        if (!rascunhoPossuiConteudoMaterial() || confirmarLimpezaRascunho()) {
            limparEntradaSemConfirmacao();
        }
    }

    private boolean confirmarLimpezaRascunho() {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Limpar Entrada");
        alerta.setHeaderText(null);
        alerta.setContentText(
                "Os dados ainda não confirmados serão descartados. Deseja continuar?"
        );

        ButtonType botaoLimpar = new ButtonType(
                "Limpar Entrada",
                ButtonBar.ButtonData.OK_DONE
        );
        ButtonType botaoContinuar = new ButtonType(
                "Continuar preenchendo",
                ButtonBar.ButtonData.CANCEL_CLOSE
        );
        alerta.getButtonTypes().setAll(botaoLimpar, botaoContinuar);
        return alerta.showAndWait().orElse(botaoContinuar) == botaoLimpar;
    }

    private void limparEntradaSemConfirmacao() {
        txtReferencia.clear();
        txtObservacao.clear();
        itensRascunho.clear();
        limparEdicaoItem();
        atualizarTotalizadores();
        atualizarEstadoControles();
    }

    private boolean rascunhoPossuiConteudoMaterial() {
        return !textoVazio(txtReferencia.getText())
                || !textoVazio(txtObservacao.getText())
                || !itensRascunho.isEmpty()
                || cbProduto.getValue() != null
                || !textoVazio(txtPrecoCompra.getText())
                || spnQuantidade.getValue() == null
                || spnQuantidade.getValue() != QUANTIDADE_INICIAL;
    }

    private boolean textoVazio(String texto) {
        return texto == null || texto.isBlank();
    }

    @FXML
    private void acaoConfirmarEntrada() {
        if (tarefaConfirmacaoAtual != null) {
            return;
        }

        Usuario usuario = obterAdministradorAptoDaSessao();
        if (usuario == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Acesso negado",
                    "Somente um administrador ativo e autorizado pode confirmar uma Entrada de Estoque."
            );
            atualizarEstadoControles();
            return;
        }

        if (itensRascunho.isEmpty()) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Atenção",
                    "Adicione ao menos um item antes de confirmar a Entrada de Estoque."
            );
            return;
        }

        EntradaEstoque rascunho;
        try {
            rascunho = montarNovoRascunhoParaConfirmacao();
        } catch (IllegalArgumentException e) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atenção", e.getMessage());
            return;
        }

        Integer usuarioId = usuario.getIdUsuario();
        Task<EntradaEstoque> novaTarefa = new Task<>() {
            @Override
            protected EntradaEstoque call() {
                return entradaEstoqueService.confirmarEntrada(
                        rascunho,
                        usuarioId
                );
            }
        };

        tarefaConfirmacaoAtual = novaTarefa;
        atualizarEstadoControles();

        novaTarefa.setOnSucceeded(evento -> {
            if (tarefaConfirmacaoAtual != novaTarefa) {
                return;
            }

            EntradaEstoque confirmada = novaTarefa.getValue();
            mostrarResumoEntradaConfirmada(confirmada);
            limparEntradaSemConfirmacao();
            tarefaConfirmacaoAtual = null;
            atualizarEstadoControles();
            carregarProdutosAtivos();
        });

        novaTarefa.setOnFailed(evento -> {
            if (tarefaConfirmacaoAtual != novaTarefa) {
                return;
            }

            Throwable erro = novaTarefa.getException();
            tarefaConfirmacaoAtual = null;
            atualizarEstadoControles();
            System.err.println("[ERRO] Falha ao confirmar Entrada de Estoque.");
            if (erro != null) {
                erro.printStackTrace();
            }

            String mensagem = erro instanceof IllegalArgumentException
                    || erro instanceof IllegalStateException
                    ? erro.getMessage()
                    : "Não foi possível confirmar a Entrada de Estoque. Tente novamente.";
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Entrada não confirmada",
                    mensagem
            );
        });

        novaTarefa.setOnCancelled(evento -> {
            if (tarefaConfirmacaoAtual == novaTarefa) {
                tarefaConfirmacaoAtual = null;
                atualizarEstadoControles();
            }
        });

        Thread thread = new Thread(
                novaTarefa,
                "entrada-estoque-confirmacao"
        );
        thread.setDaemon(true);
        thread.start();
    }

    private EntradaEstoque montarNovoRascunhoParaConfirmacao() {
        List<ItemEntradaEstoque> copiaItens = itensRascunho.stream()
                .map(item -> new ItemEntradaEstoque(
                        null,
                        null,
                        item.getProdutoId(),
                        item.getDescricaoProduto(),
                        item.getQuantidadeRecebida(),
                        item.getPrecoCompraUnitario()
                ))
                .toList();

        EntradaEstoque novoRascunho = new EntradaEstoque();
        novoRascunho.setReferencia(txtReferencia.getText());
        novoRascunho.setObservacao(txtObservacao.getText());
        novoRascunho.setItens(copiaItens);
        return novoRascunho;
    }

    private void mostrarResumoEntradaConfirmada(EntradaEstoque entrada) {
        if (entrada == null || entrada.getIdEntrada() == null
                || entrada.getDataHora() == null) {
            throw new IllegalStateException(
                    "A confirmação não retornou os dados persistidos da Entrada de Estoque."
            );
        }

        List<ItemEntradaEstoque> itensConfirmados = entrada.getItens();
        String mensagem = "Entrada de Estoque confirmada com sucesso."
                + "\n\nNúmero da Entrada: " + entrada.getIdEntrada()
                + "\nData/hora: " + entrada.getDataHora().format(FORMATADOR_DATA_HORA)
                + "\nResponsável: " + entrada.getNomeUsuario()
                + "\nProdutos distintos: " + itensConfirmados.size()
                + "\nUnidades recebidas: " + calcularTotalUnidades(itensConfirmados)
                + "\nValor total: " + formatarMoeda(calcularValorTotal(itensConfirmados));

        mostrarAlerta(
                Alert.AlertType.INFORMATION,
                "Entrada confirmada",
                mensagem
        );
    }

    private Usuario obterAdministradorAptoDaSessao() {
        Usuario usuario = SessaoUsuario
                .getInstance()
                .getUsuarioLogado();

        if (usuario == null || usuario.getIdUsuario() == null
                || usuario.getIdUsuario() <= 0) {
            return null;
        }

        if (!"ADMIN".equals(usuario.getPerfil())
                || !"ATIVO".equals(usuario.getStatus())
                || usuario.isTrocaSenhaObrigatoria()) {
            return null;
        }
        return usuario;
    }

    private void atualizarEstadoControles() {
        boolean confirmando = tarefaConfirmacaoAtual != null;
        boolean carregando = tarefaCarregamentoProdutosAtual != null;
        boolean administradorApto = obterAdministradorAptoDaSessao() != null;

        btnVoltar.setDisable(confirmando);
        txtReferencia.setDisable(confirmando);
        txtObservacao.setDisable(confirmando);
        cbProduto.setDisable(confirmando || carregando);
        spnQuantidade.setDisable(confirmando);
        txtPrecoCompra.setDisable(confirmando);
        tabelaItens.setDisable(confirmando);
        btnAdicionarAtualizarItem.setDisable(confirmando || carregando);
        btnRemoverItem.setDisable(
                confirmando
                        || tabelaItens.getSelectionModel().getSelectedItem() == null
        );
        boolean editandoItem = itemEmEdicao != null;
        lblOrientacaoEdicaoItem.setManaged(editandoItem);
        lblOrientacaoEdicaoItem.setVisible(editandoItem);
        btnLimparEntrada.setDisable(confirmando);
        btnConfirmarEntrada.setDisable(
                confirmando
                        || carregando
                        || itensRascunho.isEmpty()
                        || !administradorApto
        );

        boolean operacaoEmAndamento = confirmando || carregando;
        progressoOperacao.setVisible(operacaoEmAndamento);
        progressoOperacao.setManaged(operacaoEmAndamento);

        if (confirmando) {
            lblEstadoOperacao.setText("Confirmando Entrada de Estoque...");
        } else if (carregando) {
            lblEstadoOperacao.setText("Carregando produtos ativos...");
        } else if (!administradorApto) {
            lblEstadoOperacao.setText(
                    "Sessão sem autorização para confirmar entradas."
            );
        } else {
            lblEstadoOperacao.setText("");
        }
    }

    @FXML
    private void onVoltar() {
        if (tarefaConfirmacaoAtual != null) {
            return;
        }

        if (rascunhoPossuiConteudoMaterial() && !confirmarSaidaSemConfirmar()) {
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
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro",
                    "Não foi possível retornar para a Tela Principal."
            );
        }
    }

    private boolean confirmarSaidaSemConfirmar() {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Entrada não confirmada");
        alerta.setHeaderText(null);
        alerta.setContentText(
                "Existe um rascunho de Entrada de Estoque. Deseja sair sem confirmar?"
        );

        ButtonType botaoSair = new ButtonType(
                "Sair sem confirmar",
                ButtonBar.ButtonData.OK_DONE
        );
        ButtonType botaoContinuar = new ButtonType(
                "Continuar preenchendo",
                ButtonBar.ButtonData.CANCEL_CLOSE
        );
        alerta.getButtonTypes().setAll(botaoSair, botaoContinuar);
        Optional<ButtonType> resultado = alerta.showAndWait();
        return resultado.isPresent() && resultado.get() == botaoSair;
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
}
