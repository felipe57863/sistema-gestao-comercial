package br.com.luis.viewmodel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Representa a fotografia completa e imutável de uma consulta concluída do
 * relatório de contas a receber.
 *
 * A classe transporta o filtro efetivamente aplicado, a data única usada como
 * referência temporal, as linhas finais e os totais consolidados da mesma
 * consulta. A data de referência permite identificar com precisão qual fotografia
 * temporal sustentou as classificações A_VENCER e VENCIDA.
 *
 * A classe não acessa banco de dados, DAO, Service, sessão, componentes JavaFX
 * ou mecanismos de formatação visual.
 */
public final class ResultadoRelatorioContaReceber {

    private static final int ESCALA_MONETARIA = 2;

    private static final RoundingMode ARREDONDAMENTO_MONETARIO =
            RoundingMode.HALF_UP;

    private final FiltroRelatorioContaReceber filtroAplicado;
    private final LocalDate dataReferencia;
    private final List<ContaReceberRelatorioView> contas;
    private final BigDecimal valorListado;
    private final BigDecimal valorPendente;
    private final BigDecimal valorVencido;

    /**
     * Cria uma fotografia imutável do relatório de contas a receber.
     *
     * Os totais recebidos são normalizados e conferidos contra as linhas finais
     * exibidas. O valor pendente corresponde às situações A_VENCER e VENCIDA;
     * o valor vencido corresponde somente à situação VENCIDA.
     *
     * @param filtroAplicado filtro efetivamente usado na consulta.
     * @param dataReferencia data única usada para a classificação temporal.
     * @param contas linhas finais da consulta.
     * @param valorListado soma do valor de todas as linhas exibidas.
     * @param valorPendente soma das linhas A_VENCER e VENCIDA.
     * @param valorVencido soma das linhas VENCIDA.
     * @throws IllegalArgumentException quando algum argumento obrigatório for
     *                                  inválido ou quando linhas e totais forem
     *                                  incoerentes.
     */
    public ResultadoRelatorioContaReceber(
            FiltroRelatorioContaReceber filtroAplicado,
            LocalDate dataReferencia,
            List<ContaReceberRelatorioView> contas,
            BigDecimal valorListado,
            BigDecimal valorPendente,
            BigDecimal valorVencido
    ) {
        if (filtroAplicado == null) {
            throw new IllegalArgumentException(
                    "Filtro aplicado ao relatório de contas a receber é obrigatório."
            );
        }

        filtroAplicado.validar();

        if (dataReferencia == null) {
            throw new IllegalArgumentException(
                    "Data de referência do relatório de contas a receber é obrigatória."
            );
        }

        if (contas == null) {
            throw new IllegalArgumentException(
                    "Lista de contas a receber é obrigatória."
            );
        }

        for (ContaReceberRelatorioView conta : contas) {
            if (conta == null) {
                throw new IllegalArgumentException(
                        "Lista de contas a receber não pode conter elemento nulo."
                );
            }
        }

        if (valorListado == null) {
            throw new IllegalArgumentException(
                    "Valor listado é obrigatório."
            );
        }

        if (valorPendente == null) {
            throw new IllegalArgumentException(
                    "Valor pendente é obrigatório."
            );
        }

        if (valorVencido == null) {
            throw new IllegalArgumentException(
                    "Valor vencido é obrigatório."
            );
        }

        BigDecimal valorListadoNormalizado =
                normalizarValorMonetario(valorListado);

        BigDecimal valorPendenteNormalizado =
                normalizarValorMonetario(valorPendente);

        BigDecimal valorVencidoNormalizado =
                normalizarValorMonetario(valorVencido);

        if (valorListadoNormalizado.signum() < 0) {
            throw new IllegalArgumentException(
                    "Valor listado não pode ser negativo."
            );
        }

        if (valorPendenteNormalizado.signum() < 0) {
            throw new IllegalArgumentException(
                    "Valor pendente não pode ser negativo."
            );
        }

        if (valorVencidoNormalizado.signum() < 0) {
            throw new IllegalArgumentException(
                    "Valor vencido não pode ser negativo."
            );
        }

        BigDecimal somaValorListado = criarValorMonetarioZero();
        BigDecimal somaValorPendente = criarValorMonetarioZero();
        BigDecimal somaValorVencido = criarValorMonetarioZero();

        for (ContaReceberRelatorioView conta : contas) {
            BigDecimal valorConta = conta.getValor();

            somaValorListado = somaValorListado.add(valorConta);

            SituacaoRelatorioContaReceber situacao =
                    conta.getSituacao();

            if (situacao == SituacaoRelatorioContaReceber.A_VENCER
                    || situacao == SituacaoRelatorioContaReceber.VENCIDA) {

                somaValorPendente =
                        somaValorPendente.add(valorConta);
            }

            if (situacao == SituacaoRelatorioContaReceber.VENCIDA) {
                somaValorVencido =
                        somaValorVencido.add(valorConta);
            }
        }

        somaValorListado = normalizarValorMonetario(somaValorListado);
        somaValorPendente = normalizarValorMonetario(somaValorPendente);
        somaValorVencido = normalizarValorMonetario(somaValorVencido);

        if (somaValorListado.compareTo(valorListadoNormalizado) != 0) {
            throw new IllegalArgumentException(
                    "Valor listado não corresponde à soma das linhas do relatório."
            );
        }

        if (somaValorPendente.compareTo(valorPendenteNormalizado) != 0) {
            throw new IllegalArgumentException(
                    "Valor pendente não corresponde às linhas pendentes do relatório."
            );
        }

        if (somaValorVencido.compareTo(valorVencidoNormalizado) != 0) {
            throw new IllegalArgumentException(
                    "Valor vencido não corresponde às linhas vencidas do relatório."
            );
        }

        if (valorVencidoNormalizado.compareTo(valorPendenteNormalizado) > 0) {
            throw new IllegalArgumentException(
                    "Valor vencido não pode ser maior que o valor pendente."
            );
        }

        if (valorPendenteNormalizado.compareTo(valorListadoNormalizado) > 0) {
            throw new IllegalArgumentException(
                    "Valor pendente não pode ser maior que o valor listado."
            );
        }

        this.filtroAplicado = filtroAplicado;
        this.dataReferencia = dataReferencia;
        this.contas = List.copyOf(contas);
        this.valorListado = valorListadoNormalizado;
        this.valorPendente = valorPendenteNormalizado;
        this.valorVencido = valorVencidoNormalizado;
    }

    private static BigDecimal criarValorMonetarioZero() {
        return BigDecimal.ZERO.setScale(
                ESCALA_MONETARIA,
                ARREDONDAMENTO_MONETARIO
        );
    }

    private static BigDecimal normalizarValorMonetario(BigDecimal valor) {
        return valor.setScale(
                ESCALA_MONETARIA,
                ARREDONDAMENTO_MONETARIO
        );
    }

    public FiltroRelatorioContaReceber getFiltroAplicado() {
        return filtroAplicado;
    }

    public LocalDate getDataReferencia() {
        return dataReferencia;
    }

    public List<ContaReceberRelatorioView> getContas() {
        return contas;
    }

    /**
     * Retorna a quantidade de contas diretamente do tamanho da lista imutável.
     */
    public int getQuantidadeContas() {
        return contas.size();
    }

    public BigDecimal getValorListado() {
        return valorListado;
    }

    public BigDecimal getValorPendente() {
        return valorPendente;
    }

    public BigDecimal getValorVencido() {
        return valorVencido;
    }
}
