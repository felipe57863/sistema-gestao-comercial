package br.com.luis.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Entidade que representa um Produto no sistema.
 * Aplica validações (Fail-Fast) para garantir integridade dos dados.
 */
public class Produto {

    private Integer idProduto;
    private String descricao;
    private BigDecimal preco;
    private Integer quantidadeEstoque;
    private Integer estoqueMinimo;
    private boolean ativo; // No banco: 1 (true) / 0 (false)

    /**
     * Construtor vazio (necessário para frameworks e reflexão)
     */
    public Produto() {
    }

    /**
     * Construtor completo com validação via setters (padrão seguro)
     */
    public Produto(Integer idProduto, String descricao, BigDecimal preco,
                   Integer quantidadeEstoque, Integer estoqueMinimo, boolean ativo) {

        // Usa setters para reaproveitar validações
        setIdProduto(idProduto);
        setDescricao(descricao);
        setPreco(preco);
        setQuantidadeEstoque(quantidadeEstoque);
        setEstoqueMinimo(estoqueMinimo);
        setAtivo(ativo);
    }

    // --- GETTERS E SETTERS COM VALIDAÇÃO (FAIL-FAST) ---

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
     * equals e hashCode baseados no ID.
     * Essencial para funcionamento correto em TableView (seleção e atualização).
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

    /**
     * toString útil para ComboBox e logs
     */
    @Override
    public String toString() {
        if (descricao == null || preco == null) {
            return "Produto não definido";
        }

        return descricao + " - R$ " + preco;
    }
}