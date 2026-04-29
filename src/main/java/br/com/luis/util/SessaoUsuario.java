package br.com.luis.util;

import br.com.luis.model.Usuario;

/**
 * Gerenciador de Sessão utilizando o Padrão Singleton.
 * Mantém o usuário logado em memória durante a execução do ERP.
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

    public Usuario getUsuarioLogado() {
        return usuarioLogado;
    }

    public void setUsuarioLogado(Usuario usuarioLogado) {
        this.usuarioLogado = usuarioLogado;
    }

    /**
     * Limpa a sessão atual (Função de Logout).
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