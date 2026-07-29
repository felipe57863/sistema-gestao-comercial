package br.com.luis.viewmodel;

import br.com.luis.model.FormaPagamento;
import br.com.luis.model.OrigemMovimentacaoFinanceira;
import br.com.luis.model.TipoMovimentacaoFinanceira;

import java.time.LocalDate;

/**
 * Transporta os filtros aplicados ao relatório de movimentações financeiras.
 *
 * A classe é imutável e representa uma fotografia dos filtros informados no
 * momento em que a consulta é iniciada. Valores nulos para tipo, origem ou
 * forma de pagamento representam a opção visual "Todos".
 *
 * Esta classe não acessa banco de dados, DAO, Service, sessão, componentes
 * JavaFX ou mecanismos de formatação visual.
 */
public final class FiltroRelatorioMovimentacaoFinanceira {

    private final LocalDate dataInicial;
    private final LocalDate dataFinal;
    private final TipoMovimentacaoFinanceira tipo;
    private final OrigemMovimentacaoFinanceira origem;
    private final FormaPagamento formaPagamento;

    /**
     * Cria uma fotografia imutável dos filtros do relatório financeiro.
     *
     * Tipo, origem e forma de pagamento podem ser nulos para representar a
     * ausência do respectivo filtro. A forma {@link FormaPagamento#A_PRAZO}
     * não é aceita porque não representa uma movimentação financeira.
     *
     * @param dataInicial data inicial inclusiva do período.
     * @param dataFinal data final inclusiva do período.
     * @param tipo tipo da movimentação ou null para todos.
     * @param origem origem da movimentação ou null para todas.
     * @param formaPagamento forma de pagamento ou null para todas.
     * @throws IllegalArgumentException quando alguma data obrigatória estiver
     *                                  ausente, quando o período for inválido ou
     *                                  quando a forma A_PRAZO for informada.
     */
    public FiltroRelatorioMovimentacaoFinanceira(
            LocalDate dataInicial,
            LocalDate dataFinal,
            TipoMovimentacaoFinanceira tipo,
            OrigemMovimentacaoFinanceira origem,
            FormaPagamento formaPagamento
    ) {
        this.dataInicial = dataInicial;
        this.dataFinal = dataFinal;
        this.tipo = tipo;
        this.origem = origem;
        this.formaPagamento = formaPagamento;

        validar();
    }

    /**
     * Valida o contrato completo dos filtros.
     *
     * O método pode ser chamado novamente pelo Service antes da abertura da
     * Connection, preservando a validação definitiva fora da camada visual.
     */
    public void validar() {
        if (dataInicial == null) {
            throw new IllegalArgumentException(
                    "A data inicial do relatório financeiro é obrigatória."
            );
        }

        if (dataFinal == null) {
            throw new IllegalArgumentException(
                    "A data final do relatório financeiro é obrigatória."
            );
        }

        if (dataInicial.isAfter(dataFinal)) {
            throw new IllegalArgumentException(
                    "A data inicial não pode ser posterior à data final."
            );
        }

        if (formaPagamento == FormaPagamento.A_PRAZO) {
            throw new IllegalArgumentException(
                    "O relatório financeiro não aceita a forma de pagamento A_PRAZO."
            );
        }
    }

    /**
     * Retorna a data inicial inclusiva do período.
     */
    public LocalDate getDataInicial() {
        return dataInicial;
    }

    /**
     * Retorna a data final inclusiva do período.
     */
    public LocalDate getDataFinal() {
        return dataFinal;
    }

    /**
     * Retorna o tipo selecionado ou null quando todos forem permitidos.
     */
    public TipoMovimentacaoFinanceira getTipo() {
        return tipo;
    }

    /**
     * Retorna a origem selecionada ou null quando todas forem permitidas.
     */
    public OrigemMovimentacaoFinanceira getOrigem() {
        return origem;
    }

    /**
     * Retorna a forma selecionada ou null quando todas forem permitidas.
     */
    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

}
