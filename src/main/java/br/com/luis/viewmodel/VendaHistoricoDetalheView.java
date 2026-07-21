package br.com.luis.viewmodel;

import br.com.luis.model.FormaPagamento;
import br.com.luis.model.OrigemMovimentacaoFinanceira;
import br.com.luis.model.StatusContaReceber;
import br.com.luis.model.StatusVenda;
import br.com.luis.model.TipoMovimentacaoFinanceira;
import br.com.luis.model.TipoVenda;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Consolida todos os dados necessários para a exibição dos detalhes
 * de uma venda no histórico.
 *
 * Esta classe não acessa DAO, Service, banco de dados ou componentes JavaFX.
 */
public class VendaHistoricoDetalheView {

    private static final String CLIENTE_NAO_IDENTIFICADO =
            "Consumidor não identificado";

    private Integer vendaId;
    private LocalDateTime dataHora;
    private TipoVenda tipoVenda;
    private FormaPagamento formaPagamento;
    private StatusVenda statusVenda;
    private BigDecimal valorTotal;

    private Integer clienteId;
    private String nomeCliente;
    private String documentoCliente;

    private Integer usuarioVendaId;
    private String nomeUsuarioVenda;

    private List<ItemVendaHistoricoView> itens;

    private Integer contaReceberId;
    private StatusContaReceber statusContaReceber;
    private BigDecimal valorContaReceber;

    private Integer movimentacaoOriginalId;
    private TipoMovimentacaoFinanceira tipoMovimentacaoOriginal;
    private OrigemMovimentacaoFinanceira origemMovimentacaoOriginal;
    private FormaPagamento formaPagamentoMovimentacaoOriginal;
    private BigDecimal valorMovimentacaoOriginal;
    private LocalDateTime dataHoraMovimentacaoOriginal;

    private Integer movimentacaoSaidaId;
    private TipoMovimentacaoFinanceira tipoMovimentacaoSaida;
    private OrigemMovimentacaoFinanceira origemMovimentacaoSaida;
    private FormaPagamento formaPagamentoMovimentacaoSaida;
    private BigDecimal valorMovimentacaoSaida;
    private LocalDateTime dataHoraMovimentacaoSaida;

    private Integer auditoriaId;
    private Integer usuarioEstornoId;
    private String nomeUsuarioEstorno;
    private LocalDateTime dataHoraEstorno;
    private String motivoEstorno;
    private StatusVenda statusVendaAnterior;
    private StatusContaReceber statusContaReceberAnterior;

    /**
     * Construtor padrão.
     */
    public VendaHistoricoDetalheView() {
        this.nomeCliente = CLIENTE_NAO_IDENTIFICADO;
        this.valorTotal = BigDecimal.ZERO.setScale(
                2,
                RoundingMode.HALF_UP
        );
        this.itens = new ArrayList<>();
    }

    /**
     * Construtor completo.
     */
    public VendaHistoricoDetalheView(
            Integer vendaId,
            LocalDateTime dataHora,
            TipoVenda tipoVenda,
            FormaPagamento formaPagamento,
            StatusVenda statusVenda,
            BigDecimal valorTotal,
            Integer clienteId,
            String nomeCliente,
            String documentoCliente,
            Integer usuarioVendaId,
            String nomeUsuarioVenda,
            List<ItemVendaHistoricoView> itens,
            Integer contaReceberId,
            StatusContaReceber statusContaReceber,
            BigDecimal valorContaReceber,
            Integer movimentacaoOriginalId,
            TipoMovimentacaoFinanceira tipoMovimentacaoOriginal,
            OrigemMovimentacaoFinanceira origemMovimentacaoOriginal,
            FormaPagamento formaPagamentoMovimentacaoOriginal,
            BigDecimal valorMovimentacaoOriginal,
            LocalDateTime dataHoraMovimentacaoOriginal,
            Integer movimentacaoSaidaId,
            TipoMovimentacaoFinanceira tipoMovimentacaoSaida,
            OrigemMovimentacaoFinanceira origemMovimentacaoSaida,
            FormaPagamento formaPagamentoMovimentacaoSaida,
            BigDecimal valorMovimentacaoSaida,
            LocalDateTime dataHoraMovimentacaoSaida,
            Integer auditoriaId,
            Integer usuarioEstornoId,
            String nomeUsuarioEstorno,
            LocalDateTime dataHoraEstorno,
            String motivoEstorno,
            StatusVenda statusVendaAnterior,
            StatusContaReceber statusContaReceberAnterior
    ) {
        this();

        this.vendaId = vendaId;
        this.dataHora = dataHora;
        this.tipoVenda = tipoVenda;
        this.formaPagamento = formaPagamento;
        this.statusVenda = statusVenda;
        setValorTotal(valorTotal);

        this.clienteId = clienteId;
        setNomeCliente(nomeCliente);
        setDocumentoCliente(documentoCliente);

        this.usuarioVendaId = usuarioVendaId;
        this.nomeUsuarioVenda = nomeUsuarioVenda;

        setItens(itens);

        this.contaReceberId = contaReceberId;
        this.statusContaReceber = statusContaReceber;
        setValorContaReceber(valorContaReceber);

        this.movimentacaoOriginalId = movimentacaoOriginalId;
        this.tipoMovimentacaoOriginal = tipoMovimentacaoOriginal;
        this.origemMovimentacaoOriginal = origemMovimentacaoOriginal;
        this.formaPagamentoMovimentacaoOriginal =
                formaPagamentoMovimentacaoOriginal;
        setValorMovimentacaoOriginal(valorMovimentacaoOriginal);
        this.dataHoraMovimentacaoOriginal =
                dataHoraMovimentacaoOriginal;

        this.movimentacaoSaidaId = movimentacaoSaidaId;
        this.tipoMovimentacaoSaida = tipoMovimentacaoSaida;
        this.origemMovimentacaoSaida = origemMovimentacaoSaida;
        this.formaPagamentoMovimentacaoSaida =
                formaPagamentoMovimentacaoSaida;
        setValorMovimentacaoSaida(valorMovimentacaoSaida);
        this.dataHoraMovimentacaoSaida =
                dataHoraMovimentacaoSaida;

        this.auditoriaId = auditoriaId;
        this.usuarioEstornoId = usuarioEstornoId;
        this.nomeUsuarioEstorno = nomeUsuarioEstorno;
        this.dataHoraEstorno = dataHoraEstorno;
        this.motivoEstorno = motivoEstorno;
        this.statusVendaAnterior = statusVendaAnterior;
        this.statusContaReceberAnterior =
                statusContaReceberAnterior;
    }

    private BigDecimal normalizarValorObrigatorio(
            BigDecimal valor
    ) {
        BigDecimal valorSeguro =
                valor != null
                        ? valor
                        : BigDecimal.ZERO;

        return valorSeguro.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal normalizarValorOpcional(
            BigDecimal valor
    ) {
        if (valor == null) {
            return null;
        }

        return valor.setScale(
                2,
                RoundingMode.HALF_UP
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

    public TipoVenda getTipoVenda() {
        return tipoVenda;
    }

    public void setTipoVenda(TipoVenda tipoVenda) {
        this.tipoVenda = tipoVenda;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(
            FormaPagamento formaPagamento
    ) {
        this.formaPagamento = formaPagamento;
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
        this.valorTotal =
                normalizarValorObrigatorio(valorTotal);
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

        if (nomeCliente == null || nomeCliente.isBlank()) {
            this.nomeCliente = CLIENTE_NAO_IDENTIFICADO;
            return;
        }

        this.nomeCliente = nomeCliente.trim();
    }

    public String getDocumentoCliente() {
        return documentoCliente;
    }

    public void setDocumentoCliente(String documentoCliente) {

        if (documentoCliente == null
                || documentoCliente.isBlank()) {

            this.documentoCliente = null;
            return;
        }

        this.documentoCliente = documentoCliente.trim();
    }

    public Integer getUsuarioVendaId() {
        return usuarioVendaId;
    }

    public void setUsuarioVendaId(Integer usuarioVendaId) {
        this.usuarioVendaId = usuarioVendaId;
    }

    public String getNomeUsuarioVenda() {
        return nomeUsuarioVenda;
    }

    public void setNomeUsuarioVenda(String nomeUsuarioVenda) {
        this.nomeUsuarioVenda = nomeUsuarioVenda;
    }

    public List<ItemVendaHistoricoView> getItens() {
        return itens;
    }

    public void setItens(
            List<ItemVendaHistoricoView> itens
    ) {
        this.itens =
                itens != null
                        ? new ArrayList<>(itens)
                        : new ArrayList<>();
    }

    public Integer getContaReceberId() {
        return contaReceberId;
    }

    public void setContaReceberId(Integer contaReceberId) {
        this.contaReceberId = contaReceberId;
    }

    public StatusContaReceber getStatusContaReceber() {
        return statusContaReceber;
    }

    public void setStatusContaReceber(
            StatusContaReceber statusContaReceber
    ) {
        this.statusContaReceber = statusContaReceber;
    }

    public BigDecimal getValorContaReceber() {
        return valorContaReceber;
    }

    public void setValorContaReceber(
            BigDecimal valorContaReceber
    ) {
        this.valorContaReceber =
                normalizarValorOpcional(valorContaReceber);
    }

    public Integer getMovimentacaoOriginalId() {
        return movimentacaoOriginalId;
    }

    public void setMovimentacaoOriginalId(
            Integer movimentacaoOriginalId
    ) {
        this.movimentacaoOriginalId = movimentacaoOriginalId;
    }

    public TipoMovimentacaoFinanceira
    getTipoMovimentacaoOriginal() {
        return tipoMovimentacaoOriginal;
    }

    public void setTipoMovimentacaoOriginal(
            TipoMovimentacaoFinanceira tipoMovimentacaoOriginal
    ) {
        this.tipoMovimentacaoOriginal =
                tipoMovimentacaoOriginal;
    }

    public OrigemMovimentacaoFinanceira
    getOrigemMovimentacaoOriginal() {
        return origemMovimentacaoOriginal;
    }

    public void setOrigemMovimentacaoOriginal(
            OrigemMovimentacaoFinanceira origemMovimentacaoOriginal
    ) {
        this.origemMovimentacaoOriginal =
                origemMovimentacaoOriginal;
    }

    public FormaPagamento
    getFormaPagamentoMovimentacaoOriginal() {
        return formaPagamentoMovimentacaoOriginal;
    }

    public void setFormaPagamentoMovimentacaoOriginal(
            FormaPagamento formaPagamentoMovimentacaoOriginal
    ) {
        this.formaPagamentoMovimentacaoOriginal =
                formaPagamentoMovimentacaoOriginal;
    }

    public BigDecimal getValorMovimentacaoOriginal() {
        return valorMovimentacaoOriginal;
    }

    public void setValorMovimentacaoOriginal(
            BigDecimal valorMovimentacaoOriginal
    ) {
        this.valorMovimentacaoOriginal =
                normalizarValorOpcional(
                        valorMovimentacaoOriginal
                );
    }

    public LocalDateTime getDataHoraMovimentacaoOriginal() {
        return dataHoraMovimentacaoOriginal;
    }

    public void setDataHoraMovimentacaoOriginal(
            LocalDateTime dataHoraMovimentacaoOriginal
    ) {
        this.dataHoraMovimentacaoOriginal =
                dataHoraMovimentacaoOriginal;
    }

    public Integer getMovimentacaoSaidaId() {
        return movimentacaoSaidaId;
    }

    public void setMovimentacaoSaidaId(
            Integer movimentacaoSaidaId
    ) {
        this.movimentacaoSaidaId = movimentacaoSaidaId;
    }

    public TipoMovimentacaoFinanceira
    getTipoMovimentacaoSaida() {
        return tipoMovimentacaoSaida;
    }

    public void setTipoMovimentacaoSaida(
            TipoMovimentacaoFinanceira tipoMovimentacaoSaida
    ) {
        this.tipoMovimentacaoSaida =
                tipoMovimentacaoSaida;
    }

    public OrigemMovimentacaoFinanceira
    getOrigemMovimentacaoSaida() {
        return origemMovimentacaoSaida;
    }

    public void setOrigemMovimentacaoSaida(
            OrigemMovimentacaoFinanceira origemMovimentacaoSaida
    ) {
        this.origemMovimentacaoSaida =
                origemMovimentacaoSaida;
    }

    public FormaPagamento
    getFormaPagamentoMovimentacaoSaida() {
        return formaPagamentoMovimentacaoSaida;
    }

    public void setFormaPagamentoMovimentacaoSaida(
            FormaPagamento formaPagamentoMovimentacaoSaida
    ) {
        this.formaPagamentoMovimentacaoSaida =
                formaPagamentoMovimentacaoSaida;
    }

    public BigDecimal getValorMovimentacaoSaida() {
        return valorMovimentacaoSaida;
    }

    public void setValorMovimentacaoSaida(
            BigDecimal valorMovimentacaoSaida
    ) {
        this.valorMovimentacaoSaida =
                normalizarValorOpcional(
                        valorMovimentacaoSaida
                );
    }

    public LocalDateTime getDataHoraMovimentacaoSaida() {
        return dataHoraMovimentacaoSaida;
    }

    public void setDataHoraMovimentacaoSaida(
            LocalDateTime dataHoraMovimentacaoSaida
    ) {
        this.dataHoraMovimentacaoSaida =
                dataHoraMovimentacaoSaida;
    }

    public Integer getAuditoriaId() {
        return auditoriaId;
    }

    public void setAuditoriaId(Integer auditoriaId) {
        this.auditoriaId = auditoriaId;
    }

    public Integer getUsuarioEstornoId() {
        return usuarioEstornoId;
    }

    public void setUsuarioEstornoId(
            Integer usuarioEstornoId
    ) {
        this.usuarioEstornoId = usuarioEstornoId;
    }

    public String getNomeUsuarioEstorno() {
        return nomeUsuarioEstorno;
    }

    public void setNomeUsuarioEstorno(
            String nomeUsuarioEstorno
    ) {
        this.nomeUsuarioEstorno = nomeUsuarioEstorno;
    }

    public LocalDateTime getDataHoraEstorno() {
        return dataHoraEstorno;
    }

    public void setDataHoraEstorno(
            LocalDateTime dataHoraEstorno
    ) {
        this.dataHoraEstorno = dataHoraEstorno;
    }

    public String getMotivoEstorno() {
        return motivoEstorno;
    }

    public void setMotivoEstorno(String motivoEstorno) {
        this.motivoEstorno = motivoEstorno;
    }

    public StatusVenda getStatusVendaAnterior() {
        return statusVendaAnterior;
    }

    public void setStatusVendaAnterior(
            StatusVenda statusVendaAnterior
    ) {
        this.statusVendaAnterior = statusVendaAnterior;
    }

    public StatusContaReceber
    getStatusContaReceberAnterior() {
        return statusContaReceberAnterior;
    }

    public void setStatusContaReceberAnterior(
            StatusContaReceber statusContaReceberAnterior
    ) {
        this.statusContaReceberAnterior =
                statusContaReceberAnterior;
    }

    @Override
    public String toString() {
        return "VendaHistoricoDetalheView{" +
                "vendaId=" + vendaId +
                ", dataHora=" + dataHora +
                ", tipoVenda=" + tipoVenda +
                ", formaPagamento=" + formaPagamento +
                ", statusVenda=" + statusVenda +
                ", valorTotal=" + valorTotal +
                ", clienteId=" + clienteId +
                ", nomeCliente='" + nomeCliente + '\'' +
                ", documentoCliente='" + documentoCliente + '\'' +
                ", usuarioVendaId=" + usuarioVendaId +
                ", nomeUsuarioVenda='" + nomeUsuarioVenda + '\'' +
                ", itens=" + itens +
                ", contaReceberId=" + contaReceberId +
                ", statusContaReceber=" + statusContaReceber +
                ", valorContaReceber=" + valorContaReceber +
                ", movimentacaoOriginalId=" +
                movimentacaoOriginalId +
                ", movimentacaoSaidaId=" +
                movimentacaoSaidaId +
                ", auditoriaId=" + auditoriaId +
                ", usuarioEstornoId=" + usuarioEstornoId +
                ", nomeUsuarioEstorno='" +
                nomeUsuarioEstorno + '\'' +
                ", dataHoraEstorno=" + dataHoraEstorno +
                ", motivoEstorno='" + motivoEstorno + '\'' +
                ", statusVendaAnterior=" +
                statusVendaAnterior +
                ", statusContaReceberAnterior=" +
                statusContaReceberAnterior +
                '}';
    }
}