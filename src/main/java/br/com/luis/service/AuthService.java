package br.com.luis.service;

import br.com.luis.dao.UsuarioDAO;
import br.com.luis.model.Usuario;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Serviço responsável por regras de negócio de Autenticação e Segurança.
 * Atua entre a camada Controller (interface) e DAO (persistência).
 */
public class AuthService {

    // Dependência responsável pelo acesso ao banco
    private final UsuarioDAO usuarioDAO;

    // Construtor padrão (injeção simples de dependência)
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
     * Inicializa o usuário administrador padrão (seed do sistema).
     * Executado na primeira inicialização do sistema.
     *
     * Regra:
     * - Se não existir usuário "admin", ele será criado automaticamente.
     * - Se já existir, nenhuma ação é realizada.
     *
     * @implNote Seed inicial da Fase 2:
     * garante que o sistema tenha um usuário ADMIN criado com senha criptografada via BCrypt.
     */
    public void inicializarAdminBase() {

        // Verifica se já existe um usuário com login "admin"
        Usuario adminExistente = usuarioDAO.buscarPorLogin("admin");

        if (adminExistente == null) {
            System.out.println("[INFO] Administrador não encontrado. Criando usuário padrão...");

            // Regra de segurança:
            // A senha padrão deve ser usada apenas na primeira execução.
            // Recomenda-se exigir troca de senha posteriormente.
            String senhaSegura = gerarHash("admin123");

            // Criação do usuário administrador
            Usuario novoAdmin = new Usuario(
                    null, // ID gerado automaticamente pelo banco
                    "Administrador do Sistema",
                    "admin",
                    senhaSegura, // Senha já criptografada
                    "ADMIN",
                    "ATIVO"
            );

            // Persiste no banco
            usuarioDAO.cadastrar(novoAdmin);

            System.out.println("[INFO] Administrador criado com sucesso. Login: admin | Senha: admin123");

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
     *
     * @param login Login digitado
     * @param senhaLimpa Senha digitada (texto puro)
     * @return Usuario autenticado
     * @throws RuntimeException se login/senha inválidos ou usuário inativo
     *
     * @implNote Regra de segurança da Fase 2:
     * autentica o usuário usando BCrypt e bloqueia acesso de usuários inativos.
     */
    public Usuario autenticar(String login, String senhaLimpa) {

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

        // Log de auditoria
        System.out.println("[LOG] Login realizado com sucesso: " + usuario.getLogin());

        return usuario;
    }
}