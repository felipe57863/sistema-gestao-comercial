package br.com.luis.dao;

import br.com.luis.model.Usuario;
import br.com.luis.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * DAO da entidade Usuario.
 */
public class UsuarioDAO {

    public void cadastrar(Usuario usuario) {

        String sql = "INSERT INTO Usuario (nome, login, senha, perfil, status) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, usuario.getNome());
            stmt.setString(2, usuario.getLogin());
            stmt.setString(3, usuario.getSenha());
            stmt.setString(4, usuario.getPerfil());
            stmt.setString(5, usuario.getStatus());

            stmt.executeUpdate();

            // Recupera o ID gerado pelo banco
            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    usuario.setIdUsuario(rs.getInt(1));
                }
            }

            System.out.println("[LOG] Usuário cadastrado com sucesso: " + usuario.getLogin());

        } catch (SQLException e) {

            if (e.getMessage().contains("UNIQUE")) {
                throw new RuntimeException("Já existe um usuário com esse login.");
            }

            throw new RuntimeException("Erro ao cadastrar usuário no banco.", e);
        }
    }
}