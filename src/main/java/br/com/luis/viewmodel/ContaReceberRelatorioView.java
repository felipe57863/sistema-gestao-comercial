package br.com.luis.viewmodel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Representa uma única linha final do relatório de contas a receber.
 *
 * A classe é imutável e transporta somente os dados necessários para futura
 * apresentação na TableView. A situação já deve ter sido calculada pelo Service
 * antes da criação desta projeção.
 *
 * Esta classe não acessa banco de dados, DAO, Service, sessão, componentes
 * JavaFX ou mecanismos de formatação visual.
 */
public final class ContaReceberRelatorioView {

    private static final int ESCALA_MONETARIA = 2;

    private static final RoundingMode ARREDONDAMENTO_MONETARIO =
            RoundingMode.HALF_UP;

    private final Integer idConta;
    private final Integer vendaId;
    private final String nomeCliente;
    private final BigDecimal valor;
    private final LocalDate dataVencimento;
    private final SituacaoRelatorioContaReceber situacao;

    /**
     * Cria uma linha imutável do relatório de contas a receber.
     *
     * @param idConta identificador da conta a receber.
     * @param vendaId identificador da venda vinculada.
     * @param nomeCliente nome atual do cliente vinculado.
     * @param valor valor da conta a receber.
     * @param dataVencimento data de vencimento da conta.
     * @param situacao situação gerencial calculada pelo Service.
     * @throws IllegalArgumentException quando algum dado obrigatório for inválido.
     */
    public ContaReceberRelatorioView(
            Integer idConta,
            Integer vendaId,
            String nomeCliente,
            BigDecimal valor,
            LocalDate dataVencimento,
            SituacaoRelatorioContaReceber situacao
    ) {
        if (idConta == null || idConta <= 0) {
            throw new IllegalArgumentException(
                    "ID da conta a receber deve ser maior que zero."
            );
        }

        if (vendaId == null || vendaId <= 0) {
            throw new IllegalArgumentException(
                    "ID da venda deve ser maior que zero."
            );
        }

        if (nomeCliente == null || nomeCliente.isBlank()) {
            throw new IllegalArgumentException(
                    "Nome do cliente é obrigatório."
            );
        }

        if (valor == null) {
            throw new IllegalArgumentException(
                    "Valor da conta a receber é obrigatório."
            );
        }

        BigDecimal valorNormalizado = normalizarValorMonetario(valor);

        if (valorNormalizado.signum() < 0) {
            throw new IllegalArgumentException(
                    "Valor da conta a receber não pode ser negativo."
            );
        }

        if (dataVencimento == null) {
            throw new IllegalArgumentException(
                    "Data de vencimento da conta a receber é obrigatória."
            );
        }

        if (situacao == null) {
            throw new IllegalArgumentException(
                    "Situação da conta a receber é obrigatória."
            );
        }

        this.idConta = idConta;
        this.vendaId = vendaId;
        this.nomeCliente = nomeCliente.trim();
        this.valor = valorNormalizado;
        this.dataVencimento = dataVencimento;
        this.situacao = situacao;
    }

    private static BigDecimal normalizarValorMonetario(BigDecimal valor) {
        return valor.setScale(
                ESCALA_MONETARIA,
                ARREDONDAMENTO_MONETARIO
        );
    }

    public Integer getIdConta() {
        return idConta;
    }

    public Integer getVendaId() {
        return vendaId;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public SituacaoRelatorioContaReceber getSituacao() {
        return situacao;
    }
}
