package br.com.luis.model;

/**
 * Enum que representa os tipos oficiais de venda do sistema.
 *
 * Padroniza a finalização da venda, evitando o uso de textos soltos como
 * "avista", "prazo" ou variações similares.
 *
 * Regras:
 * - A_VISTA: venda paga no momento da finalização.
 * - A_PRAZO: venda que gera uma ContaReceber.
 */
public enum TipoVenda {

    /**
     * Venda à vista.
     *
     * Deve gerar Venda com status PAGA e MovimentacaoFinanceira.
     */
    A_VISTA,

    /**
     * Venda a prazo.
     *
     * Deve gerar Venda com status PENDENTE e ContaReceber.
     */
    A_PRAZO
}