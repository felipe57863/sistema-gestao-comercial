package br.com.luis.viewmodel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Transporta o resumo consolidado apresentado no dashboard da Tela Principal.
 *
 * A classe é imutável e representa uma fotografia completa dos indicadores
 * obtidos em um carregamento do dashboard.
 *
 * Os indicadores de vendas e de valor recebido líquido obedecem ao período
 * informado. Os indicadores de contas a receber e estoque baixo representam
 * a posição atual do sistema e não são limitados pelo período selecionado.
 *
 * Esta classe não acessa banco de dados, DAO, Service, componentes JavaFX ou
 * qualquer mecanismo de persistência. Sua responsabilidade é apenas transportar
 * dados já consolidados pelas camadas responsáveis.
 *
 * Todos os valores monetários são armazenados com escala 2 e arredondamento
 * {@link RoundingMode#HALF_UP}. O valor recebido líquido pode ser negativo
 * quando as saídas financeiras do período forem superiores às entradas.
 */
public final class DashboardResumoView {

    private static final int ESCALA_MONETARIA = 2;

    private static final RoundingMode ARREDONDAMENTO_MONETARIO =
            RoundingMode.HALF_UP;

    private final LocalDate dataInicial;
    private final LocalDate dataFinal;

    private final int quantidadeVendas;
    private final BigDecimal valorTotalVendido;

    private final BigDecimal valorRecebidoLiquido;

    private final int quantidadeContasPendentes;
    private final BigDecimal valorTotalPendente;

    private final int quantidadeContasVencidas;
    private final BigDecimal valorTotalVencido;

    private final int quantidadeProdutosEstoqueBaixo;

    /**
     * Cria um resumo completo e imutável do dashboard.
     *
     * {@code dataInicial} e {@code dataFinal} representam o período aplicado
     * às vendas e às movimentações financeiras. As informações de contas a
     * receber e estoque baixo representam a posição atual, independentemente
     * dessas datas.
     *
     * O valor recebido líquido pode ser negativo quando as saídas compensatórias
     * de estorno registradas no período forem superiores às entradas.
     *
     * @param dataInicial data inicial inclusiva apresentada no dashboard.
     * @param dataFinal data final inclusiva apresentada no dashboard.
     * @param quantidadeVendas quantidade de vendas válidas no período.
     * @param valorTotalVendido valor total das vendas pagas e pendentes no período.
     * @param valorRecebidoLiquido resultado das entradas menos as saídas financeiras
     *                             do período.
     * @param quantidadeContasPendentes quantidade atual de contas pendentes.
     * @param valorTotalPendente valor atual total das contas pendentes.
     * @param quantidadeContasVencidas quantidade atual de contas pendentes vencidas.
     * @param valorTotalVencido valor atual total das contas pendentes vencidas.
     * @param quantidadeProdutosEstoqueBaixo quantidade atual de produtos ativos
     *                                       abaixo ou no estoque mínimo.
     *
     * @throws IllegalArgumentException quando alguma data ou valor monetário for
     *                                  nulo, quando o período for inválido, quando
     *                                  alguma quantidade for negativa, quando um
     *                                  valor que deveria ser não negativo for
     *                                  negativo ou quando os dados vencidos forem
     *                                  superiores aos dados pendentes.
     */
    public DashboardResumoView(
            LocalDate dataInicial,
            LocalDate dataFinal,
            int quantidadeVendas,
            BigDecimal valorTotalVendido,
            BigDecimal valorRecebidoLiquido,
            int quantidadeContasPendentes,
            BigDecimal valorTotalPendente,
            int quantidadeContasVencidas,
            BigDecimal valorTotalVencido,
            int quantidadeProdutosEstoqueBaixo
    ) {
        if (dataInicial == null) {
            throw new IllegalArgumentException(
                    "A data inicial do dashboard é obrigatória."
            );
        }

        if (dataFinal == null) {
            throw new IllegalArgumentException(
                    "A data final do dashboard é obrigatória."
            );
        }

        if (dataFinal.isBefore(dataInicial)) {
            throw new IllegalArgumentException(
                    "A data final do dashboard não pode ser anterior à data inicial."
            );
        }

        if (quantidadeVendas < 0) {
            throw new IllegalArgumentException(
                    "A quantidade de vendas não pode ser negativa."
            );
        }

        if (quantidadeContasPendentes < 0) {
            throw new IllegalArgumentException(
                    "A quantidade de contas pendentes não pode ser negativa."
            );
        }

        if (quantidadeContasVencidas < 0) {
            throw new IllegalArgumentException(
                    "A quantidade de contas vencidas não pode ser negativa."
            );
        }

        if (quantidadeProdutosEstoqueBaixo < 0) {
            throw new IllegalArgumentException(
                    "A quantidade de produtos com estoque baixo não pode ser negativa."
            );
        }

        if (quantidadeContasVencidas > quantidadeContasPendentes) {
            throw new IllegalArgumentException(
                    "A quantidade de contas vencidas não pode ser maior "
                            + "que a quantidade de contas pendentes."
            );
        }

        if (valorTotalVendido == null) {
            throw new IllegalArgumentException(
                    "O valor total vendido é obrigatório."
            );
        }

        if (valorTotalVendido.signum() < 0) {
            throw new IllegalArgumentException(
                    "O valor total vendido não pode ser negativo."
            );
        }

        if (valorRecebidoLiquido == null) {
            throw new IllegalArgumentException(
                    "O valor recebido líquido é obrigatório."
            );
        }

        if (valorTotalPendente == null) {
            throw new IllegalArgumentException(
                    "O valor total pendente é obrigatório."
            );
        }

        if (valorTotalPendente.signum() < 0) {
            throw new IllegalArgumentException(
                    "O valor total pendente não pode ser negativo."
            );
        }

        if (valorTotalVencido == null) {
            throw new IllegalArgumentException(
                    "O valor total vencido é obrigatório."
            );
        }

        if (valorTotalVencido.signum() < 0) {
            throw new IllegalArgumentException(
                    "O valor total vencido não pode ser negativo."
            );
        }

        BigDecimal valorTotalVendidoNormalizado =
                valorTotalVendido.setScale(
                        ESCALA_MONETARIA,
                        ARREDONDAMENTO_MONETARIO
                );

        BigDecimal valorRecebidoLiquidoNormalizado =
                valorRecebidoLiquido.setScale(
                        ESCALA_MONETARIA,
                        ARREDONDAMENTO_MONETARIO
                );

        BigDecimal valorTotalPendenteNormalizado =
                valorTotalPendente.setScale(
                        ESCALA_MONETARIA,
                        ARREDONDAMENTO_MONETARIO
                );

        BigDecimal valorTotalVencidoNormalizado =
                valorTotalVencido.setScale(
                        ESCALA_MONETARIA,
                        ARREDONDAMENTO_MONETARIO
                );

        if (valorTotalVencidoNormalizado.compareTo(
                valorTotalPendenteNormalizado
        ) > 0) {
            throw new IllegalArgumentException(
                    "O valor total vencido não pode ser maior "
                            + "que o valor total pendente."
            );
        }

        this.dataInicial = dataInicial;
        this.dataFinal = dataFinal;
        this.quantidadeVendas = quantidadeVendas;
        this.valorTotalVendido = valorTotalVendidoNormalizado;
        this.valorRecebidoLiquido = valorRecebidoLiquidoNormalizado;
        this.quantidadeContasPendentes = quantidadeContasPendentes;
        this.valorTotalPendente = valorTotalPendenteNormalizado;
        this.quantidadeContasVencidas = quantidadeContasVencidas;
        this.valorTotalVencido = valorTotalVencidoNormalizado;
        this.quantidadeProdutosEstoqueBaixo =
                quantidadeProdutosEstoqueBaixo;
    }

    /**
     * Retorna a data inicial inclusiva do período apresentado.
     */
    public LocalDate getDataInicial() {
        return dataInicial;
    }

    /**
     * Retorna a data final inclusiva do período apresentado.
     */
    public LocalDate getDataFinal() {
        return dataFinal;
    }

    /**
     * Retorna a quantidade de vendas válidas encontradas no período.
     */
    public int getQuantidadeVendas() {
        return quantidadeVendas;
    }

    /**
     * Retorna o valor total das vendas pagas e pendentes no período.
     */
    public BigDecimal getValorTotalVendido() {
        return valorTotalVendido;
    }

    /**
     * Retorna o resultado das entradas menos as saídas financeiras do período.
     *
     * O resultado pode ser negativo.
     */
    public BigDecimal getValorRecebidoLiquido() {
        return valorRecebidoLiquido;
    }

    /**
     * Retorna a quantidade atual de contas pendentes.
     */
    public int getQuantidadeContasPendentes() {
        return quantidadeContasPendentes;
    }

    /**
     * Retorna o valor atual total das contas pendentes.
     */
    public BigDecimal getValorTotalPendente() {
        return valorTotalPendente;
    }

    /**
     * Retorna a quantidade atual de contas pendentes vencidas.
     */
    public int getQuantidadeContasVencidas() {
        return quantidadeContasVencidas;
    }

    /**
     * Retorna o valor atual total das contas pendentes vencidas.
     */
    public BigDecimal getValorTotalVencido() {
        return valorTotalVencido;
    }

    /**
     * Retorna a quantidade atual de produtos ativos abaixo ou no estoque mínimo.
     */
    public int getQuantidadeProdutosEstoqueBaixo() {
        return quantidadeProdutosEstoqueBaixo;
    }
}
