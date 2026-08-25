package br.com.luis.dao;

import br.com.luis.model.EntradaEstoque;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

/**
 * Persiste entradas de estoque usando uma Connection externa.
 */
public class EntradaEstoqueDAO {

    /**
     * Insere o cabeçalho da entrada na transação controlada pela camada Service.
     * Encerra apenas o PreparedStatement e o ResultSet que cria.
     */
    public int inserir(Connection conn, EntradaEstoque entradaEstoque) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (entradaEstoque == null) {
            throw new IllegalArgumentException("Entrada de estoque não pode ser nula.");
        }

        if (entradaEstoque.getDataHora() == null) {
            throw new IllegalArgumentException("Data e hora da entrada não podem ser nulas.");
        }

        if (entradaEstoque.getUsuarioId() == null || entradaEstoque.getUsuarioId() <= 0) {
            throw new IllegalArgumentException("ID do usuário deve ser maior que zero.");
        }

        if (entradaEstoque.getNomeUsuario() == null
                || entradaEstoque.getNomeUsuario().isBlank()) {
            throw new IllegalArgumentException("Nome do usuário não pode ser vazio.");
        }

        String sql = """
                INSERT INTO EntradaEstoque (
                    data_hora,
                    usuario_id,
                    nome_usuario,
                    referencia,
                    observacao
                ) VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt =
                     conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, entradaEstoque.getDataHora().toString());
            stmt.setInt(2, entradaEstoque.getUsuarioId());
            stmt.setString(3, entradaEstoque.getNomeUsuario());

            if (entradaEstoque.getReferencia() != null) {
                stmt.setString(4, entradaEstoque.getReferencia());
            } else {
                stmt.setNull(4, Types.VARCHAR);
            }

            if (entradaEstoque.getObservacao() != null) {
                stmt.setString(5, entradaEstoque.getObservacao());
            } else {
                stmt.setNull(5, Types.VARCHAR);
            }

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int idGerado = rs.getInt(1);

                    if (idGerado > 0) {
                        return idGerado;
                    }
                }
            }

            throw new IllegalStateException(
                    "Entrada de estoque inserida, mas o ID gerado não foi retornado."
            );

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao inserir entrada de estoque usando conexão externa.",
                    e
            );
        }
    }
}
