package br.com.luis.service;

import br.com.luis.dao.UsuarioDAO;
import br.com.luis.model.Usuario;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Serviço responsável por regras de negócio de Autenticação e Segurança.
 * Fica entre o Controller (Interface) e o DAO (Banco de Dados).
 */
public class AuthService {

    private final UsuarioDAO usuarioDAO;

    public AuthService() {
        this.usuarioDAO = new UsuarioDAO();
    }

    /**
     * Aplica o algoritmo BCrypt para gerar um hash irreversível da senha.
     * @param senhaLimpa Senha em texto puro digitada pelo usuário.
     * @return String contendo o hash criptografado.
     */
    private String gerarHash(String senhaLimpa) {
        // O BCrypt.gensalt() gera uma "pitada" aleatória de dados para a senha.
        // O número '12' é o "workload" (peso). É o padrão de mercado atual para equilibrar segurança e velocidade.
        return BCrypt.hashpw(senhaLimpa, BCrypt.gensalt(12));
    }

    /**
     * Passo 2.0 do Roadmap: Seed (Semente) do sistema.
     * Verifica se o utilizador administrador já existe. Se não, cria-o de forma segura.
     */
    public void inicializarAdminBase() {
        Usuario adminExistente = usuarioDAO.buscarPorLogin("admin");

        if (adminExistente == null) {
            System.out.println("[INFO] Utilizador 'admin' não encontrado. Gerando administrador padrão...");

            // ⚠️ REGRA CRÍTICA: Aplicamos o hash ANTES de enviar para a entidade/banco
            String senhaSegura = gerarHash("admin123");

            Usuario novoAdmin = new Usuario(
                    null, // O ID será gerado pelo SQLite
                    "Administrador do Sistema",
                    "admin",
                    senhaSegura, // Hash irreversível injetado aqui
                    "ADMIN",
                    "ATIVO"
            );

            usuarioDAO.cadastrar(novoAdmin);
            System.out.println("[INFO] Administrador padrão criado com sucesso! Login: admin | Senha: admin123");

        } else {
            System.out.println("[INFO] O administrador padrão já existe na base de dados.");
        }
    }
}