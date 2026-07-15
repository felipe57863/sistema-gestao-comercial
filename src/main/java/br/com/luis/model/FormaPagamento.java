package br.com.luis.model;

/**
 * Enum que representa as formas oficiais de pagamento do sistema.
 *
 * Padroniza a finalização da venda e o recebimento de contas, evitando o uso de
 * textos soltos como "dinheiro", "cartao", "pix" ou variações similares.
 *
 * Regras:
 * - DINHEIRO, PIX e CARTAO podem registrar vendas à vista e recebimentos integrais
 *   de contas a receber.
 * - Na venda à vista em DINHEIRO, o valor recebido é validado e o troco é calculado.
 * - No recebimento integral, DINHEIRO identifica apenas a forma usada no recebimento.
 * - A_PRAZO é exclusivo da finalização de venda a prazo e não pode ser usado
 *   para receber uma conta.
 */
public enum FormaPagamento {

    /**
     * Pagamento em dinheiro.
     *
     * Na venda à vista, exige validação do valor recebido e cálculo de troco.
     * No recebimento integral de conta, registra somente a forma de recebimento.
     */
    DINHEIRO,

    /**
     * Pagamento via PIX em venda à vista ou recebimento integral de conta.
     */
    PIX,

    /**
     * Pagamento via cartão em venda à vista ou recebimento integral de conta.
     */
    CARTAO,

    /**
     * Pagamento a prazo.
     *
     * Deve ser usado somente em vendas do tipo A_PRAZO e não é aceito no
     * recebimento de contas.
     */
    A_PRAZO
}