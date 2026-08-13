package br.com.luis.viewmodel;

import br.com.luis.model.Promocao.TipoDesconto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Representa uma única linha final da visão de produtos em promoção.
 *
 * A classe é imutável e transporta somente os dados necessários ao relatório.
 * Não calcula preço promocional, não depende dos objetos completos Produto ou
 * Promocao e não acessa banco de dados, DAO, Service, sessão ou JavaFX.
 */
public final class ProdutoPromocaoRelatorioView {

    private static final int ESCALA_MONETARIA = 2;

    private static final RoundingMode ARREDONDAMENTO_MONETARIO =
            RoundingMode.HALF_UP;

    private final Integer produtoId;
    private final String descricao;
    private final BigDecimal precoNormal;
    private final TipoDesconto tipoDesconto;
    private final BigDecimal valorDesconto;
    private final boolean produtoAtivo;

    /**
     * Cria uma linha imutável da visão de produtos em promoção.
     *
     * O preço normal é armazenado com a escala monetária do projeto. O valor do
     * desconto é preservado, pois pode representar percentual ou valor fixo. As
     * regras de limite percentual e de relação entre desconto fixo e preço
     * pertencem ao domínio e não são recalculadas neste contrato.
     *
     * @param produtoId identificador do produto.
     * @param descricao descrição atual do produto.
     * @param precoNormal preço cadastral normal do produto.
     * @param tipoDesconto tipo real do desconto da promoção ativa.
     * @param valorDesconto valor informado para o desconto.
     * @param produtoAtivo status cadastral atual do produto.
     * @throws IllegalArgumentException quando algum dado obrigatório for inválido.
     */
    public ProdutoPromocaoRelatorioView(
            Integer produtoId,
            String descricao,
            BigDecimal precoNormal,
            TipoDesconto tipoDesconto,
            BigDecimal valorDesconto,
            boolean produtoAtivo
    ) {
        if (produtoId == null || produtoId <= 0) {
            throw new IllegalArgumentException(
                    "ID do produto deve ser maior que zero."
            );
        }

        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException(
                    "Descrição do produto é obrigatória."
            );
        }

        if (precoNormal == null) {
            throw new IllegalArgumentException(
                    "Preço normal do produto é obrigatório."
            );
        }

        if (precoNormal.signum() < 0) {
            throw new IllegalArgumentException(
                    "Preço normal do produto não pode ser negativo."
            );
        }

        if (tipoDesconto == null) {
            throw new IllegalArgumentException(
                    "Tipo de desconto é obrigatório."
            );
        }

        if (valorDesconto == null) {
            throw new IllegalArgumentException(
                    "Valor do desconto é obrigatório."
            );
        }

        if (valorDesconto.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Valor do desconto deve ser maior que zero."
            );
        }

        this.produtoId = produtoId;
        this.descricao = descricao.trim();
        this.precoNormal = normalizarValorMonetario(precoNormal);
        this.tipoDesconto = tipoDesconto;
        this.valorDesconto = valorDesconto;
        this.produtoAtivo = produtoAtivo;
    }

    private static BigDecimal normalizarValorMonetario(BigDecimal valor) {
        return valor.setScale(
                ESCALA_MONETARIA,
                ARREDONDAMENTO_MONETARIO
        );
    }

    public Integer getProdutoId() {
        return produtoId;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getPrecoNormal() {
        return precoNormal;
    }

    public TipoDesconto getTipoDesconto() {
        return tipoDesconto;
    }

    public BigDecimal getValorDesconto() {
        return valorDesconto;
    }

    public boolean isProdutoAtivo() {
        return produtoAtivo;
    }
}
