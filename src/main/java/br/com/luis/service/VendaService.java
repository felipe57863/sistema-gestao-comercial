package br.com.luis.service;

import br.com.luis.model.ItemVenda;
import br.com.luis.model.Produto;
import br.com.luis.model.Promocao;
import br.com.luis.model.Venda;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Camada de Serviço responsável pelas regras de negócio da venda.
 *
 * Nesta fase, o serviço trabalha apenas com o carrinho em memória.
 * A persistência completa da venda, finalização, baixa de estoque e financeiro
 * serão tratados em etapas futuras.
 */
public class VendaService {

    private final ProdutoService produtoService;
    private final PromocaoService promocaoService;

    public VendaService() {
        this.produtoService = new ProdutoService();
        this.promocaoService = new PromocaoService();
    }

    /**
     * Adiciona um produto ao carrinho da venda.
     *
     * Se o produto ainda não existir no carrinho, cria um novo ItemVenda.
     * Se o produto já existir, soma a nova quantidade à quantidade existente,
     * valida o estoque com base na quantidade total acumulada e recalcula
     * subtotal e total.
     *
     * Importante:
     * nesta fase, o estoque NÃO é baixado. Apenas validamos se existe estoque
     * suficiente para permitir a inclusão no carrinho.
     *
     * @implNote Implementa a RN01 - Não permitir venda sem estoque.
     * @implNote Implementa a RN02 - Aplicação automática de promoção.
     *
     * @param venda venda em memória que receberá o item.
     * @param idProduto ID do produto que será adicionado.
     * @param quantidade quantidade desejada do produto.
     */
    public void adicionarItemAoCarrinho(Venda venda, Integer idProduto, Integer quantidade) {

        if (venda == null) {
            throw new IllegalArgumentException("Venda inválida para adicionar item.");
        }

        Produto produto = produtoService.buscarPorId(idProduto);

        validarProdutoParaVenda(produto);
        validarQuantidade(quantidade);

        Promocao promocaoAtiva = promocaoService.buscarPromocaoAtivaPorProduto(produto);

        ItemVenda itemExistente = buscarItemPorProduto(venda, produto.getIdProduto());

        if (itemExistente != null) {
            atualizarItemExistente(itemExistente, produto, promocaoAtiva, quantidade);
            venda.recalcularTotal();
            return;
        }

        validarEstoque(produto, quantidade);

        ItemVenda novoItem = criarNovoItem(produto, promocaoAtiva, quantidade);

        venda.adicionarItem(novoItem);
    }

    /**
     * Busca no carrinho um item que já represente o produto informado.
     *
     * @implNote Apoia a regra do carrinho da Fase 4.2:
     * produtos repetidos devem ter suas quantidades somadas.
     */
    private ItemVenda buscarItemPorProduto(Venda venda, Integer idProduto) {

        for (ItemVenda item : venda.getItens()) {
            if (item.getProdutoId() != null && item.getProdutoId().equals(idProduto)) {
                return item;
            }
        }

        return null;
    }

    /**
     * Atualiza um item já existente no carrinho, somando a nova quantidade,
     * validando o estoque acumulado e recalculando desconto e subtotal.
     *
     * @implNote Implementa a RN01 - Não permitir venda sem estoque.
     * @implNote Implementa a RN02 - Aplicação automática de promoção.
     */
    private void atualizarItemExistente(
            ItemVenda itemExistente,
            Produto produto,
            Promocao promocaoAtiva,
            Integer quantidadeNova
    ) {

        Integer quantidadeTotal = itemExistente.getQuantidade() + quantidadeNova;

        validarEstoque(produto, quantidadeTotal);

        itemExistente.setQuantidade(quantidadeTotal);

        BigDecimal descontoPromocional = calcularDescontoPromocional(
                itemExistente.getPrecoUnitario(),
                promocaoAtiva,
                quantidadeTotal
        );

        itemExistente.setDescontoPromocional(descontoPromocional);
        itemExistente.calcularSubtotal();
    }

    /**
     * Cria um novo item de venda para produto que ainda não existe no carrinho.
     *
     * @implNote Implementa a RN02 - Aplicação automática de promoção.
     */
    private ItemVenda criarNovoItem(
            Produto produto,
            Promocao promocaoAtiva,
            Integer quantidade
    ) {

        ItemVenda item = new ItemVenda(
                produto.getIdProduto(),
                quantidade,
                produto.getPreco()
        );

        BigDecimal descontoPromocional = calcularDescontoPromocional(
                produto.getPreco(),
                promocaoAtiva,
                quantidade
        );

        item.setDescontoPromocional(descontoPromocional);
        item.calcularSubtotal();

        return item;
    }

    /**
     * Valida se o produto pode ser usado em uma venda.
     *
     * @implNote Implementa validação de apoio à RN01 - Não permitir venda sem estoque.
     */
    private void validarProdutoParaVenda(Produto produto) {

        if (produto == null) {
            throw new IllegalArgumentException("Produto não encontrado.");
        }

        if (!produto.isAtivo()) {
            throw new IllegalArgumentException("Produto inativo não pode ser vendido.");
        }

        if (produto.getPreco() == null) {
            throw new IllegalArgumentException("Produto sem preço cadastrado.");
        }

        if (produto.getQuantidadeEstoque() == null) {
            throw new IllegalArgumentException("Produto sem quantidade de estoque cadastrada.");
        }
    }

    /**
     * Valida se a quantidade informada é permitida.
     *
     * @implNote Implementa validação de apoio à RN01 - Não permitir venda sem estoque.
     */
    private void validarQuantidade(Integer quantidade) {

        if (quantidade == null) {
            throw new IllegalArgumentException("Quantidade do item é obrigatória.");
        }

        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade do item deve ser maior que zero.");
        }
    }

    /**
     * Valida se existe estoque suficiente para adicionar o item ao carrinho.
     *
     * @implNote Implementa a RN01 - Não permitir venda sem estoque.
     */
    private void validarEstoque(Produto produto, Integer quantidade) {

        if (produto.getQuantidadeEstoque() <= 0) {
            throw new IllegalArgumentException("Produto sem estoque disponível.");
        }

        if (quantidade > produto.getQuantidadeEstoque()) {
            throw new IllegalArgumentException(
                    "Estoque insuficiente. Disponível: " + produto.getQuantidadeEstoque()
            );
        }
    }

    /**
     * Calcula o desconto promocional aplicado ao item.
     *
     * Para desconto percentual, calcula sobre o valor bruto do item:
     * quantidade * preço unitário.
     *
     * Para desconto de valor fixo, considera o desconto por unidade
     * multiplicado pela quantidade.
     *
     * Caso não exista promoção ativa, retorna zero.
     *
     * @implNote Implementa a RN02 - Aplicação automática de promoção.
     */
    private BigDecimal calcularDescontoPromocional(
            BigDecimal precoUnitario,
            Promocao promocao,
            Integer quantidade
    ) {

        if (promocao == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal quantidadeBigDecimal = BigDecimal.valueOf(quantidade);
        BigDecimal valorBruto = precoUnitario.multiply(quantidadeBigDecimal);

        BigDecimal desconto;

        if (promocao.getTipoDesconto() == Promocao.TipoDesconto.PERCENTUAL) {
            desconto = valorBruto
                    .multiply(promocao.getValorDesconto())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else if (promocao.getTipoDesconto() == Promocao.TipoDesconto.VALOR_FIXO) {
            desconto = promocao.getValorDesconto().multiply(quantidadeBigDecimal);
        } else {
            desconto = BigDecimal.ZERO;
        }

        if (desconto.compareTo(valorBruto) > 0) {
            return valorBruto;
        }

        return desconto.setScale(2, RoundingMode.HALF_UP);
    }
}