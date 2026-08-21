package br.com.luis.model;

import java.math.BigDecimal;

/**
 * Promoção vinculada a um produto, com desconto percentual ou por valor fixo.
 */
public class Promocao {

    private Integer idPromocao;
    private TipoDesconto tipoDesconto;
    private BigDecimal valorDesconto;
    private boolean ativa;

    private Produto produto;

    public Promocao() {
    }

    public Promocao(Integer idPromocao, TipoDesconto tipoDesconto, BigDecimal valorDesconto,
                    boolean ativa, Produto produto) {

        this.idPromocao = idPromocao;

        // O produto precisa estar definido antes da validação do desconto.
        setProduto(produto);
        setTipoDesconto(tipoDesconto);
        setValorDesconto(valorDesconto);

        this.ativa = ativa;
    }

    public enum TipoDesconto {
        PERCENTUAL,
        VALOR_FIXO
    }

    public Integer getIdPromocao() {
        return idPromocao;
    }

    public void setIdPromocao(Integer idPromocao) {
        this.idPromocao = idPromocao;
    }

    public TipoDesconto getTipoDesconto() {
        return tipoDesconto;
    }

    public void setTipoDesconto(TipoDesconto tipoDesconto) {
        if (tipoDesconto == null) {
            throw new IllegalArgumentException("O tipo de desconto é obrigatório.");
        }
        this.tipoDesconto = tipoDesconto;
    }

    public BigDecimal getValorDesconto() {
        return valorDesconto;
    }

    public void setValorDesconto(BigDecimal valorDesconto) {

        if (valorDesconto == null || valorDesconto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do desconto deve ser maior que zero.");
        }

        if (tipoDesconto == TipoDesconto.PERCENTUAL) {

            if (valorDesconto.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Desconto percentual não pode ser maior que 100%.");
            }

        } else if (tipoDesconto == TipoDesconto.VALOR_FIXO) {

            if (produto != null && produto.getPreco() != null &&
                    valorDesconto.compareTo(produto.getPreco()) > 0) {
                throw new IllegalArgumentException("Desconto não pode ser maior que o preço do produto.");
            }
        }

        this.valorDesconto = valorDesconto;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        if (produto == null) {
            throw new IllegalArgumentException("A promoção deve estar vinculada a um produto.");
        }
        this.produto = produto;
    }

    /**
     * A identidade da promoção é definida pelo ID persistido.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Promocao promocao = (Promocao) o;

        return idPromocao != null && idPromocao.equals(promocao.idPromocao);
    }

    @Override
    public int hashCode() {
        return idPromocao != null ? idPromocao.hashCode() : 0;
    }

    @Override
    public String toString() {
        return tipoDesconto + " - " + valorDesconto;
    }
}