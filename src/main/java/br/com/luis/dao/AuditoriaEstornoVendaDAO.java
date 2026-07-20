package br.com.luis.dao;

import br.com.luis.model.AuditoriaEstornoVenda;
import br.com.luis.model.StatusContaReceber;
import br.com.luis.model.StatusVenda;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;

/**
 * DAO responsável pela persistência e pelas consultas somente leitura
 * da auditoria de estorno de venda.
 *
 * Os registros de auditoria são imutáveis no fluxo normal. Este DAO não
 * disponibiliza operações de atualização ou exclusão.
 */
public class AuditoriaEstornoVendaDAO {

    /**
     * Insere um registro de auditoria usando uma Connection externa.
     *
     * Participa da mesma transação do estorno coordenada pelo
     * EstornoVendaService. Encerra somente o PreparedStatement e o ResultSet
     * criados pelo método.
     *
     * Importante:
     * - não abre nova Connection;
     * - não executa commit;
     * - não executa rollback;
     * - não fecha a Connection recebida.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param auditoria registro de auditoria que será persistido.
     * @return identificador gerado para a auditoria.
     */
    public int inserir(
            Connection conn,
            AuditoriaEstornoVenda auditoria
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (auditoria == null) {
            throw new IllegalArgumentException(
                    "Auditoria do estorno não pode ser nula."
            );
        }

        if (auditoria.getIdAuditoria() != null) {
            throw new IllegalArgumentException(
                    "Nova auditoria do estorno não deve possuir ID preenchido."
            );
        }

        String sql = """
                INSERT INTO AuditoriaEstornoVenda (
                    venda_id,
                    usuario_id,
                    data_hora,
                    motivo,
                    status_venda_anterior,
                    conta_receber_id,
                    status_conta_receber_anterior,
                    movimentacao_original_id,
                    movimentacao_saida_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
        )) {

            stmt.setInt(1, auditoria.getVendaId());
            stmt.setInt(2, auditoria.getUsuarioId());
            stmt.setString(3, auditoria.getDataHora().toString());
            stmt.setString(4, auditoria.getMotivo());
            stmt.setString(5, auditoria.getStatusVendaAnterior().name());

            if (auditoria.getContaReceberId() != null) {
                stmt.setInt(6, auditoria.getContaReceberId());
            } else {
                stmt.setNull(6, Types.INTEGER);
            }

            if (auditoria.getStatusContaReceberAnterior() != null) {
                stmt.setString(
                        7,
                        auditoria.getStatusContaReceberAnterior().name()
                );
            } else {
                stmt.setNull(7, Types.VARCHAR);
            }

            if (auditoria.getMovimentacaoOriginalId() != null) {
                stmt.setInt(8, auditoria.getMovimentacaoOriginalId());
            } else {
                stmt.setNull(8, Types.INTEGER);
            }

            if (auditoria.getMovimentacaoSaidaId() != null) {
                stmt.setInt(9, auditoria.getMovimentacaoSaidaId());
            } else {
                stmt.setNull(9, Types.INTEGER);
            }

            int linhasAfetadas = stmt.executeUpdate();

            if (linhasAfetadas != 1) {
                throw new IllegalStateException(
                        "A inserção da auditoria do estorno não afetou "
                                + "exatamente uma linha."
                );
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int idGerado = rs.getInt(1);

                    if (idGerado <= 0) {
                        throw new IllegalStateException(
                                "O banco retornou um ID inválido para a auditoria do estorno."
                        );
                    }

                    return idGerado;
                }
            }

            throw new IllegalStateException(
                    "Auditoria do estorno inserida, mas o ID gerado "
                            + "não foi retornado pelo banco."
            );

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao inserir auditoria do estorno no banco de dados.",
                    e
            );
        }
    }

    /**
     * Busca a auditoria de estorno vinculada a uma venda usando uma
     * Connection externa.
     *
     * Participa da transação controlada pela camada Service e encerra somente
     * o PreparedStatement e o ResultSet criados pelo método.
     *
     * A tabela garante uma única auditoria por venda.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param vendaId identificador da venda.
     * @return auditoria encontrada ou {@code null} quando a venda ainda não
     *         possuir auditoria de estorno.
     */
    public AuditoriaEstornoVenda buscarPorVendaId(
            Connection conn,
            Integer vendaId
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (vendaId == null || vendaId <= 0) {
            throw new IllegalArgumentException(
                    "ID da venda deve ser maior que zero."
            );
        }

        String sql = """
                SELECT id_auditoria,
                       venda_id,
                       usuario_id,
                       data_hora,
                       motivo,
                       status_venda_anterior,
                       conta_receber_id,
                       status_conta_receber_anterior,
                       movimentacao_original_id,
                       movimentacao_saida_id
                FROM AuditoriaEstornoVenda
                WHERE venda_id = ?
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, vendaId);

            try (ResultSet rs = stmt.executeQuery()) {

                if (!rs.next()) {
                    return null;
                }

                int contaReceberId = rs.getInt("conta_receber_id");
                Integer contaReceberIdMapeado =
                        rs.wasNull() ? null : contaReceberId;

                String statusContaAnterior =
                        rs.getString("status_conta_receber_anterior");

                int movimentacaoOriginalId =
                        rs.getInt("movimentacao_original_id");
                Integer movimentacaoOriginalIdMapeado =
                        rs.wasNull() ? null : movimentacaoOriginalId;

                int movimentacaoSaidaId =
                        rs.getInt("movimentacao_saida_id");
                Integer movimentacaoSaidaIdMapeado =
                        rs.wasNull() ? null : movimentacaoSaidaId;

                return new AuditoriaEstornoVenda(
                        rs.getInt("id_auditoria"),
                        rs.getInt("venda_id"),
                        rs.getInt("usuario_id"),
                        LocalDateTime.parse(rs.getString("data_hora")),
                        rs.getString("motivo"),
                        StatusVenda.valueOf(
                                rs.getString("status_venda_anterior")
                        ),
                        contaReceberIdMapeado,
                        statusContaAnterior == null
                                ? null
                                : StatusContaReceber.valueOf(
                                statusContaAnterior
                        ),
                        movimentacaoOriginalIdMapeado,
                        movimentacaoSaidaIdMapeado
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao buscar auditoria de estorno pela venda.",
                    e
            );
        }
    }
}