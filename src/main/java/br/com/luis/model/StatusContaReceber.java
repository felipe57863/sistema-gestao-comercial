package br.com.luis.model;

/**
 * Enum que representa os status oficiais de uma conta a receber.
 *
 * Na primeira entrega da Fase 5, a ContaReceber será criada apenas
 * quando uma venda a prazo for finalizada.
 *
 * Regra inicial:
 * - PENDENTE: conta ainda não recebida.
 *
 * Status como PAGA, CANCELADA, VENCIDA ou ESTORNADA não entram nesta etapa.
 * Recebimento completo de conta será tratado futuramente na UC09.
 * Estorno pertence à Fase 6.
 */
public enum StatusContaReceber {

    /**
     * Conta a receber pendente de pagamento.
     *
     * Deve ser o status inicial de toda ContaReceber criada
     * a partir de uma venda a prazo.
     */
    PENDENTE
}