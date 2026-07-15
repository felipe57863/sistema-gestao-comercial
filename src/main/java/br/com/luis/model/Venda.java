package br.com.luis.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade que representa uma venda no sistema.
 *
 * Armazena os dados principais da venda e mantém em memória os itens associados.
 * Pode representar tanto uma venda aberta quanto uma venda preparada pelo
 * VendaService para persistência, à vista ou a prazo.
 *
 * Esta entidade não acessa banco de dados nem controla transações. As validações
 * e regras de finalização pertencem ao VendaService, enquanto a persistência dos
 * dados da venda é executada pelo VendaDAO no fluxo transacional.
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
     * Cada item representa um produto adicionado ao carrinho ou vinculado à venda
     * preparada para persistência.
     */
    private List<ItemVenda> itens;

    /**
     * Construtor padrão.
     *
     * Inicializa uma venda aberta com data e hora atuais, valores monetários
     * zerados e lista de itens vazia. Permite o preenchimento gradual dos dados
     * durante a montagem e a finalização da venda.
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
     * Manipula somente a lista mantida pela entidade. Validação de estoque,
     * consulta e aplicação de promoção e demais regras de negócio pertencem
     * ao VendaService.
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
     * Manipula somente a lista mantida pela entidade. Não remove dados já
     * persistidos nem executa baixa, reposição ou validação de estoque.
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
     * O total é formado exclusivamente pela soma dos subtotais atuais de cada
     * ItemVenda. Descontos promocionais e globais já refletidos nesses subtotais
     * não são novamente subtraídos por este método.
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