package br.com.luis.model;

/**
 * Enum que representa as formas oficiais de pagamento do sistema.
 *
 * Na Fase 5, este enum será usado para padronizar a finalização da venda,
 * evitando uso de textos soltos como "dinheiro", "cartao", "pix" ou variações similares.
 *
 * Regras:
 * - DINHEIRO: venda à vista com validação de valor recebido e cálculo de troco.
 * - PIX: venda à vista sem cálculo de troco.
 * - CARTAO: venda à vista sem cálculo de troco.
 * - A_PRAZO: venda a prazo, gerando ContaReceber.
 */
public enum FormaPagamento {

    /**
     * Pagamento em dinheiro.
     *
     * Exige validação de valor recebido e cálculo de troco.
     */
    DINHEIRO,

    /**
     * Pagamento via PIX.
     */
    PIX,

    /**
     * Pagamento via cartão.
     */
    CARTAO,

    /**
     * Pagamento a prazo.
     *
     * Deve ser usado somente em vendas do tipo A_PRAZO.
     */
    A_PRAZO
}