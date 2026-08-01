package br.com.luis.viewmodel;

import br.com.luis.model.Usuario;

/**
 * Representa o resultado de uma autenticação bem-sucedida.
 *
 * Credenciais autenticadas não significam necessariamente acesso normal ao
 * ERP. O usuário pode estar ativo e autenticado, mas ainda precisar definir
 * uma nova senha antes que o Controller crie a sessão da aplicação.
 *
 * Esta classe não transporta a senha digitada nem uma cópia do hash. O estado
 * da troca obrigatória permanece representado exclusivamente pelo Usuario.
 */
public final class ResultadoAutenticacao {

    private final Usuario usuario;

    /**
     * Cria o resultado para o usuário cujas credenciais foram validadas.
     *
     * @param usuario usuário autenticado.
     * @throws IllegalArgumentException quando o usuário for nulo.
     */
    public ResultadoAutenticacao(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException(
                    "Usuário autenticado é obrigatório."
            );
        }

        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public boolean isTrocaSenhaObrigatoria() {
        return usuario.isTrocaSenhaObrigatoria();
    }

    public boolean isAcessoNormalPermitido() {
        return !isTrocaSenhaObrigatoria();
    }
}
