package br.com.luis.viewmodel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Projeção imutável de uma entrada completa na listagem do relatório.
 */
public final class EntradaEstoqueRelatorioView {

    private final Integer idEntrada;
    private final LocalDateTime dataHora;
    private final Integer usuarioId;
    private final String nomeUsuario;
    private final String referencia;
    private final String observacao;
    private final Integer quantidadeProdutosDistintos;
    private final Integer totalUnidades;
    private final BigDecimal valorTotal;

    public EntradaEstoqueRelatorioView(
            Integer idEntrada,
            LocalDateTime dataHora,
            Integer usuarioId,
            String nomeUsuario,
            String referencia,
            String observacao,
            Integer quantidadeProdutosDistintos,
            Integer totalUnidades,
            BigDecimal valorTotal
    ) {
        validarId(idEntrada, "ID da entrada");
        if (dataHora == null) {
            throw new IllegalArgumentException("Data e hora são obrigatórias.");
        }
        validarId(usuarioId, "ID do usuário");
        if (nomeUsuario == null || nomeUsuario.isBlank()) {
            throw new IllegalArgumentException("Nome do usuário é obrigatório.");
        }
        if (quantidadeProdutosDistintos == null
                || quantidadeProdutosDistintos <= 0) {
            throw new IllegalArgumentException(
                    "Quantidade de produtos distintos deve ser maior que zero."
            );
        }
        if (totalUnidades == null || totalUnidades <= 0) {
            throw new IllegalArgumentException(
                    "Total de unidades deve ser maior que zero."
            );
        }
        if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor total deve ser maior que zero.");
        }

        this.idEntrada = idEntrada;
        this.dataHora = dataHora;
        this.usuarioId = usuarioId;
        this.nomeUsuario = nomeUsuario.trim();
        this.referencia = normalizarTextoOpcional(referencia);
        this.observacao = normalizarTextoOpcional(observacao);
        this.quantidadeProdutosDistintos = quantidadeProdutosDistintos;
        this.totalUnidades = totalUnidades;
        this.valorTotal = valorTotal.setScale(2, RoundingMode.HALF_UP);
    }

    private static void validarId(Integer id, String campo) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(campo + " deve ser maior que zero.");
        }
    }

    private static String normalizarTextoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    public Integer getIdEntrada() {
        return idEntrada;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }

    public String getReferencia() {
        return referencia;
    }

    public String getObservacao() {
        return observacao;
    }

    public Integer getQuantidadeProdutosDistintos() {
        return quantidadeProdutosDistintos;
    }

    public Integer getTotalUnidades() {
        return totalUnidades;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }
}
