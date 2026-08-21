package br.com.luis.viewmodel;

import br.com.luis.model.ItemVenda;

/**
 * Linha visual do carrinho com valores já formatados e referência ao ItemVenda original.
 *
 * Não representa entidade persistida.
 */
public class ItemCarrinhoView {

    private Integer produtoId;
    private String nomeProduto;
    private String precoFormatado;
    private String promocaoFormatada;
    private Integer quantidade;
    private String subtotalFormatado;
    private ItemVenda itemVenda;

    public ItemCarrinhoView() {
    }

    public ItemCarrinhoView(
            Integer produtoId,
            String nomeProduto,
            String precoFormatado,
            String promocaoFormatada,
            Integer quantidade,
            String subtotalFormatado,
            ItemVenda itemVenda
    ) {
        this.produtoId = produtoId;
        this.nomeProduto = nomeProduto;
        this.precoFormatado = precoFormatado;
        this.promocaoFormatada = promocaoFormatada;
        this.quantidade = quantidade;
        this.subtotalFormatado = subtotalFormatado;
        this.itemVenda = itemVenda;
    }

    public Integer getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Integer produtoId) {
        this.produtoId = produtoId;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public void setNomeProduto(String nomeProduto) {
        this.nomeProduto = nomeProduto;
    }

    public String getPrecoFormatado() {
        return precoFormatado;
    }

    public void setPrecoFormatado(String precoFormatado) {
        this.precoFormatado = precoFormatado;
    }

    public String getPromocaoFormatada() {
        return promocaoFormatada;
    }

    public void setPromocaoFormatada(String promocaoFormatada) {
        this.promocaoFormatada = promocaoFormatada;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public String getSubtotalFormatado() {
        return subtotalFormatado;
    }

    public void setSubtotalFormatado(String subtotalFormatado) {
        this.subtotalFormatado = subtotalFormatado;
    }

    public ItemVenda getItemVenda() {
        return itemVenda;
    }

    public void setItemVenda(ItemVenda itemVenda) {
        this.itemVenda = itemVenda;
    }

    @Override
    public String toString() {
        return "ItemCarrinhoView{" +
                "produtoId=" + produtoId +
                ", nomeProduto='" + nomeProduto + '\'' +
                ", precoFormatado='" + precoFormatado + '\'' +
                ", promocaoFormatada='" + promocaoFormatada + '\'' +
                ", quantidade=" + quantidade +
                ", subtotalFormatado='" + subtotalFormatado + '\'' +
                '}';
    }
}