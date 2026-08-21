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
     * Valida e cadastra um prazo novo, impedindo descrição duplicada e
     * definindo-o como ativo.
     */
    public void cadastrar(PrazoPagamento prazo) {

        // Fail-fast: validação dos dados administrativos obrigatórios
        validarDadosObrigatorios(prazo);

        // Regra de negócio: evitar duplicidade de descrição
        validarDescricaoDuplicada(prazo.getDescricao(), null);

        // Regra de negócio: todo novo prazo inicia como ativo
        prazo.setAtivo(true);

        dao.cadastrar(prazo);
    }

    /**
     * Valida e atualiza um prazo existente, exigindo ID válido e descrição
     * única entre os demais registros.
     */
    public void atualizar(PrazoPagamento prazo) {

        // Fail-fast: validação dos dados administrativos obrigatórios
        validarDadosObrigatorios(prazo);

        if (prazo.getIdPrazo() == null || prazo.getIdPrazo() <= 0) {
            throw new IllegalArgumentException("ID do prazo de pagamento deve ser válido para atualização.");
        }

        // Regra de negócio: evitar duplicidade de descrição em outro registro
        validarDescricaoDuplicada(prazo.getDescricao(), prazo.getIdPrazo());

        dao.atualizar(prazo);
    }

    /**
     * Inativa o prazo sem exclusão física, preservando o histórico cadastral.
     */
    public void inativar(Integer idPrazo) {

        // Fail-fast: validação básica
        if (idPrazo == null || idPrazo <= 0) {
            throw new IllegalArgumentException("ID do prazo de pagamento é obrigatório para inativação.");
        }

        dao.inativar(idPrazo);
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

    /**
     * Valida os dados administrativos obrigatórios de um prazo de pagamento.
     */
    private void validarDadosObrigatorios(PrazoPagamento prazo) {

        if (prazo == null) {
            throw new IllegalArgumentException("Prazo de pagamento é obrigatório.");
        }

        if (prazo.getDescricao() == null || prazo.getDescricao().isBlank()) {
            throw new IllegalArgumentException("Descrição do prazo é obrigatória.");
        }

        if (prazo.getQuantidadeDias() == null) {
            throw new IllegalArgumentException("Quantidade de dias é obrigatória.");
        }

        if (prazo.getQuantidadeDias() <= 0) {
            throw new IllegalArgumentException("Quantidade de dias deve ser maior que zero.");
        }
    }

    /**
     * Impede duplicidade de descrição entre os prazos cadastrados.
     */
    private void validarDescricaoDuplicada(String descricao, Integer idAtual) {

        List<PrazoPagamento> existentes = dao.listarTodos();

        for (PrazoPagamento p : existentes) {

            boolean mesmaDescricao = p.getDescricao().equalsIgnoreCase(descricao);
            boolean mesmoRegistro = idAtual != null && p.getIdPrazo().equals(idAtual);

            if (mesmaDescricao && !mesmoRegistro) {
                throw new RuntimeException("Já existe um prazo com essa descrição.");
            }
        }
    }
    /**
     * Garante a disponibilidade dos prazos padrão usados pelo sistema.
     */
    public void inicializarPrazosPadrao() {

        int[] prazosPadrao = {15, 30, 45, 60, 90};

        for (int dias : prazosPadrao) {

            if (!existePrazoComQuantidadeDias(dias)) {

                PrazoPagamento prazo = new PrazoPagamento(
                        null,
                        dias + " Dias",
                        dias,
                        true
                );

                dao.cadastrar(prazo);

                System.out.println("[LOG] Prazo padrão criado: " + prazo.getDescricao());
            }
        }
    }

    /**
     * Verifica se a quantidade de dias de um prazo padrão já está cadastrada.
     */
    private boolean existePrazoComQuantidadeDias(int quantidadeDias) {

        List<PrazoPagamento> prazos = dao.listarTodos();

        for (PrazoPagamento prazo : prazos) {
            if (prazo.getQuantidadeDias() != null
                    && prazo.getQuantidadeDias() == quantidadeDias) {
                return true;
            }
        }

        return false;
    }
}
