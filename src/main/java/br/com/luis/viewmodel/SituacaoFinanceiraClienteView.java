package br.com.luis.viewmodel;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Representa uma fotografia imutável da situação financeira persistida de um cliente.
 *
 * Os valores são normalizados com escala monetária 2 e arredondamento
 * {@link RoundingMode#HALF_UP}. O saldo devedor não pode ser negativo, enquanto
 * o limite disponível pode ser negativo quando o saldo supera o limite de crédito.
 */
public final class SituacaoFinanceiraClienteView {

    private static final int ESCALA_MONETARIA = 2;
    private static final RoundingMode ARREDONDAMENTO_MONETARIO = RoundingMode.HALF_UP;

    private final BigDecimal saldoDevedor;
    private final BigDecimal limiteDisponivel;

    /**
     * Cria uma fotografia financeira normalizada do cliente.
     *
     * @param saldoDevedor total persistido das contas a receber pendentes.
     * @param limiteDisponivel limite de crédito persistido menos o saldo devedor.
     */
    public SituacaoFinanceiraClienteView(
            BigDecimal saldoDevedor,
            BigDecimal limiteDisponivel
    ) {
        if (saldoDevedor == null) {
            throw new IllegalArgumentException("Saldo devedor é obrigatório.");
        }

        if (limiteDisponivel == null) {
            throw new IllegalArgumentException("Limite disponível é obrigatório.");
        }

        BigDecimal saldoDevedorNormalizado = saldoDevedor.setScale(
                ESCALA_MONETARIA,
                ARREDONDAMENTO_MONETARIO
        );

        if (saldoDevedorNormalizado.signum() < 0) {
            throw new IllegalArgumentException("Saldo devedor não pode ser negativo.");
        }

        this.saldoDevedor = saldoDevedorNormalizado;
        this.limiteDisponivel = limiteDisponivel.setScale(
                ESCALA_MONETARIA,
                ARREDONDAMENTO_MONETARIO
        );
    }

    /**
     * Retorna o total persistido das contas a receber pendentes.
     */
    public BigDecimal getSaldoDevedor() {
        return saldoDevedor;
    }

    /**
     * Retorna o limite de crédito persistido menos o saldo devedor.
     */
    public BigDecimal getLimiteDisponivel() {
        return limiteDisponivel;
    }
}
