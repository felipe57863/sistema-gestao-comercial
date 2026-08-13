package br.com.luis.viewmodel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Representa a fotografia completa de uma consulta concluída do relatório de
 * clientes com pendências financeiras.
 *
 * A classe é imutável e confere os totalizadores contra a lista final já
 * filtrada. Não consulta, filtra ou recalcula vencimentos.
 */
public final class ResultadoRelatorioClientePendencia {

    private static final int ESCALA_MONETARIA = 2;

    private static final RoundingMode ARREDONDAMENTO_MONETARIO =
            RoundingMode.HALF_UP;

    private final FiltroRelatorioClientePendencia filtroAplicado;
    private final LocalDate dataReferencia;
    private final List<ClientePendenciaRelatorioView> clientes;
    private final int quantidadeClientes;
    private final BigDecimal valorTotalPendente;
    private final int quantidadeClientesComVencidas;
    private final BigDecimal valorTotalVencido;

    /**
     * Cria uma fotografia imutável e coerente da consulta.
     *
     * Uma lista vazia com todos os totalizadores zerados é válida.
     *
     * @throws IllegalArgumentException quando argumentos obrigatórios estiverem
     *                                  ausentes ou os totais divergirem das linhas.
     */
    public ResultadoRelatorioClientePendencia(
            FiltroRelatorioClientePendencia filtroAplicado,
            LocalDate dataReferencia,
            List<ClientePendenciaRelatorioView> clientes,
            int quantidadeClientes,
            BigDecimal valorTotalPendente,
            int quantidadeClientesComVencidas,
            BigDecimal valorTotalVencido
    ) {
        if (filtroAplicado == null) {
            throw new IllegalArgumentException(
                    "Filtro aplicado ao relatório de pendências é obrigatório."
            );
        }

        filtroAplicado.validar();

        if (dataReferencia == null) {
            throw new IllegalArgumentException(
                    "Data de referência do relatório de pendências é obrigatória."
            );
        }

        if (clientes == null) {
            throw new IllegalArgumentException(
                    "Lista de clientes com pendências é obrigatória."
            );
        }

        for (ClientePendenciaRelatorioView cliente : clientes) {
            if (cliente == null) {
                throw new IllegalArgumentException(
                        "Lista de clientes não pode conter elemento nulo."
                );
            }
        }

        if (quantidadeClientes < 0) {
            throw new IllegalArgumentException(
                    "Quantidade de clientes não pode ser negativa."
            );
        }

        if (quantidadeClientesComVencidas < 0) {
            throw new IllegalArgumentException(
                    "Quantidade de clientes com vencidas não pode ser negativa."
            );
        }

        if (valorTotalPendente == null) {
            throw new IllegalArgumentException(
                    "Valor total pendente é obrigatório."
            );
        }

        if (valorTotalVencido == null) {
            throw new IllegalArgumentException(
                    "Valor total vencido é obrigatório."
            );
        }

        List<ClientePendenciaRelatorioView> clientesImutaveis =
                List.copyOf(clientes);

        if (quantidadeClientes != clientesImutaveis.size()) {
            throw new IllegalArgumentException(
                    "Quantidade de clientes não corresponde às linhas do relatório."
            );
        }

        BigDecimal valorPendenteNormalizado =
                normalizarValorMonetario(valorTotalPendente);

        BigDecimal valorVencidoNormalizado =
                normalizarValorMonetario(valorTotalVencido);

        if (valorPendenteNormalizado.signum() < 0) {
            throw new IllegalArgumentException(
                    "Valor total pendente não pode ser negativo."
            );
        }

        if (valorVencidoNormalizado.signum() < 0) {
            throw new IllegalArgumentException(
                    "Valor total vencido não pode ser negativo."
            );
        }

        BigDecimal somaPendente = criarValorMonetarioZero();
        BigDecimal somaVencida = criarValorMonetarioZero();
        int clientesComVencidas = 0;

        for (ClientePendenciaRelatorioView cliente : clientesImutaveis) {
            somaPendente = somaPendente.add(cliente.getValorPendente());
            somaVencida = somaVencida.add(cliente.getValorVencido());

            if (cliente.getQuantidadeContasVencidas() > 0) {
                clientesComVencidas++;
            }
        }

        somaPendente = normalizarValorMonetario(somaPendente);
        somaVencida = normalizarValorMonetario(somaVencida);

        if (somaPendente.compareTo(valorPendenteNormalizado) != 0) {
            throw new IllegalArgumentException(
                    "Valor total pendente não corresponde às linhas do relatório."
            );
        }

        if (somaVencida.compareTo(valorVencidoNormalizado) != 0) {
            throw new IllegalArgumentException(
                    "Valor total vencido não corresponde às linhas do relatório."
            );
        }

        if (clientesComVencidas != quantidadeClientesComVencidas) {
            throw new IllegalArgumentException(
                    "Quantidade de clientes com vencidas não corresponde às linhas."
            );
        }

        if (quantidadeClientesComVencidas > quantidadeClientes) {
            throw new IllegalArgumentException(
                    "Clientes com vencidas não podem superar os clientes listados."
            );
        }

        if (valorVencidoNormalizado.compareTo(valorPendenteNormalizado) > 0) {
            throw new IllegalArgumentException(
                    "Valor total vencido não pode superar o total pendente."
            );
        }

        this.filtroAplicado = filtroAplicado;
        this.dataReferencia = dataReferencia;
        this.clientes = clientesImutaveis;
        this.quantidadeClientes = quantidadeClientes;
        this.valorTotalPendente = valorPendenteNormalizado;
        this.quantidadeClientesComVencidas = quantidadeClientesComVencidas;
        this.valorTotalVencido = valorVencidoNormalizado;
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

    public FiltroRelatorioClientePendencia getFiltroAplicado() {
        return filtroAplicado;
    }

    public LocalDate getDataReferencia() {
        return dataReferencia;
    }

    public List<ClientePendenciaRelatorioView> getClientes() {
        return clientes;
    }

    public int getQuantidadeClientes() {
        return quantidadeClientes;
    }

    public BigDecimal getValorTotalPendente() {
        return valorTotalPendente;
    }

    public int getQuantidadeClientesComVencidas() {
        return quantidadeClientesComVencidas;
    }

    public BigDecimal getValorTotalVencido() {
        return valorTotalVencido;
    }
}
