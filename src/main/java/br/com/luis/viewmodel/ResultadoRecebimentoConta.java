package br.com.luis.viewmodel;

import br.com.luis.model.FormaPagamento;
import br.com.luis.model.StatusContaReceber;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * ViewModel usado para retornar o resultado do recebimento integral
 * de uma conta a receber.
 *
 * Esta classe não representa uma entidade persistida no banco de dados.
 * Ela serve apenas para transportar dados do ContaReceberService para o Controller.
 */
public class ResultadoRecebimentoConta {

    private Integer contaReceberId;
    private Integer movimentacaoFinanceiraId;
    private Integer vendaId;
    private BigDecimal valorRecebido;
    private FormaPagamento formaPagamento;
    private LocalDateTime dataHoraRecebimento;
    private StatusContaReceber statusContaReceber;

    public ResultadoRecebimentoConta() {
        this.valorRecebido = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public ResultadoRecebimentoConta(
            Integer contaReceberId,
            Integer movimentacaoFinanceiraId,
            Integer vendaId,
            BigDecimal valorRecebido,
            FormaPagamento formaPagamento,
            LocalDateTime dataHoraRecebimento,
            StatusContaReceber statusContaReceber
    ) {
        this.contaReceberId = contaReceberId;
        this.movimentacaoFinanceiraId = movimentacaoFinanceiraId;
        this.vendaId = vendaId;
        setValorRecebido(valorRecebido);
        this.formaPagamento = formaPagamento;
        this.dataHoraRecebimento = dataHoraRecebimento;
        this.statusContaReceber = statusContaReceber;
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

    public Integer getVendaId() {
        return vendaId;
    }

    public void setVendaId(Integer vendaId) {
        this.vendaId = vendaId;
    }

    public BigDecimal getValorRecebido() {
        return valorRecebido;
    }

    public void setValorRecebido(BigDecimal valorRecebido) {
        if (valorRecebido == null) {
            this.valorRecebido = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return;
        }

        this.valorRecebido = valorRecebido.setScale(2, RoundingMode.HALF_UP);
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(FormaPagamento formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public LocalDateTime getDataHoraRecebimento() {
        return dataHoraRecebimento;
    }

    public void setDataHoraRecebimento(LocalDateTime dataHoraRecebimento) {
        this.dataHoraRecebimento = dataHoraRecebimento;
    }

    public StatusContaReceber getStatusContaReceber() {
        return statusContaReceber;
    }

    public void setStatusContaReceber(StatusContaReceber statusContaReceber) {
        this.statusContaReceber = statusContaReceber;
    }

    @Override
    public String toString() {
        return "ResultadoRecebimentoConta{" +
                "contaReceberId=" + contaReceberId +
                ", movimentacaoFinanceiraId=" + movimentacaoFinanceiraId +
                ", vendaId=" + vendaId +
                ", valorRecebido=" + valorRecebido +
                ", formaPagamento=" + formaPagamento +
                ", dataHoraRecebimento=" + dataHoraRecebimento +
                ", statusContaReceber=" + statusContaReceber +
                '}';
    }
}