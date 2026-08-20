package br.com.luis.viewmodel;

import br.com.luis.model.Promocao.TipoDesconto;

/**
 * Transporta os filtros aplicados à visão de produtos em promoção.
 *
 * A classe é imutável e representa uma fotografia dos filtros informados no
 * momento em que a consulta é iniciada. Descrição nula representa ausência de
 * filtro textual, produto ativo nulo representa todos os status cadastrais e
 * tipo de desconto nulo representa todos os tipos.
 *
 * Esta classe não acessa banco de dados, DAO, Service, sessão, componentes
 * JavaFX ou mecanismos de formatação visual.
 */
public final class FiltroRelatorioPromocaoProduto {

    private final String descricao;
    private final Boolean produtoAtivo;
    private final TipoDesconto tipoDesconto;

    /**
     * Cria uma fotografia imutável dos filtros da visão de promoções.
     *
     * A descrição é normalizada somente pela remoção dos espaços externos.
     * Valor nulo ou em branco representa ausência desse filtro. O status do
     * produto e o tipo de desconto permanecem nulos quando a consulta deve
     * abranger todas as opções.
     *
     * @param descricao descrição parcial do produto, ou null para todos.
     * @param produtoAtivo true para produtos ativos, false para inativos ou
     *                     null para todos.
     * @param tipoDesconto tipo específico, ou null para todos.
     */
    public FiltroRelatorioPromocaoProduto(
            String descricao,
            Boolean produtoAtivo,
            TipoDesconto tipoDesconto
    ) {
        this.descricao = normalizarDescricao(descricao);
        this.produtoAtivo = produtoAtivo;
        this.tipoDesconto = tipoDesconto;

        validar();
    }

    /**
     * Valida o estado estrutural da fotografia de filtros.
     *
     * Todos os critérios são opcionais. O método é chamado pelo
     * RelatorioProdutoService antes da consulta, seguindo o contrato dos relatórios atuais.
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

    public Boolean getProdutoAtivo() {
        return produtoAtivo;
    }

    public TipoDesconto getTipoDesconto() {
        return tipoDesconto;
    }
}
