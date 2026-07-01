package br.com.luis.dao;

import br.com.luis.model.MovimentacaoFinanceira;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

/**
 * DAO responsável pela persistência da entidade MovimentacaoFinanceira.
 *
 * Esta classe não contém regra de negócio.
 * Sua responsabilidade é apenas inserir e consultar dados
 * relacionados à tabela MovimentacaoFinanceira.
 *
 * Regra importante:
 * MovimentacaoFinanceira é imutável.
 * Portanto, este DAO não deve possuir métodos de update ou delete.
 */
public class MovimentacaoFinanceiraDAO {

    /**
     * Insere uma movimentação financeira usando uma Connection externa.
     *
     * Este método foi preparado para participar da mesma transação
     * da finalização da venda.
     *
     * Importante:
     * - não abre nova conexão;
     * - não faz commit;
     * - não faz rollback;
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
}