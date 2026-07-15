package br.com.luis.controller;

import br.com.luis.model.Cliente;
import br.com.luis.model.PrazoPagamento;
import br.com.luis.service.ClienteService;
import br.com.luis.service.PrazoPagamentoService;
import br.com.luis.util.NavegacaoUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Controller da Tela de Cadastro de Clientes.
 * Responsável por intermediar a UI e a camada de serviço.
 */
public class ClienteController {

    // --- CABEÇALHO ---
    @FXML private Button btnVoltar;
    @FXML private Label lblUsuario;

    // --- FORMULÁRIO ---
    @FXML private TextField txtNome;
    @FXML private TextField txtDocumento;
    @FXML private TextField txtLimiteCredito;

    @FXML private RadioButton rbFisica;
    @FXML private RadioButton rbJuridica;
    private ToggleGroup tgTipoCliente;

    @FXML private RadioButton rbAtivo;
    @FXML private RadioButton rbBloqueado;
    private ToggleGroup tgStatusCliente;

    @FXML private ComboBox<PrazoPagamento> cbPrazoPagamento;

    // --- BOTÕES ---
    @FXML private Button btnNovo;
    @FXML private Button btnSalvar;
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
    private final ObservableList<Cliente> listaClientesMaster = FXCollections.observableArrayList();

    // --- SERVICES ---
    private ClienteService clienteService;
    private PrazoPagamentoService prazoService;

    // Variável para controlar se estamos criando (null) ou editando (com dados)
    private Cliente clienteSelecionado;

    @FXML
    public void initialize() {

        this.clienteService = new ClienteService();
        this.prazoService = new PrazoPagamentoService();

        configurarRadioButtons();
        configurarCabecalho();
        configurarMascaraDocumento();
        configurarTabela();
        configurarBusca();

        carregarPrazosPagamento();
        atualizarTabela();

        btnSalvar.setText("Salvar");
        txtNome.requestFocus();
    }

    /**
     * Configura os grupos dos RadioButtons e os valores padrão da tela.
     */
    private void configurarRadioButtons() {

        tgTipoCliente = new ToggleGroup();
        rbFisica.setToggleGroup(tgTipoCliente);
        rbJuridica.setToggleGroup(tgTipoCliente);

        tgStatusCliente = new ToggleGroup();
        rbAtivo.setToggleGroup(tgStatusCliente);
        rbBloqueado.setToggleGroup(tgStatusCliente);

        // Estado inicial seguro da tela
        rbFisica.setSelected(true);
        rbAtivo.setSelected(true);
    }

    /**
     * Configura o cabeçalho da tela.
     */
    private void configurarCabecalho() {

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // Exibe a identificação da tela e a data/hora atuais, sem simular um usuário.
        lblUsuario.setText("Cadastro de Clientes | " + dtf.format(LocalDateTime.now()));
    }

    /**
     * Retorna diretamente para a Tela Principal usando o mesmo Stage atual.
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
            System.err.println("[ERRO] Falha ao voltar para a Tela Principal.");
            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro",
                    "Não foi possível retornar para a Tela Principal."
            );
        }
    }

    /**
     * Configura máscara simples de CPF/CNPJ conforme o tipo de cliente selecionado.
     */
    private void configurarMascaraDocumento() {

        atualizarPromptDocumento();

        tgTipoCliente.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            atualizarPromptDocumento();
            txtDocumento.clear();
        });

        txtDocumento.textProperty().addListener((observable, oldValue, newValue) -> {

            if (newValue == null) {
                return;
            }

            String formatado = formatarDocumentoConformeTipo(newValue);

            if (!formatado.equals(newValue)) {
                txtDocumento.setText(formatado);
                txtDocumento.positionCaret(formatado.length());
            }
        });
    }

    /**
     * Atualiza o texto de ajuda do campo documento conforme PF/PJ.
     */
    private void atualizarPromptDocumento() {
        if (rbFisica.isSelected()) {
            txtDocumento.setPromptText("CPF");
        } else {
            txtDocumento.setPromptText("CNPJ");
        }
    }

    /**
     * Aplica máscara de CPF ou CNPJ conforme o tipo selecionado.
     */
    private String formatarDocumentoConformeTipo(String texto) {

        String numeros = texto.replaceAll("[^0-9]", "");

        if (rbFisica.isSelected()) {
            numeros = limitarTexto(numeros, 11);
            return formatarCpf(numeros);
        }

        numeros = limitarTexto(numeros, 14);
        return formatarCnpj(numeros);
    }

    /**
     * Limita o texto ao tamanho máximo informado.
     */
    private String limitarTexto(String texto, int tamanhoMaximo) {
        if (texto.length() <= tamanhoMaximo) {
            return texto;
        }

        return texto.substring(0, tamanhoMaximo);
    }

    /**
     * Formata CPF parcialmente durante a digitação.
     */
    private String formatarCpf(String numeros) {

        if (numeros.length() <= 3) {
            return numeros;
        }

        if (numeros.length() <= 6) {
            return numeros.substring(0, 3) + "." + numeros.substring(3);
        }

        if (numeros.length() <= 9) {
            return numeros.substring(0, 3) + "."
                    + numeros.substring(3, 6) + "."
                    + numeros.substring(6);
        }

        return numeros.substring(0, 3) + "."
                + numeros.substring(3, 6) + "."
                + numeros.substring(6, 9) + "-"
                + numeros.substring(9);
    }

    /**
     * Formata CNPJ parcialmente durante a digitação.
     */
    private String formatarCnpj(String numeros) {

        if (numeros.length() <= 2) {
            return numeros;
        }

        if (numeros.length() <= 5) {
            return numeros.substring(0, 2) + "." + numeros.substring(2);
        }

        if (numeros.length() <= 8) {
            return numeros.substring(0, 2) + "."
                    + numeros.substring(2, 5) + "."
                    + numeros.substring(5);
        }

        if (numeros.length() <= 12) {
            return numeros.substring(0, 2) + "."
                    + numeros.substring(2, 5) + "."
                    + numeros.substring(5, 8) + "/"
                    + numeros.substring(8);
        }

        return numeros.substring(0, 2) + "."
                + numeros.substring(2, 5) + "."
                + numeros.substring(5, 8) + "/"
                + numeros.substring(8, 12) + "-"
                + numeros.substring(12);
    }

    /**
     * Configura colunas e formatação da tabela.
     */
    private void configurarTabela() {

        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colLimite.setCellValueFactory(new PropertyValueFactory<>("limiteCredito"));

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

        tabelaClientes.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        preencherFormulario(newValue);
                        btnSalvar.setText("Atualizar");
                    }
                }
        );
    }

    /**
     * Configura o filtro de busca reativo da tabela.
     */
    private void configurarBusca() {

        FilteredList<Cliente> filteredData = new FilteredList<>(listaClientesMaster, p -> true);

        txtBusca.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(cliente -> {

                if (newValue == null || newValue.isBlank()) {
                    return true;
                }

                String filtro = newValue.toLowerCase();

                return (cliente.getNome() != null && cliente.getNome().toLowerCase().contains(filtro))
                        || (cliente.getDocumento() != null && cliente.getDocumento().toLowerCase().contains(filtro));
            });
        });

        SortedList<Cliente> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tabelaClientes.comparatorProperty());

        tabelaClientes.setItems(sortedData);

        tabelaClientes.getItems().addListener(
                (ListChangeListener<Cliente>) change -> atualizarContador()
        );

        atualizarContador();
    }

    /**
     * Atualiza o contador do rodapé.
     */
    private void atualizarContador() {
        int total = tabelaClientes.getItems().size();
        lblTotalClientes.setText("Total: " + total + " cliente" + (total == 1 ? "" : "s"));
    }

    /**
     * Carrega os prazos ativos no ComboBox usando Task para não congelar a interface.
     */
    private void carregarPrazosPagamento() {

        Task<List<PrazoPagamento>> task = new Task<>() {
            @Override
            protected List<PrazoPagamento> call() {
                return prazoService.listarAtivos();
            }
        };

        task.setOnSucceeded(event -> {
            ObservableList<PrazoPagamento> prazos =
                    FXCollections.observableArrayList(task.getValue());

            cbPrazoPagamento.setItems(prazos);
        });

        task.setOnFailed(event -> {
            System.err.println("[ERRO] Falha ao carregar prazos de pagamento.");
            task.getException().printStackTrace();

            mostrarAlerta(Alert.AlertType.ERROR,
                    "Erro",
                    "Não foi possível carregar os prazos de pagamento.");
        });

        Thread thread = new Thread(task, "carregar-prazos-pagamento");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Ação do botão "Salvar".
     */
    @FXML
    public void salvar() {

        try {
            String nome = txtNome.getText();
            String documento = txtDocumento.getText();
            BigDecimal limite = converterLimiteCredito(txtLimiteCredito.getText());

            Cliente.TipoCliente tipo = obterTipoSelecionado();
            Cliente.StatusCliente status = obterStatusSelecionado();

            PrazoPagamento prazo = cbPrazoPagamento.getValue();

            if (prazo == null) {
                cbPrazoPagamento.requestFocus();
                throw new IllegalArgumentException("Selecione um prazo de pagamento.");
            }

            if (clienteSelecionado == null) {

                Cliente cliente = new Cliente(null, nome, documento, tipo, limite, status, prazo);
                clienteService.cadastrar(cliente);

                System.out.println("[LOG] Cliente cadastrado via UI: " + cliente.getNome());

            } else {

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

            atualizarTabela();

            mostrarAlerta(Alert.AlertType.INFORMATION,
                    "Sucesso",
                    "Operação realizada com sucesso!");

            limparCamposFormulario();
            voltarModoCadastro();

        } catch (NumberFormatException e) {

            mostrarAlerta(Alert.AlertType.WARNING,
                    "Aviso",
                    "Limite de crédito inválido.");

            txtLimiteCredito.requestFocus();

        } catch (IllegalArgumentException e) {

            mostrarAlerta(Alert.AlertType.WARNING,
                    "Aviso",
                    e.getMessage());

        } catch (RuntimeException e) {

            System.err.println("[ERRO] Falha ao salvar cliente.");
            e.printStackTrace();

            mostrarAlerta(Alert.AlertType.ERROR,
                    "Erro",
                    "Não foi possível salvar o cliente. Verifique os dados e tente novamente.");
        }
    }

    /**
     * Converte o texto do limite de crédito para BigDecimal.
     */
    private BigDecimal converterLimiteCredito(String texto) {

        if (texto == null || texto.isBlank()) {
            throw new NumberFormatException("Limite de crédito vazio.");
        }

        String normalizado = texto.trim()
                .replace("R$", "")
                .replace(" ", "")
                .replace(".", "")
                .replace(",", ".");

        return new BigDecimal(normalizado);
    }

    /**
     * Obtém o tipo de cliente selecionado.
     */
    private Cliente.TipoCliente obterTipoSelecionado() {

        if (rbFisica.isSelected()) {
            return Cliente.TipoCliente.PF;
        }

        if (rbJuridica.isSelected()) {
            return Cliente.TipoCliente.PJ;
        }

        throw new IllegalArgumentException("Selecione o tipo de cliente.");
    }

    /**
     * Obtém o status de cliente selecionado.
     */
    private Cliente.StatusCliente obterStatusSelecionado() {

        if (rbAtivo.isSelected()) {
            return Cliente.StatusCliente.ATIVO;
        }

        if (rbBloqueado.isSelected()) {
            return Cliente.StatusCliente.BLOQUEADO;
        }

        throw new IllegalArgumentException("Selecione o status do cliente.");
    }

    /**
     * Limpa apenas os campos do formulário.
     * Não remove seleção da tabela e não sai do modo edição.
     */
    @FXML
    public void limpar() {
        limparCamposFormulario();

        if (clienteSelecionado != null) {
            btnSalvar.setText("Atualizar");
        }
    }

    /**
     * Prepara a tela para cadastrar um novo cliente.
     */
    @FXML
    public void novo() {
        limparCamposFormulario();
        voltarModoCadastro();

        System.out.println("[LOG] Novo cadastro de cliente iniciado.");
    }

    /**
     * Cancela a operação atual e volta ao modo cadastro.
     */
    @FXML
    public void cancelar() {
        limparCamposFormulario();
        voltarModoCadastro();

        System.out.println("[LOG] Cadastro cancelado pelo usuário.");
    }

    /**
     * Limpa somente os campos visuais do formulário.
     * Não altera clienteSelecionado e não mexe na seleção da tabela.
     */
    private void limparCamposFormulario() {

        txtNome.clear();
        txtDocumento.clear();
        txtLimiteCredito.clear();

        cbPrazoPagamento.getSelectionModel().clearSelection();

        rbFisica.setSelected(true);
        rbJuridica.setSelected(false);

        rbAtivo.setSelected(true);
        rbBloqueado.setSelected(false);

        atualizarPromptDocumento();

        txtNome.requestFocus();
    }

    /**
     * Volta a tela para modo cadastro.
     * Remove seleção, limpa clienteSelecionado e restaura o botão Salvar.
     */
    private void voltarModoCadastro() {

        this.clienteSelecionado = null;

        tabelaClientes.getSelectionModel().clearSelection();

        btnSalvar.setText("Salvar");
    }

    /**
     * Atualiza dados da tabela usando Task para evitar travamento da interface.
     */
    private void atualizarTabela() {

        tabelaClientes.setPlaceholder(new Label("Carregando clientes..."));

        Task<List<Cliente>> task = new Task<>() {
            @Override
            protected List<Cliente> call() {
                return clienteService.listarTodos();
            }
        };

        task.setOnSucceeded(event -> {
            listaClientesMaster.setAll(task.getValue());
            atualizarContador();

            if (listaClientesMaster.isEmpty()) {
                tabelaClientes.setPlaceholder(new Label("Nenhum cliente cadastrado."));
            }
        });

        task.setOnFailed(event -> {
            System.err.println("[ERRO] Falha ao carregar clientes.");
            task.getException().printStackTrace();

            tabelaClientes.setPlaceholder(new Label("Não foi possível carregar os clientes."));

            mostrarAlerta(Alert.AlertType.ERROR,
                    "Erro",
                    "Não foi possível carregar os clientes.");
        });

        Thread thread = new Thread(task, "carregar-clientes");
        thread.setDaemon(true);
        thread.start();
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

        rbFisica.setSelected(cliente.getTipo() == Cliente.TipoCliente.PF);
        rbJuridica.setSelected(cliente.getTipo() == Cliente.TipoCliente.PJ);

        rbAtivo.setSelected(cliente.getStatus() == Cliente.StatusCliente.ATIVO);
        rbBloqueado.setSelected(cliente.getStatus() == Cliente.StatusCliente.BLOQUEADO);

        atualizarPromptDocumento();

        txtNome.setText(cliente.getNome());
        txtDocumento.setText(formatarDocumentoConformeTipo(cliente.getDocumento()));
        txtLimiteCredito.setText(cliente.getLimiteCredito().toString().replace(".", ","));

        selecionarPrazoNoCombo(cliente.getPrazoPagamento());
    }

    /**
     * Seleciona no ComboBox o prazo correspondente ao ID do cliente.
     */
    private void selecionarPrazoNoCombo(PrazoPagamento prazoCliente) {

        if (prazoCliente == null || prazoCliente.getIdPrazo() == null) {
            cbPrazoPagamento.getSelectionModel().clearSelection();
            return;
        }

        for (PrazoPagamento prazo : cbPrazoPagamento.getItems()) {
            if (prazo.getIdPrazo().equals(prazoCliente.getIdPrazo())) {
                cbPrazoPagamento.getSelectionModel().select(prazo);
                return;
            }
        }

        cbPrazoPagamento.setValue(prazoCliente);
    }
}