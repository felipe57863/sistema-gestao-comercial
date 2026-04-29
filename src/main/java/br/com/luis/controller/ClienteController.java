package br.com.luis.controller;

import br.com.luis.model.Cliente;
import br.com.luis.model.PrazoPagamento;
import br.com.luis.service.ClienteService;
import br.com.luis.service.PrazoPagamentoService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller da Tela de Cadastro de Clientes.
 */
public class ClienteController {

    // --- MAPEAMENTO DO CABEÇALHO ---
    @FXML private Label lblUsuario;

    // --- MAPEAMENTO DO FORMULÁRIO ---
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

    // --- MAPEAMENTO DA TABELA (Preparação para o próximo passo) ---
    @FXML private TableView<Cliente> tabelaClientes;
    @FXML private TableColumn<Cliente, String> colNome;
    @FXML private TableColumn<Cliente, String> colTipo;
    @FXML private TableColumn<Cliente, BigDecimal> colLimite;
    @FXML private TableColumn<Cliente, String> colStatus;

    // --- SERVIÇOS ---
    private ClienteService clienteService;
    private PrazoPagamentoService prazoService;

    @FXML
    public void initialize() {
        this.clienteService = new ClienteService();
        this.prazoService = new PrazoPagamentoService();

        // 1. Configura os Grupos de RadioButtons via código (Foge do bug do Scene Builder)
        if (tgTipoCliente == null) {
            tgTipoCliente = new ToggleGroup();
            rbFisica.setToggleGroup(tgTipoCliente);
            rbJuridica.setToggleGroup(tgTipoCliente);
        }

        if (tgStatusCliente == null) {
            tgStatusCliente = new ToggleGroup();
            rbAtivo.setToggleGroup(tgStatusCliente);
            rbBloqueado.setToggleGroup(tgStatusCliente);
        }

        // 2. Preenche o Label de Usuário com a data e hora atual (Simulação de Sessão)
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        lblUsuario.setText("Usuário: Administrador | " + dtf.format(LocalDateTime.now()));

        // 3. Carrega o ComboBox
        carregarPrazosPagamento();

        // 4. Foco inicial
        txtNome.requestFocus();
    }

    private void carregarPrazosPagamento() {
        try {
            ObservableList<PrazoPagamento> prazos = FXCollections.observableArrayList(prazoService.listarAtivos());
            cbPrazoPagamento.setItems(prazos);
        } catch (RuntimeException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Não foi possível carregar os prazos.\n" + e.getMessage());
        }
    }

    @FXML
    public void salvar() {
        try {
            String nome = txtNome.getText();
            String documento = txtDocumento.getText();
            BigDecimal limite = new BigDecimal(txtLimiteCredito.getText());

            Cliente.TipoCliente tipo = rbFisica.isSelected() ? Cliente.TipoCliente.PF : Cliente.TipoCliente.PJ;
            Cliente.StatusCliente status = rbAtivo.isSelected() ? Cliente.StatusCliente.ATIVO : Cliente.StatusCliente.BLOQUEADO;

            PrazoPagamento prazo = cbPrazoPagamento.getValue();
            if (prazo == null) throw new IllegalArgumentException("Selecione um prazo de pagamento.");

            Cliente cliente = new Cliente(null, nome, documento, tipo, limite, status, prazo);

            clienteService.cadastrar(cliente);
            System.out.println("[LOG] Cliente cadastrado via UI: " + cliente.getNome());

            mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Cliente cadastrado com sucesso!");
            limpar(); // Chama o método público de limpar

        } catch (NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.WARNING, "Aviso", "Limite de crédito inválido.");
            txtLimiteCredito.requestFocus();
        } catch (RuntimeException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", e.getMessage());
        }
    }

    /**
     * Mapeado para o botão "Limpar" no FXML
     */
    @FXML
    public void limpar() {
        txtNome.clear();
        txtDocumento.clear();
        txtLimiteCredito.clear();
        cbPrazoPagamento.getSelectionModel().clearSelection();

        // Reseta os RadioButtons para o padrão
        rbFisica.setSelected(true);
        rbAtivo.setSelected(true);

        txtNome.requestFocus();
    }

    /**
     * Mapeado para o botão "Cancelar" no FXML
     */
    @FXML
    public void cancelar() {
        limpar();
        System.out.println("[LOG] Operação de cadastro cancelada pelo usuário.");
        // Futuramente, este botão pode fechar a janela (Stage) ou voltar ao Menu Principal
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensagem);
        alerta.showAndWait();
    }
}