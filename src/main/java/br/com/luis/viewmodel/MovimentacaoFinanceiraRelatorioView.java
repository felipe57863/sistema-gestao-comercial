package br.com.luis.viewmodel;

import br.com.luis.model.FormaPagamento;
import br.com.luis.model.OrigemMovimentacaoFinanceira;
import br.com.luis.model.TipoMovimentacaoFinanceira;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Representa uma única linha projetada do relatório de movimentações financeiras.
 *
 * A classe é imutável e transporta somente os dados consolidados necessários
 * para apresentação do relatório. Não acessa banco de dados, DAO, Service,
 * sessão, componentes JavaFX ou mecanismos de formatação visual.
 */
public final class MovimentacaoFinanceiraRelatorioView {

    private static final int ESCALA_MONETARIA = 2;

    private static final RoundingMode ARREDONDAMENTO_MONETARIO =
            RoundingMode.HALF_UP;

    private final Integer movimentacaoFinanceiraId;
    private final LocalDateTime dataHora;
    private final TipoMovimentacaoFinanceira tipo;
    private final OrigemMovimentacaoFinanceira origem;
    private final FormaPagamento formaPagamento;
    private final BigDecimal valor;
    private final Integer vendaId;
    private final Integer contaReceberId;
    private final Integer usuarioId;
    private final String nomeResponsavel;

    /**
     * Cria uma linha imutável do relatório de movimentações financeiras.
     *
     * @param movimentacaoFinanceiraId identificador da movimentação financeira.
     * @param dataHora data e hora da movimentação.
     * @param tipo tipo da movimentação financeira.
     * @param origem origem da movimentação financeira.
     * @param formaPagamento forma de pagamento registrada.
     * @param valor valor positivo da movimentação.
     * @param vendaId identificador da venda vinculada.
     * @param contaReceberId identificador opcional da conta a receber.
     * @param usuarioId identificador do usuário responsável.
     * @param nomeResponsavel nome do usuário responsável.
     * @throws IllegalArgumentException quando algum dado obrigatório for inválido.
     */
    public MovimentacaoFinanceiraRelatorioView(
            Integer movimentacaoFinanceiraId,
            LocalDateTime dataHora,
            TipoMovimentacaoFinanceira tipo,
            OrigemMovimentacaoFinanceira origem,
            FormaPagamento formaPagamento,
            BigDecimal valor,
            Integer vendaId,
            Integer contaReceberId,
            Integer usuarioId,
            String nomeResponsavel
    ) {
        if (movimentacaoFinanceiraId == null
                || movimentacaoFinanceiraId <= 0) {

            throw new IllegalArgumentException(
                    "ID da movimentação financeira deve ser maior que zero."
            );
        }

        if (dataHora == null) {
            throw new IllegalArgumentException(
                    "Data e hora da movimentação financeira são obrigatórias."
            );
        }

        if (tipo == null) {
            throw new IllegalArgumentException(
                    "Tipo da movimentação financeira é obrigatório."
            );
        }

        if (origem == null) {
            throw new IllegalArgumentException(
                    "Origem da movimentação financeira é obrigatória."
            );
        }

        if (formaPagamento == null) {
            throw new IllegalArgumentException(
                    "Forma de pagamento da movimentação financeira é obrigatória."
            );
        }

        if (formaPagamento == FormaPagamento.A_PRAZO) {
            throw new IllegalArgumentException(
                    "Movimentação financeira não pode usar a forma de pagamento A_PRAZO."
            );
        }

        if (valor == null) {
            throw new IllegalArgumentException(
                    "Valor da movimentação financeira é obrigatório."
            );
        }

        BigDecimal valorNormalizado =
                valor.setScale(
                        ESCALA_MONETARIA,
                        ARREDONDAMENTO_MONETARIO
                );

        if (valorNormalizado.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Valor da movimentação financeira deve ser maior que zero."
            );
        }

        if (vendaId == null || vendaId <= 0) {
            throw new IllegalArgumentException(
                    "ID da venda deve ser maior que zero."
            );
        }

        if (contaReceberId != null && contaReceberId <= 0) {
            throw new IllegalArgumentException(
                    "ID da conta a receber deve ser maior que zero quando informado."
            );
        }

        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException(
                    "ID do usuário responsável deve ser maior que zero."
            );
        }

        if (nomeResponsavel == null || nomeResponsavel.isBlank()) {
            throw new IllegalArgumentException(
                    "Nome do responsável é obrigatório."
            );
        }

        this.movimentacaoFinanceiraId = movimentacaoFinanceiraId;
        this.dataHora = dataHora;
        this.tipo = tipo;
        this.origem = origem;
        this.formaPagamento = formaPagamento;
        this.valor = valorNormalizado;
        this.vendaId = vendaId;
        this.contaReceberId = contaReceberId;
        this.usuarioId = usuarioId;
        this.nomeResponsavel = nomeResponsavel.trim();
    }

    public Integer getMovimentacaoFinanceiraId() {
        return movimentacaoFinanceiraId;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public TipoMovimentacaoFinanceira getTipo() {
        return tipo;
    }

    public OrigemMovimentacaoFinanceira getOrigem() {
        return origem;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public Integer getVendaId() {
        return vendaId;
    }

    public Integer getContaReceberId() {
        return contaReceberId;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public String getNomeResponsavel() {
        return nomeResponsavel;
    }
}
