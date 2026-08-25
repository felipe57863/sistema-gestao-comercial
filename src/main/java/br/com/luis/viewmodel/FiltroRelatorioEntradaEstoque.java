package br.com.luis.viewmodel;

import java.time.LocalDate;

/**
 * Fotografia imutável dos filtros do relatório de entradas de estoque.
 */
public final class FiltroRelatorioEntradaEstoque {

    private final LocalDate dataInicial;
    private final LocalDate dataFinal;
    private final Integer entradaId;
    private final Integer usuarioId;
    private final Integer produtoId;
    private final String referencia;

    public FiltroRelatorioEntradaEstoque(
            LocalDate dataInicial,
            LocalDate dataFinal,
            Integer entradaId,
            Integer usuarioId,
            Integer produtoId,
            String referencia
    ) {
        this.dataInicial = dataInicial;
        this.dataFinal = dataFinal;
        this.entradaId = entradaId;
        this.usuarioId = usuarioId;
        this.produtoId = produtoId;
        this.referencia = normalizarTextoOpcional(referencia);
        validar();
    }

    public void validar() {
        if (dataInicial == null) {
            throw new IllegalArgumentException("Data inicial é obrigatória.");
        }
        if (dataFinal == null) {
            throw new IllegalArgumentException("Data final é obrigatória.");
        }
        if (dataInicial.isAfter(dataFinal)) {
            throw new IllegalArgumentException(
                    "A data inicial não pode ser posterior à data final."
            );
        }
        validarIdOpcional(entradaId, "ID da entrada");
        validarIdOpcional(usuarioId, "ID do responsável");
        validarIdOpcional(produtoId, "ID do produto");
    }

    private static void validarIdOpcional(Integer id, String campo) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException(campo + " deve ser maior que zero.");
        }
    }

    private static String normalizarTextoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    public LocalDate getDataInicial() {
        return dataInicial;
    }

    public LocalDate getDataFinal() {
        return dataFinal;
    }

    public Integer getEntradaId() {
        return entradaId;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public Integer getProdutoId() {
        return produtoId;
    }

    public String getReferencia() {
        return referencia;
    }
}
