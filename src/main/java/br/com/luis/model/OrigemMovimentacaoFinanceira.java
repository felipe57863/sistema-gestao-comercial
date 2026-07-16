package br.com.luis.model;

/**
 * Enum que representa as origens oficiais de uma movimentação financeira.
 *
 * Regras atuais:
 * - VENDA_A_VISTA: entrada gerada pela finalização de uma venda à vista.
 * - RECEBIMENTO_CONTA: entrada gerada pelo recebimento integral de uma conta.
 * - ESTORNO_VENDA_A_VISTA: saída gerada pelo estorno de uma venda à vista paga.
 * - ESTORNO_RECEBIMENTO_CONTA: saída gerada pelo estorno de uma venda a prazo já recebida.
 */
public enum OrigemMovimentacaoFinanceira {

    /**
     * Movimentação financeira originada por venda à vista.
     *
     * Deve ser usada para registrar a entrada financeira
     * gerada no momento da finalização da venda à vista.
     */
    VENDA_A_VISTA,

    /**
     * Movimentação financeira originada pelo recebimento integral
     * de uma conta a receber.
     */
    RECEBIMENTO_CONTA,

    /**
     * Movimentação financeira de saída originada pelo estorno total
     * de uma venda à vista paga.
     */
    ESTORNO_VENDA_A_VISTA,

    /**
     * Movimentação financeira de saída originada pelo estorno total
     * de uma venda a prazo cujo recebimento já foi realizado.
     */
    ESTORNO_RECEBIMENTO_CONTA
}