package br.com.luis.model;

/**
 * Enum que representa os tipos oficiais de movimentação financeira.
 *
 * Regras atuais:
 * - ENTRADA: valor recebido pelo sistema.
 * - SAIDA: valor devolvido ou revertido pelo sistema.
 */
public enum TipoMovimentacaoFinanceira {

    /**
     * Entrada financeira.
     *
     * Usada para registrar valores de vendas à vista
     * e recebimentos integrais de contas.
     */
    ENTRADA,

    /**
     * Saída financeira.
     *
     * Usada para registrar a reversão financeira decorrente
     * do estorno total de uma venda.
     */
    SAIDA
}