package br.com.luis.model;

/**
 * Enum que representa as origens oficiais de uma movimentação financeira.
 *
 * Na primeira entrega da Fase 5, a movimentação financeira será criada
 * apenas para vendas à vista.
 *
 * Regra inicial:
 * - VENDA_A_VISTA: movimentação gerada pela finalização de uma venda à vista.
 *
 * Origens como RECEBIMENTO_CONTA, ESTORNO ou AJUSTE_MANUAL não entram nesta etapa.
 * Recebimento completo de conta será tratado futuramente na UC09.
 * Estorno pertence à Fase 6.
 */
public enum OrigemMovimentacaoFinanceira {

    /**
     * Movimentação financeira originada por venda à vista.
     *
     * Deve ser usada inicialmente para registrar a entrada financeira
     * gerada no momento da finalização da venda à vista.
     */
    VENDA_A_VISTA
}