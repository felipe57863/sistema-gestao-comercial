package br.com.luis.viewmodel;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Projeção imutável de um item histórico de entrada de estoque.
 */
public final class ItemEntradaEstoqueRelatorioView {

    private final Integer idItemEntrada;
    private final Integer entradaId;
    private final Integer produtoId;
    private final String descricaoProduto;
    private final Integer quantidadeRecebida;
    private final BigDecimal precoCompraUnitario;
    private final BigDecimal subtotal;

    public ItemEntradaEstoqueRelatorioView(
            Integer idItemEntrada,
            Integer entradaId,
            Integer produtoId,
            String descricaoProduto,
            Integer quantidadeRecebida,
            BigDecimal precoCompraUnitario,
            BigDecimal subtotal
    ) {
        validarId(idItemEntrada, "ID do item");
        validarId(entradaId, "ID da entrada");
        validarId(produtoId, "ID do produto");
        if (descricaoProduto == null || descricaoProduto.isBlank()) {
            throw new IllegalArgumentException("Descrição do produto é obrigatória.");
        }
        if (quantidadeRecebida == null || quantidadeRecebida <= 0) {
            throw new IllegalArgumentException(
                    "Quantidade recebida deve ser maior que zero."
            );
        }
        validarValorPositivo(precoCompraUnitario, "Preço de compra unitário");
        validarValorPositivo(subtotal, "Subtotal");

        this.idItemEntrada = idItemEntrada;
        this.entradaId = entradaId;
        this.produtoId = produtoId;
        this.descricaoProduto = descricaoProduto.trim();
        this.quantidadeRecebida = quantidadeRecebida;
        this.precoCompraUnitario = normalizarValor(precoCompraUnitario);
        this.subtotal = normalizarValor(subtotal);
    }

    private static void validarId(Integer id, String campo) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(campo + " deve ser maior que zero.");
        }
    }

    private static void validarValorPositivo(BigDecimal valor, String campo) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(campo + " deve ser maior que zero.");
        }
    }

    private static BigDecimal normalizarValor(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    public Integer getIdItemEntrada() {
        return idItemEntrada;
    }

    public Integer getEntradaId() {
        return entradaId;
    }

    public Integer getProdutoId() {
        return produtoId;
    }

    public String getDescricaoProduto() {
        return descricaoProduto;
    }

    public Integer getQuantidadeRecebida() {
        return quantidadeRecebida;
    }

    public BigDecimal getPrecoCompraUnitario() {
        return precoCompraUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}
