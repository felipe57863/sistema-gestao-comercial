package br.com.luis.controller;

import br.com.luis.model.StatusContaReceber;
import br.com.luis.service.ContaReceberService;
import br.com.luis.viewmodel.ContaReceberListagemView;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Controller da tela de Contas a Receber.
 *
 * Nesta etapa, o Controller configura a tabela e carrega
 * as contas pendentes usando a camada de Service.
 *
 * A seleção da conta, navegação e recebimento serão implementados
 * em passos posteriores.
 */
public class ContasReceberController {

    @FXML private Label lblUsuario;
    @FXML private Label lblDataHora;

    @FXML private Label lblContaId;
    @FXML private Label lblCliente;
    @FXML private Label lblVendaId;
    @FXML private Label lblValor;
    @FXML private Label lblVencimento;
    @FXML private Label lblStatus;
    @FXML private Label lblSituacao;

    @FXML private Button btnVoltar;
    @FXML private Button btnReceberConta;
    @FXML private Button btnAtualizar;

    @FXML private TableView<ContaReceberListagemView> tabelaContasPendentes;

    @FXML private TableColumn<ContaReceberListagemView, Integer> colConta;
    @FXML private TableColumn<ContaReceberListagemView, String> colCliente;
    @FXML private TableColumn<ContaReceberListagemView, Integer> colVenda;
    @FXML private TableColumn<ContaReceberListagemView, BigDecimal> colValor;
    @FXML private TableColumn<ContaReceberListagemView, LocalDate> colVencimento;
    @FXML private TableColumn<ContaReceberListagemView, StatusContaReceber> colStatus;
    @FXML private TableColumn<ContaReceberListagemView, String> colSituacao;

    @FXML private Label lblTotalContas;

    private final ContaReceberService contaReceberService;
    private final ObservableList<ContaReceberListagemView> contasPendentes;

    /**
     * Construtor do Controller.
     *
     * Inicializa o Service e a lista observável usada pela TableView.
     * Não acessa banco de dados.
     */
    public ContasReceberController() {
        this.contaReceberService = new ContaReceberService();
        this.contasPendentes = FXCollections.observableArrayList();
    }

    /**
     * Inicialização da tela.
     */
    @FXML
    public void initialize() {
        btnReceberConta.setDisable(true);
        configurarTabela();
        carregarContasPendentes();
    }

    /**
     * Configura as colunas da TableView de contas pendentes.
     *
     * A formatação visual permanece no Controller.
     */
    private void configurarTabela() {

        colConta.setCellValueFactory(new PropertyValueFactory<>("contaReceberId"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("nomeCliente"));
        colVenda.setCellValueFactory(new PropertyValueFactory<>("vendaId"));
        colValor.setCellValueFactory(new PropertyValueFactory<>("valor"));
        colVencimento.setCellValueFactory(new PropertyValueFactory<>("dataVencimento"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colSituacao.setCellValueFactory(cellData -> {
            ContaReceberListagemView contaPendente = cellData.getValue();

            if (contaPendente == null) {
                return new SimpleStringProperty("-");
            }

            String situacao = contaPendente.isVencida()
                    ? "Vencida"
                    : "Em aberto";

            return new SimpleStringProperty(situacao);
        });

        configurarFormatacaoColunaValor();
        configurarFormatacaoColunaVencimento();

        tabelaContasPendentes.setItems(contasPendentes);
    }

    /**
     * Configura a formatação monetária da coluna Valor.
     */
    private void configurarFormatacaoColunaValor() {

        NumberFormat formatoMoeda = NumberFormat.getCurrencyInstance(
                new Locale("pt", "BR")
        );

        colValor.setCellFactory(coluna -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal valor, boolean empty) {
                super.updateItem(valor, empty);

                if (empty || valor == null) {
                    setText(null);
                    return;
                }

                setText(formatoMoeda.format(valor).replace('\u00A0', ' '));
            }
        });
    }

    /**
     * Configura a formatação da coluna Vencimento.
     */
    private void configurarFormatacaoColunaVencimento() {

        DateTimeFormatter formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        colVencimento.setCellFactory(coluna -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate dataVencimento, boolean empty) {
                super.updateItem(dataVencimento, empty);

                if (empty || dataVencimento == null) {
                    setText(null);
                    return;
                }

                setText(dataVencimento.format(formatoData));
            }
        });
    }

    /**
     * Carrega as contas pendentes usando o Service.
     *
     * O Controller não acessa DAO e não abre Connection diretamente.
     */
    private void carregarContasPendentes() {

        try {
            List<ContaReceberListagemView> listaRetornada =
                    contaReceberService.listarContasPendentes();

            if (listaRetornada == null) {
                contasPendentes.clear();
            } else {
                contasPendentes.setAll(listaRetornada);
            }

            atualizarContador();

        } catch (RuntimeException e) {
            contasPendentes.clear();
            atualizarContador();

            System.err.println("[ERRO] Falha ao carregar contas pendentes.");
            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro",
                    "Não foi possível carregar as contas pendentes."
            );
        }
    }

    /**
     * Atualiza o contador de contas pendentes exibidas na tabela.
     */
    private void atualizarContador() {

        int totalContasPendentes = contasPendentes.size();

        if (totalContasPendentes == 1) {
            lblTotalContas.setText("Total: 1 conta pendente");
            return;
        }

        lblTotalContas.setText(
                "Total: " + totalContasPendentes + " contas pendentes"
        );
    }

    /**
     * Ação do botão Atualizar.
     */
    @FXML
    private void onAtualizar() {
        carregarContasPendentes();
    }

    /**
     * Ação temporária do botão Voltar.
     *
     * A navegação real será implementada em passo posterior.
     */
    @FXML
    private void onVoltar() {
    }

    /**
     * Ação temporária do botão Receber Conta.
     *
     * O fluxo de recebimento será implementado em passo posterior.
     */
    @FXML
    private void onReceberConta() {
    }

    /**
     * Exibe alertas padronizados para o usuário.
     */
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