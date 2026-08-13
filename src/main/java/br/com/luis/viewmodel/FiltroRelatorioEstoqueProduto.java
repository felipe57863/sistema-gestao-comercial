package br.com.luis.viewmodel;

/**
 * Transporta os filtros aplicados à visão de relatório de estoque de produtos.
 *
 * A classe é imutável e representa uma fotografia dos filtros informados no
 * momento em que a consulta é iniciada. Descrição nula representa ausência de
 * filtro textual, ativo nulo representa todos os status e situação nula
 * representa todas as situações de estoque.
 *
 * Esta classe não acessa banco de dados, DAO, Service, sessão, componentes
 * JavaFX ou mecanismos de formatação visual.
 */
public final class FiltroRelatorioEstoqueProduto {

    private final String descricao;
    private final Boolean ativo;
    private final SituacaoEstoqueProduto situacao;

    /**
     * Cria uma fotografia imutável dos filtros da visão de estoque.
     *
     * A descrição é normalizada somente pela remoção dos espaços externos.
     * Valor nulo ou em branco representa ausência desse filtro. O status e a
     * situação permanecem nulos quando a consulta deve abranger todas as opções.
     *
     * @param descricao descrição parcial do produto, ou null para todos.
     * @param ativo true para ativos, false para inativos ou null para todos.
     * @param situacao situação específica, ou null para todas.
     */
    public FiltroRelatorioEstoqueProduto(
            String descricao,
            Boolean ativo,
            SituacaoEstoqueProduto situacao
    ) {
        this.descricao = normalizarDescricao(descricao);
        this.ativo = ativo;
        this.situacao = situacao;

        validar();
    }

    /**
     * Valida o estado estrutural da fotografia de filtros.
     *
     * Todos os critérios são opcionais. O método pode ser chamado novamente pelo
     * futuro Service antes da consulta, seguindo o contrato dos relatórios atuais.
     */
    public void validar() {
        if (descricao != null && descricao.isBlank()) {
            throw new IllegalArgumentException(
                    "Descrição do produto não pode ser vazia quando informada."
            );
        }
    }

    /**
     * Normaliza somente os espaços externos da descrição informada.
     */
    private static String normalizarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            return null;
        }

        return descricao.trim();
    }

    public String getDescricao() {
        return descricao;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public SituacaoEstoqueProduto getSituacao() {
        return situacao;
    }
}
