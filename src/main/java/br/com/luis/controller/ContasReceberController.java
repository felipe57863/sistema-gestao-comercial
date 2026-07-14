package br.com.luis.controller;

import br.com.luis.model.StatusContaReceber;
import br.com.luis.viewmodel.ContaReceberListagemView;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Controller da tela de Contas a Receber.
 *
 * Nesta etapa inicial, o Controller existe apenas para tornar
 * o FXML válido e preparar a estrutura visual da tela.
 *
 * A listagem real, configuração da tabela, navegação e recebimento
 * serão implementados em passos posteriores.
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

    /**
     * Inicialização da tela.
     *
     * Nesta etapa, apenas garante que o botão de recebimento
     * comece desabilitado.
     */
    @FXML
    public void initialize() {
        btnReceberConta.setDisable(true);
    }

    /**
     * Ação temporária do botão Atualizar.
     *
     * A listagem real será implementada em passo posterior.
     */
    @FXML
    private void onAtualizar() {
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
}