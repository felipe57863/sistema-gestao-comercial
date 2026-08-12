package br.com.luis.viewmodel;

/**
 * Representa a situação calculada de uma conta a receber no relatório gerencial.
 *
 * Diferentemente de {@code StatusContaReceber}, este tipo não representa um
 * estado persistido no banco. A situação é determinada pelo Service a partir
 * do status atual da conta, da data de vencimento e da data de referência da
 * consulta.
 *
 * A ausência de filtro visual, apresentada como "Todas", é representada por
 * valor nulo no filtro do relatório e por isso não integra este enum.
 */
public enum SituacaoRelatorioContaReceber {

    /**
     * Conta pendente cujo vencimento é igual ou posterior à data de referência.
     */
    A_VENCER,

    /**
     * Conta pendente cujo vencimento é anterior à data de referência.
     */
    VENCIDA,

    /**
     * Conta cujo status persistido atual é PAGA.
     */
    PAGA,

    /**
     * Conta cujo status persistido atual é CANCELADA.
     */
    CANCELADA
}
