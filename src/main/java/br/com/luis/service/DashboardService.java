package br.com.luis.service;

import br.com.luis.dao.MovimentacaoFinanceiraDAO;
import br.com.luis.dao.VendaDAO;
import br.com.luis.model.Produto;
import br.com.luis.model.StatusContaReceber;
import br.com.luis.util.ConnectionFactory;
import br.com.luis.viewmodel.ContaReceberListagemView;
import br.com.luis.viewmodel.DashboardResumoView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Camada de serviço responsável por coordenar os indicadores apresentados no
 * dashboard da Tela Principal.
 *
 * Os indicadores de vendas e de valor financeiro líquido são calculados de
 * acordo com o período solicitado. As informações de contas a receber e de
 * estoque baixo representam a posição atual do sistema, independentemente do
 * período selecionado.
 *
 * O serviço reutiliza os DAOs e Services existentes e realiza cinco operações
 * de leitura: contagem de vendas, soma das vendas, cálculo financeiro líquido,
 * listagem das contas pendentes e listagem dos produtos com estoque baixo.
 *
 * Esta classe não executa SQL diretamente, não altera dados, não formata moeda
 * ou datas para apresentação e não acessa componentes JavaFX. O resultado é
 * devolvido por meio de um {@link DashboardResumoView} imutável. O tratamento
 * visual de falhas pertence à camada Controller.
 */
public class DashboardService {

    private static final int ESCALA_MONETARIA = 2;

    private static final RoundingMode ARREDONDAMENTO_MONETARIO =
            RoundingMode.HALF_UP;

    private final VendaDAO vendaDAO;
    private final MovimentacaoFinanceiraDAO movimentacaoFinanceiraDAO;
    private final ContaReceberService contaReceberService;
    private final ProdutoService produtoService;

    /**
     * Cria o serviço com as dependências responsáveis pelos indicadores do
     * dashboard.
     */
    public DashboardService() {
        this.vendaDAO = new VendaDAO();
        this.movimentacaoFinanceiraDAO =
                new MovimentacaoFinanceiraDAO();
        this.contaReceberService = new ContaReceberService();
        this.produtoService = new ProdutoService();
    }

    /**
     * Períodos fixos disponíveis para o dashboard.
     *
     * Cada opção possui uma descrição simples destinada à futura apresentação
     * no seletor da Tela Principal. O enum não representa intervalos
     * personalizados; o cálculo efetivo das datas permanece sob responsabilidade
     * do {@link DashboardService}.
     */
    public enum PeriodoDashboard {

        HOJE("Hoje"),
        ULTIMOS_7_DIAS("Últimos 7 dias"),
        MES_ATUAL("Mês atual"),
        MES_ANTERIOR("Mês anterior");

        private final String descricao;

        PeriodoDashboard(String descricao) {
            this.descricao = descricao;
        }

        /**
         * Retorna a descrição de apresentação do período.
         */
        @Override
        public String toString() {
            return descricao;
        }
    }

    /**
     * Carrega o resumo consolidado do dashboard para o período solicitado.
     *
     * As datas inicial e final entregues ao {@link DashboardResumoView} são
     * inclusivas e representam o período exibido ao usuário. Internamente, as
     * consultas de vendas e financeiro utilizam um limite inicial inclusivo e
     * um limite final exclusivo.
     *
     * O carregamento executa cinco operações de leitura:
     * três agregações usando uma mesma Connection, uma listagem de contas
     * pendentes e uma listagem de produtos com estoque baixo.
     *
     * O valor financeiro líquido pode ser negativo quando as saídas
     * compensatórias registradas no período forem superiores às entradas.
     * Contas a receber e estoque baixo representam a posição atual e não são
     * filtrados pelo período informado.
     *
     * @param periodo período que será aplicado às vendas e às movimentações
     *                financeiras.
     * @return resumo completo e imutável dos indicadores do dashboard.
     * @throws IllegalArgumentException quando o período for nulo ou quando o
     *                                  contrato final do resumo for inválido.
     * @throws IllegalStateException quando algum contrato interno de consulta
     *                               retornar dados inesperados ou incoerentes.
     * @throws RuntimeException quando ocorrer falha de conexão ou consulta ao
     *                          banco de dados.
     */
    public DashboardResumoView carregarResumo(
            PeriodoDashboard periodo
    ) {

        if (periodo == null) {
            throw new IllegalArgumentException(
                    "Período do dashboard é obrigatório."
            );
        }

        LocalDate dataReferencia = LocalDate.now();

        PeriodoCalculado periodoCalculado =
                calcularPeriodo(
                        periodo,
                        dataReferencia
                );

        int quantidadeVendas;
        BigDecimal valorTotalVendido;
        BigDecimal valorRecebidoLiquido;

        try (Connection conn = ConnectionFactory.getConnection()) {

            quantidadeVendas =
                    vendaDAO.contarVendasValidasNoPeriodo(
                            conn,
                            periodoCalculado.getInicioInclusivo(),
                            periodoCalculado.getFimExclusivo()
                    );

            valorTotalVendido =
                    vendaDAO.somarValorVendasValidasNoPeriodo(
                            conn,
                            periodoCalculado.getInicioInclusivo(),
                            periodoCalculado.getFimExclusivo()
                    );

            valorRecebidoLiquido =
                    movimentacaoFinanceiraDAO
                            .calcularValorRecebidoLiquidoNoPeriodo(
                                    conn,
                                    periodoCalculado.getInicioInclusivo(),
                                    periodoCalculado.getFimExclusivo()
                            );

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao encerrar a conexão usada no carregamento "
                            + "de vendas e financeiro do dashboard.",
                    e
            );
        }

        List<ContaReceberListagemView> contasPendentes =
                contaReceberService.listarContasPendentes();

        if (contasPendentes == null) {
            throw new IllegalStateException(
                    "A consulta de contas pendentes retornou uma lista nula."
            );
        }

        int quantidadeContasPendentes =
                contasPendentes.size();

        int quantidadeContasVencidas = 0;

        BigDecimal valorTotalPendente =
                BigDecimal.ZERO;

        BigDecimal valorTotalVencido =
                BigDecimal.ZERO;

        for (ContaReceberListagemView contaPendente
                : contasPendentes) {

            if (contaPendente == null) {
                throw new IllegalStateException(
                        "A lista de contas pendentes contém "
                                + "um elemento nulo."
                );
            }

            if (contaPendente.getStatus()
                    != StatusContaReceber.PENDENTE) {

                throw new IllegalStateException(
                        "A listagem de contas pendentes contém "
                                + "uma conta com status diferente "
                                + "de PENDENTE."
                );
            }

            BigDecimal valorConta =
                    contaPendente.getValor();

            if (valorConta == null) {
                throw new IllegalStateException(
                        "Uma conta a receber pendente possui valor nulo."
                );
            }

            if (valorConta.signum() < 0) {
                throw new IllegalStateException(
                        "Uma conta a receber pendente possui valor negativo."
                );
            }

            valorTotalPendente =
                    valorTotalPendente.add(valorConta);

            if (contaPendente.isVencida()) {
                quantidadeContasVencidas++;

                valorTotalVencido =
                        valorTotalVencido.add(valorConta);
            }
        }

        valorTotalPendente =
                valorTotalPendente.setScale(
                        ESCALA_MONETARIA,
                        ARREDONDAMENTO_MONETARIO
                );

        valorTotalVencido =
                valorTotalVencido.setScale(
                        ESCALA_MONETARIA,
                        ARREDONDAMENTO_MONETARIO
                );

        List<Produto> produtosAbaixoDoMinimo =
                produtoService.listarAbaixoDoMinimo();

        if (produtosAbaixoDoMinimo == null) {
            throw new IllegalStateException(
                    "A consulta de produtos com estoque baixo "
                            + "retornou uma lista nula."
            );
        }

        int quantidadeProdutosEstoqueBaixo =
                produtosAbaixoDoMinimo.size();

        return new DashboardResumoView(
                periodoCalculado.getDataInicial(),
                periodoCalculado.getDataFinal(),
                quantidadeVendas,
                valorTotalVendido,
                valorRecebidoLiquido,
                quantidadeContasPendentes,
                valorTotalPendente,
                quantidadeContasVencidas,
                valorTotalVencido,
                quantidadeProdutosEstoqueBaixo
        );
    }

    /**
     * Calcula as datas inclusivas de apresentação e os limites temporais usados
     * pelas consultas.
     *
     * O cálculo utiliza uma única data de referência. O início das consultas é
     * inclusivo e o limite final é exclusivo, sempre à meia-noite do dia
     * seguinte ao período ou do primeiro dia do mês seguinte.
     *
     * @param periodo período que será calculado.
     * @param dataReferencia data atual capturada uma única vez pelo carregamento.
     * @return estrutura interna com as quatro representações do período.
     */
    private static PeriodoCalculado calcularPeriodo(
            PeriodoDashboard periodo,
            LocalDate dataReferencia
    ) {

        return switch (periodo) {

            case HOJE -> {

                LocalDate dataInicial =
                        dataReferencia;

                LocalDate dataFinal =
                        dataReferencia;

                LocalDateTime inicioInclusivo =
                        dataInicial.atStartOfDay();

                LocalDateTime fimExclusivo =
                        dataReferencia
                                .plusDays(1)
                                .atStartOfDay();

                yield new PeriodoCalculado(
                        dataInicial,
                        dataFinal,
                        inicioInclusivo,
                        fimExclusivo
                );
            }

            case ULTIMOS_7_DIAS -> {

                LocalDate dataInicial =
                        dataReferencia.minusDays(6);

                LocalDate dataFinal =
                        dataReferencia;

                LocalDateTime inicioInclusivo =
                        dataInicial.atStartOfDay();

                LocalDateTime fimExclusivo =
                        dataReferencia
                                .plusDays(1)
                                .atStartOfDay();

                yield new PeriodoCalculado(
                        dataInicial,
                        dataFinal,
                        inicioInclusivo,
                        fimExclusivo
                );
            }

            case MES_ATUAL -> {

                LocalDate primeiroDiaMesAtual =
                        dataReferencia.withDayOfMonth(1);

                LocalDate primeiroDiaProximoMes =
                        primeiroDiaMesAtual.plusMonths(1);

                LocalDate dataInicial =
                        primeiroDiaMesAtual;

                LocalDate dataFinal =
                        primeiroDiaProximoMes.minusDays(1);

                LocalDateTime inicioInclusivo =
                        primeiroDiaMesAtual.atStartOfDay();

                LocalDateTime fimExclusivo =
                        primeiroDiaProximoMes.atStartOfDay();

                yield new PeriodoCalculado(
                        dataInicial,
                        dataFinal,
                        inicioInclusivo,
                        fimExclusivo
                );
            }

            case MES_ANTERIOR -> {

                LocalDate primeiroDiaMesAtual =
                        dataReferencia.withDayOfMonth(1);

                LocalDate dataInicial =
                        primeiroDiaMesAtual.minusMonths(1);

                LocalDate dataFinal =
                        primeiroDiaMesAtual.minusDays(1);

                LocalDateTime inicioInclusivo =
                        dataInicial.atStartOfDay();

                LocalDateTime fimExclusivo =
                        primeiroDiaMesAtual.atStartOfDay();

                yield new PeriodoCalculado(
                        dataInicial,
                        dataFinal,
                        inicioInclusivo,
                        fimExclusivo
                );
            }
        };
    }

    /**
     * Transporta internamente as datas inclusivas apresentadas no dashboard e
     * os limites temporais utilizados pelas consultas.
     *
     * A estrutura é imutável, não acessa banco de dados e não contém qualquer
     * formatação visual.
     */
    private static final class PeriodoCalculado {

        private final LocalDate dataInicial;
        private final LocalDate dataFinal;
        private final LocalDateTime inicioInclusivo;
        private final LocalDateTime fimExclusivo;

        private PeriodoCalculado(
                LocalDate dataInicial,
                LocalDate dataFinal,
                LocalDateTime inicioInclusivo,
                LocalDateTime fimExclusivo
        ) {
            this.dataInicial = dataInicial;
            this.dataFinal = dataFinal;
            this.inicioInclusivo = inicioInclusivo;
            this.fimExclusivo = fimExclusivo;
        }

        private LocalDate getDataInicial() {
            return dataInicial;
        }

        private LocalDate getDataFinal() {
            return dataFinal;
        }

        private LocalDateTime getInicioInclusivo() {
            return inicioInclusivo;
        }

        private LocalDateTime getFimExclusivo() {
            return fimExclusivo;
        }
    }
}