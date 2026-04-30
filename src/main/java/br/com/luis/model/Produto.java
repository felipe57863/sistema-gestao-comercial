package br.com.luis.model;

import java.math.BigDecimal;

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
    private StatusProduto status;

    /**
     * Construtor vazio (necessário para frameworks e reflexão)
     */
    public Produto() {
    }

    /**
     * Construtor completo com validação via setters (padrão seguro)
     */
    public Produto(Integer idProduto, String descricao, BigDecimal preco,
                   Integer quantidadeEstoque, Integer estoqueMinimo, StatusProduto status) {

        this.idProduto = idProduto;

        // Usa setters para reaproveitar validações
        setDescricao(descricao);
        setPreco(preco);
        setQuantidadeEstoque(quantidadeEstoque);
        setEstoqueMinimo(estoqueMinimo);
        setStatus(status);
    }

    /**
     * ENUM para garantir integridade do status
     */
    public enum StatusProduto {
        ATIVO, INATIVO
    }

    // --- GETTERS E SETTERS COM VALIDAÇÃO (FAIL-FAST) ---

    public Integer getIdProduto() {
        return idProduto;
    }

    public void setIdProduto(Integer idProduto) {
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
        if (preco == null || preco.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O preço deve ser maior que zero.");
        }
        this.preco = preco;
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

    public StatusProduto getStatus() {
        return status;
    }

    public void setStatus(StatusProduto status) {
        if (status == null) {
            throw new IllegalArgumentException("O status do produto é obrigatório.");
        }
        this.status = status;
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
        return descricao + " - R$ " + preco;
    }
}