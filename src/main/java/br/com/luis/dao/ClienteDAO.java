package br.com.luis.dao;

import br.com.luis.model.Cliente;
import br.com.luis.model.PrazoPagamento;
import br.com.luis.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO da entidade Cliente.
 * Responsável pela persistência e leitura de dados no SQLite.
 */
public class ClienteDAO {

    /**
     * Insere um novo cliente no banco.
     */
    public void cadastrar(Cliente cliente) {

        String sql = "INSERT INTO Cliente (nome, documento, tipo_cliente, limite_credito, status, prazo_pagamento_id) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, cliente.getNome());
            stmt.setString(2, cliente.getDocumento());

            // ⚠️ SQLite não possui ENUM → armazenamos como texto
            stmt.setString(3, cliente.getTipo().name());

            // JDBC converte BigDecimal corretamente
            stmt.setBigDecimal(4, cliente.getLimiteCredito());

            stmt.setString(5, cliente.getStatus().name());

            // Validação defensiva da FK
            if (cliente.getPrazoPagamento() == null || cliente.getPrazoPagamento().getIdPrazo() == null) {
                throw new IllegalArgumentException("Prazo de pagamento inválido.");
            }

            stmt.setInt(6, cliente.getPrazoPagamento().getIdPrazo());

            stmt.executeUpdate();

            // Recupera ID gerado
            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    cliente.setIdCliente(rs.getInt(1));
                }
            }

            // Log
            System.out.println("[LOG] Cliente cadastrado: " + cliente.getNome());

        } catch (SQLException e) {

            // Tratamento de duplicidade (UNIQUE)
            if (e.getMessage().contains("UNIQUE")) {
                throw new RuntimeException("Já existe um cliente com esse documento.");
            }

            throw new RuntimeException("Erro ao cadastrar cliente.", e);
        }
    }

    /**
     * Lista todos os clientes com seus respectivos prazos de pagamento.
     */
    public List<Cliente> listarTodos() {

        String sql = """
            SELECT c.id_cliente, c.nome, c.documento, c.tipo_cliente, c.limite_credito, c.status,
                   p.id_prazo, p.descricao AS prazo_descricao, p.quantidade_dias, p.ativo
            FROM Cliente c
            INNER JOIN PrazoPagamento p ON c.prazo_pagamento_id = p.id_prazo
            ORDER BY c.nome
        """;

        List<Cliente> clientes = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {

            while (rs.next()) {

                // Reconstrói o PrazoPagamento
                PrazoPagamento prazo = new PrazoPagamento(
                        rs.getInt("id_prazo"),
                        rs.getString("prazo_descricao"),
                        rs.getInt("quantidade_dias"),
                        rs.getInt("ativo") == 1
                );

                // Reconstrói o Cliente com ENUMs
                Cliente cliente = new Cliente(
                        rs.getInt("id_cliente"),
                        rs.getString("nome"),
                        rs.getString("documento"),
                        Cliente.TipoCliente.valueOf(rs.getString("tipo_cliente")),
                        rs.getBigDecimal("limite_credito"),
                        Cliente.StatusCliente.valueOf(rs.getString("status")),
                        prazo
                );

                clientes.add(cliente);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar clientes.", e);
        }

        return clientes;
    }
}