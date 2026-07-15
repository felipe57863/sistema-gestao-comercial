package br.com.luis.dao;

import br.com.luis.model.Venda;
import br.com.luis.model.StatusVenda;
import br.com.luis.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

/**
 * DAO responsável pela persistência da entidade Venda.
 *
 * Insere os dados principais da venda e retorna o identificador gerado. Os itens
 * associados são persistidos separadamente pelo ItemVendaDAO.
 *
 * Não contém regras de finalização, estoque ou financeiro. Essas regras e a
 * coordenação transacional pertencem ao VendaService.
 */
public class VendaDAO {

    /**
     * Insere uma nova venda no banco de dados.
     *
     * Abre e fecha sua própria Connection e usa try-with-resources para encerrar
     * o PreparedStatement e o ResultSet. Persiste apenas os dados principais;
     * os itens vinculados são inseridos separadamente pelo ItemVendaDAO.
     *
     * @param venda venda que será persistida.
     * @return ID gerado pelo banco para a venda inserida.
     */
    public int inserir(Venda venda) {
        String sql = """
                INSERT INTO Venda (
                    data_hora,
                    tipo_venda,
                    forma_pagamento,
                    valor_total,
                    valor_desconto_global,
                    status,
                    usuario_id,
                    cliente_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, venda.getDataHora().toString());
            stmt.setString(2, venda.getTipoVenda());
            stmt.setString(3, venda.getFormaPagamento());
            stmt.setBigDecimal(4, venda.getValorTotal());
            stmt.setBigDecimal(5, venda.getValorDescontoGlobal());
            stmt.setString(6, venda.getStatus());
            stmt.setInt(7, venda.getUsuarioId());

            if (venda.getClienteId() != null) {
                stmt.setInt(8, venda.getClienteId());
            } else {
                stmt.setNull(8, Types.INTEGER);
            }

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            throw new RuntimeException("Venda inserida, mas o ID gerado não foi retornado pelo banco.");

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir venda no banco de dados.", e);
        }
    }

    /**
     * Insere uma nova venda usando uma Connection externa.
     *
     * Participa da mesma transação de finalização coordenada pelo VendaService.
     * Encerra o PreparedStatement e o ResultSet que cria, mas respeita a
     * propriedade da Connection recebida.
     *
     * Importante:
     * - não abre nova Connection;
     * - não executa commit;
     * - não executa rollback;
     * - não fecha a Connection recebida.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param venda venda que será persistida.
     * @return ID gerado pelo banco para a venda inserida.
     */
    public int inserir(Connection conn, Venda venda) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (venda == null) {
            throw new IllegalArgumentException("Venda não pode ser nula.");
        }

        String sql = """
                INSERT INTO Venda (
                    data_hora,
                    tipo_venda,
                    forma_pagamento,
                    valor_total,
                    valor_desconto_global,
                    status,
                    usuario_id,
                    cliente_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, venda.getDataHora().toString());
            stmt.setString(2, venda.getTipoVenda());
            stmt.setString(3, venda.getFormaPagamento());
            stmt.setBigDecimal(4, venda.getValorTotal());
            stmt.setBigDecimal(5, venda.getValorDescontoGlobal());
            stmt.setString(6, venda.getStatus());
            stmt.setInt(7, venda.getUsuarioId());

            if (venda.getClienteId() != null) {
                stmt.setInt(8, venda.getClienteId());
            } else {
                stmt.setNull(8, Types.INTEGER);
            }

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            throw new RuntimeException("Venda inserida, mas o ID gerado não foi retornado pelo banco.");

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir venda no banco de dados usando conexão externa.", e);
        }
    }

    /**
     * Atualiza o status de uma venda somente quando o status atual persistido
     * corresponde ao status esperado.
     *
     * Usa uma Connection externa e encerra apenas o PreparedStatement criado.
     * Não executa commit, rollback nem fecha a Connection recebida.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param vendaId identificador da venda que será atualizada.
     * @param statusAtual status atual esperado para a venda.
     * @param novoStatus novo status que será persistido.
     * @return {@code true} quando exatamente uma venda for atualizada;
     *         {@code false} quando nenhuma venda corresponder ao ID e status atual.
     */
    public boolean atualizarStatus(
            Connection conn,
            Integer vendaId,
            StatusVenda statusAtual,
            StatusVenda novoStatus
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (vendaId == null || vendaId <= 0) {
            throw new IllegalArgumentException("ID da venda deve ser maior que zero.");
        }

        if (statusAtual == null) {
            throw new IllegalArgumentException("Status atual não pode ser nulo.");
        }

        if (novoStatus == null) {
            throw new IllegalArgumentException("Novo status não pode ser nulo.");
        }

        if (statusAtual == novoStatus) {
            throw new IllegalArgumentException("Status atual e novo status devem ser diferentes.");
        }

        String sql = """
                UPDATE Venda
                SET status = ?
                WHERE id_venda = ?
                  AND status = ?
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, novoStatus.name());
            stmt.setInt(2, vendaId);
            stmt.setString(3, statusAtual.name());

            int linhasAfetadas = stmt.executeUpdate();

            if (linhasAfetadas > 1) {
                throw new IllegalStateException("Mais de uma venda foi atualizada para o mesmo ID.");
            }

            return linhasAfetadas == 1;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar o status da venda no banco de dados.", e);
        }
    }
}
