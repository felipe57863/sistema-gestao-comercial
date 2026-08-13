package br.com.luis.viewmodel;

import java.util.List;

/**
 * Representa a fotografia completa e imutável de uma consulta concluída da
 * visão de produtos em promoção.
 *
 * Cada elemento da lista corresponde a uma promoção ativa encontrada. A classe
 * preserva linhas distintas mesmo quando possuem o mesmo produto e não realiza
 * consulta, filtragem, deduplicação ou cálculo de preço promocional.
 */
public final class ResultadoRelatorioPromocaoProduto {

    private final FiltroRelatorioPromocaoProduto filtroAplicado;
    private final List<ProdutoPromocaoRelatorioView> promocoes;
    private final int quantidadePromocoes;

    /**
     * Cria uma fotografia imutável da visão de produtos em promoção.
     *
     * A quantidade de promoções é obtida diretamente do tamanho da lista. Uma
     * lista vazia é válida e resulta em quantidade zero. Nenhuma linha é
     * eliminada com base no ID do produto.
     *
     * @param filtroAplicado filtro efetivamente usado na consulta.
     * @param promocoes linhas finais correspondentes às promoções ativas.
     * @throws IllegalArgumentException quando algum argumento obrigatório for
     *                                  inválido.
     */
    public ResultadoRelatorioPromocaoProduto(
            FiltroRelatorioPromocaoProduto filtroAplicado,
            List<ProdutoPromocaoRelatorioView> promocoes
    ) {
        if (filtroAplicado == null) {
            throw new IllegalArgumentException(
                    "Filtro aplicado ao relatório de promoções é obrigatório."
            );
        }

        filtroAplicado.validar();

        if (promocoes == null) {
            throw new IllegalArgumentException(
                    "Lista de promoções do relatório é obrigatória."
            );
        }

        for (ProdutoPromocaoRelatorioView promocao : promocoes) {
            if (promocao == null) {
                throw new IllegalArgumentException(
                        "Lista de promoções não pode conter elemento nulo."
                );
            }
        }

        List<ProdutoPromocaoRelatorioView> promocoesImutaveis =
                List.copyOf(promocoes);

        this.filtroAplicado = filtroAplicado;
        this.promocoes = promocoesImutaveis;
        this.quantidadePromocoes = promocoesImutaveis.size();
    }

    public FiltroRelatorioPromocaoProduto getFiltroAplicado() {
        return filtroAplicado;
    }

    public List<ProdutoPromocaoRelatorioView> getPromocoes() {
        return promocoes;
    }

    public int getQuantidadePromocoes() {
        return quantidadePromocoes;
    }
}
