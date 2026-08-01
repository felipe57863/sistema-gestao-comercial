package br.com.luis.dao;

import br.com.luis.model.Usuario;
import br.com.luis.util.ConnectionFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

/**
 * DAO responsável pela persistência e consulta de usuários por JDBC.
 *
 * Recebe do Service o hash de senha já preparado e recupera os dados usados no
 * fluxo de autenticação. Não verifica a senha com BCrypt, não decide se o login
 * será aceito e não controla SessaoUsuario, componentes da interface ou navegação.
 */
public class UsuarioDAO {

    /**
     * Persiste um usuário usando conexão própria e atualiza o objeto com o ID gerado.
     *
     * A senha recebida já deve estar representada pelo hash preparado na camada
     * Service; o DAO apenas grava os valores informados.
     *
     * @param usuario usuário que será persistido.
     */
    public void cadastrar(Usuario usuario) {

        if (usuario == null) {
            throw new IllegalArgumentException("Usuário é obrigatório para cadastro.");
        }

        String sql = """
            INSERT INTO Usuario (
                nome,
                login,
                senha,
                perfil,
                status,
                troca_senha_obrigatoria
            ) VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, usuario.getNome());
            stmt.setString(2, usuario.getLogin().trim().toLowerCase());
            stmt.setString(3, usuario.getSenha());
            stmt.setString(4, usuario.getPerfil());
            stmt.setString(5, usuario.getStatus());
            stmt.setInt(6, usuario.isTrocaSenhaObrigatoria() ? 1 : 0);

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

    /**
     * Busca um usuário pelo identificador usando uma Connection externa.
     *
     * Participa da transação controlada pela camada Service e encerra somente
     * o PreparedStatement e o ResultSet criados pelo método.
     *
     * O DAO apenas consulta e reconstrói o usuário. A validação de usuário ativo
     * e perfil administrativo pertence ao Service responsável pelo estorno.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param usuarioId identificador do usuário.
     * @return usuário encontrado ou {@code null} quando não existir.
     */
    public Usuario buscarPorId(
            Connection conn,
            Integer usuarioId
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException("ID do usuário deve ser maior que zero.");
        }

        String sql = """
            SELECT id_usuario,
                   nome,
                   login,
                   senha,
                   perfil,
                   status,
                   troca_senha_obrigatoria
            FROM Usuario
            WHERE id_usuario = ?
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, usuarioId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Usuario(
                            rs.getInt("id_usuario"),
                            rs.getString("nome"),
                            rs.getString("login"),
                            rs.getString("senha"),
                            rs.getString("perfil"),
                            rs.getString("status"),
                            lerTrocaSenhaObrigatoria(rs)
                    );
                }
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao buscar usuário por ID no banco de dados.",
                    e
            );
        }
    }
    /**
     * Busca um usuário pelo login normalizado usando conexão própria.
     *
     * Retorna os dados necessários ao Service de autenticação, mas não compara a
     * senha, não valida a aceitação do acesso e não altera a sessão da aplicação.
     *
     * @param login login que será normalizado e consultado.
     * @return usuário encontrado ou {@code null} quando não existir.
     */
    public Usuario buscarPorLogin(String login) {

        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Login é obrigatório para busca.");
        }

        String sql = """
            SELECT id_usuario,
                   nome,
                   login,
                   senha,
                   perfil,
                   status,
                   troca_senha_obrigatoria
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
                            rs.getString("status"),
                            lerTrocaSenhaObrigatoria(rs)
                    );
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar usuário pelo login.", e);
        }

        return null;
    }

    /**
     * Persiste o novo hash e encerra a troca obrigatória na mesma instrução SQL.
     *
     * O método participa da transação controlada pelo Service: não abre outra
     * Connection, não executa commit, não executa rollback e não fecha a conexão
     * recebida. A condição do UPDATE impede a conclusão para usuário inativo ou
     * para um indicador que já não esteja pendente.
     *
     * @param conn conexão externa controlada pelo Service.
     * @param usuarioId identificador do usuário autenticado.
     * @param novoHash novo hash BCrypt que substituirá o anterior.
     * @return quantidade de registros atualizados.
     */
    public int concluirTrocaSenhaObrigatoria(
            Connection conn,
            Integer usuarioId,
            String novoHash
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException(
                    "ID do usuário deve ser maior que zero."
            );
        }

        if (novoHash == null || novoHash.isBlank()) {
            throw new IllegalArgumentException(
                    "Novo hash de senha é obrigatório."
            );
        }

        String sql = """
            UPDATE Usuario
            SET senha = ?,
                troca_senha_obrigatoria = 0
            WHERE id_usuario = ?
              AND status = 'ATIVO'
              AND troca_senha_obrigatoria = 1
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, novoHash);
            stmt.setInt(2, usuarioId);

            return stmt.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erro ao concluir a troca obrigatória de senha no banco.",
                    e
            );
        }
    }

    /**
     * Converte rigorosamente o INTEGER do SQLite para boolean.
     *
     * A leitura como Object evita conversões permissivas do JDBC, como truncar
     * um valor fracionário. Assim, somente os valores numéricos exatos 0 e 1
     * são aceitos.
     */
    private boolean lerTrocaSenhaObrigatoria(ResultSet rs) throws SQLException {

        Object valorPersistido = rs.getObject("troca_senha_obrigatoria");

        if (valorPersistido == null) {
            throw new IllegalStateException(
                    "O indicador de troca obrigatória de senha não pode ser nulo."
            );
        }

        if (!(valorPersistido instanceof Number valorNumerico)) {
            throw new IllegalStateException(
                    "O indicador de troca obrigatória de senha deve ser numérico."
            );
        }

        BigDecimal valor;

        try {
            valor = new BigDecimal(valorNumerico.toString());
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Valor numérico inválido para troca_senha_obrigatoria.",
                    e
            );
        }

        if (valor.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }

        if (valor.compareTo(BigDecimal.ONE) == 0) {
            return true;
        }

        throw new IllegalStateException(
                "Valor inválido para troca_senha_obrigatoria: " + valorPersistido
        );
    }
}
