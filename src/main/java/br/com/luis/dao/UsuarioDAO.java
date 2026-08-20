package br.com.luis.dao;

import br.com.luis.model.Usuario;
import br.com.luis.util.ConnectionFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

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
     * Lista todos os usuários em ordem estável de nome, login e ID.
     *
     * @return usuários cadastrados no banco.
     */
    public List<Usuario> listarTodos() {

        String sql = """
            SELECT id_usuario,
                   nome,
                   login,
                   senha,
                   perfil,
                   status,
                   troca_senha_obrigatoria
            FROM Usuario
            ORDER BY nome COLLATE NOCASE,
                     login COLLATE NOCASE,
                     id_usuario
            """;

        List<Usuario> usuarios = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                usuarios.add(new Usuario(
                        rs.getInt("id_usuario"),
                        rs.getString("nome"),
                        rs.getString("login"),
                        rs.getString("senha"),
                        rs.getString("perfil"),
                        rs.getString("status"),
                        lerTrocaSenhaObrigatoria(rs)
                ));
            }

            return usuarios;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erro ao listar usuários no banco de dados.",
                    e
            );
        }
    }

    /**
     * Verifica a existência de um login normalizado sem carregar a senha.
     *
     * @param login login que será consultado.
     * @return true quando o login já estiver cadastrado.
     */
    public boolean existeLogin(String login) {

        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException(
                    "Login é obrigatório para consulta."
            );
        }

        String sql = """
            SELECT 1
            FROM Usuario
            WHERE login = ?
            LIMIT 1
            """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, login.trim().toLowerCase());

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erro ao verificar a existência do login.",
                    e
            );
        }
    }

    /**
     * Verifica se o login pertence a outro usuário dentro da Connection
     * transacional recebida. A comparação é insensível a maiúsculas e minúsculas
     * e exclui o identificador informado. O DAO não executa commit, rollback nem
     * fecha a Connection, cujo ciclo pertence ao Service chamador.
     *
     * @param conn Connection externa que participa do fluxo transacional.
     * @param login login cuja disponibilidade será consultada.
     * @param usuarioIdExcluido identificador que não deve participar do conflito.
     * @return {@code true} quando o login pertence a outro usuário; caso contrário,
     *         {@code false}.
     * @throws IllegalArgumentException se a Connection, o login ou o identificador
     *                                  forem inválidos.
     * @throws IllegalStateException se a consulta JDBC falhar.
     */
    public boolean existeLoginParaOutroUsuario(
            Connection conn,
            String login,
            Integer usuarioIdExcluido
    ) {

        validarConexaoEUsuarioId(conn, usuarioIdExcluido);

        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException(
                    "Login é obrigatório para consulta."
            );
        }

        String sql = """
            SELECT 1
            FROM Usuario
            WHERE login COLLATE NOCASE = ?
              AND id_usuario <> ?
            LIMIT 1
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, login.trim().toLowerCase());
            stmt.setInt(2, usuarioIdExcluido);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erro ao verificar a disponibilidade do login.",
                    e
            );
        }
    }

    /**
     * Verifica se existe qualquer usuário com perfil administrativo.
     *
     * O status não participa da consulta, pois um administrador inativo ainda
     * representa uma instalação já configurada.
     *
     * @return true quando existir ao menos um usuário com perfil ADMIN.
     */
    public boolean existeAdministrador() {

        String sql = """
            SELECT 1
            FROM Usuario
            WHERE perfil = 'ADMIN'
            LIMIT 1
            """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            return rs.next();

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erro ao verificar a existência de administrador.",
                    e
            );
        }
    }

    /**
     * Altera somente o status quando o valor persistido ainda corresponde ao
     * estado conhecido pelo Service. O UPDATE protegido permite ao chamador
     * detectar concorrência pelo número de linhas afetadas. O DAO não executa
     * commit, rollback nem fecha a Connection recebida.
     *
     * @param conn Connection externa controlada pelo Service.
     * @param usuarioId identificador do usuário.
     * @param statusAtual status esperado no registro persistido.
     * @param novoStatus status que será persistido.
     * @return quantidade de registros atualizados.
     * @throws IllegalArgumentException se a Connection, o identificador ou os
     *                                  status forem inválidos.
     * @throws IllegalStateException se o UPDATE JDBC falhar.
     */
    public int atualizarStatus(
            Connection conn,
            Integer usuarioId,
            String statusAtual,
            String novoStatus
    ) {

        validarConexaoEUsuarioId(conn, usuarioId);

        if (statusAtual == null || statusAtual.isBlank()) {
            throw new IllegalArgumentException("Status atual é obrigatório.");
        }

        if (novoStatus == null || novoStatus.isBlank()) {
            throw new IllegalArgumentException("Novo status é obrigatório.");
        }

        String sql = """
            UPDATE Usuario
            SET status = ?
            WHERE id_usuario = ?
              AND status = ?
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, novoStatus);
            stmt.setInt(2, usuarioId);
            stmt.setString(3, statusAtual);

            return stmt.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erro ao atualizar o status do usuário.",
                    e
            );
        }
    }

    /**
     * Atualiza somente os dados cadastrais quando o snapshot anterior ainda
     * corresponde ao registro persistido. O login anterior é comparado sem
     * distinção entre maiúsculas e minúsculas. O DAO não executa commit, rollback
     * nem fecha a Connection recebida.
     *
     * @param conn Connection externa controlada pelo Service.
     * @param usuarioId identificador do usuário alvo.
     * @param nomeAtual nome esperado no registro persistido.
     * @param loginAtual login esperado no registro persistido.
     * @param perfilAtual perfil esperado no registro persistido.
     * @param novoNome nome que será persistido.
     * @param novoLogin login que será persistido.
     * @param novoPerfil perfil que será persistido.
     * @return quantidade de registros atualizados.
     * @throws IllegalArgumentException se a Connection ou o identificador forem inválidos.
     * @throws IllegalStateException se o UPDATE JDBC falhar.
     */
    public int atualizarDadosCadastrais(
            Connection conn,
            Integer usuarioId,
            String nomeAtual,
            String loginAtual,
            String perfilAtual,
            String novoNome,
            String novoLogin,
            String novoPerfil
    ) {

        validarConexaoEUsuarioId(conn, usuarioId);

        String sql = """
            UPDATE Usuario
            SET nome = ?,
                login = ?,
                perfil = ?
            WHERE id_usuario = ?
              AND nome = ?
              AND login COLLATE NOCASE = ?
              AND perfil = ?
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, novoNome);
            stmt.setString(2, novoLogin);
            stmt.setString(3, novoPerfil);
            stmt.setInt(4, usuarioId);
            stmt.setString(5, nomeAtual);
            stmt.setString(6, loginAtual);
            stmt.setString(7, perfilAtual);

            return stmt.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erro ao atualizar os dados cadastrais do usuário.",
                    e
            );
        }
    }

    /**
     * Conta administradores ativos dentro da transação do Service.
     */
    public int contarAdministradoresAtivos(Connection conn) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        String sql = """
            SELECT COUNT(*)
            FROM Usuario
            WHERE perfil = 'ADMIN'
              AND status = 'ATIVO'
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (!rs.next()) {
                throw new IllegalStateException(
                        "Não foi possível contar os administradores ativos."
                );
            }

            return rs.getInt(1);

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erro ao contar administradores ativos.",
                    e
            );
        }
    }

    /**
     * Persiste o novo hash e ativa a troca obrigatória sem alterar os demais
     * dados do usuário. O DAO não executa commit, rollback nem fecha a Connection
     * recebida do fluxo transacional do Service.
     *
     * @param conn Connection externa controlada pelo Service.
     * @param usuarioId identificador do usuário alvo.
     * @param novoHash hash da nova senha temporária.
     * @return quantidade de registros atualizados.
     * @throws IllegalArgumentException se a Connection, o identificador ou o hash
     *                                  forem inválidos.
     * @throws IllegalStateException se o UPDATE JDBC falhar.
     */
    public int redefinirSenhaAdministrativamente(
            Connection conn,
            Integer usuarioId,
            String novoHash
    ) {

        validarConexaoEUsuarioId(conn, usuarioId);

        if (novoHash == null || novoHash.isBlank()) {
            throw new IllegalArgumentException(
                    "Novo hash de senha é obrigatório."
            );
        }

        String sql = """
            UPDATE Usuario
            SET senha = ?,
                troca_senha_obrigatoria = 1
            WHERE id_usuario = ?
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, novoHash);
            stmt.setInt(2, usuarioId);

            return stmt.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erro ao redefinir administrativamente a senha.",
                    e
            );
        }
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
     * Altera somente a senha de um usuário já liberado para o acesso normal.
     *
     * O hash atual participa da condição do UPDATE para impedir que uma sessão
     * desatualizada sobrescreva uma senha alterada por outro fluxo. O método usa
     * a conexão externa e não controla commit, rollback ou seu fechamento.
     *
     * @param conn conexão externa controlada pelo Service.
     * @param usuarioId identificador do usuário logado.
     * @param hashAtual hash conhecido pela sessão atual.
     * @param novoHash novo hash BCrypt que substituirá o atual.
     * @return quantidade de registros atualizados.
     */
    public int alterarSenhaVoluntariamente(
            Connection conn,
            Integer usuarioId,
            String hashAtual,
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

        if (hashAtual == null || hashAtual.isBlank()) {
            throw new IllegalArgumentException(
                    "Hash atual de senha é obrigatório."
            );
        }

        if (novoHash == null || novoHash.isBlank()) {
            throw new IllegalArgumentException(
                    "Novo hash de senha é obrigatório."
            );
        }

        String sql = """
            UPDATE Usuario
            SET senha = ?
            WHERE id_usuario = ?
              AND senha = ?
              AND status = 'ATIVO'
              AND troca_senha_obrigatoria = 0
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, novoHash);
            stmt.setInt(2, usuarioId);
            stmt.setString(3, hashAtual);

            return stmt.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erro ao alterar voluntariamente a senha no banco.",
                    e
            );
        }
    }

    private void validarConexaoEUsuarioId(
            Connection conn,
            Integer usuarioId
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException(
                    "ID do usuário deve ser maior que zero."
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
