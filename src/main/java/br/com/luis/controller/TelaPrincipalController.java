package br.com.luis.controller;

import br.com.luis.model.Usuario;
import br.com.luis.util.NavegacaoUtil;
import br.com.luis.util.SessaoUsuario;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller da tela temporária de navegação apresentada após o login.
 *
 * Centraliza provisoriamente o acesso aos módulos funcionais de Clientes,
 * Produtos, Registro de Venda e Contas a Receber enquanto o dashboard gerencial
 * definitivo não está implementado. Exibe o usuário mantido na SessaoUsuario e,
 * em cada navegação, reutiliza o Stage atual, substitui sua Scene, atualiza o
 * título e mantém a janela maximizada.
 *
 * Não contém regras de negócio e delega as funcionalidades aos Controllers e
 * Services de cada módulo. O caráter temporário pertence somente a esta tela de
 * acesso e não indica que os módulos abertos sejam protótipos ou simulações.
 */
public class TelaPrincipalController {

    @FXML private Label lblUsuario;

    /**
     * Inicializa a identificação visual com o usuário armazenado na SessaoUsuario.
     * Se não houver usuário na sessão, exibe a identificação alternativa prevista
     * pela própria tela.
     */
    @FXML
    public void initialize() {
        Usuario usuarioLogado = SessaoUsuario.getInstance().getUsuarioLogado();

        if (usuarioLogado != null) {
            lblUsuario.setText("Usuário: " + usuarioLogado.getNome());
        } else {
            lblUsuario.setText("Usuário não identificado");
        }
    }

    /**
     * Abre a tela funcional de cadastro de clientes no mesmo Stage.
     */
    @FXML
    public void abrirClientes() {
        abrirTela("/br/com/luis/view/Cliente.fxml", "Cadastro de Clientes");
    }

    /**
     * Abre a tela funcional de cadastro de produtos no mesmo Stage.
     */
    @FXML
    public void abrirProdutos() {
        abrirTela("/br/com/luis/view/Produto.fxml", "Cadastro de Produtos");
    }

    /**
     * Abre a tela de registro de venda.
     *
     * A tela de destino mantém o carrinho e realiza os fluxos de venda à vista
     * e a prazo, delegando suas regras de negócio e persistência aos Services.
     */
    @FXML
    public void abrirVendas() {
        abrirTela("/br/com/luis/view/RegistroVenda.fxml", "Registro de Venda");
    }

    /**
     * Abre a tela funcional de Contas a Receber no mesmo Stage.
     */
    @FXML
    public void abrirContasReceber() {
        abrirTela("/br/com/luis/view/ContasReceber.fxml", "Contas a Receber");
    }

    /**
     * Encerra a aplicação fechando o Stage atual.
     */
    @FXML
    public void sair() {
        Stage stage = (Stage) lblUsuario.getScene().getWindow();
        stage.close();
    }

    /**
     * Carrega uma tela FXML e substitui a Scene do Stage atual.
     *
     * Atualiza o título, mantém a janela maximizada e apresenta um Alert quando
     * o recurso não pode ser localizado ou carregado. Este método cuida apenas
     * da navegação e não executa regras de negócio dos módulos abertos.
     */
    private void abrirTela(String caminhoFxml, String titulo) {
        try {
            NavegacaoUtil.abrirTela(lblUsuario, caminhoFxml, titulo);

        } catch (IOException | RuntimeException e) {
            System.err.println("[ERRO] Falha ao abrir tela: " + caminhoFxml);
            e.printStackTrace();

            mostrarAlerta(Alert.AlertType.ERROR,
                    "Erro",
                    "Não foi possível abrir a tela solicitada.");
        }
    }

    /**
     * Exibe alertas padronizados.
     */
    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensagem);
        alerta.showAndWait();
    }
}