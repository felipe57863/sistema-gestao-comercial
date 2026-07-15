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
     * Venda paga no momento da finalização.
     *
     * Deve ser usada em venda do tipo A_VISTA.
     */
    PAGA,

    /**
     * Venda pendente de recebimento.
     *
     * Deve ser usada em venda do tipo A_PRAZO.
     */
    PENDENTE
}