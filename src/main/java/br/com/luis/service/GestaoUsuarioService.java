package br.com.luis.service;

import br.com.luis.dao.UsuarioDAO;
import br.com.luis.model.Usuario;
import br.com.luis.util.ConnectionFactory;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/**
 * Concentra as regras da gestão administrativa de usuários.
 *
 * O administrador é recebido explicitamente em cada operação. O Service não
 * acessa a sessão, permitindo que a interface preserve a sessão já existente
 * sem criar ou substituir usuários logados.
 */
public class GestaoUsuarioService {

    private static final int TAMANHO_MINIMO_SENHA = 8;
    private static final int CUSTO_BCRYPT = 12;

    private final UsuarioDAO usuarioDAO;

    public GestaoUsuarioService() {
        this.usuarioDAO = new UsuarioDAO();
    }

    /**
     * Lista os usuários após validar o administrador executor.
     */
    public List<Usuario> listarUsuarios(Usuario administrador) {
        validarAdministrador(administrador);
        return usuarioDAO.listarTodos();
    }

    /**
     * Cadastra um usuário ativo com senha temporária e troca obrigatória.
     */
    public Usuario cadastrarUsuario(
            Usuario administrador,
            String nome,
            String login,
            String perfil,
            String senhaTemporaria,
            String confirmacao
    ) {

        validarAdministrador(administrador);

        String nomeNormalizado = normalizarCampoObrigatorio(
                nome,
                "Nome é obrigatório."
        );
        String loginNormalizado = normalizarCampoObrigatorio(
                login,
                "Login é obrigatório."
        ).toLowerCase();
        String perfilNormalizado = normalizarPerfil(perfil);

        validarSenhaTemporaria(senhaTemporaria, confirmacao);

        if (usuarioDAO.existeLogin(loginNormalizado)) {
            throw new IllegalArgumentException(
                    "Já existe um usuário com esse login."
            );
        }

        String hash = BCrypt.hashpw(
                senhaTemporaria,
                BCrypt.gensalt(CUSTO_BCRYPT)
        );

        Usuario novoUsuario = new Usuario(
                null,
                nomeNormalizado,
                loginNormalizado,
                hash,
                perfilNormalizado,
                "ATIVO",
                true
        );

        usuarioDAO.cadastrar(novoUsuario);

        if (novoUsuario.getIdUsuario() == null
                || novoUsuario.getIdUsuario() <= 0) {
            throw new IllegalStateException(
                    "O usuário foi cadastrado sem um ID válido."
            );
        }

        return novoUsuario;
    }

    /**
     * Edita nome, login e perfil após revalidar, na mesma transação, o
     * administrador executor, o usuário alvo e o snapshot apresentado pela UI.
     */
    public Usuario editarUsuario(
            Integer administradorId,
            Integer usuarioAlvoId,
            String nomeOriginal,
            String loginOriginal,
            String perfilOriginal,
            String novoNome,
            String novoLogin,
            String novoPerfil
    ) {

        validarAdministradorId(administradorId);
        validarUsuarioAlvoId(usuarioAlvoId);

        String nomeOriginalNormalizado = normalizarSnapshotNome(nomeOriginal);
        String loginOriginalNormalizado = normalizarSnapshotLogin(loginOriginal);
        String perfilOriginalNormalizado = normalizarSnapshotPerfil(perfilOriginal);
        String novoNomeNormalizado = normalizarCampoObrigatorio(
                novoNome,
                "Nome é obrigatório."
        );
        String novoLoginNormalizado = normalizarCampoObrigatorio(
                novoLogin,
                "Login é obrigatório."
        ).toLowerCase(Locale.ROOT);
        String novoPerfilNormalizado = normalizarPerfil(novoPerfil);

        try (Connection conn = ConnectionFactory.getConnection()) {
            boolean autoCommitOriginal = conn.getAutoCommit();
            Throwable falhaOriginal = null;
            Usuario usuarioAlvoPersistido;

            try {
                conn.setAutoCommit(false);

                Usuario administradorPersistido = usuarioDAO.buscarPorId(
                        conn,
                        administradorId
                );
                validarAdministradorPersistido(
                        administradorPersistido,
                        administradorId
                );

                usuarioAlvoPersistido = usuarioDAO.buscarPorId(
                        conn,
                        usuarioAlvoId
                );
                validarUsuarioAlvoPersistido(
                        usuarioAlvoPersistido,
                        usuarioAlvoId
                );

                validarSnapshot(
                        usuarioAlvoPersistido,
                        nomeOriginalNormalizado,
                        loginOriginalNormalizado,
                        perfilOriginalNormalizado
                );

                boolean autoedicao = administradorId.equals(usuarioAlvoId);

                if (autoedicao
                        && !novoPerfilNormalizado.equals(
                                usuarioAlvoPersistido.getPerfil()
                        )) {

                    throw new IllegalStateException(
                            "O administrador não pode alterar o próprio perfil."
                    );
                }

                if (!autoedicao
                        && "ADMIN".equals(usuarioAlvoPersistido.getPerfil())
                        && "ATIVO".equals(usuarioAlvoPersistido.getStatus())
                        && "VENDEDOR".equals(novoPerfilNormalizado)
                        && usuarioDAO.contarAdministradoresAtivos(conn) <= 1) {

                    throw new IllegalStateException(
                            "Não é permitido alterar o perfil do último administrador ativo."
                    );
                }

                if (usuarioDAO.existeLoginParaOutroUsuario(
                        conn,
                        novoLoginNormalizado,
                        usuarioAlvoId
                )) {

                    throw new IllegalArgumentException(
                            "Já existe outro usuário com esse login."
                    );
                }

                int registrosAtualizados = usuarioDAO.atualizarDadosCadastrais(
                        conn,
                        usuarioAlvoId,
                        nomeOriginalNormalizado,
                        loginOriginalNormalizado,
                        perfilOriginalNormalizado,
                        novoNomeNormalizado,
                        novoLoginNormalizado,
                        novoPerfilNormalizado
                );

                if (registrosAtualizados != 1) {
                    throw dadosDesatualizados();
                }

                conn.commit();

            } catch (SQLException | RuntimeException e) {
                RuntimeException falha = converterFalhaEdicao(e);
                falhaOriginal = falha;
                executarRollbackSeguro(conn, falha);
                throw falha;

            } finally {
                restaurarAutoCommitSeguro(
                        conn,
                        autoCommitOriginal,
                        falhaOriginal
                );
            }

            usuarioAlvoPersistido.setNome(novoNomeNormalizado);
            usuarioAlvoPersistido.setLogin(novoLoginNormalizado);
            usuarioAlvoPersistido.setPerfil(novoPerfilNormalizado);
            return usuarioAlvoPersistido;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erro ao controlar a transação de edição do usuário.",
                    e
            );
        }
    }

    /**
     * Ativa ou inativa um usuário em transação própria.
     *
     * A contagem do último administrador ocorre na mesma Connection da mudança
     * para impedir que a regra seja avaliada fora da operação persistente.
     */
    public void alterarStatusUsuario(
            Usuario administrador,
            Usuario usuarioAlvo,
            String novoStatus
    ) {

        validarAdministrador(administrador);
        validarUsuarioAlvo(usuarioAlvo);

        String statusNormalizado = normalizarStatus(novoStatus);
        String statusAtual = usuarioAlvo.getStatus();

        if (statusNormalizado.equals(statusAtual)) {
            throw new IllegalArgumentException(
                    "O usuário já possui o status informado."
            );
        }

        if (mesmoUsuario(administrador, usuarioAlvo)
                && "INATIVO".equals(statusNormalizado)) {
            throw new IllegalStateException(
                    "O administrador não pode inativar a própria conta."
            );
        }

        try (Connection conn = ConnectionFactory.getConnection()) {
            boolean autoCommitOriginal = conn.getAutoCommit();
            Throwable falhaOriginal = null;

            try {
                conn.setAutoCommit(false);

                if ("ADMIN".equals(usuarioAlvo.getPerfil())
                        && "ATIVO".equals(statusAtual)
                        && "INATIVO".equals(statusNormalizado)
                        && usuarioDAO.contarAdministradoresAtivos(conn) <= 1) {

                    throw new IllegalStateException(
                            "Não é permitido inativar o último administrador ativo."
                    );
                }

                int registrosAtualizados = usuarioDAO.atualizarStatus(
                        conn,
                        usuarioAlvo.getIdUsuario(),
                        statusAtual,
                        statusNormalizado
                );

                if (registrosAtualizados != 1) {
                    throw new IllegalStateException(
                            "Não foi possível alterar o status. "
                                    + "Os dados do usuário ficaram desatualizados."
                    );
                }

                conn.commit();

            } catch (SQLException | RuntimeException e) {
                falhaOriginal = e;
                executarRollbackSeguro(conn, e);

                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }

                throw new IllegalStateException(
                        "Erro ao alterar o status do usuário.",
                        e
                );

            } finally {
                restaurarAutoCommitSeguro(
                        conn,
                        autoCommitOriginal,
                        falhaOriginal
                );
            }

            // O objeto usado pela interface somente acompanha um commit confirmado.
            usuarioAlvo.setStatus(statusNormalizado);

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erro ao controlar a transação de status do usuário.",
                    e
            );
        }
    }

    /**
     * Redefine a senha de outro usuário e exige nova troca no próximo acesso.
     */
    public void redefinirSenhaAdministrativamente(
            Usuario administrador,
            Usuario usuarioAlvo,
            String senhaTemporaria,
            String confirmacao
    ) {

        validarAdministrador(administrador);
        validarUsuarioAlvo(usuarioAlvo);

        if (mesmoUsuario(administrador, usuarioAlvo)) {
            throw new IllegalStateException(
                    "Use a opção Alterar Senha para modificar a própria senha."
            );
        }

        validarSenhaTemporaria(senhaTemporaria, confirmacao);

        String novoHash = BCrypt.hashpw(
                senhaTemporaria,
                BCrypt.gensalt(CUSTO_BCRYPT)
        );

        try (Connection conn = ConnectionFactory.getConnection()) {
            boolean autoCommitOriginal = conn.getAutoCommit();
            Throwable falhaOriginal = null;

            try {
                conn.setAutoCommit(false);

                int registrosAtualizados =
                        usuarioDAO.redefinirSenhaAdministrativamente(
                                conn,
                                usuarioAlvo.getIdUsuario(),
                                novoHash
                        );

                if (registrosAtualizados != 1) {
                    throw new IllegalStateException(
                            "Não foi possível redefinir a senha do usuário."
                    );
                }

                conn.commit();

            } catch (SQLException | RuntimeException e) {
                falhaOriginal = e;
                executarRollbackSeguro(conn, e);

                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }

                throw new IllegalStateException(
                        "Erro ao redefinir administrativamente a senha.",
                        e
                );

            } finally {
                restaurarAutoCommitSeguro(
                        conn,
                        autoCommitOriginal,
                        falhaOriginal
                );
            }

            // A troca obrigatória nasce junto com o novo hash e só chega ao
            // objeto depois do commit, sem criar sessão para o usuário alvo.
            usuarioAlvo.setSenha(novoHash);
            usuarioAlvo.setTrocaSenhaObrigatoria(true);

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erro ao controlar a transação de redefinição de senha.",
                    e
            );
        }
    }

    private void validarAdministrador(Usuario administrador) {
        if (administrador == null) {
            throw new IllegalStateException(
                    "Administrador logado é obrigatório."
            );
        }

        if (administrador.getIdUsuario() == null
                || administrador.getIdUsuario() <= 0) {
            throw new IllegalStateException(
                    "Administrador logado não possui um ID válido."
            );
        }

        if (!"ATIVO".equals(administrador.getStatus())) {
            throw new IllegalStateException(
                    "A gestão de usuários exige um administrador ativo."
            );
        }

        if (!"ADMIN".equals(administrador.getPerfil())) {
            throw new IllegalStateException(
                    "A gestão de usuários é exclusiva para administradores."
            );
        }

        if (administrador.isTrocaSenhaObrigatoria()) {
            throw new IllegalStateException(
                    "Conclua a troca obrigatória antes de gerenciar usuários."
            );
        }
    }

    private void validarAdministradorId(Integer administradorId) {
        if (administradorId == null || administradorId <= 0) {
            throw new IllegalStateException(
                    "Usuário não autorizado a editar usuários."
            );
        }
    }

    private void validarUsuarioAlvoId(Integer usuarioAlvoId) {
        if (usuarioAlvoId == null || usuarioAlvoId <= 0) {
            throw new IllegalStateException(
                    "Usuário não encontrado para edição."
            );
        }
    }

    private void validarAdministradorPersistido(
            Usuario administrador,
            Integer administradorId
    ) {

        if (administrador == null
                || administrador.getIdUsuario() == null
                || !administradorId.equals(administrador.getIdUsuario())
                || !"ADMIN".equals(administrador.getPerfil())
                || !"ATIVO".equals(administrador.getStatus())
                || administrador.isTrocaSenhaObrigatoria()) {

            throw new IllegalStateException(
                    "Usuário não autorizado a editar usuários."
            );
        }
    }

    private void validarUsuarioAlvoPersistido(
            Usuario usuarioAlvo,
            Integer usuarioAlvoId
    ) {

        if (usuarioAlvo == null
                || usuarioAlvo.getIdUsuario() == null
                || !usuarioAlvoId.equals(usuarioAlvo.getIdUsuario())) {

            throw new IllegalStateException(
                    "Usuário não encontrado para edição."
            );
        }
    }

    private void validarSnapshot(
            Usuario usuarioAlvo,
            String nomeOriginal,
            String loginOriginal,
            String perfilOriginal
    ) {

        if (!nomeOriginal.equals(usuarioAlvo.getNome())
                || !loginOriginal.equals(usuarioAlvo.getLogin())
                || !perfilOriginal.equals(usuarioAlvo.getPerfil())) {

            throw dadosDesatualizados();
        }
    }

    private String normalizarSnapshotNome(String nome) {
        return normalizarSnapshotObrigatorio(nome).trim();
    }

    private String normalizarSnapshotLogin(String login) {
        return normalizarSnapshotObrigatorio(login)
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String normalizarSnapshotPerfil(String perfil) {
        String perfilNormalizado = normalizarSnapshotObrigatorio(perfil)
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!"ADMIN".equals(perfilNormalizado)
                && !"VENDEDOR".equals(perfilNormalizado)) {
            throw dadosDesatualizados();
        }

        return perfilNormalizado;
    }

    private String normalizarSnapshotObrigatorio(String valor) {
        if (valor == null || valor.isBlank()) {
            throw dadosDesatualizados();
        }

        return valor;
    }

    private IllegalStateException dadosDesatualizados() {
        return new IllegalStateException(
                "Não foi possível salvar. Os dados do usuário foram alterados; "
                        + "atualize a lista e tente novamente."
        );
    }

    private RuntimeException converterFalhaEdicao(Throwable falha) {
        if (causadaPorRestricaoUnique(falha)) {
            return new IllegalArgumentException(
                    "Já existe outro usuário com esse login.",
                    falha
            );
        }

        if (falha instanceof RuntimeException runtimeException) {
            return runtimeException;
        }

        return new IllegalStateException(
                "Erro ao editar os dados cadastrais do usuário.",
                falha
        );
    }

    private boolean causadaPorRestricaoUnique(Throwable falha) {
        Throwable causa = falha;

        while (causa != null) {
            String mensagem = causa.getMessage();

            if (mensagem != null
                    && mensagem.toUpperCase(Locale.ROOT).contains("UNIQUE")) {
                return true;
            }

            causa = causa.getCause();
        }

        return false;
    }

    private void validarUsuarioAlvo(Usuario usuarioAlvo) {
        if (usuarioAlvo == null) {
            throw new IllegalArgumentException(
                    "Selecione um usuário para realizar a operação."
            );
        }

        if (usuarioAlvo.getIdUsuario() == null
                || usuarioAlvo.getIdUsuario() <= 0) {
            throw new IllegalStateException(
                    "O usuário selecionado não possui um ID válido."
            );
        }
    }

    private String normalizarCampoObrigatorio(
            String valor,
            String mensagem
    ) {

        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }

        return valor.trim();
    }

    private String normalizarPerfil(String perfil) {
        String perfilNormalizado = normalizarCampoObrigatorio(
                perfil,
                "Perfil é obrigatório."
        ).toUpperCase(Locale.ROOT);

        if (!"ADMIN".equals(perfilNormalizado)
                && !"VENDEDOR".equals(perfilNormalizado)) {
            throw new IllegalArgumentException(
                    "Perfil inválido. Use ADMIN ou VENDEDOR."
            );
        }

        return perfilNormalizado;
    }

    private String normalizarStatus(String status) {
        String statusNormalizado = normalizarCampoObrigatorio(
                status,
                "Novo status é obrigatório."
        ).toUpperCase();

        if (!"ATIVO".equals(statusNormalizado)
                && !"INATIVO".equals(statusNormalizado)) {
            throw new IllegalArgumentException(
                    "Status inválido. Use ATIVO ou INATIVO."
            );
        }

        return statusNormalizado;
    }

    private void validarSenhaTemporaria(
            String senhaTemporaria,
            String confirmacao
    ) {

        if (senhaTemporaria == null || senhaTemporaria.isBlank()) {
            throw new IllegalArgumentException(
                    "A senha temporária é obrigatória."
            );
        }

        if (confirmacao == null || confirmacao.isBlank()) {
            throw new IllegalArgumentException(
                    "A confirmação da senha temporária é obrigatória."
            );
        }

        if (!senhaTemporaria.equals(confirmacao)) {
            throw new IllegalArgumentException(
                    "A senha temporária e a confirmação devem ser iguais."
            );
        }

        if (senhaTemporaria.length() < TAMANHO_MINIMO_SENHA) {
            throw new IllegalArgumentException(
                    "A senha temporária deve possuir pelo menos 8 caracteres."
            );
        }
    }

    private boolean mesmoUsuario(
            Usuario administrador,
            Usuario usuarioAlvo
    ) {
        return administrador.getIdUsuario().equals(
                usuarioAlvo.getIdUsuario()
        );
    }

    private void executarRollbackSeguro(
            Connection conn,
            Throwable falhaOriginal
    ) {

        try {
            if (!conn.getAutoCommit()) {
                conn.rollback();
            }
        } catch (SQLException e) {
            falhaOriginal.addSuppressed(e);
        }
    }

    private void restaurarAutoCommitSeguro(
            Connection conn,
            boolean autoCommitOriginal,
            Throwable falhaOriginal
    ) throws SQLException {

        try {
            conn.setAutoCommit(autoCommitOriginal);
        } catch (SQLException e) {
            if (falhaOriginal != null) {
                falhaOriginal.addSuppressed(e);
                return;
            }

            throw e;
        }
    }
}
