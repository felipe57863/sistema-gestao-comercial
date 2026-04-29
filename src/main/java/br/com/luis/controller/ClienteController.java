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

/**
 * Controller da Tela de Cadastro de Clientes.
 * Responsável apenas pela interação com a interface.
 */
public class ClienteController {

    @FXML private TextField txtNome;
    @FXML private TextField txtDocumento;
    @FXML private TextField txtLimiteCredito;

    @FXML private RadioButton rbFisica;
    @FXML private RadioButton rbJuridica;
    @FXML private ToggleGroup tgTipoCliente;

    @FXML private ComboBox<PrazoPagamento> cbPrazoPagamento;

    private ClienteService clienteService;
    private PrazoPagamentoService prazoService;

    /**
     * Inicialização automática da tela.
     */
    @FXML
    public void initialize() {
        this.clienteService = new ClienteService();
        this.prazoService = new PrazoPagamentoService();

        // Garante que os RadioButtons estão no mesmo grupo
        if (tgTipoCliente == null) {
            tgTipoCliente = new ToggleGroup();
            rbFisica.setToggleGroup(tgTipoCliente);
            rbJuridica.setToggleGroup(tgTipoCliente);
        }

        carregarPrazosPagamento();

        txtNome.requestFocus();
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

            // Converte limite de crédito
            BigDecimal limite = new BigDecimal(txtLimiteCredito.getText());

            // Determina tipo com base no RadioButton
            Cliente.TipoCliente tipo;

            if (rbFisica.isSelected()) {
                tipo = Cliente.TipoCliente.PF;
            } else if (rbJuridica.isSelected()) {
                tipo = Cliente.TipoCliente.PJ;
            } else {
                throw new IllegalArgumentException("Selecione o tipo de cliente.");
            }

            // Obtém prazo selecionado
            PrazoPagamento prazo = cbPrazoPagamento.getValue();

            if (prazo == null) {
                throw new IllegalArgumentException("Selecione um prazo de pagamento.");
            }

            // Cria entidade
            Cliente cliente = new Cliente(
                    null,
                    nome,
                    documento,
                    tipo,
                    limite,
                    Cliente.StatusCliente.ATIVO,
                    prazo
            );

            // Envia para Service
            clienteService.cadastrar(cliente);

            // Log
            System.out.println("[LOG] Cliente cadastrado via UI: " + cliente.getNome());

            mostrarAlerta(Alert.AlertType.INFORMATION,
                    "Sucesso",
                    "Cliente cadastrado com sucesso!");

            limparFormulario();

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
     * Limpa os campos após cadastro.
     */
    private void limparFormulario() {
        txtNome.clear();
        txtDocumento.clear();
        txtLimiteCredito.clear();
        cbPrazoPagamento.getSelectionModel().clearSelection();
        tgTipoCliente.selectToggle(null);

        txtNome.requestFocus();
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