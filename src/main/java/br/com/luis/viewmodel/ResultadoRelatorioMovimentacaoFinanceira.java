package br.com.luis.viewmodel;

import br.com.luis.model.TipoMovimentacaoFinanceira;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Representa a fotografia completa e imutável de uma consulta concluída
 * do relatório de movimentações financeiras.
 *
 * A classe transporta o filtro efetivamente aplicado, as linhas projetadas e
 * os totais consolidados da mesma consulta. Não acessa banco de dados, DAO,
 * Service, sessão, componentes JavaFX ou mecanismos de formatação visual.
 */
public final class ResultadoRelatorioMovimentacaoFinanceira {

    private static final int ESCALA_MONETARIA = 2;

    private static final RoundingMode ARREDONDAMENTO_MONETARIO =
            RoundingMode.HALF_UP;

    private final FiltroRelatorioMovimentacaoFinanceira filtroAplicado;
    private final List<MovimentacaoFinanceiraRelatorioView> movimentacoes;
    private final int quantidadeMovimentacoes;
    private final BigDecimal totalEntradas;
    private final BigDecimal totalSaidas;
    private final BigDecimal resultadoLiquido;

    /**
     * Cria uma fotografia imutável do relatório de movimentações financeiras.
     *
     * A quantidade de movimentações é determinada pelo tamanho da lista recebida.
     * O resultado líquido é calculado pela diferença entre entradas e saídas.
     * As somas das linhas são verificadas contra os totais informados.
     *
     * @param filtroAplicado filtro efetivamente usado na consulta.
     * @param movimentacoes linhas projetadas da consulta.
     * @param totalEntradas total consolidado das movimentações de entrada.
     * @param totalSaidas total consolidado das movimentações de saída.
     * @throws IllegalArgumentException quando algum argumento obrigatório for
     *                                  inválido ou quando linhas e totais forem
     *                                  incoerentes.
     */
    public ResultadoRelatorioMovimentacaoFinanceira(
            FiltroRelatorioMovimentacaoFinanceira filtroAplicado,
            List<MovimentacaoFinanceiraRelatorioView> movimentacoes,
            BigDecimal totalEntradas,
            BigDecimal totalSaidas
    ) {
        if (filtroAplicado == null) {
            throw new IllegalArgumentException(
                    "Filtro aplicado ao relatório financeiro é obrigatório."
            );
        }

        filtroAplicado.validar();

        if (movimentacoes == null) {
            throw new IllegalArgumentException(
                    "Lista de movimentações financeiras é obrigatória."
            );
        }

        for (MovimentacaoFinanceiraRelatorioView movimentacao
                : movimentacoes) {

            if (movimentacao == null) {
                throw new IllegalArgumentException(
                        "Lista de movimentações financeiras não pode conter elemento nulo."
                );
            }
        }

        if (totalEntradas == null) {
            throw new IllegalArgumentException(
                    "Total de entradas é obrigatório."
            );
        }

        if (totalSaidas == null) {
            throw new IllegalArgumentException(
                    "Total de saídas é obrigatório."
            );
        }

        BigDecimal totalEntradasNormalizado =
                normalizarValorMonetario(totalEntradas);

        BigDecimal totalSaidasNormalizado =
                normalizarValorMonetario(totalSaidas);

        if (totalEntradasNormalizado.signum() < 0) {
            throw new IllegalArgumentException(
                    "Total de entradas não pode ser negativo."
            );
        }

        if (totalSaidasNormalizado.signum() < 0) {
            throw new IllegalArgumentException(
                    "Total de saídas não pode ser negativo."
            );
        }

        BigDecimal somaEntradas =
                BigDecimal.ZERO.setScale(
                        ESCALA_MONETARIA,
                        ARREDONDAMENTO_MONETARIO
                );

        BigDecimal somaSaidas =
                BigDecimal.ZERO.setScale(
                        ESCALA_MONETARIA,
                        ARREDONDAMENTO_MONETARIO
                );

        for (MovimentacaoFinanceiraRelatorioView movimentacao
                : movimentacoes) {

            if (movimentacao.getTipo()
                    == TipoMovimentacaoFinanceira.ENTRADA) {

                somaEntradas =
                        somaEntradas.add(
                                movimentacao.getValor()
                        );

            } else if (movimentacao.getTipo()
                    == TipoMovimentacaoFinanceira.SAIDA) {

                somaSaidas =
                        somaSaidas.add(
                                movimentacao.getValor()
                        );
            }
        }

        somaEntradas =
                normalizarValorMonetario(somaEntradas);

        somaSaidas =
                normalizarValorMonetario(somaSaidas);

        if (somaEntradas.compareTo(totalEntradasNormalizado) != 0) {
            throw new IllegalArgumentException(
                    "Total de entradas não corresponde à soma das linhas do relatório."
            );
        }

        if (somaSaidas.compareTo(totalSaidasNormalizado) != 0) {
            throw new IllegalArgumentException(
                    "Total de saídas não corresponde à soma das linhas do relatório."
            );
        }

        List<MovimentacaoFinanceiraRelatorioView> movimentacoesImutaveis =
                List.copyOf(movimentacoes);

        BigDecimal resultadoLiquidoCalculado =
                normalizarValorMonetario(
                        totalEntradasNormalizado.subtract(
                                totalSaidasNormalizado
                        )
                );

        this.filtroAplicado = filtroAplicado;
        this.movimentacoes = movimentacoesImutaveis;
        this.quantidadeMovimentacoes =
                movimentacoesImutaveis.size();
        this.totalEntradas = totalEntradasNormalizado;
        this.totalSaidas = totalSaidasNormalizado;
        this.resultadoLiquido = resultadoLiquidoCalculado;
    }

    private static BigDecimal normalizarValorMonetario(
            BigDecimal valor
    ) {
        return valor.setScale(
                ESCALA_MONETARIA,
                ARREDONDAMENTO_MONETARIO
        );
    }

    public FiltroRelatorioMovimentacaoFinanceira getFiltroAplicado() {
        return filtroAplicado;
    }

    public List<MovimentacaoFinanceiraRelatorioView> getMovimentacoes() {
        return movimentacoes;
    }

    public int getQuantidadeMovimentacoes() {
        return quantidadeMovimentacoes;
    }

    public BigDecimal getTotalEntradas() {
        return totalEntradas;
    }

    public BigDecimal getTotalSaidas() {
        return totalSaidas;
    }

    public BigDecimal getResultadoLiquido() {
        return resultadoLiquido;
    }
}
