package br.com.luis.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Dados de um produto e suas validações básicas de integridade.
 */
public class Produto {

    private Integer idProduto;
    private String descricao;
    private BigDecimal preco;
    private Integer quantidadeEstoque;
    private Integer estoqueMinimo;
    private boolean ativo;

    public Produto() {
    }

    public Produto(Integer idProduto, String descricao, BigDecimal preco,
                   Integer quantidadeEstoque, Integer estoqueMinimo, boolean ativo) {

        setIdProduto(idProduto);
        setDescricao(descricao);
        setPreco(preco);
        setQuantidadeEstoque(quantidadeEstoque);
        setEstoqueMinimo(estoqueMinimo);
        setAtivo(ativo);
    }

    public Integer getIdProduto() {
        return idProduto;
    }

    public void setIdProduto(Integer idProduto) {
        if (idProduto != null && idProduto <= 0) {
            throw new IllegalArgumentException("ID do produto deve ser positivo.");
        }
        this.idProduto = idProduto;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("A descrição do produto é obrigatória.");
        }
        this.descricao = descricao.trim();
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        if (preco == null || preco.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O preço não pode ser negativo.");
        }
        this.preco = preco.setScale(2, RoundingMode.HALF_UP);
    }

    public Integer getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public void setQuantidadeEstoque(Integer quantidadeEstoque) {
        if (quantidadeEstoque == null || quantidadeEstoque < 0) {
            throw new IllegalArgumentException("Quantidade em estoque não pode ser negativa.");
        }
        this.quantidadeEstoque = quantidadeEstoque;
    }

    public Integer getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public void setEstoqueMinimo(Integer estoqueMinimo) {
        if (estoqueMinimo == null || estoqueMinimo < 0) {
            throw new IllegalArgumentException("Estoque mínimo não pode ser negativo.");
        }
        this.estoqueMinimo = estoqueMinimo;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    /**
     * A identidade do produto é definida pelo ID persistido.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Produto produto = (Produto) o;

        return idProduto != null && idProduto.equals(produto.idProduto);
    }

    @Override
    public int hashCode() {
        return idProduto != null ? idProduto.hashCode() : 0;
    }

    @Override
    public String toString() {
        if (descricao == null || preco == null) {
            return "Produto não definido";
        }

        return descricao + " - R$ " + preco;
    }
}
