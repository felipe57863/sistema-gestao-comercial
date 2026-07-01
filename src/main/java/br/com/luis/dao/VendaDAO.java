package br.com.luis.dao;

import br.com.luis.model.Venda;
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
 * Esta classe não contém regra de negócio.
 * Sua responsabilidade é apenas inserir, consultar ou atualizar dados
 * relacionados à tabela Venda.
 */
public class VendaDAO {

    /**
     * Insere uma nova venda no banco de dados.
     *
     * Este método persiste apenas os dados principais da venda.
     * Os itens da venda serão persistidos pelo ItemVendaDAO em outro passo.
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
}