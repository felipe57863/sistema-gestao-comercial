package br.com.luis.service;

import br.com.luis.dao.PrazoPagamentoDAO;
import br.com.luis.model.PrazoPagamento;

import java.util.List;

/**
 * Camada de Serviço da entidade PrazoPagamento.
 * Responsável por aplicar regras de negócio antes da persistência.
 */
public class PrazoPagamentoService {

    private final PrazoPagamentoDAO dao;

    public PrazoPagamentoService() {
        this.dao = new PrazoPagamentoDAO();
    }

    /**
     * Valida e cadastra um novo prazo de pagamento.
     */
    public void cadastrar(PrazoPagamento prazo) {

        // Fail-fast: validação básica
        if (prazo == null) {
            throw new IllegalArgumentException("Prazo de pagamento é obrigatório.");
        }

        // Regra de negócio: evitar duplicidade de descrição
        List<PrazoPagamento> existentes = dao.listarTodos();

        for (PrazoPagamento p : existentes) {
            if (p.getDescricao().equalsIgnoreCase(prazo.getDescricao())) {
                throw new RuntimeException("Já existe um prazo com essa descrição.");
            }
        }

        // Regra de negócio: todo novo prazo inicia como ativo
        prazo.setAtivo(true);

        // Persistência
        dao.cadastrar(prazo);
    }

    /**
     * Retorna apenas prazos ativos.
     * Usado em ComboBox e telas operacionais.
     */
    public List<PrazoPagamento> listarAtivos() {
        return dao.listarAtivos();
    }

    /**
     * Retorna todos os prazos (ativos e inativos).
     * Usado em telas administrativas.
     */
    public List<PrazoPagamento> listarTodos() {
        return dao.listarTodos();
    }
}