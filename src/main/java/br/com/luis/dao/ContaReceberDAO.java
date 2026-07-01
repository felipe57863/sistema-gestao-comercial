package br.com.luis.dao;

import br.com.luis.model.ContaReceber;
import br.com.luis.model.StatusContaReceber;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /**
     * Soma o total pendente de contas a receber de um cliente.
     *
     * Regra da Fase 5:
     * o limite disponível deve considerar somente contas com status PENDENTE.
     *
     * Importante:
     * - não abre nova conexão;
     * - não faz commit;
     * - não faz rollback;
     * - não fecha a Connection recebida.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param clienteId ID do cliente.
     * @return soma dos valores pendentes do cliente.
     */
    public BigDecimal somarTotalPendentePorCliente(Connection conn, Integer clienteId) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (clienteId == null || clienteId <= 0) {
            throw new IllegalArgumentException("ID do cliente inválido.");
        }

        String sql = """
                SELECT COALESCE(SUM(valor), 0) AS total_pendente
                FROM ContaReceber
                WHERE cliente_id = ?
                  AND status = ?
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, clienteId);
            stmt.setString(2, StatusContaReceber.PENDENTE.name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal totalPendente = rs.getBigDecimal("total_pendente");

                    if (totalPendente == null) {
                        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    }

                    return totalPendente.setScale(2, RoundingMode.HALF_UP);
                }
            }

            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao somar total pendente do cliente.", e);
        }
    }
}