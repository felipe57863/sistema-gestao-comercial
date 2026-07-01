package br.com.luis.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade que representa uma conta a receber gerada por uma venda a prazo.
 *
 * Na primeira entrega da Fase 5, a ContaReceber será criada somente
 * no momento da finalização de uma venda a prazo.
 *
 * Regras importantes:
 * - Toda ContaReceber nasce com status PENDENTE.
 * - A data de vencimento é calculada no Service.
 * - O prazo efetivo é validado no Service.
 * - O limite de crédito é validado no Service.
 * - O recebimento completo da conta fica fora desta etapa.
 */
public class ContaReceber {

    private Integer idConta;
    private BigDecimal valor;
    private LocalDate dataVencimento;
    private StatusContaReceber status;
    private Integer vendaId;
    private Integer clienteId;
    private Integer prazoPagamentoId;
    private Integer quantidadeDiasPrazo;
    private LocalDateTime dataCriacao;

    /**
     * Construtor padrão.
     *
     * Define valores iniciais seguros para criação gradual do objeto.
     */
    public ContaReceber() {
        this.valor = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.status = StatusContaReceber.PENDENTE;
        this.dataCriacao = LocalDateTime.now();
    }

    /**
     * Construtor completo.
     *
     * Deve ser usado quando todos os dados principais da conta já estiverem definidos.
     */
    public ContaReceber(
            Integer idConta,
            BigDecimal valor,
            LocalDate dataVencimento,
            StatusContaReceber status,
            Integer vendaId,
            Integer clienteId,
            Integer prazoPagamentoId,
            Integer quantidadeDiasPrazo,
            LocalDateTime dataCriacao
    ) {
        setIdConta(idConta);
        setValor(valor);
        setDataVencimento(dataVencimento);
        setStatus(status);
        setVendaId(vendaId);
        setClienteId(clienteId);
        setPrazoPagamentoId(prazoPagamentoId);
        setQuantidadeDiasPrazo(quantidadeDiasPrazo);
        setDataCriacao(dataCriacao);
    }

    public Integer getIdConta() {
        return idConta;
    }

    public void setIdConta(Integer idConta) {
        if (idConta != null && idConta <= 0) {
            throw new IllegalArgumentException("ID da conta a receber deve ser positivo.");
        }
        this.idConta = idConta;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Valor da conta a receber é obrigatório.");
        }

        if (valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor da conta a receber não pode ser negativo.");
        }

        this.valor = valor.setScale(2, RoundingMode.HALF_UP);
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        if (dataVencimento == null) {
            throw new IllegalArgumentException("Data de vencimento é obrigatória.");
        }
        this.dataVencimento = dataVencimento;
    }

    public StatusContaReceber getStatus() {
        return status;
    }

    public void setStatus(StatusContaReceber status) {
        if (status == null) {
            throw new IllegalArgumentException("Status da conta a receber é obrigatório.");
        }
        this.status = status;
    }

    public Integer getVendaId() {
        return vendaId;
    }

    public void setVendaId(Integer vendaId) {
        if (vendaId == null || vendaId <= 0) {
            throw new IllegalArgumentException("ID da venda é obrigatório para a conta a receber.");
        }
        this.vendaId = vendaId;
    }

    public Integer getClienteId() {
        return clienteId;
    }

    public void setClienteId(Integer clienteId) {
        if (clienteId == null || clienteId <= 0) {
            throw new IllegalArgumentException("ID do cliente é obrigatório para a conta a receber.");
        }
        this.clienteId = clienteId;
    }

    public Integer getPrazoPagamentoId() {
        return prazoPagamentoId;
    }

    public void setPrazoPagamentoId(Integer prazoPagamentoId) {
        if (prazoPagamentoId == null || prazoPagamentoId <= 0) {
            throw new IllegalArgumentException("ID do prazo de pagamento é obrigatório para a conta a receber.");
        }
        this.prazoPagamentoId = prazoPagamentoId;
    }

    public Integer getQuantidadeDiasPrazo() {
        return quantidadeDiasPrazo;
    }

    public void setQuantidadeDiasPrazo(Integer quantidadeDiasPrazo) {
        if (quantidadeDiasPrazo == null || quantidadeDiasPrazo <= 0) {
            throw new IllegalArgumentException("Quantidade de dias do prazo deve ser maior que zero.");
        }
        this.quantidadeDiasPrazo = quantidadeDiasPrazo;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        if (dataCriacao == null) {
            throw new IllegalArgumentException("Data de criação é obrigatória.");
        }
        this.dataCriacao = dataCriacao;
    }

    @Override
    public String toString() {
        return "ContaReceber{" +
                "idConta=" + idConta +
                ", valor=" + valor +
                ", dataVencimento=" + dataVencimento +
                ", status=" + status +
                ", vendaId=" + vendaId +
                ", clienteId=" + clienteId +
                ", prazoPagamentoId=" + prazoPagamentoId +
                ", quantidadeDiasPrazo=" + quantidadeDiasPrazo +
                ", dataCriacao=" + dataCriacao +
                '}';
    }
}