package br.com.luis.model;

/**
 * Enum que representa a situação documental da Nota de Venda.
 *
 * A Nota nasce ATIVA junto com a finalização da venda. Quando a venda é
 * estornada, a Nota correspondente é marcada como ESTORNADA dentro da mesma
 * transação, preservando integralmente sua fotografia histórica.
 */
public enum StatusNotaVenda {

    /**
     * Nota vinculada a uma venda não estornada.
     */
    ATIVA,

    /**
     * Nota preservada para consulta e reimpressão histórica após o estorno
     * da venda correspondente.
     */
    ESTORNADA
}
