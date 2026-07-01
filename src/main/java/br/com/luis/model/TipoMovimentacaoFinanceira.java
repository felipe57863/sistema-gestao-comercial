package br.com.luis.model;

/**
 * Enum que representa os tipos oficiais de movimentação financeira.
 *
 * Na primeira entrega da Fase 5, a movimentação financeira será criada
 * apenas para vendas à vista.
 *
 * Regra inicial:
 * - ENTRADA: valor financeiro que entra no caixa/sistema.
 *
 * Tipos como SAIDA, ESTORNO ou AJUSTE não entram nesta etapa.
 * Estorno pertence à Fase 6.
 */
public enum TipoMovimentacaoFinanceira {

    /**
     * Entrada financeira.
     *
     * Deve ser usada inicialmente para registrar o valor de uma venda à vista.
     */
    ENTRADA
}