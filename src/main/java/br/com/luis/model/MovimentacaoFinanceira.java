package br.com.luis.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Entidade que representa um lançamento financeiro persistido no sistema.
 * Entradas são geradas pela finalização de vendas à vista e pelo recebimento
 * integral de contas a receber. Quando necessário, o estorno registra uma nova
 * movimentação de saída compensatória, preservando os lançamentos anteriores
 * para manter a rastreabilidade e o histórico financeiro.
 *
 * Regras importantes:
 * - A persistência atual registra movimentações por INSERT e não disponibiliza
 *   UPDATE ou DELETE.
 * - Venda a prazo não gera movimentação financeira imediata.
 * - O recebimento integral da conta gera uma entrada financeira vinculada.
 * - A definição do tipo, da origem, do valor e das referências pertence ao
 *   Service responsável pelo fluxo; o Model apenas representa esses dados.
 */
public class MovimentacaoFinanceira {

    private Integer idMovimentacao;
    private LocalDateTime dataHora;
    private TipoMovimentacaoFinanceira tipo;
    private OrigemMovimentacaoFinanceira origem;
    private FormaPagamento formaPagamento;
    private BigDecimal valor;
    private Integer vendaId;
    private Integer contaReceberId;
    private Integer usuarioId;

    /**
     * Construtor padrão.
     *
     * Define valores iniciais seguros para criação gradual do objeto.
     */
    public MovimentacaoFinanceira() {
        this.dataHora = LocalDateTime.now();
        this.tipo = TipoMovimentacaoFinanceira.ENTRADA;
        this.origem = OrigemMovimentacaoFinanceira.VENDA_A_VISTA;
        this.valor = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Construtor completo.
     *
     * Deve ser usado quando todos os dados principais da movimentação
     * já estiverem definidos.
     */
    public MovimentacaoFinanceira(
            Integer idMovimentacao,
            LocalDateTime dataHora,
            TipoMovimentacaoFinanceira tipo,
            OrigemMovimentacaoFinanceira origem,
            FormaPagamento formaPagamento,
            BigDecimal valor,
            Integer vendaId,
            Integer contaReceberId,
            Integer usuarioId
    ) {
        setIdMovimentacao(idMovimentacao);
        setDataHora(dataHora);
        setTipo(tipo);
        setOrigem(origem);
        setFormaPagamento(formaPagamento);
        setValor(valor);
        setVendaId(vendaId);
        setContaReceberId(contaReceberId);
        setUsuarioId(usuarioId);
    }

    public Integer getIdMovimentacao() {
        return idMovimentacao;
    }

    public void setIdMovimentacao(Integer idMovimentacao) {
        if (idMovimentacao != null && idMovimentacao <= 0) {
            throw new IllegalArgumentException("ID da movimentação financeira deve ser positivo.");
        }
        this.idMovimentacao = idMovimentacao;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        if (dataHora == null) {
            throw new IllegalArgumentException("Data e hora da movimentação financeira são obrigatórias.");
        }
        this.dataHora = dataHora;
    }

    public TipoMovimentacaoFinanceira getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimentacaoFinanceira tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo da movimentação financeira é obrigatório.");
        }
        this.tipo = tipo;
    }

    public OrigemMovimentacaoFinanceira getOrigem() {
        return origem;
    }

    public void setOrigem(OrigemMovimentacaoFinanceira origem) {
        if (origem == null) {
            throw new IllegalArgumentException("Origem da movimentação financeira é obrigatória.");
        }
        this.origem = origem;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(FormaPagamento formaPagamento) {
        if (formaPagamento == null) {
            throw new IllegalArgumentException("Forma de pagamento da movimentação financeira é obrigatória.");
        }

        if (formaPagamento == FormaPagamento.A_PRAZO) {
            throw new IllegalArgumentException("Movimentação financeira não pode usar forma de pagamento A_PRAZO.");
        }

        this.formaPagamento = formaPagamento;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Valor da movimentação financeira é obrigatório.");
        }

        if (valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor da movimentação financeira não pode ser negativo.");
        }

        this.valor = valor.setScale(2, RoundingMode.HALF_UP);
    }

    public Integer getVendaId() {
        return vendaId;
    }

    public void setVendaId(Integer vendaId) {
        if (vendaId == null || vendaId <= 0) {
            throw new IllegalArgumentException("ID da venda é obrigatório para a movimentação financeira.");
        }
        this.vendaId = vendaId;
    }

    public Integer getContaReceberId() {
        return contaReceberId;
    }

    public void setContaReceberId(Integer contaReceberId) {
        if (contaReceberId != null && contaReceberId <= 0) {
            throw new IllegalArgumentException("ID da conta a receber deve ser positivo.");
        }
        this.contaReceberId = contaReceberId;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException("ID do usuário é obrigatório para a movimentação financeira.");
        }
        this.usuarioId = usuarioId;
    }

    @Override
    public String toString() {
        return "MovimentacaoFinanceira{" +
                "idMovimentacao=" + idMovimentacao +
                ", dataHora=" + dataHora +
                ", tipo=" + tipo +
                ", origem=" + origem +
                ", formaPagamento=" + formaPagamento +
                ", valor=" + valor +
                ", vendaId=" + vendaId +
                ", contaReceberId=" + contaReceberId +
                ", usuarioId=" + usuarioId +
                '}';
    }
}