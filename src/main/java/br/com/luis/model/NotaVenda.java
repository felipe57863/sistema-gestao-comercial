package br.com.luis.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade que representa a fotografia histórica documental de uma venda.
 *
 * Uma nova venda finalizada deve gerar exatamente uma NotaVenda dentro da mesma
 * transação. Depois de criada, sua fotografia comercial e financeira não deve
 * ser reconstruída a partir dos cadastros atuais. O único dado mutável é o
 * status documental, que pode passar de ATIVA para ESTORNADA no fluxo de estorno.
 */
public class NotaVenda {

    private static final int ESCALA_MONETARIA = 2;

    private Integer idNota;
    private Integer vendaId;
    private StatusNotaVenda status;

    private LocalDateTime dataHoraVenda;
    private TipoVenda tipoVenda;
    private FormaPagamento formaPagamento;

    private Integer usuarioId;
    private String nomeUsuario;

    private Integer clienteId;
    private String nomeCliente;
    private String documentoCliente;

    private BigDecimal valorTotal;
    private BigDecimal valorDescontoGlobal;

    private BigDecimal valorRecebido;
    private BigDecimal troco;

    private Integer prazoPagamentoId;
    private Integer quantidadeDiasPrazo;
    private LocalDate dataVencimento;

    private List<ItemNotaVenda> itens;

    public NotaVenda() {
        this.status = StatusNotaVenda.ATIVA;
        this.valorTotal = BigDecimal.ZERO.setScale(
                ESCALA_MONETARIA,
                RoundingMode.HALF_UP
        );
        this.valorDescontoGlobal = BigDecimal.ZERO.setScale(
                ESCALA_MONETARIA,
                RoundingMode.HALF_UP
        );
        this.itens = new ArrayList<>();
    }

    public NotaVenda(
            Integer idNota,
            Integer vendaId,
            StatusNotaVenda status,
            LocalDateTime dataHoraVenda,
            TipoVenda tipoVenda,
            FormaPagamento formaPagamento,
            Integer usuarioId,
            String nomeUsuario,
            Integer clienteId,
            String nomeCliente,
            String documentoCliente,
            BigDecimal valorTotal,
            BigDecimal valorDescontoGlobal,
            BigDecimal valorRecebido,
            BigDecimal troco,
            Integer prazoPagamentoId,
            Integer quantidadeDiasPrazo,
            LocalDate dataVencimento,
            List<ItemNotaVenda> itens
    ) {
        this();
        setIdNota(idNota);
        setVendaId(vendaId);
        setStatus(status);
        setDataHoraVenda(dataHoraVenda);
        setTipoVenda(tipoVenda);
        setFormaPagamento(formaPagamento);
        setUsuarioId(usuarioId);
        setNomeUsuario(nomeUsuario);
        setClienteId(clienteId);
        setNomeCliente(nomeCliente);
        setDocumentoCliente(documentoCliente);
        setValorTotal(valorTotal);
        setValorDescontoGlobal(valorDescontoGlobal);
        setValorRecebido(valorRecebido);
        setTroco(troco);
        setPrazoPagamentoId(prazoPagamentoId);
        setQuantidadeDiasPrazo(quantidadeDiasPrazo);
        setDataVencimento(dataVencimento);
        setItens(itens);
    }

    public Integer getIdNota() {
        return idNota;
    }

    public void setIdNota(Integer idNota) {
        if (idNota != null && idNota <= 0) {
            throw new IllegalArgumentException(
                    "ID da Nota de Venda deve ser positivo."
            );
        }
        this.idNota = idNota;
    }

    public Integer getVendaId() {
        return vendaId;
    }

    public void setVendaId(Integer vendaId) {
        if (vendaId == null || vendaId <= 0) {
            throw new IllegalArgumentException(
                    "ID da venda é obrigatório para a Nota de Venda."
            );
        }
        this.vendaId = vendaId;
    }

    public StatusNotaVenda getStatus() {
        return status;
    }

    public void setStatus(StatusNotaVenda status) {
        if (status == null) {
            throw new IllegalArgumentException(
                    "Status da Nota de Venda é obrigatório."
            );
        }
        this.status = status;
    }

    public LocalDateTime getDataHoraVenda() {
        return dataHoraVenda;
    }

    public void setDataHoraVenda(LocalDateTime dataHoraVenda) {
        if (dataHoraVenda == null) {
            throw new IllegalArgumentException(
                    "Data e hora da venda são obrigatórias na Nota de Venda."
            );
        }
        this.dataHoraVenda = dataHoraVenda;
    }

    public TipoVenda getTipoVenda() {
        return tipoVenda;
    }

    public void setTipoVenda(TipoVenda tipoVenda) {
        if (tipoVenda == null) {
            throw new IllegalArgumentException(
                    "Tipo da venda é obrigatório na Nota de Venda."
            );
        }
        this.tipoVenda = tipoVenda;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(FormaPagamento formaPagamento) {
        if (formaPagamento == null) {
            throw new IllegalArgumentException(
                    "Forma de pagamento é obrigatória na Nota de Venda."
            );
        }
        this.formaPagamento = formaPagamento;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException(
                    "ID do usuário deve ser maior que zero na Nota de Venda."
            );
        }
        this.usuarioId = usuarioId;
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }

    public void setNomeUsuario(String nomeUsuario) {
        if (nomeUsuario == null || nomeUsuario.isBlank()) {
            throw new IllegalArgumentException(
                    "Nome histórico do usuário é obrigatório na Nota de Venda."
            );
        }
        this.nomeUsuario = nomeUsuario;
    }

    public Integer getClienteId() {
        return clienteId;
    }

    public void setClienteId(Integer clienteId) {
        if (clienteId != null && clienteId <= 0) {
            throw new IllegalArgumentException(
                    "ID histórico do cliente deve ser positivo quando informado."
            );
        }
        this.clienteId = clienteId;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public void setNomeCliente(String nomeCliente) {
        if (nomeCliente != null && nomeCliente.isBlank()) {
            throw new IllegalArgumentException(
                    "Nome histórico do cliente não pode ser vazio."
            );
        }
        this.nomeCliente = nomeCliente;
    }

    public String getDocumentoCliente() {
        return documentoCliente;
    }

    public void setDocumentoCliente(String documentoCliente) {
        if (documentoCliente != null && documentoCliente.isBlank()) {
            throw new IllegalArgumentException(
                    "Documento histórico do cliente não pode ser vazio."
            );
        }
        this.documentoCliente = documentoCliente;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        if (valorTotal == null) {
            throw new IllegalArgumentException(
                    "Valor total é obrigatório na Nota de Venda."
            );
        }

        if (valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Valor total da Nota de Venda deve ser maior que zero."
            );
        }

        this.valorTotal = normalizarMoeda(valorTotal);
    }

    public BigDecimal getValorDescontoGlobal() {
        return valorDescontoGlobal;
    }

    public void setValorDescontoGlobal(BigDecimal valorDescontoGlobal) {
        if (valorDescontoGlobal == null) {
            throw new IllegalArgumentException(
                    "Valor do desconto global é obrigatório na Nota de Venda."
            );
        }

        if (valorDescontoGlobal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Desconto global da Nota de Venda não pode ser negativo."
            );
        }

        this.valorDescontoGlobal = normalizarMoeda(valorDescontoGlobal);
    }

    public BigDecimal getValorRecebido() {
        return valorRecebido;
    }

    public void setValorRecebido(BigDecimal valorRecebido) {
        if (valorRecebido != null
                && valorRecebido.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Valor recebido não pode ser negativo."
            );
        }

        this.valorRecebido = valorRecebido == null
                ? null
                : normalizarMoeda(valorRecebido);
    }

    public BigDecimal getTroco() {
        return troco;
    }

    public void setTroco(BigDecimal troco) {
        if (troco != null && troco.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Troco não pode ser negativo."
            );
        }

        this.troco = troco == null
                ? null
                : normalizarMoeda(troco);
    }

    public Integer getPrazoPagamentoId() {
        return prazoPagamentoId;
    }

    public void setPrazoPagamentoId(Integer prazoPagamentoId) {
        if (prazoPagamentoId != null && prazoPagamentoId <= 0) {
            throw new IllegalArgumentException(
                    "ID histórico do prazo deve ser positivo quando informado."
            );
        }
        this.prazoPagamentoId = prazoPagamentoId;
    }

    public Integer getQuantidadeDiasPrazo() {
        return quantidadeDiasPrazo;
    }

    public void setQuantidadeDiasPrazo(Integer quantidadeDiasPrazo) {
        if (quantidadeDiasPrazo != null && quantidadeDiasPrazo <= 0) {
            throw new IllegalArgumentException(
                    "Quantidade histórica de dias do prazo deve ser maior que zero."
            );
        }
        this.quantidadeDiasPrazo = quantidadeDiasPrazo;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }

    public List<ItemNotaVenda> getItens() {
        return itens;
    }

    public void setItens(List<ItemNotaVenda> itens) {
        this.itens = itens == null
                ? new ArrayList<>()
                : new ArrayList<>(itens);
    }

    private BigDecimal normalizarMoeda(BigDecimal valor) {
        return valor.setScale(
                ESCALA_MONETARIA,
                RoundingMode.HALF_UP
        );
    }

    @Override
    public String toString() {
        return "NotaVenda{" +
                "idNota=" + idNota +
                ", vendaId=" + vendaId +
                ", status=" + status +
                ", dataHoraVenda=" + dataHoraVenda +
                ", tipoVenda=" + tipoVenda +
                ", formaPagamento=" + formaPagamento +
                ", usuarioId=" + usuarioId +
                ", nomeUsuario='" + nomeUsuario + '\'' +
                ", clienteId=" + clienteId +
                ", nomeCliente='" + nomeCliente + '\'' +
                ", documentoCliente='" + documentoCliente + '\'' +
                ", valorTotal=" + valorTotal +
                ", valorDescontoGlobal=" + valorDescontoGlobal +
                ", valorRecebido=" + valorRecebido +
                ", troco=" + troco +
                ", prazoPagamentoId=" + prazoPagamentoId +
                ", quantidadeDiasPrazo=" + quantidadeDiasPrazo +
                ", dataVencimento=" + dataVencimento +
                ", itens=" + itens +
                '}';
    }
}
