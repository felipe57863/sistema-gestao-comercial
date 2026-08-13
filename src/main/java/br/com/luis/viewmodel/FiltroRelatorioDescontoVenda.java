package br.com.luis.viewmodel;

import br.com.luis.model.TipoVenda;

import java.time.LocalDate;

/**
 * Transporta os filtros aplicados ao relatório de descontos concedidos.
 *
 * A classe é imutável e representa uma fotografia do período inclusivo e do
 * tipo de venda selecionado. Tipo nulo representa a opção visual "Todas".
 */
public final class FiltroRelatorioDescontoVenda {

    private final LocalDate dataInicial;
    private final LocalDate dataFinal;
    private final TipoVenda tipoVenda;

    /**
     * Cria uma fotografia imutável dos filtros do relatório.
     *
     * @param dataInicial data inicial inclusiva do período.
     * @param dataFinal data final inclusiva do período.
     * @param tipoVenda tipo específico ou null para todas as vendas.
     */
    public FiltroRelatorioDescontoVenda(
            LocalDate dataInicial,
            LocalDate dataFinal,
            TipoVenda tipoVenda
    ) {
        this.dataInicial = dataInicial;
        this.dataFinal = dataFinal;
        this.tipoVenda = tipoVenda;

        validar();
    }

    /**
     * Valida as datas obrigatórias e a ordem do período.
     */
    public void validar() {
        if (dataInicial == null) {
            throw new IllegalArgumentException(
                    "A data inicial do relatório de descontos é obrigatória."
            );
        }

        if (dataFinal == null) {
            throw new IllegalArgumentException(
                    "A data final do relatório de descontos é obrigatória."
            );
        }

        if (dataInicial.isAfter(dataFinal)) {
            throw new IllegalArgumentException(
                    "A data inicial não pode ser posterior à data final."
            );
        }
    }

    public LocalDate getDataInicial() {
        return dataInicial;
    }

    public LocalDate getDataFinal() {
        return dataFinal;
    }

    public TipoVenda getTipoVenda() {
        return tipoVenda;
    }
}
