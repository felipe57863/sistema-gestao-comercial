package br.com.luis.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        this.valorTotal = valorTotal;
    }

    public BigDecimal getValorDescontoGlobal() {
        return valorDescontoGlobal;
    }

    public void setValorDescontoGlobal(BigDecimal valorDescontoGlobal) {
        this.valorDescontoGlobal = valorDescontoGlobal;
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
                '}';
    }
}