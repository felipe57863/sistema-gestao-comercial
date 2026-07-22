package br.com.luis.controller;

import br.com.luis.util.CabecalhoUtil;
import br.com.luis.util.NavegacaoUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller da tela temporária de navegação apresentada após o login.
 *
 * Centraliza provisoriamente o acesso aos módulos funcionais de Clientes,
 * Produtos, Registro de Venda, Contas a Receber e Histórico de Vendas
 * enquanto o dashboard gerencial definitivo não está implementado.
 *
 * Exibe o usuário mantido na sessão e, em cada navegação, reutiliza o Stage
 * atual, substitui sua Scene, atualiza o título e mantém a janela maximizada.
 *
 * Não contém regras de negócio e delega as funcionalidades aos Controllers
 * e Services de cada módulo. O caráter temporário pertence somente a esta
 * tela de acesso e não indica que os módulos abertos sejam protótipos
 * ou simulações.
 */
public class TelaPrincipalController {

    @FXML private Label lblUsuario;
    @FXML private Label lblDataHora;

    /**
     * Inicializa a identificação visual do usuário e o relógio do cabeçalho.
     */
    @FXML
    public void initialize() {
        CabecalhoUtil.configurarUsuarioEDataHora(
                lblUsuario,
                lblDataHora
        );
    }

    /**
     * Abre a tela funcional de cadastro de clientes no mesmo Stage.
     */
    @FXML
    public void abrirClientes() {
        abrirTela(
                "/br/com/luis/view/Cliente.fxml",
                "Cadastro de Clientes"
        );
    }

    /**
     * Abre a tela funcional de cadastro de produtos no mesmo Stage.
     */
    @FXML
    public void abrirProdutos() {
        abrirTela(
                "/br/com/luis/view/Produto.fxml",
                "Cadastro de Produtos"
        );
    }

    /**
     * Abre a tela de registro de venda.
     *
     * A tela de destino mantém o carrinho e realiza os fluxos de venda
     * à vista e a prazo, delegando suas regras de negócio e persistência
     * aos Services.
     */
    @FXML
    public void abrirVendas() {
        abrirTela(
                "/br/com/luis/view/RegistroVenda.fxml",
                "Registro de Venda"
        );
    }

    /**
     * Abre a tela funcional de Contas a Receber no mesmo Stage.
     */
    @FXML
    public void abrirContasReceber() {
        abrirTela(
                "/br/com/luis/view/ContasReceber.fxml",
                "Contas a Receber"
        );
    }

    /**
     * Abre a tela funcional de Histórico de Vendas no mesmo Stage.
     */
    @FXML
    public void abrirHistoricoVendas() {
        abrirTela(
                "/br/com/luis/view/HistoricoVendas.fxml",
                "Histórico de Vendas"
        );
    }

    /**
     * Encerra a aplicação fechando o Stage atual.
     */
    @FXML
    public void sair() {
        Stage stage =
                (Stage) lblUsuario
                        .getScene()
                        .getWindow();

        stage.close();
    }

    /**
     * Carrega uma tela FXML e substitui a Scene do Stage atual.
     *
     * Atualiza o título, mantém a janela maximizada e apresenta um Alert
     * quando o recurso não pode ser localizado ou carregado.
     *
     * Este método cuida apenas da navegação e não executa regras de negócio
     * dos módulos abertos.
     */
    private void abrirTela(
            String caminhoFxml,
            String titulo
    ) {

        try {
            NavegacaoUtil.abrirTela(
                    lblUsuario,
                    caminhoFxml,
                    titulo
            );

        } catch (IOException | RuntimeException e) {
            System.err.println(
                    "[ERRO] Falha ao abrir tela: "
                            + caminhoFxml
            );

            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro",
                    "Não foi possível abrir a tela solicitada."
            );
        }
    }

    /**
     * Exibe alertas padronizados.
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