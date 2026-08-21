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

    private static SessaoUsuario instancia;

    private Usuario usuarioLogado;

    private SessaoUsuario() {
    }

    public static SessaoUsuario getInstance() {
        if (instancia == null) {
            instancia = new SessaoUsuario();
        }
        return instancia;
    }

    /**
     * Retorna o usuário mantido na sessão atual.
     *
     * @return usuário autenticado ou {@code null} quando a sessão estiver vazia.
     */
    public Usuario getUsuarioLogado() {
        return usuarioLogado;
    }

    public void setUsuarioLogado(Usuario usuarioLogado) {
        this.usuarioLogado = usuarioLogado;
    }

    public void fazerLogout() {
        this.usuarioLogado = null;
    }

    public boolean isUsuarioLogado() {
        return usuarioLogado != null;
    }
}