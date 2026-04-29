package br.com.luis.controller;

import br.com.luis.model.PrazoPagamento;
import br.com.luis.service.PrazoPagamentoService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

/**
 * Controller da Tela de Cadastro de Prazos de Pagamento.
 * Responsável apenas por interação com a interface.
 */
public class PrazoPagamentoController {

    @FXML private TextField txtDescricao;
    @FXML private TextField txtDias;

    private PrazoPagamentoService service;

    /**
     * Inicialização automática do JavaFX.
     */
    @FXML
    public void initialize() {
        this.service = new PrazoPagamentoService();

        // UX: foco inicial
        txtDescricao.requestFocus();
    }

    /**
     * Ação do botão "Salvar".
     */
    @FXML
    public void salvar(ActionEvent event) {

        try {
            // Evita múltiplos cliques (problema comum em UI)
            ((javafx.scene.control.Button) event.getSource()).setDisable(true);

            String descricao = txtDescricao.getText();

            // Validação básica antes de conversão
            if (descricao == null || descricao.isBlank()) {
                throw new IllegalArgumentException("Descrição é obrigatória.");
            }

            String diasTexto = txtDias.getText();

            if (diasTexto == null || diasTexto.isBlank()) {
                throw new IllegalArgumentException("Quantidade de dias é obrigatória.");
            }

            // Converte para número
            int dias = Integer.parseInt(diasTexto);

            // Cria entidade (ativo será garantido no Service)
            PrazoPagamento novoPrazo = new PrazoPagamento(null, descricao.trim(), dias, true);

            // Delega regra de negócio
            service.cadastrar(novoPrazo);

            // Log de ação
            System.out.println("[LOG] Prazo cadastrado via UI: " + novoPrazo.getDescricao());

            // Feedback ao usuário
            mostrarAlerta(Alert.AlertType.INFORMATION,
                    "Sucesso",
                    "Prazo de pagamento cadastrado com sucesso!");

            // Limpa formulário
            txtDescricao.clear();
            txtDias.clear();
            txtDescricao.requestFocus();

        } catch (NumberFormatException e) {

            // Erro de conversão numérica
            mostrarAlerta(Alert.AlertType.WARNING,
                    "Aviso",
                    "A quantidade de dias deve ser um número válido.");

            txtDias.requestFocus();

        } catch (RuntimeException e) {

            // Erros vindos do Model ou Service
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Erro de Validação",
                    e.getMessage());

        } finally {
            // Reativa botão
            ((javafx.scene.control.Button) event.getSource()).setDisable(false);
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