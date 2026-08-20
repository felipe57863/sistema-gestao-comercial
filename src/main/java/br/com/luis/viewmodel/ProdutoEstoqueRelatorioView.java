package br.com.luis.viewmodel;

/**
 * Representa uma única linha final da visão de relatório de estoque.
 *
 * A classe é imutável e recebe a diferença e a situação já calculadas pelo
 * RelatorioProdutoService. Não classifica o estoque, não depende do Model Produto e não
 * acessa banco de dados, DAO, Service, sessão, JavaFX ou formatação visual.
 */
public final class ProdutoEstoqueRelatorioView {

    private final Integer produtoId;
    private final String descricao;
    private final Integer estoqueAtual;
    private final Integer estoqueMinimo;
    private final Integer diferenca;
    private final SituacaoEstoqueProduto situacao;
    private final boolean ativo;

    /**
     * Cria uma linha imutável da visão de estoque.
     *
     * Estoque mínimo igual a zero é válido. A diferença pode ser negativa, zero
     * ou positiva. A coerência da diferença e da situação com as quantidades é
     * responsabilidade do RelatorioProdutoService, que prepara a linha.
     *
     * @param produtoId identificador do produto.
     * @param descricao descrição atual do produto.
     * @param estoqueAtual quantidade atual em estoque.
     * @param estoqueMinimo quantidade mínima cadastrada.
     * @param diferenca diferença calculada entre estoque atual e mínimo.
     * @param situacao situação já calculada pelo Service.
     * @param ativo status cadastral atual do produto.
     * @throws IllegalArgumentException quando algum dado obrigatório for inválido.
     */
    public ProdutoEstoqueRelatorioView(
            Integer produtoId,
            String descricao,
            Integer estoqueAtual,
            Integer estoqueMinimo,
            Integer diferenca,
            SituacaoEstoqueProduto situacao,
            boolean ativo
    ) {
        if (produtoId == null || produtoId <= 0) {
            throw new IllegalArgumentException(
                    "ID do produto deve ser maior que zero."
            );
        }

        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException(
                    "Descrição do produto é obrigatória."
            );
        }

        if (estoqueAtual == null || estoqueAtual < 0) {
            throw new IllegalArgumentException(
                    "Estoque atual não pode ser nulo ou negativo."
            );
        }

        if (estoqueMinimo == null || estoqueMinimo < 0) {
            throw new IllegalArgumentException(
                    "Estoque mínimo não pode ser nulo ou negativo."
            );
        }

        if (diferenca == null) {
            throw new IllegalArgumentException(
                    "Diferença de estoque é obrigatória."
            );
        }

        if (situacao == null) {
            throw new IllegalArgumentException(
                    "Situação de estoque é obrigatória."
            );
        }

        this.produtoId = produtoId;
        this.descricao = descricao.trim();
        this.estoqueAtual = estoqueAtual;
        this.estoqueMinimo = estoqueMinimo;
        this.diferenca = diferenca;
        this.situacao = situacao;
        this.ativo = ativo;
    }

    public Integer getProdutoId() {
        return produtoId;
    }

    public String getDescricao() {
        return descricao;
    }

    public Integer getEstoqueAtual() {
        return estoqueAtual;
    }

    public Integer getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public Integer getDiferenca() {
        return diferenca;
    }

    public SituacaoEstoqueProduto getSituacao() {
        return situacao;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
