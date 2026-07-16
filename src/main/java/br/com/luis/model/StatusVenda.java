package br.com.luis.model;

/**
 * Enum que representa os status oficiais da venda no sistema.
 *
 * Padroniza o status persistido após a finalização da venda.
 *
 * Regras:
 * - PAGA: venda à vista finalizada com pagamento confirmado.
 * - PENDENTE: venda a prazo finalizada com ContaReceber pendente.
 *
 * O enum atualmente representa apenas os estados gerados pelos fluxos de
 * finalização à vista e a prazo.
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