package br.com.luis.dao;

import br.com.luis.model.ContaReceber;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DAO responsável pela persistência da entidade ContaReceber.
 *
 * Esta classe não contém regra de negócio.
 * Sua responsabilidade é apenas inserir e consultar dados
 * relacionados à tabela ContaReceber.
 */
public class ContaReceberDAO {

    /**
     * Insere uma conta a receber usando uma Connection externa.
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
     * @param contaReceber conta a receber que será persistida.
     * @return ID gerado pelo banco para a conta inserida.
     */
    public int inserir(Connection conn, ContaReceber contaReceber) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (contaReceber == null) {
            throw new IllegalArgumentException("Conta a receber não pode ser nula.");
        }

        String sql = """
                INSERT INTO ContaReceber (
                    valor,
                    data_vencimento,
                    status,
                    venda_id,
                    cliente_id,
                    prazo_pagamento_id,
                    quantidade_dias_prazo,
                    data_criacao
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setBigDecimal(1, contaReceber.getValor());
            stmt.setString(2, contaReceber.getDataVencimento().toString());
            stmt.setString(3, contaReceber.getStatus().name());
            stmt.setInt(4, contaReceber.getVendaId());
            stmt.setInt(5, contaReceber.getClienteId());
            stmt.setInt(6, contaReceber.getPrazoPagamentoId());
            stmt.setInt(7, contaReceber.getQuantidadeDiasPrazo());
            stmt.setString(8, contaReceber.getDataCriacao().toString());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            throw new RuntimeException("Conta a receber inserida, mas o ID gerado não foi retornado pelo banco.");

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir conta a receber no banco de dados.", e);
        }
    }
}