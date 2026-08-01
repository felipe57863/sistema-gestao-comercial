package br.com.luis.service;

import br.com.luis.dao.UsuarioDAO;
import br.com.luis.model.Usuario;
import br.com.luis.viewmodel.ResultadoAutenticacao;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Service responsável pela autenticação e pela preparação do usuário
 * administrativo inicial. Normaliza o login, consulta o usuário por meio do
 * UsuarioDAO, verifica a senha usando o hash BCrypt e rejeita usuários inativos.
 * Não controla componentes JavaFX, a navegação ou a SessaoUsuario; após a
 * autenticação, cabe ao Controller registrar o usuário na sessão e decidir a
 * navegação da aplicação. Credenciais válidas não significam necessariamente
 * acesso normal liberado, pois pode existir uma troca de senha pendente.
 */
public class AuthService {

    // Dependência responsável pelo acesso ao banco
    private final UsuarioDAO usuarioDAO;

    // Construtor padrão com inicialização direta do DAO utilizado pelo Service.
    public AuthService() {
        this.usuarioDAO = new UsuarioDAO();
    }

    /**
     * Gera um hash seguro da senha utilizando BCrypt.
     * O hash é irreversível e contém um "salt" automático para maior segurança.
     *
     * @param senhaLimpa Senha em texto puro digitada pelo usuário
     * @return Hash seguro da senha
     */
    private String gerarHash(String senhaLimpa) {
        // O parâmetro 12 define o nível de custo (work factor)
        return BCrypt.hashpw(senhaLimpa, BCrypt.gensalt(12));
    }

    /**
     * Garante a existência do usuário administrador padrão durante a preparação
     * da aplicação.
     *
     * Regra:
     * - Consulta se o usuário "admin" já existe.
     * - Se não existir, cria o administrador padrão automaticamente.
     * - Se já existir, não cria outro registro.
     *
     * @implNote A verificação torna a operação idempotente em relação à criação
     * do administrador, cuja senha padrão é armazenada com hash BCrypt.
     */
    public void inicializarAdminBase() {

        // Verifica se já existe um usuário com login "admin"
        Usuario adminExistente = usuarioDAO.buscarPorLogin("admin");

        if (adminExistente == null) {
            System.out.println("[INFO] Administrador não encontrado. Criando usuário padrão...");

            // A senha padrão existe somente para permitir a autenticação inicial.
            // Seu valor nunca deve ser impresso em logs ou mensagens.
            String senhaSegura = gerarHash("admin123");

            // Criação do usuário administrador
            Usuario novoAdmin = new Usuario(
                    null, // ID gerado automaticamente pelo banco
                    "Administrador do Sistema",
                    "admin",
                    senhaSegura, // Hash BCrypt da senha já gerado
                    "ADMIN",
                    "ATIVO",
                    true
            );

            // Persiste no banco
            usuarioDAO.cadastrar(novoAdmin);

            System.out.println(
                    "[INFO] Administrador-base criado. "
                            + "A troca de senha será obrigatória antes do acesso normal."
            );

        } else {
            System.out.println("[INFO] Administrador já existente.");
        }
    }

    /**
     * Realiza a autenticação do usuário no sistema.
     *
     * Fluxo:
     * 1. Valida entrada (login e senha)
     * 2. Busca usuário no banco
     * 3. Compara senha usando BCrypt
     * 4. Verifica status do usuário
     * 5. Informa se o acesso normal está liberado ou se a troca é obrigatória
     *
     * @param login Login digitado
     * @param senhaLimpa Senha digitada (texto puro)
     * @return resultado explícito de uma autenticação bem-sucedida
     * @throws RuntimeException se login/senha inválidos ou usuário inativo
     *
     * @implNote Autentica o usuário usando BCrypt e bloqueia usuários inativos.
     * A criação da sessão permanece sob responsabilidade do Controller, depois
     * da decisão sobre a troca obrigatória de senha.
     */
    public ResultadoAutenticacao autenticar(String login, String senhaLimpa) {

        // Validação básica (fail-fast)
        if (login == null || login.isBlank() || senhaLimpa == null || senhaLimpa.isBlank()) {
            throw new IllegalArgumentException("Login e senha são obrigatórios.");
        }

        String loginNormalizado = login.trim();

        // Busca usuário no banco
        Usuario usuario = usuarioDAO.buscarPorLogin(loginNormalizado);

        // Evita informar se foi login ou senha que falhou (segurança)
        if (usuario == null) {
            throw new RuntimeException("Usuário ou senha inválidos.");
        }

        // Valida senha utilizando BCrypt
        boolean senhaValida;

        try {
            senhaValida = BCrypt.checkpw(senhaLimpa, usuario.getSenha());
        } catch (IllegalArgumentException e) {
            System.err.println("[ERRO] Hash de senha inválido para o usuário: " + usuario.getLogin());
            throw new RuntimeException("Usuário ou senha inválidos.");
        }

        if (!senhaValida) {
            throw new RuntimeException("Usuário ou senha inválidos.");
        }

        // Verifica se o usuário está ativo
        if (!"ATIVO".equalsIgnoreCase(usuario.getStatus())) {
            throw new RuntimeException("Usuário inativo. Contate o administrador.");
        }

        // As credenciais podem ser válidas mesmo quando o acesso normal ainda
        // depende da conclusão da troca obrigatória de senha.
        System.out.println("[LOG] Credenciais validadas para: " + usuario.getLogin());

        return new ResultadoAutenticacao(usuario);
    }
}
