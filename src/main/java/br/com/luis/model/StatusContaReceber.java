package br.com.luis.model;

/**
 * Enum que representa os status oficiais de uma conta a receber.
 *
 * Regras atuais:
 * - PENDENTE: conta ainda não recebida.
 * - PAGA: conta recebida integralmente.
 *
 * Status como CANCELADA, VENCIDA, PARCIAL ou ESTORNADA
 * não entram nesta etapa.
 */
public enum StatusContaReceber {

    /**
     * Conta a receber pendente de pagamento.
     *
     * Deve ser o status inicial de toda ContaReceber criada
     * a partir de uma venda a prazo.
     */
    PENDENTE,

    /**
     * Conta a receber paga integralmente.
     *
     * Deve ser usado quando o recebimento integral da conta
     * for concluído com sucesso.
     */
    PAGA
}