package br.com.luis.viewmodel;

import br.com.luis.model.StatusContaReceber;
import br.com.luis.model.StatusVenda;
import br.com.luis.model.TipoVenda;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * ViewModel usado para retornar o resultado do estorno total de uma venda.
 *
 * Esta classe não representa uma entidade persistida no banco de dados.
 * Ela transporta para o Controller os dados consolidados pelo
 * EstornoVendaService após a conclusão da transação.
 */
public class ResultadoEstornoVenda {

    private Integer vendaId;
    private TipoVenda tipoVenda;
    private StatusVenda statusVendaFinal;
    private Integer contaReceberId;
    private StatusContaReceber statusContaReceberFinal;
    private Integer movimentacaoFinanceiraOriginalId;
    private Integer movimentacaoFinanceiraSaidaId;
    private BigDecimal valorSaida;
    private Integer auditoriaId;
    private LocalDateTime dataHoraEstorno;
    private Integer quantidadeDeItensRestaurados;
    private Integer quantidadeTotalDeUnidadesRestauradas;

    /**
     * Construtor padrão.
     *
     * Inicializa os valores numéricos com valores seguros.
     */
    public ResultadoEstornoVenda() {
        this.valorSaida = BigDecimal.ZERO.setScale(
                2,
                RoundingMode.HALF_UP
        );
        this.quantidadeDeItensRestaurados = 0;
        this.quantidadeTotalDeUnidadesRestauradas = 0;
    }

    /**
     * Construtor completo.
     *
     * @param vendaId identificador da venda estornada.
     * @param tipoVenda tipo da venda estornada.
     * @param statusVendaFinal status final da venda.
     * @param contaReceberId conta vinculada, quando aplicável.
     * @param statusContaReceberFinal status final da conta, quando aplicável.
     * @param movimentacaoFinanceiraOriginalId movimentação financeira original,
     *                                          quando aplicável.
     * @param movimentacaoFinanceiraSaidaId movimentação de saída criada,
     *                                      quando aplicável.
     * @param valorSaida valor da saída financeira, zero quando não houver saída.
     * @param auditoriaId identificador da auditoria persistida.
     * @param dataHoraEstorno data e hora da conclusão do estorno.
     * @param quantidadeDeItensRestaurados quantidade de itens distintos restaurados.
     * @param quantidadeTotalDeUnidadesRestauradas total de unidades devolvidas ao estoque.
     */
    public ResultadoEstornoVenda(
            Integer vendaId,
            TipoVenda tipoVenda,
            StatusVenda statusVendaFinal,
            Integer contaReceberId,
            StatusContaReceber statusContaReceberFinal,
            Integer movimentacaoFinanceiraOriginalId,
            Integer movimentacaoFinanceiraSaidaId,
            BigDecimal valorSaida,
            Integer auditoriaId,
            LocalDateTime dataHoraEstorno,
            Integer quantidadeDeItensRestaurados,
            Integer quantidadeTotalDeUnidadesRestauradas
    ) {
        this.vendaId = vendaId;
        this.tipoVenda = tipoVenda;
        this.statusVendaFinal = statusVendaFinal;
        this.contaReceberId = contaReceberId;
        this.statusContaReceberFinal = statusContaReceberFinal;
        this.movimentacaoFinanceiraOriginalId =
                movimentacaoFinanceiraOriginalId;
        this.movimentacaoFinanceiraSaidaId =
                movimentacaoFinanceiraSaidaId;
        setValorSaida(valorSaida);
        this.auditoriaId = auditoriaId;
        this.dataHoraEstorno = dataHoraEstorno;
        setQuantidadeDeItensRestaurados(
                quantidadeDeItensRestaurados
        );
        setQuantidadeTotalDeUnidadesRestauradas(
                quantidadeTotalDeUnidadesRestauradas
        );
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

    public StatusVenda getStatusVendaFinal() {
        return statusVendaFinal;
    }

    public void setStatusVendaFinal(StatusVenda statusVendaFinal) {
        this.statusVendaFinal = statusVendaFinal;
    }

    public Integer getContaReceberId() {
        return contaReceberId;
    }

    public void setContaReceberId(Integer contaReceberId) {
        this.contaReceberId = contaReceberId;
    }

    public StatusContaReceber getStatusContaReceberFinal() {
        return statusContaReceberFinal;
    }

    public void setStatusContaReceberFinal(
            StatusContaReceber statusContaReceberFinal
    ) {
        this.statusContaReceberFinal = statusContaReceberFinal;
    }

    public Integer getMovimentacaoFinanceiraOriginalId() {
        return movimentacaoFinanceiraOriginalId;
    }

    public void setMovimentacaoFinanceiraOriginalId(
            Integer movimentacaoFinanceiraOriginalId
    ) {
        this.movimentacaoFinanceiraOriginalId =
                movimentacaoFinanceiraOriginalId;
    }

    public Integer getMovimentacaoFinanceiraSaidaId() {
        return movimentacaoFinanceiraSaidaId;
    }

    public void setMovimentacaoFinanceiraSaidaId(
            Integer movimentacaoFinanceiraSaidaId
    ) {
        this.movimentacaoFinanceiraSaidaId =
                movimentacaoFinanceiraSaidaId;
    }

    public BigDecimal getValorSaida() {
        return valorSaida;
    }

    public void setValorSaida(BigDecimal valorSaida) {
        if (valorSaida == null) {
            this.valorSaida = BigDecimal.ZERO.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
            return;
        }

        this.valorSaida = valorSaida.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    public Integer getAuditoriaId() {
        return auditoriaId;
    }

    public void setAuditoriaId(Integer auditoriaId) {
        this.auditoriaId = auditoriaId;
    }

    public LocalDateTime getDataHoraEstorno() {
        return dataHoraEstorno;
    }

    public void setDataHoraEstorno(LocalDateTime dataHoraEstorno) {
        this.dataHoraEstorno = dataHoraEstorno;
    }

    public Integer getQuantidadeDeItensRestaurados() {
        return quantidadeDeItensRestaurados;
    }

    public void setQuantidadeDeItensRestaurados(
            Integer quantidadeDeItensRestaurados
    ) {
        this.quantidadeDeItensRestaurados =
                quantidadeDeItensRestaurados != null
                        ? quantidadeDeItensRestaurados
                        : 0;
    }

    public Integer getQuantidadeTotalDeUnidadesRestauradas() {
        return quantidadeTotalDeUnidadesRestauradas;
    }

    public void setQuantidadeTotalDeUnidadesRestauradas(
            Integer quantidadeTotalDeUnidadesRestauradas
    ) {
        this.quantidadeTotalDeUnidadesRestauradas =
                quantidadeTotalDeUnidadesRestauradas != null
                        ? quantidadeTotalDeUnidadesRestauradas
                        : 0;
    }

    @Override
    public String toString() {
        return "ResultadoEstornoVenda{" +
                "vendaId=" + vendaId +
                ", tipoVenda=" + tipoVenda +
                ", statusVendaFinal=" + statusVendaFinal +
                ", contaReceberId=" + contaReceberId +
                ", statusContaReceberFinal=" + statusContaReceberFinal +
                ", movimentacaoFinanceiraOriginalId="
                + movimentacaoFinanceiraOriginalId +
                ", movimentacaoFinanceiraSaidaId="
                + movimentacaoFinanceiraSaidaId +
                ", valorSaida=" + valorSaida +
                ", auditoriaId=" + auditoriaId +
                ", dataHoraEstorno=" + dataHoraEstorno +
                ", quantidadeDeItensRestaurados="
                + quantidadeDeItensRestaurados +
                ", quantidadeTotalDeUnidadesRestauradas="
                + quantidadeTotalDeUnidadesRestauradas +
                '}';
    }
}