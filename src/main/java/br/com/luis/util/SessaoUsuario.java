package br.com.luis.util;

import br.com.luis.model.Usuario;

/**
 * Mantém em memória o usuário autenticado durante a execução da aplicação.
 *
 * Utiliza uma instância Singleton para registrar, disponibilizar ou encerrar a
 * sessão atual. Não realiza autenticação, consulta ao banco ou persistência entre
 * execuções. A ausência de usuário representa uma sessão vazia ou encerrada;
 * Controllers que dependem de autoria devem validar o usuário e seu identificador.
 */
public class SessaoUsuario {

    // A única instância estática que existirá em toda a aplicação
    private static SessaoUsuario instancia;

    // O usuário que está atualmente logado
    private Usuario usuarioLogado;

    /**
     * Construtor privado para impedir que outras classes façam "new SessaoUsuario()".
     */
    private SessaoUsuario() {
    }

    /**
     * Ponto de acesso global à instância do Singleton.
     * @return A única instância de SessaoUsuario.
     */
    public static SessaoUsuario getInstance() {
        if (instancia == null) {
            instancia = new SessaoUsuario();
        }
        return instancia;
    }

    // --- GETTERS E SETTERS ---

    /**
     * Retorna o usuário mantido na sessão atual.
     *
     * @return usuário autenticado ou {@code null} quando a sessão estiver vazia.
     */
    public Usuario getUsuarioLogado() {
        return usuarioLogado;
    }

    /**
     * Registra em memória o usuário autenticado pelo fluxo de login.
     *
     * A classe apenas armazena a referência recebida; não valida credenciais nem
     * persiste a sessão.
     *
     * @param usuarioLogado usuário que ficará disponível na sessão atual.
     */
    public void setUsuarioLogado(Usuario usuarioLogado) {
        this.usuarioLogado = usuarioLogado;
    }

    /**
     * Encerra a sessão atual removendo da memória o usuário autenticado.
     */
    public void fazerLogout() {
        this.usuarioLogado = null;
    }

    /**
     * Verifica de forma rápida se há alguém logado.
     * @return true se existe um usuário autenticado.
     */
    public boolean isUsuarioLogado() {
        return usuarioLogado != null;
    }
}