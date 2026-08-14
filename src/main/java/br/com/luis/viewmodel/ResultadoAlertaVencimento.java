package br.com.luis.viewmodel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Fotografia imutável de uma consulta dos alertas automáticos de vencimento.
 *
 * Contém a configuração utilizada, a data única de referência, a janela
 * calculada, as contas elegíveis e os respectivos totalizadores.
 */
public final class ResultadoAlertaVencimento {

    private static final int ESCALA_MONETARIA = 2;

    private static final RoundingMode ARREDONDAMENTO_MONETARIO =
            RoundingMode.HALF_UP;

    private final int diasAntecedencia;
    private final LocalDate dataReferencia;
    private final LocalDate limiteInclusivo;
    private final List<ContaAlertaVencimentoView> contas;
    private final int quantidadeVencidas;
    private final BigDecimal valorTotalVencido;
    private final int quantidadeProximas;
    private final BigDecimal valorTotalProximo;

    public ResultadoAlertaVencimento(
            int diasAntecedencia,
            LocalDate dataReferencia,
            LocalDate limiteInclusivo,
            List<ContaAlertaVencimentoView> contas,
            int quantidadeVencidas,
            BigDecimal valorTotalVencido,
            int quantidadeProximas,
            BigDecimal valorTotalProximo
    ) {

        if (diasAntecedencia < 0 || diasAntecedencia > 365) {
            throw new IllegalArgumentException(
                    "Dias de antecedência devem estar entre 0 e 365."
            );
        }

        if (dataReferencia == null) {
            throw new IllegalArgumentException(
                    "Data de referência é obrigatória."
            );
        }

        if (limiteInclusivo == null) {
            throw new IllegalArgumentException(
                    "Limite da janela de alerta é obrigatório."
            );
        }

        LocalDate limiteEsperado = dataReferencia.plusDays(diasAntecedencia);

        if (!limiteEsperado.equals(limiteInclusivo)) {
            throw new IllegalArgumentException(
                    "Limite da janela não corresponde aos dias de antecedência."
            );
        }

        if (contas == null) {
            throw new IllegalArgumentException(
                    "Lista de contas do alerta é obrigatória."
            );
        }

        if (valorTotalVencido == null || valorTotalProximo == null) {
            throw new IllegalArgumentException(
                    "Totalizadores monetários são obrigatórios."
            );
        }

        List<ContaAlertaVencimentoView> contasImutaveis =
                List.copyOf(contas);

        BigDecimal somaVencida = criarZeroMonetario();
        BigDecimal somaProxima = criarZeroMonetario();

        int vencidasCalculadas = 0;
        int proximasCalculadas = 0;

        for (ContaAlertaVencimentoView conta : contasImutaveis) {

            if (conta == null) {
                throw new IllegalArgumentException(
                        "Lista de alertas não pode conter elemento nulo."
                );
            }

            SituacaoAlertaVencimento situacaoEsperada;

            if (conta.getDataVencimento().isBefore(dataReferencia)) {
                situacaoEsperada = SituacaoAlertaVencimento.VENCIDA;

            } else if (!conta.getDataVencimento().isAfter(limiteInclusivo)) {
                situacaoEsperada =
                        SituacaoAlertaVencimento.PROXIMA_DO_VENCIMENTO;

            } else {
                throw new IllegalArgumentException(
                        "Conta fora da janela foi incluída no resultado."
                );
            }

            if (conta.getSituacao() != situacaoEsperada) {
                throw new IllegalArgumentException(
                        "Situação da conta não corresponde à data de vencimento."
                );
            }

            if (situacaoEsperada == SituacaoAlertaVencimento.VENCIDA) {
                vencidasCalculadas++;
                somaVencida = somaVencida.add(conta.getValor());

            } else {
                proximasCalculadas++;
                somaProxima = somaProxima.add(conta.getValor());
            }
        }

        somaVencida = normalizarValorMonetario(somaVencida);
        somaProxima = normalizarValorMonetario(somaProxima);

        BigDecimal totalVencidoNormalizado =
                normalizarValorMonetario(valorTotalVencido);

        BigDecimal totalProximoNormalizado =
                normalizarValorMonetario(valorTotalProximo);

        if (quantidadeVencidas != vencidasCalculadas) {
            throw new IllegalArgumentException(
                    "Quantidade de contas vencidas não corresponde à lista."
            );
        }

        if (quantidadeProximas != proximasCalculadas) {
            throw new IllegalArgumentException(
                    "Quantidade de contas próximas não corresponde à lista."
            );
        }

        if (somaVencida.compareTo(totalVencidoNormalizado) != 0) {
            throw new IllegalArgumentException(
                    "Valor total vencido não corresponde à lista."
            );
        }

        if (somaProxima.compareTo(totalProximoNormalizado) != 0) {
            throw new IllegalArgumentException(
                    "Valor total próximo não corresponde à lista."
            );
        }

        this.diasAntecedencia = diasAntecedencia;
        this.dataReferencia = dataReferencia;
        this.limiteInclusivo = limiteInclusivo;
        this.contas = contasImutaveis;
        this.quantidadeVencidas = quantidadeVencidas;
        this.valorTotalVencido = totalVencidoNormalizado;
        this.quantidadeProximas = quantidadeProximas;
        this.valorTotalProximo = totalProximoNormalizado;
    }

    private static BigDecimal criarZeroMonetario() {
        return BigDecimal.ZERO.setScale(
                ESCALA_MONETARIA,
                ARREDONDAMENTO_MONETARIO
        );
    }

    private static BigDecimal normalizarValorMonetario(BigDecimal valor) {

        BigDecimal valorNormalizado = valor.setScale(
                ESCALA_MONETARIA,
                ARREDONDAMENTO_MONETARIO
        );

        if (valorNormalizado.signum() < 0) {
            throw new IllegalArgumentException(
                    "Totalizador monetário não pode ser negativo."
            );
        }

        return valorNormalizado;
    }

    public int getDiasAntecedencia() {
        return diasAntecedencia;
    }

    public LocalDate getDataReferencia() {
        return dataReferencia;
    }

    public LocalDate getLimiteInclusivo() {
        return limiteInclusivo;
    }

    public List<ContaAlertaVencimentoView> getContas() {
        return contas;
    }

    public int getQuantidadeVencidas() {
        return quantidadeVencidas;
    }

    public BigDecimal getValorTotalVencido() {
        return valorTotalVencido;
    }

    public int getQuantidadeProximas() {
        return quantidadeProximas;
    }

    public BigDecimal getValorTotalProximo() {
        return valorTotalProximo;
    }
}