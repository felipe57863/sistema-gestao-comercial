package br.com.luis.model;

/**
 * Enum que representa os status oficiais persistidos da venda no sistema.
 *
 * A finalização gera PAGA para vendas à vista ou PENDENTE para vendas a prazo.
 * O recebimento integral da conta vinculada altera a venda pendente para PAGA,
 * enquanto o estorno total de uma venda elegível altera seu estado para
 * ESTORNADA. As regras que determinam essas transições pertencem aos Services;
 * o enum apenas padroniza os estados representados pelo sistema.
 */
public enum StatusVenda {
    /**
     * Venda paga no momento da finalização ou após o recebimento integral.
     */
    PAGA,
    /**
     * Venda a prazo com pagamento integral ainda pendente.
     */
    PENDENTE,
    /**
     * Venda finalizada cujos efeitos comerciais, financeiros e de estoque
     * foram revertidos pelo processo de estorno.
     *
     * É um estado terminal.
     */
    ESTORNADA
}