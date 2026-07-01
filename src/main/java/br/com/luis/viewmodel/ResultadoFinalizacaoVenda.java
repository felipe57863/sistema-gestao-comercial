package br.com.luis.viewmodel;

import br.com.luis.model.FormaPagamento;
import br.com.luis.model.StatusVenda;
import br.com.luis.model.TipoVenda;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * ViewModel usado para retornar o resultado da finalização da venda.
 *
 * Esta classe não representa uma entidade persistida no banco de dados.
 * Ela serve apenas para transportar dados do VendaService para o Controller.
 */
public class ResultadoFinalizacaoVenda {

    private Integer vendaId;
    private TipoVenda tipoVenda;
    private StatusVenda statusVenda;
    private FormaPagamento formaPagamento;
    private BigDecimal valorTotal;
    private BigDecimal troco;
    private LocalDate dataVencimento;
    private Integer contaReceberId;
    private Integer movimentacaoFinanceiraId;

    public ResultadoFinalizacaoVenda() {
        this.valorTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.troco = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public ResultadoFinalizacaoVenda(
            Integer vendaId,
            TipoVenda tipoVenda,
            StatusVenda statusVenda,
            FormaPagamento formaPagamento,
            BigDecimal valorTotal,
            BigDecimal troco,
            LocalDate dataVencimento,
            Integer contaReceberId,
            Integer movimentacaoFinanceiraId
    ) {
        this.vendaId = vendaId;
        this.tipoVenda = tipoVenda;
        this.statusVenda = statusVenda;
        this.formaPagamento = formaPagamento;
        setValorTotal(valorTotal);
        setTroco(troco);
        this.dataVencimento = dataVencimento;
        this.contaReceberId = contaReceberId;
        this.movimentacaoFinanceiraId = movimentacaoFinanceiraId;
    }

    public Integer getVendaId() {
        return vendaId;
    }

    public void setVendaId(Integer vendaId) {
        this.vendaId = vendaId;
    }

    public TipoVenda getTipoVenda() {
        return tipoVenda;
    }

    public void setTipoVenda(TipoVenda tipoVenda) {
        this.tipoVenda = tipoVenda;
    }

    public StatusVenda getStatusVenda() {
        return statusVenda;
    }

    public void setStatusVenda(StatusVenda statusVenda) {
        this.statusVenda = statusVenda;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(FormaPagamento formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        if (valorTotal == null) {
            this.valorTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return;
        }

        this.valorTotal = valorTotal.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTroco() {
        return troco;
    }

    public void setTroco(BigDecimal troco) {
        if (troco == null) {
            this.troco = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return;
        }

        this.troco = troco.setScale(2, RoundingMode.HALF_UP);
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }

    public Integer getContaReceberId() {
        return contaReceberId;
    }

    public void setContaReceberId(Integer contaReceberId) {
        this.contaReceberId = contaReceberId;
    }

    public Integer getMovimentacaoFinanceiraId() {
        return movimentacaoFinanceiraId;
    }

    public void setMovimentacaoFinanceiraId(Integer movimentacaoFinanceiraId) {
        this.movimentacaoFinanceiraId = movimentacaoFinanceiraId;
    }

    @Override
    public String toString() {
        return "ResultadoFinalizacaoVenda{" +
                "vendaId=" + vendaId +
                ", tipoVenda=" + tipoVenda +
                ", statusVenda=" + statusVenda +
                ", formaPagamento=" + formaPagamento +
                ", valorTotal=" + valorTotal +
                ", troco=" + troco +
                ", dataVencimento=" + dataVencimento +
                ", contaReceberId=" + contaReceberId +
                ", movimentacaoFinanceiraId=" + movimentacaoFinanceiraId +
                '}';
    }
}