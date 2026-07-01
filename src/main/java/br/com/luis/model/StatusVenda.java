package br.com.luis.model;

/**
 * Enum que representa os status oficiais da venda no sistema.
 *
 * Na Fase 5, este enum será usado para padronizar o status da venda
 * após a finalização real.
 *
 * Regras:
 * - PAGA: venda à vista finalizada com pagamento confirmado.
 * - PENDENTE: venda a prazo finalizada com ContaReceber pendente.
 *
 * Status como CANCELADA ou ESTORNADA não entram nesta etapa.
 * Estorno pertence à Fase 6.
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