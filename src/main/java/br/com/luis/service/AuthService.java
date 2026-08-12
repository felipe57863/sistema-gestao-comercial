package br.com.luis.service;

import br.com.luis.dao.UsuarioDAO;
import br.com.luis.model.Usuario;
import br.com.luis.viewmodel.ResultadoAutenticacao;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Service responsável pela autenticação e pelo provisionamento seguro do
 * administrador inicial. Normaliza o login, consulta usuários por meio do
 * UsuarioDAO, aplica BCrypt e rejeita usuários inativos.
 *
 * Não controla componentes JavaFX, navegação ou SessaoUsuario. A sessão somente
 * pode ser criada pelo Controller depois do fluxo normal de autenticação.
 */
public class AuthService {

    private static final String NOME_ADMINISTRADOR_INICIAL =
            "Administrador do Sistema";
    private static final String LOGIN_ADMINISTRADOR_INICIAL = "admin";
    private static final int TAMANHO_MINIMO_SENHA = 8;
    private static final int CUSTO_BCRYPT = 12;

    // Dependência responsável pelo acesso ao banco
    private final UsuarioDAO usuarioDAO;

    // Construtor padrão com inicialização direta do DAO utilizado pelo Service.
    public AuthService() {
        this.usuarioDAO = new UsuarioDAO();
    }

    /**
     * Retorna o nome não secreto definido pelo sistema para o administrador
     * inicial.
     */
    public String obterNomeAdministradorInicial() {
        return NOME_ADMINISTRADOR_INICIAL;
    }

    /**
     * Retorna o login não secreto definido pelo sistema para o administrador
     * inicial.
     */
    public String obterLoginAdministradorInicial() {
        return LOGIN_ADMINISTRADOR_INICIAL;
    }

    /**
     * Indica se a instalação ainda não possui nenhum administrador configurado.
     *
     * O status não interfere nessa decisão: um ADMIN inativo continua
     * representando uma instalação já configurada.
     */
    public boolean precisaConfigurarAdministradorInicial() {
        return !usuarioDAO.existeAdministrador();
    }

    /**
     * Cria o primeiro administrador somente depois que o responsável define a
     * senha definitiva.
     *
     * O Service revalida a ausência de qualquer ADMIN, protege o login oficial,
     * valida os campos, gera o hash BCrypt e solicita um único cadastro ao DAO.
     * Nenhuma sessão é criada por este fluxo.
     */
    public void configurarAdministradorInicial(
            String senha,
            String confirmacaoSenha
    ) {

        if (usuarioDAO.existeAdministrador()) {
            throw new IllegalStateException(
                    "A configuração inicial já foi concluída."
            );
        }

        if (usuarioDAO.existeLogin(LOGIN_ADMINISTRADOR_INICIAL)) {
            throw new IllegalStateException(
                    "Não foi possível concluir a configuração inicial. "
                            + "O login administrativo está indisponível."
            );
        }

        validarSenhaInicial(senha, confirmacaoSenha);

        String senhaHash = BCrypt.hashpw(
                senha,
                BCrypt.gensalt(CUSTO_BCRYPT)
        );

        Usuario administradorInicial = new Usuario(
                null,
                NOME_ADMINISTRADOR_INICIAL,
                LOGIN_ADMINISTRADOR_INICIAL,
                senhaHash,
                "ADMIN",
                "ATIVO",
                false
        );

        usuarioDAO.cadastrar(administradorInicial);

        if (administradorInicial.getIdUsuario() == null
                || administradorInicial.getIdUsuario() <= 0) {
            throw new IllegalStateException(
                    "Não foi possível confirmar a configuração inicial."
            );
        }
    }

    private void validarSenhaInicial(
            String senha,
            String confirmacaoSenha
    ) {

        if (senha == null || senha.isBlank()) {
            throw new IllegalArgumentException("Senha é obrigatória.");
        }

        if (confirmacaoSenha == null || confirmacaoSenha.isBlank()) {
            throw new IllegalArgumentException(
                    "Confirmação de senha é obrigatória."
            );
        }

        if (!senha.equals(confirmacaoSenha)) {
            throw new IllegalArgumentException("As senhas não conferem.");
        }

        if (senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new IllegalArgumentException(
                    "A senha deve ter no mínimo 8 caracteres."
            );
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
