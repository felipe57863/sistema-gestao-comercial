package br.com.luis.dao;

import br.com.luis.model.MovimentacaoFinanceira;
import br.com.luis.model.FormaPagamento;
import br.com.luis.model.OrigemMovimentacaoFinanceira;
import br.com.luis.model.TipoMovimentacaoFinanceira;

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
     * Insere uma nova movimentação financeira usando a Connection informada.
     *
     * Participa da transação coordenada pelo Service que originou o lançamento,
     * seja a finalização de uma venda à vista, o recebimento integral de uma conta
     * ou uma compensação de estorno. Encerra o PreparedStatement e o ResultSet que
     * cria, mas respeita a propriedade da Connection recebida. Lançamentos
     * anteriores permanecem preservados.
     *
     * Importante:
     * - não abre nova Connection;
     * - não executa commit;
     * - não executa rollback;
     * - não fecha a Connection recebida.
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
     * Lista todas as movimentações financeiras vinculadas a uma venda usando
     * uma Connection externa.
     *
     * Participa da transação controlada pela camada Service e encerra somente
     * o PreparedStatement e o ResultSet criados pelo método.
     *
     * O DAO não escolhe a movimentação original nem valida o cenário financeiro.
     * Essas decisões pertencem ao Service responsável pelo estorno.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param vendaId identificador da venda.
     * @return movimentações vinculadas à venda, em ordem de identificação;
     *         lista vazia quando nenhuma movimentação for encontrada.
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
}