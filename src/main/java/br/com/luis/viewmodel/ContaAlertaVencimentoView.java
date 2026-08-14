package br.com.luis.viewmodel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Representa uma conta incluída no resultado dos alertas de vencimento.
 *
 * A situação já foi calculada pelo Service a partir da data única de referência.
 */
public final class ContaAlertaVencimentoView {

    private static final int ESCALA_MONETARIA = 2;

    private static final RoundingMode ARREDONDAMENTO_MONETARIO =
            RoundingMode.HALF_UP;

    private final Integer idConta;
    private final Integer vendaId;
    private final String nomeCliente;
    private final BigDecimal valor;
    private final LocalDate dataVencimento;
    private final SituacaoAlertaVencimento situacao;

    public ContaAlertaVencimentoView(
            Integer idConta,
            Integer vendaId,
            String nomeCliente,
            BigDecimal valor,
            LocalDate dataVencimento,
            SituacaoAlertaVencimento situacao
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
                    "Valor da conta é obrigatório."
            );
        }

        BigDecimal valorNormalizado = valor.setScale(
                ESCALA_MONETARIA,
                ARREDONDAMENTO_MONETARIO
        );

        if (valorNormalizado.signum() < 0) {
            throw new IllegalArgumentException(
                    "Valor da conta não pode ser negativo."
            );
        }

        if (dataVencimento == null) {
            throw new IllegalArgumentException(
                    "Data de vencimento é obrigatória."
            );
        }

        if (situacao == null) {
            throw new IllegalArgumentException(
                    "Situação do alerta é obrigatória."
            );
        }

        this.idConta = idConta;
        this.vendaId = vendaId;
        this.nomeCliente = nomeCliente.trim();
        this.valor = valorNormalizado;
        this.dataVencimento = dataVencimento;
        this.situacao = situacao;
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

    public SituacaoAlertaVencimento getSituacao() {
        return situacao;
    }
}