package br.com.luis.viewmodel;

import br.com.luis.model.FormaPagamento;
import br.com.luis.model.StatusContaReceber;
import br.com.luis.model.StatusVenda;
import br.com.luis.model.TipoVenda;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Representa uma linha da listagem do histórico de vendas.
 *
 * Não representa uma entidade persistida e não possui componentes JavaFX.
 *
 * Os campos auxiliares financeiros são usados pelo HistoricoVendaService
 * para montar o resumo financeiro sem provocar consultas N+1.
 */
public class VendaHistoricoListagemView {

    private static final String CLIENTE_NAO_IDENTIFICADO =
            "Consumidor não identificado";

    private Integer vendaId;
    private LocalDateTime dataHora;
    private String nomeCliente;
    private TipoVenda tipoVenda;
    private StatusVenda statusVenda;
    private BigDecimal valorTotal;
    private Integer quantidadeItens;
    private String resumoFinanceiro;

    /*
     * Dados auxiliares de consulta.
     * Não representam colunas obrigatórias da futura TableView.
     */
    private FormaPagamento formaPagamentoVenda;
    private StatusContaReceber statusContaReceber;
    private FormaPagamento formaPagamentoEntrada;
    private Integer quantidadeContasVinculadas;
    private Integer quantidadeEntradasCompativeis;

    /**
     * Construtor padrão.
     */
    public VendaHistoricoListagemView() {
        this.nomeCliente = CLIENTE_NAO_IDENTIFICADO;
        this.valorTotal = BigDecimal.ZERO.setScale(
                2,
                RoundingMode.HALF_UP
        );
        this.quantidadeItens = 0;
        this.quantidadeContasVinculadas = 0;
        this.quantidadeEntradasCompativeis = 0;
    }

    /**
     * Construtor com os campos destinados à interface.
     */
    public VendaHistoricoListagemView(
            Integer vendaId,
            LocalDateTime dataHora,
            String nomeCliente,
            TipoVenda tipoVenda,
            StatusVenda statusVenda,
            BigDecimal valorTotal,
            Integer quantidadeItens,
            String resumoFinanceiro
    ) {
        this();

        this.vendaId = vendaId;
        this.dataHora = dataHora;
        setNomeCliente(nomeCliente);
        this.tipoVenda = tipoVenda;
        this.statusVenda = statusVenda;
        setValorTotal(valorTotal);
        setQuantidadeItens(quantidadeItens);
        this.resumoFinanceiro = resumoFinanceiro;
    }

    /**
     * Construtor completo usado na consulta consolidada.
     */
    public VendaHistoricoListagemView(
            Integer vendaId,
            LocalDateTime dataHora,
            String nomeCliente,
            TipoVenda tipoVenda,
            StatusVenda statusVenda,
            BigDecimal valorTotal,
            Integer quantidadeItens,
            String resumoFinanceiro,
            FormaPagamento formaPagamentoVenda,
            StatusContaReceber statusContaReceber,
            FormaPagamento formaPagamentoEntrada,
            Integer quantidadeContasVinculadas,
            Integer quantidadeEntradasCompativeis
    ) {
        this(
                vendaId,
                dataHora,
                nomeCliente,
                tipoVenda,
                statusVenda,
                valorTotal,
                quantidadeItens,
                resumoFinanceiro
        );

        this.formaPagamentoVenda = formaPagamentoVenda;
        this.statusContaReceber = statusContaReceber;
        this.formaPagamentoEntrada = formaPagamentoEntrada;

        setQuantidadeContasVinculadas(
                quantidadeContasVinculadas
        );

        setQuantidadeEntradasCompativeis(
                quantidadeEntradasCompativeis
        );
    }

    public Integer getVendaId() {
        return vendaId;
    }

    public void setVendaId(Integer vendaId) {
        this.vendaId = vendaId;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public void setNomeCliente(String nomeCliente) {

        if (nomeCliente == null || nomeCliente.isBlank()) {
            this.nomeCliente = CLIENTE_NAO_IDENTIFICADO;
            return;
        }

        this.nomeCliente = nomeCliente.trim();
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

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {

        BigDecimal valorSeguro =
                valorTotal != null
                        ? valorTotal
                        : BigDecimal.ZERO;

        this.valorTotal = valorSeguro.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    public Integer getQuantidadeItens() {
        return quantidadeItens;
    }

    public void setQuantidadeItens(Integer quantidadeItens) {
        this.quantidadeItens =
                quantidadeItens != null
                        ? quantidadeItens
                        : 0;
    }

    public String getResumoFinanceiro() {
        return resumoFinanceiro;
    }

    public void setResumoFinanceiro(String resumoFinanceiro) {
        this.resumoFinanceiro = resumoFinanceiro;
    }

    public FormaPagamento getFormaPagamentoVenda() {
        return formaPagamentoVenda;
    }

    public void setFormaPagamentoVenda(
            FormaPagamento formaPagamentoVenda
    ) {
        this.formaPagamentoVenda = formaPagamentoVenda;
    }

    public StatusContaReceber getStatusContaReceber() {
        return statusContaReceber;
    }

    public void setStatusContaReceber(
            StatusContaReceber statusContaReceber
    ) {
        this.statusContaReceber = statusContaReceber;
    }

    public FormaPagamento getFormaPagamentoEntrada() {
        return formaPagamentoEntrada;
    }

    public void setFormaPagamentoEntrada(
            FormaPagamento formaPagamentoEntrada
    ) {
        this.formaPagamentoEntrada = formaPagamentoEntrada;
    }

    public Integer getQuantidadeContasVinculadas() {
        return quantidadeContasVinculadas;
    }

    public void setQuantidadeContasVinculadas(
            Integer quantidadeContasVinculadas
    ) {
        this.quantidadeContasVinculadas =
                quantidadeContasVinculadas != null
                        ? quantidadeContasVinculadas
                        : 0;
    }

    public Integer getQuantidadeEntradasCompativeis() {
        return quantidadeEntradasCompativeis;
    }

    public void setQuantidadeEntradasCompativeis(
            Integer quantidadeEntradasCompativeis
    ) {
        this.quantidadeEntradasCompativeis =
                quantidadeEntradasCompativeis != null
                        ? quantidadeEntradasCompativeis
                        : 0;
    }

    @Override
    public String toString() {
        return "VendaHistoricoListagemView{" +
                "vendaId=" + vendaId +
                ", dataHora=" + dataHora +
                ", nomeCliente='" + nomeCliente + '\'' +
                ", tipoVenda=" + tipoVenda +
                ", statusVenda=" + statusVenda +
                ", valorTotal=" + valorTotal +
                ", quantidadeItens=" + quantidadeItens +
                ", resumoFinanceiro='" + resumoFinanceiro + '\'' +
                ", formaPagamentoVenda=" + formaPagamentoVenda +
                ", statusContaReceber=" + statusContaReceber +
                ", formaPagamentoEntrada=" + formaPagamentoEntrada +
                ", quantidadeContasVinculadas=" +
                quantidadeContasVinculadas +
                ", quantidadeEntradasCompativeis=" +
                quantidadeEntradasCompativeis +
                '}';
    }
}