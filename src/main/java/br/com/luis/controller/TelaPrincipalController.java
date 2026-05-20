package br.com.luis.controller;

import br.com.luis.model.Usuario;
import br.com.luis.util.SessaoUsuario;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Controller da tela principal temporária.
 *
 * Esta tela existe apenas para permitir a navegação e os testes
 * enquanto o dashboard oficial ainda não foi implementado.
 *
 * Não contém regras de negócio.
 * Não implementa venda, financeiro, relatórios ou dashboard real.
 */
public class TelaPrincipalController {

    @FXML private Label lblUsuario;

    /**
     * Inicialização da tela.
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
     * Abre a tela de cadastro de clientes.
     */
    @FXML
    public void abrirClientes() {
        abrirTela("/br/com/luis/view/Cliente.fxml", "Cadastro de Clientes");
    }

    /**
     * Abre a tela de cadastro de produtos.
     */
    @FXML
    public void abrirProdutos() {
        abrirTela("/br/com/luis/view/Produto.fxml", "Cadastro de Produtos");
    }

    /**
     * Abre a tela de registro de venda.
     *
     * Esta navegação é temporária para testes da Fase 4.
     * A tela de venda representa apenas o carrinho em memória.
     *
     * Não finaliza venda, não baixa estoque e não executa financeiro.
     */
    @FXML
    public void abrirVendas() {
        abrirTela("/br/com/luis/view/RegistroVenda.fxml", "Registro de Venda");
    }

    /**
     * Encerra a aplicação.
     */
    @FXML
    public void sair() {
        Stage stage = (Stage) lblUsuario.getScene().getWindow();
        stage.close();
    }

    /**
     * Carrega uma tela FXML e substitui a cena atual.
     */
    private void abrirTela(String caminhoFxml, String titulo) {
        try {
            URL fxmlLocation = getClass().getResource(caminhoFxml);

            if (fxmlLocation == null) {
                throw new IllegalStateException("FXML não encontrado: " + caminhoFxml);
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            Stage stage = (Stage) lblUsuario.getScene().getWindow();
            stage.setTitle("ERP Comercial - " + titulo);
            stage.setScene(new Scene(root));
            stage.setMaximized(true);

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