package br.com.luis.viewmodel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Representa a fotografia consolidada do relatório de descontos concedidos.
 *
 * A lista e os totalizadores correspondem ao mesmo conjunto final filtrado.
 */
public final class ResultadoRelatorioDescontoVenda {

    private static final int ESCALA_MONETARIA = 2;
    private static final RoundingMode ARREDONDAMENTO_MONETARIO =
            RoundingMode.HALF_UP;

    private final FiltroRelatorioDescontoVenda filtroAplicado;
    private final List<VendaDescontoRelatorioView> vendas;
    private final int quantidadeVendas;
    private final BigDecimal totalDescontoPromocional;
    private final BigDecimal totalDescontoGlobal;
    private final BigDecimal totalDescontos;

    /**
     * Cria um resultado imutável e confere os totais contra as linhas finais.
     * Uma lista vazia acompanhada de totais zerados é válida.
     */
    public ResultadoRelatorioDescontoVenda(
            FiltroRelatorioDescontoVenda filtroAplicado,
            List<VendaDescontoRelatorioView> vendas,
            int quantidadeVendas,
            BigDecimal totalDescontoPromocional,
            BigDecimal totalDescontoGlobal,
            BigDecimal totalDescontos
    ) {
        if (filtroAplicado == null) {
            throw new IllegalArgumentException(
                    "Filtro aplicado ao relatório de descontos é obrigatório."
            );
        }

        filtroAplicado.validar();

        if (vendas == null) {
            throw new IllegalArgumentException(
                    "Lista de vendas com desconto é obrigatória."
            );
        }

        for (VendaDescontoRelatorioView venda : vendas) {
            if (venda == null) {
                throw new IllegalArgumentException(
                        "Lista de vendas não pode conter elemento nulo."
                );
            }
        }

        if (quantidadeVendas < 0) {
            throw new IllegalArgumentException(
                    "Quantidade de vendas não pode ser negativa."
            );
        }

        validarValorObrigatorio(
                totalDescontoPromocional,
                "Total de desconto promocional"
        );
        validarValorObrigatorio(
                totalDescontoGlobal,
                "Total de desconto global"
        );
        validarValorObrigatorio(totalDescontos, "Total de descontos");

        List<VendaDescontoRelatorioView> vendasImutaveis =
                List.copyOf(vendas);

        if (quantidadeVendas != vendasImutaveis.size()) {
            throw new IllegalArgumentException(
                    "Quantidade de vendas não corresponde às linhas do relatório."
            );
        }

        BigDecimal promocionalNormalizado =
                normalizarValorMonetario(totalDescontoPromocional);
        BigDecimal globalNormalizado =
                normalizarValorMonetario(totalDescontoGlobal);
        BigDecimal totalNormalizado = normalizarValorMonetario(totalDescontos);

        if (promocionalNormalizado.signum() < 0
                || globalNormalizado.signum() < 0
                || totalNormalizado.signum() < 0) {

            throw new IllegalArgumentException(
                    "Totalizadores de descontos não podem ser negativos."
            );
        }

        BigDecimal somaPromocional = criarValorMonetarioZero();
        BigDecimal somaGlobal = criarValorMonetarioZero();
        BigDecimal somaTotal = criarValorMonetarioZero();

        for (VendaDescontoRelatorioView venda : vendasImutaveis) {
            somaPromocional = somaPromocional.add(
                    venda.getDescontoPromocional()
            );
            somaGlobal = somaGlobal.add(venda.getDescontoGlobal());
            somaTotal = somaTotal.add(venda.getDescontoTotal());
        }

        somaPromocional = normalizarValorMonetario(somaPromocional);
        somaGlobal = normalizarValorMonetario(somaGlobal);
        somaTotal = normalizarValorMonetario(somaTotal);

        if (somaPromocional.compareTo(promocionalNormalizado) != 0) {
            throw new IllegalArgumentException(
                    "Total promocional não corresponde às linhas do relatório."
            );
        }

        if (somaGlobal.compareTo(globalNormalizado) != 0) {
            throw new IllegalArgumentException(
                    "Total global não corresponde às linhas do relatório."
            );
        }

        if (somaTotal.compareTo(totalNormalizado) != 0) {
            throw new IllegalArgumentException(
                    "Total de descontos não corresponde às linhas do relatório."
            );
        }

        BigDecimal totalDasOrigens = normalizarValorMonetario(
                promocionalNormalizado.add(globalNormalizado)
        );

        if (totalDasOrigens.compareTo(totalNormalizado) != 0) {
            throw new IllegalArgumentException(
                    "Total de descontos não corresponde às origens consolidadas."
            );
        }

        this.filtroAplicado = filtroAplicado;
        this.vendas = vendasImutaveis;
        this.quantidadeVendas = quantidadeVendas;
        this.totalDescontoPromocional = promocionalNormalizado;
        this.totalDescontoGlobal = globalNormalizado;
        this.totalDescontos = totalNormalizado;
    }

    private static void validarValorObrigatorio(
            BigDecimal valor,
            String nomeCampo
    ) {
        if (valor == null) {
            throw new IllegalArgumentException(nomeCampo + " é obrigatório.");
        }
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

    public FiltroRelatorioDescontoVenda getFiltroAplicado() {
        return filtroAplicado;
    }

    public List<VendaDescontoRelatorioView> getVendas() {
        return vendas;
    }

    public int getQuantidadeVendas() {
        return quantidadeVendas;
    }

    public BigDecimal getTotalDescontoPromocional() {
        return totalDescontoPromocional;
    }

    public BigDecimal getTotalDescontoGlobal() {
        return totalDescontoGlobal;
    }

    public BigDecimal getTotalDescontos() {
        return totalDescontos;
    }
}
