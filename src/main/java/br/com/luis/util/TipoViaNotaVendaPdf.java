package br.com.luis.util;

/**
 * Tipo visual usado exclusivamente na apresentação do PDF da Nota de Venda.
 *
 * Não é persistido e não altera a identidade documental da Nota. A opção
 * SEGUNDA_VIA representa somente uma reprodução posterior da mesma NotaVenda.
 */
public enum TipoViaNotaVendaPdf {
    ORIGINAL,
    SEGUNDA_VIA
}
