package br.com.luis.model;

/**
 * Enum que representa os status oficiais de uma conta a receber.
 *
 * Regras atuais:
 * - PENDENTE: conta ainda não recebida.
 * - PAGA: conta recebida integralmente.
 *
 * O enum atualmente representa apenas os estados persistidos PENDENTE e PAGA.
 * A indicação de vencimento é calculada para apresentação sem alterar o status.
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