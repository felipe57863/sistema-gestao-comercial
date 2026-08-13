package br.com.luis.viewmodel;

/**
 * Representa a situação calculada de um produto na visão de estoque.
 *
 * A situação é determinada futuramente pelo Service a partir do estoque atual
 * e do estoque mínimo. A opção visual "Todas" é representada por valor nulo no
 * filtro e, por isso, não integra este enum.
 */
public enum SituacaoEstoqueProduto {

    /**
     * Estoque atual menor que o estoque mínimo.
     */
    ABAIXO_DO_MINIMO("Abaixo do mínimo"),

    /**
     * Estoque atual igual ao estoque mínimo.
     */
    NO_MINIMO("No mínimo"),

    /**
     * Estoque atual maior que o estoque mínimo.
     */
    ACIMA_DO_MINIMO("Acima do mínimo");

    private final String descricao;

    SituacaoEstoqueProduto(String descricao) {
        this.descricao = descricao;
    }

    /**
     * Retorna a descrição amigável da situação para apresentação visual.
     */
    public String getDescricao() {
        return descricao;
    }
}
