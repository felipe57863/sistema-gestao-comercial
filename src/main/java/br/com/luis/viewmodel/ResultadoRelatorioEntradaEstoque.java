package br.com.luis.viewmodel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Fotografia imutável do relatório de entradas de estoque.
 */
public final class ResultadoRelatorioEntradaEstoque {

    private final FiltroRelatorioEntradaEstoque filtro;
    private final List<EntradaEstoqueRelatorioView> entradas;
    private final Integer quantidadeEntradas;
    private final Integer totalUnidades;
    private final BigDecimal valorTotal;

    public ResultadoRelatorioEntradaEstoque(
            FiltroRelatorioEntradaEstoque filtro,
            List<EntradaEstoqueRelatorioView> entradas,
            Integer quantidadeEntradas,
            Integer totalUnidades,
            BigDecimal valorTotal
    ) {
        if (filtro == null) {
            throw new IllegalArgumentException("Filtro aplicado é obrigatório.");
        }
        filtro.validar();
        if (entradas == null || entradas.stream().anyMatch(item -> item == null)) {
            throw new IllegalArgumentException("Lista de entradas é obrigatória e válida.");
        }
        if (quantidadeEntradas == null || quantidadeEntradas < 0) {
            throw new IllegalArgumentException(
                    "Quantidade de entradas não pode ser negativa."
            );
        }
        if (quantidadeEntradas != entradas.size()) {
            throw new IllegalArgumentException(
                    "Quantidade de entradas não corresponde à lista informada."
            );
        }
        if (totalUnidades == null || totalUnidades < 0) {
            throw new IllegalArgumentException("Total de unidades não pode ser negativo.");
        }
        if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor total não pode ser negativo.");
        }

        this.filtro = filtro;
        this.entradas = List.copyOf(entradas);
        this.quantidadeEntradas = quantidadeEntradas;
        this.totalUnidades = totalUnidades;
        this.valorTotal = valorTotal.setScale(2, RoundingMode.HALF_UP);
    }

    public FiltroRelatorioEntradaEstoque getFiltro() {
        return filtro;
    }

    public List<EntradaEstoqueRelatorioView> getEntradas() {
        return entradas;
    }

    public Integer getQuantidadeEntradas() {
        return quantidadeEntradas;
    }

    public Integer getTotalUnidades() {
        return totalUnidades;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }
}
