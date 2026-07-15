package br.com.luis.model;

/**
 * Enum que representa os tipos possíveis de desconto global
 * aplicados sobre uma venda.
 *
 * O desconto global é diferente da promoção:
 * - Promoção pertence a um produto específico.
 * - Desconto global pertence à venda/carrinho como um todo.
 *
 * A aplicação, validação e distribuição do desconto global são realizadas
 * pela camada VendaService.
 */
public enum TipoDescontoGlobal {

    /**
     * Desconto calculado em percentual sobre a base elegível da venda.
     */
    PERCENTUAL,

    /**
     * Desconto em valor monetário fixo sobre a base elegível da venda.
     */
    VALOR_FIXO
}