package br.com.luis.service;

import br.com.luis.dao.UsuarioDAO;
import br.com.luis.model.Usuario;
import br.com.luis.util.ConnectionFactory;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Coordena a definição da nova senha exigida antes do acesso normal ao ERP.
 *
 * O Service valida os dados, gera o hash BCrypt e controla a transação que
 * atualiza simultaneamente a senha e o indicador de troca obrigatória. Ele não
 * acessa JavaFX e não cria SessaoUsuario; essa liberação pertence ao Controller
 * somente depois da conclusão confirmada.
 */
public class TrocaSenhaService {

    private static final int TAMANHO_MINIMO_SENHA = 8;
    private static final int CUSTO_BCRYPT = 12;

    private final UsuarioDAO usuarioDAO;

    public TrocaSenhaService() {
        this.usuarioDAO = new UsuarioDAO();
    }

    /**
     * Conclui a troca obrigatória de senha de um usuário já autenticado.
     *
     * Hash e indicador são alterados pelo mesmo UPDATE e pela mesma transação,
     * evitando que apenas uma parte da regra seja persistida. O objeto em memória
     * é atualizado somente depois do commit, para não representar como concluída
     * uma operação que ainda possa sofrer rollback.
     *
     * @param usuario usuário cujas credenciais já foram autenticadas.
     * @param novaSenha nova senha definida pelo usuário.
     * @param confirmacao confirmação da nova senha.
     * @throws IllegalArgumentException quando a senha ou a confirmação forem
     *                                  inválidas.
     * @throws IllegalStateException quando o usuário ou a persistência estiverem
     *                               em estado incompatível.
     */
    public void concluirTrocaSenhaObrigatoria(
            Usuario usuario,
            String novaSenha,
            String confirmacao
    ) {

        validarUsuario(usuario);
        validarNovaSenha(novaSenha, confirmacao);

        String novoHash = BCrypt.hashpw(
                novaSenha,
                BCrypt.gensalt(CUSTO_BCRYPT)
        );

        try (Connection conn = ConnectionFactory.getConnection()) {
            boolean autoCommitOriginal = conn.getAutoCommit();
            Throwable falhaOriginal = null;

            try {
                conn.setAutoCommit(false);

                int registrosAtualizados =
                        usuarioDAO.concluirTrocaSenhaObrigatoria(
                                conn,
                                usuario.getIdUsuario(),
                                novoHash
                        );

                if (registrosAtualizados != 1) {
                    throw new IllegalStateException(
                            "Não foi possível concluir a troca obrigatória de senha. "
                                    + "Verifique se o usuário continua ativo e pendente."
                    );
                }

                conn.commit();

            } catch (SQLException | RuntimeException e) {
                falhaOriginal = e;
                executarRollbackSeguro(conn, e);

                if (e instanceof IllegalArgumentException) {
                    throw (IllegalArgumentException) e;
                }

                if (e instanceof IllegalStateException) {
                    throw (IllegalStateException) e;
                }

                throw new IllegalStateException(
                        "Erro ao concluir a troca obrigatória de senha.",
                        e
                );

            } finally {
                restaurarAutoCommitSeguro(
                        conn,
                        autoCommitOriginal,
                        falhaOriginal
                );
            }

            // Somente um commit confirmado autoriza o objeto usado pela interface
            // a representar a nova senha e a liberação do acesso normal.
            usuario.setSenha(novoHash);
            usuario.setTrocaSenhaObrigatoria(false);

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erro ao controlar a transação da troca obrigatória de senha.",
                    e
            );
        }
    }

    private void validarUsuario(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException(
                    "Usuário autenticado é obrigatório para a troca de senha."
            );
        }

        if (usuario.getIdUsuario() == null || usuario.getIdUsuario() <= 0) {
            throw new IllegalStateException(
                    "Usuário autenticado não possui um ID válido."
            );
        }

        if (!"ATIVO".equals(usuario.getStatus())) {
            throw new IllegalStateException(
                    "A troca de senha não pode ser concluída para usuário inativo."
            );
        }

        if (!usuario.isTrocaSenhaObrigatoria()) {
            throw new IllegalStateException(
                    "O usuário não possui troca obrigatória de senha pendente."
            );
        }
    }

    private void validarNovaSenha(
            String novaSenha,
            String confirmacao
    ) {

        if (novaSenha == null || novaSenha.isBlank()) {
            throw new IllegalArgumentException("A nova senha é obrigatória.");
        }

        if (confirmacao == null || confirmacao.isBlank()) {
            throw new IllegalArgumentException(
                    "A confirmação da nova senha é obrigatória."
            );
        }

        if (!novaSenha.equals(confirmacao)) {
            throw new IllegalArgumentException(
                    "A nova senha e a confirmação devem ser iguais."
            );
        }

        if (novaSenha.length() < TAMANHO_MINIMO_SENHA) {
            throw new IllegalArgumentException(
                    "A nova senha deve possuir pelo menos 8 caracteres."
            );
        }
    }

    /**
     * Tenta desfazer a transação sem ocultar a falha que iniciou o rollback.
     */
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

    /**
     * Devolve a Connection ao estado recebido e preserva falhas secundárias.
     */
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
