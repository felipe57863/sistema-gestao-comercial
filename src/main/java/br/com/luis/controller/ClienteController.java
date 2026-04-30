package br.com.luis.controller;

import br.com.luis.model.Cliente;
import br.com.luis.model.PrazoPagamento;
import br.com.luis.service.ClienteService;
import br.com.luis.service.PrazoPagamentoService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    private ToggleGroup tgTipoCliente;

    @FXML private RadioButton rbAtivo;
    @FXML private RadioButton rbBloqueado;
    private ToggleGroup tgStatusCliente;

    @FXML private ComboBox<PrazoPagamento> cbPrazoPagamento;

    // --- TABELA ---
    @FXML private TableView<Cliente> tabelaClientes;
    @FXML private TableColumn<Cliente, String> colNome;
    @FXML private TableColumn<Cliente, String> colTipo;
    @FXML private TableColumn<Cliente, BigDecimal> colLimite;
    @FXML private TableColumn<Cliente, String> colStatus;

    // --- SERVICES ---
    private ClienteService clienteService;
    private PrazoPagamentoService prazoService;

    @FXML
    public void initialize() {

        this.clienteService = new ClienteService();
        this.prazoService = new PrazoPagamentoService();

        // Configuração dos RadioButtons via código (evita bug do Scene Builder)
        tgTipoCliente = new ToggleGroup();
        rbFisica.setToggleGroup(tgTipoCliente);
        rbJuridica.setToggleGroup(tgTipoCliente);

        tgStatusCliente = new ToggleGroup();
        rbAtivo.setToggleGroup(tgStatusCliente);
        rbBloqueado.setToggleGroup(tgStatusCliente);

        // Simulação de sessão (no futuro virá do SessaoUsuario)
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        lblUsuario.setText("Usuário: Administrador | " + dtf.format(LocalDateTime.now()));

        // Carrega dados do banco
        carregarPrazosPagamento();

        // Foco inicial
        txtNome.requestFocus();

        // ==============================
        // CONFIGURAÇÃO DA TABLEVIEW
        // ==============================

        // Nome direto do model
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        // Limite direto do model
        colLimite.setCellValueFactory(new PropertyValueFactory<>("limiteCredito"));

        // Tipo com descrição amigável (Enum → String)
        colTipo.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTipo().getDescricao())
        );

        // Status amigável
        colStatus.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getStatus() == Cliente.StatusCliente.ATIVO
                                ? "Ativo"
                                : "Bloqueado"
                )
        );

        // Formatação monetária (R$)
        colLimite.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("R$ " + item.toString().replace(".", ","));
                }
            }
        });

        // Carrega clientes ao abrir a tela
        atualizarTabela();
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
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Erro",
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

            // Determina tipo
            Cliente.TipoCliente tipo =
                    rbFisica.isSelected() ? Cliente.TipoCliente.PF : Cliente.TipoCliente.PJ;

            // Determina status
            Cliente.StatusCliente status =
                    rbAtivo.isSelected() ? Cliente.StatusCliente.ATIVO : Cliente.StatusCliente.BLOQUEADO;

            // Prazo obrigatório
            PrazoPagamento prazo = cbPrazoPagamento.getValue();
            if (prazo == null) {
                throw new IllegalArgumentException("Selecione um prazo de pagamento.");
            }

            // Criação da entidade
            Cliente cliente = new Cliente(null, nome, documento, tipo, limite, status, prazo);

            // Persistência
            clienteService.cadastrar(cliente);

            System.out.println("[LOG] Cliente cadastrado via UI: " + cliente.getNome());

            // Atualiza tabela automaticamente
            atualizarTabela();

            mostrarAlerta(Alert.AlertType.INFORMATION,
                    "Sucesso",
                    "Cliente cadastrado com sucesso!");

            limpar();

        } catch (NumberFormatException e) {

            mostrarAlerta(Alert.AlertType.WARNING,
                    "Aviso",
                    "Limite de crédito inválido.");

            txtLimiteCredito.requestFocus();

        } catch (RuntimeException e) {

            mostrarAlerta(Alert.AlertType.ERROR,
                    "Erro",
                    e.getMessage());
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
    }

    /**
     * Ação de cancelar (preparado para navegação futura).
     */
    @FXML
    public void cancelar() {
        limpar();
        System.out.println("[LOG] Cadastro cancelado pelo usuário.");
    }

    /**
     * Atualiza a tabela com dados do banco.
     */
    private void atualizarTabela() {
        try {
            ObservableList<Cliente> clientes =
                    FXCollections.observableArrayList(clienteService.listarTodos());

            tabelaClientes.setItems(clientes);

        } catch (RuntimeException e) {
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Erro",
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
}