package br.com.luis.viewmodel;

import br.com.luis.model.StatusVenda;
import br.com.luis.model.TipoVenda;

import java.time.LocalDate;

/**
 * Transporta os filtros usados na consulta do histórico de vendas.
 *
 * Esta classe não acessa banco de dados e não possui componentes JavaFX.
 * Valores nulos representam ausência do respectivo filtro.
 */
public class FiltroHistoricoVenda {

    private LocalDate dataInicial;
    private LocalDate dataFinal;
    private String clienteOuDocumento;
    private Integer vendaId;
    private TipoVenda tipoVenda;
    private StatusVenda statusVenda;

    /**
     * Construtor padrão.
     */
    public FiltroHistoricoVenda() {
    }

    /**
     * Construtor completo.
     */
    public FiltroHistoricoVenda(
            LocalDate dataInicial,
            LocalDate dataFinal,
            String clienteOuDocumento,
            Integer vendaId,
            TipoVenda tipoVenda,
            StatusVenda statusVenda
    ) {
        setDataInicial(dataInicial);
        setDataFinal(dataFinal);
        setClienteOuDocumento(clienteOuDocumento);
        setVendaId(vendaId);
        setTipoVenda(tipoVenda);
        setStatusVenda(statusVenda);

        validar();
    }

    /**
     * Valida a combinação atual dos filtros.
     *
     * A validação é executada novamente pelo HistoricoVendaService antes
     * da abertura da Connection.
     */
    public void validar() {

        if (vendaId != null && vendaId <= 0) {
            throw new IllegalArgumentException(
                    "ID da venda deve ser maior que zero."
            );
        }

        if (dataInicial != null
                && dataFinal != null
                && dataInicial.isAfter(dataFinal)) {

            throw new IllegalArgumentException(
                    "Data inicial não pode ser posterior à data final."
            );
        }
    }

    public LocalDate getDataInicial() {
        return dataInicial;
    }

    public void setDataInicial(LocalDate dataInicial) {
        this.dataInicial = dataInicial;
    }

    public LocalDate getDataFinal() {
        return dataFinal;
    }

    public void setDataFinal(LocalDate dataFinal) {
        this.dataFinal = dataFinal;
    }

    public String getClienteOuDocumento() {
        return clienteOuDocumento;
    }

    public void setClienteOuDocumento(String clienteOuDocumento) {

        if (clienteOuDocumento == null
                || clienteOuDocumento.isBlank()) {

            this.clienteOuDocumento = null;
            return;
        }

        this.clienteOuDocumento = clienteOuDocumento.trim();
    }

    public Integer getVendaId() {
        return vendaId;
    }

    public void setVendaId(Integer vendaId) {

        if (vendaId != null && vendaId <= 0) {
            throw new IllegalArgumentException(
                    "ID da venda deve ser maior que zero."
            );
        }

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

    @Override
    public String toString() {
        return "FiltroHistoricoVenda{" +
                "dataInicial=" + dataInicial +
                ", dataFinal=" + dataFinal +
                ", clienteOuDocumento='" + clienteOuDocumento + '\'' +
                ", vendaId=" + vendaId +
                ", tipoVenda=" + tipoVenda +
                ", statusVenda=" + statusVenda +
                '}';
    }
}