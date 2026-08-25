package br.com.luis.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Dados de um produto recebido em uma entrada de estoque.
 */
public class ItemEntradaEstoque {

    private Integer idItemEntrada;
    private Integer entradaId;
    private Integer produtoId;
    private String descricaoProduto;
    private Integer quantidadeRecebida;
    private BigDecimal precoCompraUnitario;
    private BigDecimal subtotal;

    public ItemEntradaEstoque() {
    }

    public ItemEntradaEstoque(Integer idItemEntrada, Integer entradaId, Integer produtoId,
                              String descricaoProduto, Integer quantidadeRecebida,
                              BigDecimal precoCompraUnitario) {
        setIdItemEntrada(idItemEntrada);
        setEntradaId(entradaId);
        setProdutoId(produtoId);
        setDescricaoProduto(descricaoProduto);
        setQuantidadeRecebida(quantidadeRecebida);
        setPrecoCompraUnitario(precoCompraUnitario);
    }

    public Integer getIdItemEntrada() {
        return idItemEntrada;
    }

    public void setIdItemEntrada(Integer idItemEntrada) {
        if (idItemEntrada != null && idItemEntrada <= 0) {
            throw new IllegalArgumentException("ID do item da entrada deve ser positivo.");
        }
        this.idItemEntrada = idItemEntrada;
    }

    public Integer getEntradaId() {
        return entradaId;
    }

    public void setEntradaId(Integer entradaId) {
        if (entradaId != null && entradaId <= 0) {
            throw new IllegalArgumentException("ID da entrada deve ser positivo.");
        }
        this.entradaId = entradaId;
    }

    public Integer getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Integer produtoId) {
        if (produtoId != null && produtoId <= 0) {
            throw new IllegalArgumentException("ID do produto deve ser positivo.");
        }
        this.produtoId = produtoId;
    }

    public String getDescricaoProduto() {
        return descricaoProduto;
    }

    public void setDescricaoProduto(String descricaoProduto) {
        if (descricaoProduto != null && descricaoProduto.isBlank()) {
            throw new IllegalArgumentException("Descrição do produto não pode ser vazia.");
        }
        this.descricaoProduto = descricaoProduto != null ? descricaoProduto.trim() : null;
    }

    public Integer getQuantidadeRecebida() {
        return quantidadeRecebida;
    }

    public void setQuantidadeRecebida(Integer quantidadeRecebida) {
        if (quantidadeRecebida == null || quantidadeRecebida <= 0) {
            throw new IllegalArgumentException("Quantidade recebida deve ser maior que zero.");
        }
        this.quantidadeRecebida = quantidadeRecebida;
        calcularSubtotal();
    }

    public BigDecimal getPrecoCompraUnitario() {
        return precoCompraUnitario;
    }

    public void setPrecoCompraUnitario(BigDecimal precoCompraUnitario) {
        if (precoCompraUnitario == null) {
            throw new IllegalArgumentException("Preço de compra unitário é obrigatório.");
        }

        BigDecimal precoNormalizado = precoCompraUnitario.setScale(2, RoundingMode.HALF_UP);
        if (precoNormalizado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Preço de compra unitário deve ser maior que zero.");
        }

        this.precoCompraUnitario = precoNormalizado;
        calcularSubtotal();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    /**
     * Calcula o subtotal com os valores do próprio item.
     */
    public BigDecimal calcularSubtotal() {
        if (quantidadeRecebida == null || precoCompraUnitario == null) {
            this.subtotal = null;
            return null;
        }

        this.subtotal = precoCompraUnitario
                .multiply(BigDecimal.valueOf(quantidadeRecebida))
                .setScale(2, RoundingMode.HALF_UP);

        return this.subtotal;
    }
}
