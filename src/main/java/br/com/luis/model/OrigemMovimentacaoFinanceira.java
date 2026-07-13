package br.com.luis.model;

/**
 * Enum que representa as origens oficiais de uma movimentação financeira.
 *
 * Regras atuais:
 * - VENDA_A_VISTA: movimentação gerada pela finalização de uma venda à vista.
 * - RECEBIMENTO_CONTA: movimentação gerada pelo recebimento integral de uma conta a receber.
 *
 * Origens como ESTORNO ou AJUSTE_MANUAL não entram nesta etapa.
 * Estorno pertence à Fase 6.
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
    RECEBIMENTO_CONTA
}