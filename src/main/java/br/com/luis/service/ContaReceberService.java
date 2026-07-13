package br.com.luis.service;

import br.com.luis.dao.ContaReceberDAO;
import br.com.luis.dao.MovimentacaoFinanceiraDAO;
import br.com.luis.model.FormaPagamento;
import br.com.luis.viewmodel.ResultadoRecebimentoConta;

/**
 * Camada de serviço responsável pelas regras de negócio
 * relacionadas ao recebimento de contas a receber.
 *
 * Nesta primeira etapa, o serviço apenas prepara a estrutura
 * inicial para o recebimento integral.
 *
 * O fluxo transacional completo será implementado em passo futuro.
 */
public class ContaReceberService {

    private final ContaReceberDAO contaReceberDAO;
    private final MovimentacaoFinanceiraDAO movimentacaoFinanceiraDAO;

    public ContaReceberService() {
        this.contaReceberDAO = new ContaReceberDAO();
        this.movimentacaoFinanceiraDAO = new MovimentacaoFinanceiraDAO();
    }

    /**
     * Recebe integralmente uma conta a receber.
     *
     * Nesta etapa, o método apenas valida os dados básicos.
     * O fluxo transacional completo será implementado em passo futuro.
     *
     * @param contaReceberId ID da conta a receber.
     * @param formaPagamento forma de pagamento usada no recebimento.
     * @param usuarioId ID do usuário responsável pelo recebimento.
     * @return resultado do recebimento integral da conta.
     */
    public ResultadoRecebimentoConta receberConta(
            Integer contaReceberId,
            FormaPagamento formaPagamento,
            Integer usuarioId
    ) {

        validarDadosBasicosRecebimento(
                contaReceberId,
                formaPagamento,
                usuarioId
        );

        throw new IllegalStateException("Fluxo transacional de recebimento ainda não implementado.");
    }

    /**
     * Valida os dados básicos necessários para iniciar
     * o recebimento integral de uma conta a receber.
     *
     * Esta validação não acessa banco de dados e não abre conexão.
     */
    private void validarDadosBasicosRecebimento(
            Integer contaReceberId,
            FormaPagamento formaPagamento,
            Integer usuarioId
    ) {

        if (contaReceberId == null || contaReceberId <= 0) {
            throw new IllegalArgumentException("Conta a receber inválida para recebimento.");
        }

        if (formaPagamento == null) {
            throw new IllegalArgumentException("Forma de pagamento é obrigatória para recebimento.");
        }

        if (formaPagamento == FormaPagamento.A_PRAZO) {
            throw new IllegalArgumentException("Forma de pagamento A_PRAZO não é permitida para recebimento.");
        }

        if (formaPagamento != FormaPagamento.DINHEIRO
                && formaPagamento != FormaPagamento.PIX
                && formaPagamento != FormaPagamento.CARTAO) {
            throw new IllegalArgumentException("Forma de pagamento inválida para recebimento.");
        }

        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException("Usuário inválido para recebimento.");
        }
    }
}