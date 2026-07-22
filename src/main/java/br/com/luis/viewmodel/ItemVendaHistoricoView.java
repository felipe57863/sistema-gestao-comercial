package br.com.luis.viewmodel;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Representa um item persistido pertencente ao histórico de uma venda.
 *
 * Preserva os valores históricos gravados em ItemVenda e acrescenta
 * a descrição do produto para apresentação no Histórico de Vendas.
 */
public class ItemVendaHistoricoView {

    private Integer produtoId;
    private String descricaoProduto;
    private Integer quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal subtotal;

    /**
     * Construtor padrão.
     */
    public ItemVendaHistoricoView() {
        this.quantidade = 0;
        this.precoUnitario = BigDecimal.ZERO.setScale(
                2,
                RoundingMode.HALF_UP
        );
        this.subtotal = BigDecimal.ZERO.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    /**
     * Construtor completo.
     */
    public ItemVendaHistoricoView(
            Integer produtoId,
            String descricaoProduto,
            Integer quantidade,
            BigDecimal precoUnitario,
            BigDecimal subtotal
    ) {
        this();

        this.produtoId = produtoId;
        this.descricaoProduto = descricaoProduto;
        setQuantidade(quantidade);
        setPrecoUnitario(precoUnitario);
        setSubtotal(subtotal);
    }

    public Integer getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Integer produtoId) {
        this.produtoId = produtoId;
    }

    public String getDescricaoProduto() {
        return descricaoProduto;
    }

    public void setDescricaoProduto(String descricaoProduto) {
        this.descricaoProduto = descricaoProduto;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade =
                quantidade != null
                        ? quantidade
                        : 0;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {

        BigDecimal valorSeguro =
                precoUnitario != null
                        ? precoUnitario
                        : BigDecimal.ZERO;

        this.precoUnitario = valorSeguro.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {

        BigDecimal valorSeguro =
                subtotal != null
                        ? subtotal
                        : BigDecimal.ZERO;

        this.subtotal = valorSeguro.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    @Override
    public String toString() {
        return "ItemVendaHistoricoView{" +
                "produtoId=" + produtoId +
                ", descricaoProduto='" + descricaoProduto + '\'' +
                ", quantidade=" + quantidade +
                ", precoUnitario=" + precoUnitario +
                ", subtotal=" + subtotal +
                '}';
    }
}