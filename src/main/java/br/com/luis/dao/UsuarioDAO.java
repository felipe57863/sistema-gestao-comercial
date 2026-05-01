package br.com.luis.dao;

import br.com.luis.model.Usuario;
import br.com.luis.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DAO da entidade Usuario.
 */
public class UsuarioDAO {

    public void cadastrar(Usuario usuario) {

        if (usuario == null) {
            throw new IllegalArgumentException("Usuário é obrigatório para cadastro.");
        }

        String sql = "INSERT INTO Usuario (nome, login, senha, perfil, status) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, usuario.getNome());
            stmt.setString(2, usuario.getLogin().trim().toLowerCase());
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

            String mensagemErro = e.getMessage();

            if (mensagemErro != null && mensagemErro.contains("UNIQUE")) {
                throw new RuntimeException("Já existe um usuário com esse login.");
            }

            throw new RuntimeException("Erro ao cadastrar usuário no banco.", e);
        }
    }

    public Usuario buscarPorLogin(String login) {

        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Login é obrigatório para busca.");
        }

        String sql = """
            SELECT id_usuario, nome, login, senha, perfil, status
            FROM Usuario
            WHERE login = ?
        """;

        String loginFormatado = login.trim().toLowerCase();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, loginFormatado);

            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Usuario(
                            rs.getInt("id_usuario"),
                            rs.getString("nome"),
                            rs.getString("login"),
                            rs.getString("senha"),
                            rs.getString("perfil"),
                            rs.getString("status")
                    );
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar usuário pelo login.", e);
        }

        return null;
    }
}