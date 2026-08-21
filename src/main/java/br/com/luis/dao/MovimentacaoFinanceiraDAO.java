package br.com.luis.dao;

import br.com.luis.model.MovimentacaoFinanceira;
import br.com.luis.model.FormaPagamento;
import br.com.luis.model.OrigemMovimentacaoFinanceira;
import br.com.luis.model.TipoMovimentacaoFinanceira;
import br.com.luis.viewmodel.FiltroRelatorioMovimentacaoFinanceira;
import br.com.luis.viewmodel.MovimentacaoFinanceiraRelatorioView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO responsável pela inserção e consulta de movimentações financeiras por JDBC.
 *
 * É utilizado pelos fluxos de venda à vista, recebimento integral de conta e
 * estorno. Não implementa atualização ou exclusão de lançamentos anteriores.
 *
 * Não decide tipo, origem, forma de pagamento, valor ou necessidade de
 * compensação. Essas regras pertencem aos Services responsáveis por cada fluxo.
 *
 * Nos métodos que recebem uma Connection externa, o Service chamador controla
 * commit, rollback e fechamento da conexão. O DAO encerra somente os recursos
 * JDBC que cria, como PreparedStatement e ResultSet.
 */
public class MovimentacaoFinanceiraDAO {

    /**
     * Insere uma movimentação na transação controlada pelo Service responsável.
     *
     * O lançamento pode pertencer à venda, ao recebimento ou à compensação de
     * estorno e não substitui movimentações anteriores. O DAO encerra os recursos
     * JDBC que cria, mas não controla commit, rollback ou fechamento da Connection.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param movimentacaoFinanceira movimentação financeira que será persistida.
     * @return ID gerado pelo banco para a movimentação inserida.
     */
    public int inserir(Connection conn, MovimentacaoFinanceira movimentacaoFinanceira) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (movimentacaoFinanceira == null) {
            throw new IllegalArgumentException("Movimentação financeira não pode ser nula.");
        }

        String sql = """
                INSERT INTO MovimentacaoFinanceira (
                    data_hora,
                    tipo,
                    origem,
                    forma_pagamento,
                    valor,
                    venda_id,
                    conta_receber_id,
                    usuario_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, movimentacaoFinanceira.getDataHora().toString());
            stmt.setString(2, movimentacaoFinanceira.getTipo().name());
            stmt.setString(3, movimentacaoFinanceira.getOrigem().name());
            stmt.setString(4, movimentacaoFinanceira.getFormaPagamento().name());
            stmt.setBigDecimal(5, movimentacaoFinanceira.getValor());
            stmt.setInt(6, movimentacaoFinanceira.getVendaId());

            if (movimentacaoFinanceira.getContaReceberId() != null) {
                stmt.setInt(7, movimentacaoFinanceira.getContaReceberId());
            } else {
                stmt.setNull(7, Types.INTEGER);
            }

            stmt.setInt(8, movimentacaoFinanceira.getUsuarioId());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            throw new RuntimeException("Movimentação financeira inserida, mas o ID gerado não foi retornado pelo banco.");

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir movimentação financeira no banco de dados.", e);
        }
    }
    /**
     * Lista, em ordem de identificação, as movimentações vinculadas a uma venda.
     *
     * A Connection pertence ao Service e o DAO encerra somente os recursos JDBC
     * que cria. A escolha da movimentação relevante e a validação do cenário
     * financeiro permanecem no Service responsável pelo estorno.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param vendaId identificador da venda.
     * @return movimentações vinculadas à venda; lista vazia quando não houver nenhuma.
     */
    public List<MovimentacaoFinanceira> listarPorVendaId(
            Connection conn,
            Integer vendaId
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (vendaId == null || vendaId <= 0) {
            throw new IllegalArgumentException("ID da venda deve ser maior que zero.");
        }

        String sql = """
            SELECT id_movimentacao,
                   data_hora,
                   tipo,
                   origem,
                   forma_pagamento,
                   valor,
                   venda_id,
                   conta_receber_id,
                   usuario_id
            FROM MovimentacaoFinanceira
            WHERE venda_id = ?
            ORDER BY id_movimentacao
            """;

        List<MovimentacaoFinanceira> movimentacoes = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, vendaId);

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    int contaReceberId = rs.getInt("conta_receber_id");
                    Integer contaReceberIdMapeado = rs.wasNull() ? null : contaReceberId;

                    MovimentacaoFinanceira movimentacao =
                            new MovimentacaoFinanceira(
                                    rs.getInt("id_movimentacao"),
                                    LocalDateTime.parse(rs.getString("data_hora")),
                                    TipoMovimentacaoFinanceira.valueOf(rs.getString("tipo")),
                                    OrigemMovimentacaoFinanceira.valueOf(rs.getString("origem")),
                                    FormaPagamento.valueOf(rs.getString("forma_pagamento")),
                                    rs.getBigDecimal("valor"),
                                    rs.getInt("venda_id"),
                                    contaReceberIdMapeado,
                                    rs.getInt("usuario_id")
                            );

                    movimentacoes.add(movimentacao);
                }
                return movimentacoes;
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao listar movimentações financeiras da venda.",
                    e
            );
        }
    }

    /**
     * Lista as movimentações financeiras que compõem o relatório para o período
     * e os filtros informados.
     *
     * O período usa início inclusivo e limite final exclusivo. Tipo, origem e
     * forma de pagamento são filtros opcionais e somente acrescentam fragmentos
     * SQL fixos quando estiverem presentes. Todos os valores são vinculados por
     * PreparedStatement.
     *
     * A consulta usa MovimentacaoFinanceira como tabela principal e LEFT JOIN
     * com Usuario para obter o nome atual do responsável sem ocultar
     * silenciosamente uma movimentação cuja referência de usuário esteja
     * inconsistente. Não abre nem fecha a Connection recebida e não executa
     * autorização, soma de totais ou formatação visual.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param filtro fotografia imutável dos filtros aplicados ao relatório.
     * @param inicioInclusivo data e hora inicial inclusiva do período.
     * @param fimExclusivo data e hora final exclusiva do período.
     * @return linhas projetadas do relatório em ordem decrescente de data, hora
     *         e identificação; lista vazia quando nenhuma movimentação for encontrada.
     * @throws IllegalArgumentException quando a conexão, o filtro ou algum limite
     *                                  for inválido.
     * @throws IllegalStateException quando um registro persistido não puder ser
     *                               convertido para a projeção do relatório.
     * @throws RuntimeException quando ocorrer erro de acesso ao banco de dados.
     */
    public List<MovimentacaoFinanceiraRelatorioView> listarParaRelatorio(
            Connection conn,
            FiltroRelatorioMovimentacaoFinanceira filtro,
            LocalDateTime inicioInclusivo,
            LocalDateTime fimExclusivo
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (filtro == null) {
            throw new IllegalArgumentException(
                    "Filtro do relatório financeiro não pode ser nulo."
            );
        }

        filtro.validar();

        if (inicioInclusivo == null) {
            throw new IllegalArgumentException(
                    "Data e hora inicial do período são obrigatórias."
            );
        }

        if (fimExclusivo == null) {
            throw new IllegalArgumentException(
                    "Data e hora final do período são obrigatórias."
            );
        }

        if (!fimExclusivo.isAfter(inicioInclusivo)) {
            throw new IllegalArgumentException(
                    "O limite final do período deve ser posterior ao limite inicial."
            );
        }

        StringBuilder sql = new StringBuilder(
                """
                SELECT mf.id_movimentacao,
                       mf.data_hora,
                       mf.tipo,
                       mf.origem,
                       mf.forma_pagamento,
                       mf.valor,
                       mf.venda_id,
                       mf.conta_receber_id,
                       mf.usuario_id,
                       u.nome AS nome_responsavel
                FROM MovimentacaoFinanceira mf
                LEFT JOIN Usuario u
                       ON u.id_usuario = mf.usuario_id
                WHERE mf.data_hora >= ?
                  AND mf.data_hora < ?
                """
        );

        if (filtro.getTipo() != null) {
            sql.append("  AND mf.tipo = ?\n");
        }

        if (filtro.getOrigem() != null) {
            sql.append("  AND mf.origem = ?\n");
        }

        if (filtro.getFormaPagamento() != null) {
            sql.append("  AND mf.forma_pagamento = ?\n");
        }

        sql.append(
                """
                ORDER BY mf.data_hora DESC,
                         mf.id_movimentacao DESC
                """
        );

        List<MovimentacaoFinanceiraRelatorioView> movimentacoes =
                new ArrayList<>();

        try (PreparedStatement stmt =
                     conn.prepareStatement(sql.toString())) {

            int indiceParametro = 1;

            stmt.setString(
                    indiceParametro++,
                    inicioInclusivo.toString()
            );

            stmt.setString(
                    indiceParametro++,
                    fimExclusivo.toString()
            );

            if (filtro.getTipo() != null) {
                stmt.setString(
                        indiceParametro++,
                        filtro.getTipo().name()
                );
            }

            if (filtro.getOrigem() != null) {
                stmt.setString(
                        indiceParametro++,
                        filtro.getOrigem().name()
                );
            }

            if (filtro.getFormaPagamento() != null) {
                stmt.setString(
                        indiceParametro++,
                        filtro.getFormaPagamento().name()
                );
            }

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    Integer movimentacaoFinanceiraId = null;

                    try {
                        movimentacaoFinanceiraId =
                                rs.getInt("id_movimentacao");

                        int contaReceberId =
                                rs.getInt("conta_receber_id");

                        Integer contaReceberIdMapeado =
                                rs.wasNull()
                                        ? null
                                        : contaReceberId;

                        MovimentacaoFinanceiraRelatorioView movimentacao =
                                new MovimentacaoFinanceiraRelatorioView(
                                        movimentacaoFinanceiraId,
                                        LocalDateTime.parse(
                                                rs.getString("data_hora")
                                        ),
                                        TipoMovimentacaoFinanceira.valueOf(
                                                rs.getString("tipo")
                                        ),
                                        OrigemMovimentacaoFinanceira.valueOf(
                                                rs.getString("origem")
                                        ),
                                        FormaPagamento.valueOf(
                                                rs.getString("forma_pagamento")
                                        ),
                                        rs.getBigDecimal("valor"),
                                        rs.getInt("venda_id"),
                                        contaReceberIdMapeado,
                                        rs.getInt("usuario_id"),
                                        rs.getString("nome_responsavel")
                                );

                        movimentacoes.add(movimentacao);

                    } catch (RuntimeException e) {
                        String contextoIdentificacao =
                                movimentacaoFinanceiraId != null
                                        && movimentacaoFinanceiraId > 0
                                        ? " de ID "
                                        + movimentacaoFinanceiraId
                                        : "";

                        throw new IllegalStateException(
                                "Dados persistidos inválidos ao mapear "
                                        + "movimentação financeira"
                                        + contextoIdentificacao
                                        + " para o relatório.",
                                e
                        );
                    }
                }
            }

            return movimentacoes;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao listar movimentações financeiras "
                            + "para o relatório.",
                    e
            );
        }
    }

    /**
     * Calcula o valor financeiro líquido recebido no período informado.
     *
     * Soma as entradas originadas por vendas à vista e recebimentos de contas
     * e subtrai as saídas compensatórias originadas pelos respectivos estornos.
     * O período utiliza a data da movimentação financeira, com início inclusivo
     * e limite final exclusivo.
     *
     * O resultado pode ser positivo, zero ou negativo. Quando não houver
     * movimentações consideradas, retorna {@code 0.00}. O valor é normalizado
     * para escala 2 com arredondamento {@link RoundingMode#HALF_UP}, sem aplicar
     * qualquer formatação visual ou monetária.
     *
     * Usa a Connection recebida externamente e encerra somente o
     * PreparedStatement e o ResultSet criados pelo método. Não executa commit,
     * rollback nem fecha a Connection informada.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param inicioInclusivo data e hora inicial inclusiva do período.
     * @param fimExclusivo data e hora final exclusiva do período.
     * @return valor financeiro líquido do período, normalizado para escala 2.
     * @throws IllegalArgumentException quando a conexão ou algum limite do
     *                                  período for nulo, ou quando o limite final
     *                                  não for posterior ao limite inicial.
     * @throws IllegalStateException quando a consulta não retornar resultado.
     * @throws RuntimeException quando ocorrer erro de acesso ao banco de dados.
     */
    public BigDecimal calcularValorRecebidoLiquidoNoPeriodo(
            Connection conn,
            LocalDateTime inicioInclusivo,
            LocalDateTime fimExclusivo
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (inicioInclusivo == null) {
            throw new IllegalArgumentException(
                    "Data e hora inicial do período são obrigatórias."
            );
        }

        if (fimExclusivo == null) {
            throw new IllegalArgumentException(
                    "Data e hora final do período são obrigatórias."
            );
        }

        if (!fimExclusivo.isAfter(inicioInclusivo)) {
            throw new IllegalArgumentException(
                    "O limite final do período deve ser posterior ao limite inicial."
            );
        }

        String sql = """
                SELECT COALESCE(
                           SUM(
                               CASE
                                   WHEN tipo = ?
                                    AND origem IN (?, ?)
                                       THEN valor
                                   WHEN tipo = ?
                                    AND origem IN (?, ?)
                                       THEN -valor
                                   ELSE 0
                               END
                           ),
                           0
                       ) AS valor_recebido_liquido
                FROM MovimentacaoFinanceira
                WHERE data_hora >= ?
                  AND data_hora < ?
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, TipoMovimentacaoFinanceira.ENTRADA.name());
            stmt.setString(2, OrigemMovimentacaoFinanceira.VENDA_A_VISTA.name());
            stmt.setString(3, OrigemMovimentacaoFinanceira.RECEBIMENTO_CONTA.name());
            stmt.setString(4, TipoMovimentacaoFinanceira.SAIDA.name());
            stmt.setString(5, OrigemMovimentacaoFinanceira.ESTORNO_VENDA_A_VISTA.name());
            stmt.setString(6, OrigemMovimentacaoFinanceira.ESTORNO_RECEBIMENTO_CONTA.name());
            stmt.setString(7, inicioInclusivo.toString());
            stmt.setString(8, fimExclusivo.toString());

            try (ResultSet rs = stmt.executeQuery()) {

                if (!rs.next()) {
                    throw new IllegalStateException(
                            "A consulta do valor financeiro líquido não retornou resultado."
                    );
                }

                BigDecimal valorRecebidoLiquido =
                        rs.getBigDecimal("valor_recebido_liquido");

                if (valorRecebidoLiquido == null) {
                    valorRecebidoLiquido = BigDecimal.ZERO;
                }

                return valorRecebidoLiquido.setScale(
                        2,
                        RoundingMode.HALF_UP
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao calcular o valor financeiro líquido no período.",
                    e
            );
        }
    }
}
