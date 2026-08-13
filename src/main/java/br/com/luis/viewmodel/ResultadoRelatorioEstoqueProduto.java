package br.com.luis.viewmodel;

import java.util.List;

/**
 * Representa a fotografia completa e imutável de uma consulta concluída da
 * visão de relatório de estoque de produtos.
 *
 * A classe transporta o filtro efetivamente aplicado, as linhas finais e os
 * totalizadores consolidados da mesma consulta. Não classifica o estoque, não
 * acessa banco de dados, DAO, Service, sessão, JavaFX ou formatação visual.
 */
public final class ResultadoRelatorioEstoqueProduto {

    private final FiltroRelatorioEstoqueProduto filtroAplicado;
    private final List<ProdutoEstoqueRelatorioView> produtos;
    private final int quantidadeProdutos;
    private final int quantidadeAbaixoDoMinimo;
    private final int quantidadeNoMinimo;
    private final int quantidadeAcimaDoMinimo;

    /**
     * Cria uma fotografia imutável da visão de estoque.
     *
     * A quantidade total de produtos é obtida do tamanho da lista. Os demais
     * totalizadores, já calculados pelo futuro Service, são conferidos contra as
     * situações recebidas nas linhas. Uma lista vazia com totalizadores zero é
     * um resultado válido.
     *
     * @param filtroAplicado filtro efetivamente usado na consulta.
     * @param produtos linhas finais da consulta.
     * @param quantidadeAbaixoDoMinimo total de linhas abaixo do mínimo.
     * @param quantidadeNoMinimo total de linhas no mínimo.
     * @param quantidadeAcimaDoMinimo total de linhas acima do mínimo.
     * @throws IllegalArgumentException quando algum argumento for inválido ou
     *                                  quando linhas e totalizadores divergirem.
     */
    public ResultadoRelatorioEstoqueProduto(
            FiltroRelatorioEstoqueProduto filtroAplicado,
            List<ProdutoEstoqueRelatorioView> produtos,
            int quantidadeAbaixoDoMinimo,
            int quantidadeNoMinimo,
            int quantidadeAcimaDoMinimo
    ) {
        if (filtroAplicado == null) {
            throw new IllegalArgumentException(
                    "Filtro aplicado ao relatório de estoque é obrigatório."
            );
        }

        filtroAplicado.validar();

        if (produtos == null) {
            throw new IllegalArgumentException(
                    "Lista de produtos do relatório de estoque é obrigatória."
            );
        }

        for (ProdutoEstoqueRelatorioView produto : produtos) {
            if (produto == null) {
                throw new IllegalArgumentException(
                        "Lista de produtos do relatório não pode conter elemento nulo."
                );
            }
        }

        if (quantidadeAbaixoDoMinimo < 0) {
            throw new IllegalArgumentException(
                    "Quantidade abaixo do mínimo não pode ser negativa."
            );
        }

        if (quantidadeNoMinimo < 0) {
            throw new IllegalArgumentException(
                    "Quantidade no mínimo não pode ser negativa."
            );
        }

        if (quantidadeAcimaDoMinimo < 0) {
            throw new IllegalArgumentException(
                    "Quantidade acima do mínimo não pode ser negativa."
            );
        }

        int linhasAbaixoDoMinimo = 0;
        int linhasNoMinimo = 0;
        int linhasAcimaDoMinimo = 0;

        for (ProdutoEstoqueRelatorioView produto : produtos) {
            switch (produto.getSituacao()) {
                case ABAIXO_DO_MINIMO -> linhasAbaixoDoMinimo++;
                case NO_MINIMO -> linhasNoMinimo++;
                case ACIMA_DO_MINIMO -> linhasAcimaDoMinimo++;
            }
        }

        if (linhasAbaixoDoMinimo != quantidadeAbaixoDoMinimo) {
            throw new IllegalArgumentException(
                    "Quantidade abaixo do mínimo não corresponde às linhas do relatório."
            );
        }

        if (linhasNoMinimo != quantidadeNoMinimo) {
            throw new IllegalArgumentException(
                    "Quantidade no mínimo não corresponde às linhas do relatório."
            );
        }

        if (linhasAcimaDoMinimo != quantidadeAcimaDoMinimo) {
            throw new IllegalArgumentException(
                    "Quantidade acima do mínimo não corresponde às linhas do relatório."
            );
        }

        List<ProdutoEstoqueRelatorioView> produtosImutaveis =
                List.copyOf(produtos);

        this.filtroAplicado = filtroAplicado;
        this.produtos = produtosImutaveis;
        this.quantidadeProdutos = produtosImutaveis.size();
        this.quantidadeAbaixoDoMinimo = quantidadeAbaixoDoMinimo;
        this.quantidadeNoMinimo = quantidadeNoMinimo;
        this.quantidadeAcimaDoMinimo = quantidadeAcimaDoMinimo;
    }

    public FiltroRelatorioEstoqueProduto getFiltroAplicado() {
        return filtroAplicado;
    }

    public List<ProdutoEstoqueRelatorioView> getProdutos() {
        return produtos;
    }

    public int getQuantidadeProdutos() {
        return quantidadeProdutos;
    }

    public int getQuantidadeAbaixoDoMinimo() {
        return quantidadeAbaixoDoMinimo;
    }

    public int getQuantidadeNoMinimo() {
        return quantidadeNoMinimo;
    }

    public int getQuantidadeAcimaDoMinimo() {
        return quantidadeAcimaDoMinimo;
    }
}
