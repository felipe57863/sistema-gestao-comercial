package br.com.luis.service;

import br.com.luis.dao.MovimentacaoFinanceiraDAO;
import br.com.luis.dao.UsuarioDAO;
import br.com.luis.model.TipoMovimentacaoFinanceira;
import br.com.luis.model.Usuario;
import br.com.luis.util.ConnectionFactory;
import br.com.luis.viewmodel.FiltroRelatorioMovimentacaoFinanceira;
import br.com.luis.viewmodel.MovimentacaoFinanceiraRelatorioView;
import br.com.luis.viewmodel.ResultadoRelatorioMovimentacaoFinanceira;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Coordena a consulta protegida do relatório de movimentações financeiras.
 *
 * O Service valida as entradas, calcula os limites técnicos do período,
 * reconsulta e autoriza o usuário, solicita ao DAO somente as linhas projetadas
 * e consolida os totais financeiros da fotografia retornada.
 *
 * Não acessa SessaoUsuario, componentes JavaFX ou mecanismos de formatação
 * visual. Também não altera autoCommit e não executa commit ou rollback,
 * pois o fluxo é exclusivamente de leitura.
 */
public class RelatorioMovimentacaoFinanceiraService {

    private static final int ESCALA_MONETARIA = 2;

    private static final RoundingMode ARREDONDAMENTO_MONETARIO =
            RoundingMode.HALF_UP;

    private static final String STATUS_ATIVO = "ATIVO";
    private static final String PERFIL_ADMIN = "ADMIN";

    private static final String MENSAGEM_ACESSO_NEGADO =
            "Usuário não autorizado a consultar o relatório financeiro.";

    private final MovimentacaoFinanceiraDAO movimentacaoFinanceiraDAO;
    private final UsuarioDAO usuarioDAO;

    /**
     * Cria o Service com as dependências JDBC usadas na consulta financeira.
     */
    public RelatorioMovimentacaoFinanceiraService() {
        this.movimentacaoFinanceiraDAO =
                new MovimentacaoFinanceiraDAO();
        this.usuarioDAO =
                new UsuarioDAO();
    }

    /**
     * Consulta o relatório de movimentações financeiras para os filtros
     * informados e devolve uma fotografia completa e imutável.
     *
     * @param filtro fotografia imutável dos filtros solicitados.
     * @param usuarioId identificador do usuário que solicita a consulta.
     * @return resultado consolidado da consulta financeira.
     * @throws IllegalArgumentException quando o filtro, o usuário ou o período
     *                                  informado for inválido.
     * @throws SecurityException quando o usuário não estiver autorizado.
     * @throws IllegalStateException quando houver incoerência nos dados
     *                               persistidos ou na consolidação do resultado.
     * @throws RuntimeException quando ocorrer falha de conexão ou persistência.
     */
    public ResultadoRelatorioMovimentacaoFinanceira consultarRelatorio(
            FiltroRelatorioMovimentacaoFinanceira filtro,
            Integer usuarioId
    ) {

        if (filtro == null) {
            throw new IllegalArgumentException(
                    "Filtro do relatório financeiro não pode ser nulo."
            );
        }

        filtro.validar();

        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException(
                    "ID do usuário deve ser maior que zero."
            );
        }

        LocalDate dataFinal =
                filtro.getDataFinal();

        if (LocalDate.MAX.equals(dataFinal)) {
            throw new IllegalArgumentException(
                    "A data final informada não permite calcular "
                            + "o limite exclusivo do período."
            );
        }

        LocalDateTime inicioInclusivo =
                filtro.getDataInicial().atStartOfDay();

        LocalDateTime fimExclusivo =
                dataFinal.plusDays(1).atStartOfDay();

        try (Connection conn =
                     ConnectionFactory.getConnection()) {

            Usuario usuario =
                    usuarioDAO.buscarPorId(
                            conn,
                            usuarioId
                    );

            validarAutorizacao(
                    usuario,
                    usuarioId
            );

            List<MovimentacaoFinanceiraRelatorioView> movimentacoes =
                    movimentacaoFinanceiraDAO.listarParaRelatorio(
                            conn,
                            filtro,
                            inicioInclusivo,
                            fimExclusivo
                    );

            if (movimentacoes == null) {
                throw new IllegalStateException(
                        "A consulta do relatório financeiro retornou "
                                + "uma lista nula."
                );
            }

            BigDecimal totalEntradas =
                    criarValorMonetarioZero();

            BigDecimal totalSaidas =
                    criarValorMonetarioZero();

            for (MovimentacaoFinanceiraRelatorioView movimentacao
                    : movimentacoes) {

                if (movimentacao == null) {
                    throw new IllegalStateException(
                            "A consulta do relatório financeiro retornou "
                                    + "uma movimentação nula."
                    );
                }

                TipoMovimentacaoFinanceira tipo =
                        movimentacao.getTipo();

                if (tipo == null) {
                    throw new IllegalStateException(
                            "Movimentação financeira sem tipo válido "
                                    + "durante a totalização do relatório."
                    );
                }

                switch (tipo) {
                    case ENTRADA ->
                            totalEntradas =
                                    totalEntradas.add(
                                            movimentacao.getValor()
                                    );

                    case SAIDA ->
                            totalSaidas =
                                    totalSaidas.add(
                                            movimentacao.getValor()
                                    );

                    default ->
                            throw new IllegalStateException(
                                    "Tipo de movimentação financeira sem "
                                            + "regra de totalização: "
                                            + tipo
                            );
                }
            }

            totalEntradas =
                    normalizarValorMonetario(
                            totalEntradas
                    );

            totalSaidas =
                    normalizarValorMonetario(
                            totalSaidas
                    );

            try {
                return new ResultadoRelatorioMovimentacaoFinanceira(
                        filtro,
                        movimentacoes,
                        totalEntradas,
                        totalSaidas
                );

            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                        "Incoerência ao consolidar o resultado "
                                + "do relatório financeiro.",
                        e
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao acessar o banco de dados durante "
                            + "a consulta do relatório financeiro.",
                    e
            );
        }
    }

    /**
     * Verifica a autorização persistida do usuário que solicita o relatório.
     */
    private void validarAutorizacao(
            Usuario usuario,
            Integer usuarioIdSolicitado
    ) {

        boolean usuarioAutorizado =
                usuario != null
                        && usuarioIdSolicitado.equals(
                                usuario.getIdUsuario()
                        )
                        && STATUS_ATIVO.equals(
                                usuario.getStatus()
                        )
                        && PERFIL_ADMIN.equals(
                                usuario.getPerfil()
                        );

        if (!usuarioAutorizado) {
            throw new SecurityException(
                    MENSAGEM_ACESSO_NEGADO
            );
        }
    }

    /**
     * Cria o valor monetário inicial R$ 0,00.
     */
    private BigDecimal criarValorMonetarioZero() {
        return BigDecimal.ZERO.setScale(
                ESCALA_MONETARIA,
                ARREDONDAMENTO_MONETARIO
        );
    }

    /**
     * Normaliza valores monetários para escala 2 com HALF_UP.
     */
    private BigDecimal normalizarValorMonetario(
            BigDecimal valor
    ) {
        return valor.setScale(
                ESCALA_MONETARIA,
                ARREDONDAMENTO_MONETARIO
        );
    }
}
