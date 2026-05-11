package br.com.luis.model;

import java.math.BigDecimal;

/**
 * Entidade que representa um item pertencente a uma venda.
 *
 * Cada ItemVenda representa um produto adicionado à venda,
 * armazenando a quantidade, o preço praticado no momento da venda,
 * o desconto promocional aplicado, o desconto global aplicado
 * e o subtotal calculado.
 *
 * Nesta fase, esta classe ainda não valida estoque, não aplica promoção
 * automaticamente e não realiza baixa de estoque.
 */
public class ItemVenda {

    private Integer idItem;
    private Integer quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal descontoPromocional;
    private BigDecimal descontoGlobal;
    private BigDecimal subtotal;
    private Integer produtoId;
    private Integer vendaId;

    /**
     * Construtor padrão.
     *
     * Define valores iniciais seguros para evitar null em campos monetários.
     */
    public ItemVenda() {
        this.quantidade = 1;
        this.precoUnitario = BigDecimal.ZERO;
        this.descontoPromocional = BigDecimal.ZERO;
        this.descontoGlobal = BigDecimal.ZERO;
        this.subtotal = BigDecimal.ZERO;
    }

    /**
     * Construtor auxiliar para criar um item com os dados principais.
     *
     * @param produtoId ID do produto vendido.
     * @param quantidade quantidade adicionada à venda.
     * @param precoUnitario preço do produto no momento da venda.
     */
    public ItemVenda(Integer produtoId, Integer quantidade, BigDecimal precoUnitario) {
        this();
        this.produtoId = produtoId;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        calcularSubtotal();
    }

    /**
     * Calcula o subtotal do item com base na quantidade, preço unitário,
     * desconto promocional e desconto global.
     *
     * Fórmula:
     * subtotal = quantidade * precoUnitario - descontoPromocional - descontoGlobal
     *
     * @return subtotal calculado do item.
     */
    public BigDecimal calcularSubtotal() {
        BigDecimal quantidadeCalculada = BigDecimal.valueOf(this.quantidade);
        BigDecimal valorBruto = this.precoUnitario.multiply(quantidadeCalculada);

        this.subtotal = valorBruto
                .subtract(this.descontoPromocional)
                .subtract(this.descontoGlobal);

        return this.subtotal;
    }

    public Integer getIdItem() {
        return idItem;
    }

    public void setIdItem(Integer idItem) {
        this.idItem = idItem;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
        calcularSubtotal();
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario != null ? precoUnitario : BigDecimal.ZERO;
        calcularSubtotal();
    }

    public BigDecimal getDescontoPromocional() {
        return descontoPromocional;
    }

    public void setDescontoPromocional(BigDecimal descontoPromocional) {
        this.descontoPromocional = descontoPromocional != null ? descontoPromocional : BigDecimal.ZERO;
        calcularSubtotal();
    }

    public BigDecimal getDescontoGlobal() {
        return descontoGlobal;
    }

    public void setDescontoGlobal(BigDecimal descontoGlobal) {
        this.descontoGlobal = descontoGlobal != null ? descontoGlobal : BigDecimal.ZERO;
        calcularSubtotal();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal != null ? subtotal : BigDecimal.ZERO;
    }

    public Integer getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Integer produtoId) {
        this.produtoId = produtoId;
    }

    public Integer getVendaId() {
        return vendaId;
    }

    public void setVendaId(Integer vendaId) {
        this.vendaId = vendaId;
    }

    @Override
    public String toString() {
        return "ItemVenda{" +
                "idItem=" + idItem +
                ", quantidade=" + quantidade +
                ", precoUnitario=" + precoUnitario +
                ", descontoPromocional=" + descontoPromocional +
                ", descontoGlobal=" + descontoGlobal +
                ", subtotal=" + subtotal +
                ", produtoId=" + produtoId +
                ", vendaId=" + vendaId +
                '}';
    }
}