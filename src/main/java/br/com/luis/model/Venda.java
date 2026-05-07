package br.com.luis.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade que representa uma venda no sistema.
 *
 * Nesta fase, a venda ainda representa a base estrutural do motor de vendas.
 * A finalização da venda, pagamento, financeiro, baixa de estoque e estorno
 * serão implementados em fases futuras.
 */
public class Venda {

    private Integer idVenda;
    private LocalDateTime dataHora;
    private String tipoVenda;
    private String formaPagamento;
    private BigDecimal valorTotal;
    private BigDecimal valorDescontoGlobal;
    private String status;
    private Integer usuarioId;
    private Integer clienteId;

    /**
     * Lista de itens vinculados à venda.
     *
     * Cada item representa um produto adicionado ao carrinho/venda.
     */
    private List<ItemVenda> itens;

    /**
     * Construtor padrão.
     *
     * Importante para facilitar criação manual, uso em DAOs
     * e preenchimento gradual dos dados.
     */
    public Venda() {
        this.dataHora = LocalDateTime.now();
        this.valorTotal = BigDecimal.ZERO;
        this.valorDescontoGlobal = BigDecimal.ZERO;
        this.status = "ABERTA";
        this.itens = new ArrayList<>();
    }

    /**
     * Construtor usado para criar uma nova venda vinculada
     * ao usuário logado.
     *
     * @param usuarioId ID do usuário responsável pela venda.
     */
    public Venda(Integer usuarioId) {
        this();
        this.usuarioId = usuarioId;
    }

    /**
     * Adiciona um item à venda e recalcula o valor total.
     *
     * Nesta fase, este método apenas manipula a lista em memória.
     * Validação de estoque e aplicação de promoção serão feitas
     * posteriormente na camada Service.
     *
     * @param item item que será adicionado à venda.
     */
    public void adicionarItem(ItemVenda item) {
        if (item == null) {
            return;
        }

        this.itens.add(item);
        recalcularTotal();
    }

    /**
     * Remove um item da venda e recalcula o valor total.
     *
     * Nesta fase, este método apenas manipula a lista em memória.
     *
     * @param item item que será removido da venda.
     */
    public void removerItem(ItemVenda item) {
        if (item == null) {
            return;
        }

        this.itens.remove(item);
        recalcularTotal();
    }

    /**
     * Recalcula o valor total da venda com base nos subtotais dos itens.
     *
     * Neste momento, o cálculo considera apenas os subtotais já existentes
     * em cada ItemVenda.
     *
     * A regra de desconto global será implementada somente no Passo 4.3.
     *
     * @return valor total recalculado da venda.
     */
    public BigDecimal recalcularTotal() {
        BigDecimal total = BigDecimal.ZERO;

        for (ItemVenda item : this.itens) {
            if (item.getSubtotal() != null) {
                total = total.add(item.getSubtotal());
            }
        }

        this.valorTotal = total;
        return this.valorTotal;
    }

    public Integer getIdVenda() {
        return idVenda;
    }

    public void setIdVenda(Integer idVenda) {
        this.idVenda = idVenda;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getTipoVenda() {
        return tipoVenda;
    }

    public void setTipoVenda(String tipoVenda) {
        this.tipoVenda = tipoVenda;
    }

    public String getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(String formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal != null ? valorTotal : BigDecimal.ZERO;
    }

    public BigDecimal getValorDescontoGlobal() {
        return valorDescontoGlobal;
    }

    public void setValorDescontoGlobal(BigDecimal valorDescontoGlobal) {
        this.valorDescontoGlobal = valorDescontoGlobal != null ? valorDescontoGlobal : BigDecimal.ZERO;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Integer getClienteId() {
        return clienteId;
    }

    public void setClienteId(Integer clienteId) {
        this.clienteId = clienteId;
    }

    public List<ItemVenda> getItens() {
        return itens;
    }

    public void setItens(List<ItemVenda> itens) {
        this.itens = itens != null ? itens : new ArrayList<>();
        recalcularTotal();
    }

    @Override
    public String toString() {
        return "Venda{" +
                "idVenda=" + idVenda +
                ", dataHora=" + dataHora +
                ", tipoVenda='" + tipoVenda + '\'' +
                ", formaPagamento='" + formaPagamento + '\'' +
                ", valorTotal=" + valorTotal +
                ", valorDescontoGlobal=" + valorDescontoGlobal +
                ", status='" + status + '\'' +
                ", usuarioId=" + usuarioId +
                ", clienteId=" + clienteId +
                ", itens=" + itens +
                '}';
    }
}