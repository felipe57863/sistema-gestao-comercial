package br.com.luis.viewmodel;

import br.com.luis.model.StatusContaReceber;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Transporta a projeção persistida necessária ao relatório de contas a receber.
 *
 * Esta classe existe exclusivamente na passagem DAO -> Service. O DAO obtém os
 * dados persistidos da conta e o nome atual do cliente em uma única consulta e
 * os entrega ao Service sem calcular a situação gerencial da linha.
 *
 * O status persistido é mantido nesta projeção porque o Service precisa combiná-lo
 * com o vencimento e com a data de referência da consulta para produzir a situação
 * final A_VENCER, VENCIDA, PAGA ou CANCELADA.
 *
 * A classe não acessa banco de dados, não contém JavaFX, não calcula situação e
 * não executa regras de autorização ou totalização.
 */
public final class ContaReceberRelatorioDados {

    private static final int ESCALA_MONETARIA = 2;

    private static final RoundingMode ARREDONDAMENTO_MONETARIO =
            RoundingMode.HALF_UP;

    private final Integer idConta;
    private final Integer vendaId;
    private final String nomeCliente;
    private final BigDecimal valor;
    private final LocalDate dataVencimento;
    private final StatusContaReceber statusPersistido;

    /**
     * Cria uma projeção imutável dos dados persistidos de uma conta.
     *
     * @param idConta identificador da conta a receber.
     * @param vendaId identificador da venda vinculada.
     * @param nomeCliente nome atual do cliente vinculado.
     * @param valor valor persistido da conta.
     * @param dataVencimento data de vencimento persistida.
     * @param statusPersistido status atual persistido da conta.
     * @throws IllegalArgumentException quando algum dado obrigatório for inválido.
     */
    public ContaReceberRelatorioDados(
            Integer idConta,
            Integer vendaId,
            String nomeCliente,
            BigDecimal valor,
            LocalDate dataVencimento,
            StatusContaReceber statusPersistido
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

        if (statusPersistido == null) {
            throw new IllegalArgumentException(
                    "Status persistido da conta a receber é obrigatório."
            );
        }

        this.idConta = idConta;
        this.vendaId = vendaId;
        this.nomeCliente = nomeCliente.trim();
        this.valor = valorNormalizado;
        this.dataVencimento = dataVencimento;
        this.statusPersistido = statusPersistido;
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

    public StatusContaReceber getStatusPersistido() {
        return statusPersistido;
    }
}
