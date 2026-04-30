package br.com.luis.controller;

import br.com.luis.model.Cliente;
import br.com.luis.model.PrazoPagamento;
import br.com.luis.service.ClienteService;
import br.com.luis.service.PrazoPagamentoService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Controller da Tela de Cadastro de Clientes.
 * Responsável por intermediar a UI e a camada de serviço.
 */
public class ClienteController {

    // --- CABEÇALHO ---
    @FXML private Label lblUsuario;

    // --- FORMULÁRIO ---
    @FXML private TextField txtNome;
    @FXML private TextField txtDocumento;
    @FXML private TextField txtLimiteCredito;

    @FXML private RadioButton rbFisica;
    @FXML private RadioButton rbJuridica;
    private ToggleGroup tgTipoCliente; // Instanciado via código

    @FXML private RadioButton rbAtivo;
    @FXML private RadioButton rbBloqueado;
    private ToggleGroup tgStatusCliente; // Instanciado via código

    @FXML private ComboBox<PrazoPagamento> cbPrazoPagamento;

    // --- TABELA ---
    @FXML private TextField txtBusca;
    @FXML private TableView<Cliente> tabelaClientes;
    @FXML private TableColumn<Cliente, String> colNome;
    @FXML private TableColumn<Cliente, String> colTipo;
    @FXML private TableColumn<Cliente, BigDecimal> colLimite;
    @FXML private TableColumn<Cliente, String> colStatus;

    // --- RODAPÉ ---
    @FXML private Label lblTotalClientes;

    // Lista mestre que guarda os dados originais do banco
    private ObservableList<Cliente> listaClientesMaster = FXCollections.observableArrayList();

    // --- SERVICES ---
    private ClienteService clienteService;
    private PrazoPagamentoService prazoService;

    // Variável para controlar se estamos a criar (null) ou editar (com dados)
    private Cliente clienteSelecionado;

    @FXML
    public void initialize() {

        this.clienteService = new ClienteService();
        this.prazoService = new PrazoPagamentoService();

        // 1. Configura os Grupos de RadioButtons via código (Foge do bug do Scene Builder)
        tgTipoCliente = new ToggleGroup();
        rbFisica.setToggleGroup(tgTipoCliente);
        rbJuridica.setToggleGroup(tgTipoCliente);

        tgStatusCliente = new ToggleGroup();
        rbAtivo.setToggleGroup(tgStatusCliente);
        rbBloqueado.setToggleGroup(tgStatusCliente);

        // 2. Preenche o Label de Usuário com a data e hora atual (Simulação de Sessão)
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        lblUsuario.setText("Usuário: Administrador | " + dtf.format(LocalDateTime.now()));

        // 3. Carrega o ComboBox
        carregarPrazosPagamento();

        // 4. Foco inicial
        txtNome.requestFocus();

        // 5. CONFIGURAÇÃO DA TABELA

        // Mapeia a coluna "Nome" diretamente para o getNome() da classe Cliente
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        // Mapeia a coluna "Limite" diretamente para o getLimiteCredito()
        colLimite.setCellValueFactory(new PropertyValueFactory<>("limiteCredito"));

        // Tratamento Sênior para Enums: Extraindo o texto amigável em vez de usar o nome técnico
        colTipo.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTipo().getDescricao())
        );

        colStatus.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getStatus() == Cliente.StatusCliente.ATIVO
                                ? "Ativo"
                                : "Bloqueado"
                )
        );

        // Formatação monetária (R$ padrão brasileiro)
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        colLimite.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(nf.format(item));
                }
            }
        });

        // CONFIGURAÇÃO DO FILTRO DE BUSCA REATIVO

        // 1. Envolve a lista mestre num FilteredList
        FilteredList<Cliente> filteredData = new FilteredList<>(listaClientesMaster, p -> true);

        // 2. Adiciona o ouvinte no campo de busca para alterar o filtro instantaneamente
        txtBusca.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(cliente -> {

                // Se o campo estiver vazio, mostra tudo
                if (newValue == null || newValue.isBlank()) {
                    return true;
                }

                String filtro = newValue.toLowerCase();

                // Busca por nome OU documento
                return (cliente.getNome() != null && cliente.getNome().toLowerCase().contains(filtro))
                        || (cliente.getDocumento() != null && cliente.getDocumento().toLowerCase().contains(filtro));
            });
        });

        // 3. Envolve o FilteredList num SortedList (mantém ordenação por clique nas colunas)
        SortedList<Cliente> sortedData = new SortedList<>(filteredData);

        // 4. Liga a ordenação com a tabela
        sortedData.comparatorProperty().bind(tabelaClientes.comparatorProperty());

        // 5. Injeta na tabela
        tabelaClientes.setItems(sortedData);

        // 6. Atualiza contador sempre que a lista mudar
        tabelaClientes.getItems().addListener((javafx.collections.ListChangeListener<Cliente>) c -> atualizarContador());

        // 7. Carrega dados iniciais
        atualizarTabela();

        // 8. Listener para detectar clique na tabela e preencher formulário
        tabelaClientes.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        preencherFormulario(newValue);
                    }
                }
        );
    }

    /**
     * Atualiza o contador do rodapé.
     */
    private void atualizarContador() {
        int total = tabelaClientes.getItems().size();
        lblTotalClientes.setText("Total: " + total + " cliente" + (total == 1 ? "" : "s"));
    }

    /**
     * Carrega os prazos ativos no ComboBox.
     */
    private void carregarPrazosPagamento() {
        try {
            ObservableList<PrazoPagamento> prazos =
                    FXCollections.observableArrayList(prazoService.listarAtivos());

            cbPrazoPagamento.setItems(prazos);

        } catch (RuntimeException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro",
                    "Não foi possível carregar os prazos.\n" + e.getMessage());
        }
    }

    /**
     * Ação do botão "Salvar".
     */
    @FXML
    public void salvar() {
        try {

            String nome = txtNome.getText();
            String documento = txtDocumento.getText();

            // Tratamento simples para evitar erro com vírgula
            String limiteTexto = txtLimiteCredito.getText().replace(",", ".");
            BigDecimal limite = new BigDecimal(limiteTexto);

            Cliente.TipoCliente tipo =
                    rbFisica.isSelected() ? Cliente.TipoCliente.PF : Cliente.TipoCliente.PJ;

            Cliente.StatusCliente status =
                    rbAtivo.isSelected() ? Cliente.StatusCliente.ATIVO : Cliente.StatusCliente.BLOQUEADO;

            PrazoPagamento prazo = cbPrazoPagamento.getValue();
            if (prazo == null) {
                cbPrazoPagamento.requestFocus();
                throw new IllegalArgumentException("Selecione um prazo de pagamento.");
            }

            // DECISÃO: CADASTRO OU EDIÇÃO
            if (clienteSelecionado == null) {

                // NOVO CLIENTE
                Cliente cliente = new Cliente(null, nome, documento, tipo, limite, status, prazo);
                clienteService.cadastrar(cliente);
                System.out.println("[LOG] Cliente cadastrado via UI: " + cliente.getNome());

            } else {

                // EDIÇÃO (Isolamento de Memória)
                Cliente clienteAtualizado = new Cliente(
                        clienteSelecionado.getIdCliente(),
                        nome,
                        documento,
                        tipo,
                        limite,
                        status,
                        prazo
                );

                clienteService.atualizar(clienteAtualizado);
                System.out.println("[LOG] Cliente atualizado: " + clienteAtualizado.getNome());
            }

            // Atualiza tabela automaticamente
            atualizarTabela();

            mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Operação realizada com sucesso!");

            limpar();

        } catch (NumberFormatException e) {

            mostrarAlerta(Alert.AlertType.WARNING, "Aviso", "Limite de crédito inválido.");
            txtLimiteCredito.requestFocus();

        } catch (RuntimeException e) {

            mostrarAlerta(Alert.AlertType.ERROR, "Erro", e.getMessage());
        }
    }

    /**
     * Limpa o formulário.
     */
    @FXML
    public void limpar() {

        txtNome.clear();
        txtDocumento.clear();
        txtLimiteCredito.clear();

        cbPrazoPagamento.getSelectionModel().clearSelection();

        // Reseta estados
        rbFisica.setSelected(true);
        rbAtivo.setSelected(true);

        txtNome.requestFocus();

        // Limpa seleção (volta para modo cadastro)
        this.clienteSelecionado = null;
        tabelaClientes.getSelectionModel().clearSelection();
    }

    /**
     * Ação de cancelar.
     */
    @FXML
    public void cancelar() {
        limpar();
        System.out.println("[LOG] Cadastro cancelado pelo usuário.");
    }

    /**
     * Atualiza dados da tabela.
     */
    private void atualizarTabela() {
        try {
            // O setAll dispara o ListChangeListener, que atualiza o contador automaticamente
            listaClientesMaster.setAll(clienteService.listarTodos());
        } catch (RuntimeException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro",
                    "Não foi possível carregar clientes.\n" + e.getMessage());
        }
    }

    /**
     * Método utilitário para alertas.
     */
    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensagem);
        alerta.showAndWait();
    }

    /**
     * Preenche o formulário com os dados do cliente selecionado.
     */
    private void preencherFormulario(Cliente cliente) {
        this.clienteSelecionado = cliente;

        txtNome.setText(cliente.getNome());
        txtDocumento.setText(cliente.getDocumento());

        txtLimiteCredito.setText(cliente.getLimiteCredito().toString().replace(".", ","));

        rbFisica.setSelected(cliente.getTipo() == Cliente.TipoCliente.PF);
        rbAtivo.setSelected(cliente.getStatus() == Cliente.StatusCliente.ATIVO);

        cbPrazoPagamento.setValue(cliente.getPrazoPagamento());
    }
}