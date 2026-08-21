package br.com.luis.viewmodel;

import br.com.luis.model.StatusContaReceber;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Dados de uma conta pendente combinados com o cliente para exibição.
 *
 * O indicador de vencimento é recebido pronto; formatação e cálculo visual ficam fora deste ViewModel.
 */
public class ContaReceberListagemView {

    private Integer contaReceberId;
    private Integer clienteId;
    private String nomeCliente;
    private Integer vendaId;
    private BigDecimal valor;
    private LocalDate dataVencimento;
    private StatusContaReceber status;
    private boolean vencida;

    public ContaReceberListagemView() {
        this.valor = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.vencida = false;
    }

    public ContaReceberListagemView(
            Integer contaReceberId,
            Integer clienteId,
            String nomeCliente,
            Integer vendaId,
            BigDecimal valor,
            LocalDate dataVencimento,
            StatusContaReceber status,
            boolean vencida
    ) {
        this.contaReceberId = contaReceberId;
        this.clienteId = clienteId;
        this.nomeCliente = nomeCliente;
        this.vendaId = vendaId;
        setValor(valor);
        this.dataVencimento = dataVencimento;
        this.status = status;
        this.vencida = vencida;
    }

    public Integer getContaReceberId() {
        return contaReceberId;
    }

    public void setContaReceberId(Integer contaReceberId) {
        this.contaReceberId = contaReceberId;
    }

    public Integer getClienteId() {
        return clienteId;
    }

    public void setClienteId(Integer clienteId) {
        this.clienteId = clienteId;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public void setNomeCliente(String nomeCliente) {
        this.nomeCliente = nomeCliente;
    }

    public Integer getVendaId() {
        return vendaId;
    }

    public void setVendaId(Integer vendaId) {
        this.vendaId = vendaId;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        if (valor == null) {
            this.valor = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return;
        }

        this.valor = valor.setScale(2, RoundingMode.HALF_UP);
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }

    public StatusContaReceber getStatus() {
        return status;
    }

    public void setStatus(StatusContaReceber status) {
        this.status = status;
    }

    public boolean isVencida() {
        return vencida;
    }

    public void setVencida(boolean vencida) {
        this.vencida = vencida;
    }

    @Override
    public String toString() {
        return "ContaReceberListagemView{" +
                "contaReceberId=" + contaReceberId +
                ", clienteId=" + clienteId +
                ", nomeCliente='" + nomeCliente + '\'' +
                ", vendaId=" + vendaId +
                ", valor=" + valor +
                ", dataVencimento=" + dataVencimento +
                ", status=" + status +
                ", vencida=" + vencida +
                '}';
    }
}