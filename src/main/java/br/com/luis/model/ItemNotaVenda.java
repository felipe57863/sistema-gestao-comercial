package br.com.luis.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Entidade que representa um item da fotografia histórica de uma Nota de Venda.
 *
 * Os dados são copiados no momento da finalização da venda e permanecem
 * independentes dos dados cadastrais atuais do Produto. O item documental não
 * deve ser alterado durante geração ou reimpressão do PDF.
 */
public class ItemNotaVenda {

    private static final int ESCALA_MONETARIA = 2;

    private Integer idItemNota;
    private Integer notaId;
    private Integer produtoId;
    private String descricaoProduto;
    private Integer quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal descontoPromocional;
    private BigDecimal descontoGlobal;
    private BigDecimal subtotal;

    public ItemNotaVenda() {
        this.quantidade = 1;
        this.precoUnitario = zeroMonetario();
        this.descontoPromocional = zeroMonetario();
        this.descontoGlobal = zeroMonetario();
        this.subtotal = zeroMonetario();
    }

    public ItemNotaVenda(
            Integer idItemNota,
            Integer notaId,
            Integer produtoId,
            String descricaoProduto,
            Integer quantidade,
            BigDecimal precoUnitario,
            BigDecimal descontoPromocional,
            BigDecimal descontoGlobal,
            BigDecimal subtotal
    ) {
        this();
        setIdItemNota(idItemNota);
        setNotaId(notaId);
        setProdutoId(produtoId);
        setDescricaoProduto(descricaoProduto);
        setQuantidade(quantidade);
        setPrecoUnitario(precoUnitario);
        setDescontoPromocional(descontoPromocional);
        setDescontoGlobal(descontoGlobal);
        setSubtotal(subtotal);
    }

    public Integer getIdItemNota() {
        return idItemNota;
    }

    public void setIdItemNota(Integer idItemNota) {
        if (idItemNota != null && idItemNota <= 0) {
            throw new IllegalArgumentException(
                    "ID do item da Nota de Venda deve ser positivo."
            );
        }
        this.idItemNota = idItemNota;
    }

    public Integer getNotaId() {
        return notaId;
    }

    public void setNotaId(Integer notaId) {
        if (notaId == null || notaId <= 0) {
            throw new IllegalArgumentException(
                    "ID da Nota de Venda é obrigatório para o item."
            );
        }
        this.notaId = notaId;
    }

    public Integer getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Integer produtoId) {
        if (produtoId == null || produtoId <= 0) {
            throw new IllegalArgumentException(
                    "ID histórico do produto deve ser maior que zero."
            );
        }
        this.produtoId = produtoId;
    }

    public String getDescricaoProduto() {
        return descricaoProduto;
    }

    public void setDescricaoProduto(String descricaoProduto) {
        if (descricaoProduto == null || descricaoProduto.isBlank()) {
            throw new IllegalArgumentException(
                    "Descrição histórica do produto é obrigatória."
            );
        }
        this.descricaoProduto = descricaoProduto;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException(
                    "Quantidade do item da Nota de Venda deve ser maior que zero."
            );
        }
        this.quantidade = quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = validarENormalizarValorNaoNegativo(
                precoUnitario,
                "Preço unitário"
        );
    }

    public BigDecimal getDescontoPromocional() {
        return descontoPromocional;
    }

    public void setDescontoPromocional(BigDecimal descontoPromocional) {
        this.descontoPromocional = validarENormalizarValorNaoNegativo(
                descontoPromocional,
                "Desconto promocional"
        );
    }

    public BigDecimal getDescontoGlobal() {
        return descontoGlobal;
    }

    public void setDescontoGlobal(BigDecimal descontoGlobal) {
        this.descontoGlobal = validarENormalizarValorNaoNegativo(
                descontoGlobal,
                "Desconto global"
        );
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = validarENormalizarValorNaoNegativo(
                subtotal,
                "Subtotal"
        );
    }

    private BigDecimal validarENormalizarValorNaoNegativo(
            BigDecimal valor,
            String nomeCampo
    ) {
        if (valor == null) {
            throw new IllegalArgumentException(
                    nomeCampo + " é obrigatório no item da Nota de Venda."
            );
        }

        if (valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    nomeCampo + " não pode ser negativo no item da Nota de Venda."
            );
        }

        return valor.setScale(
                ESCALA_MONETARIA,
                RoundingMode.HALF_UP
        );
    }

    private static BigDecimal zeroMonetario() {
        return BigDecimal.ZERO.setScale(
                ESCALA_MONETARIA,
                RoundingMode.HALF_UP
        );
    }

    @Override
    public String toString() {
        return "ItemNotaVenda{" +
                "idItemNota=" + idItemNota +
                ", notaId=" + notaId +
                ", produtoId=" + produtoId +
                ", descricaoProduto='" + descricaoProduto + '\'' +
                ", quantidade=" + quantidade +
                ", precoUnitario=" + precoUnitario +
                ", descontoPromocional=" + descontoPromocional +
                ", descontoGlobal=" + descontoGlobal +
                ", subtotal=" + subtotal +
                '}';
    }
}
