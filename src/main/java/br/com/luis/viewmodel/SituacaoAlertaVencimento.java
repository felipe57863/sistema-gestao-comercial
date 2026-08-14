package br.com.luis.viewmodel;

/**
 * Situação calculada de uma conta incluída nos alertas de vencimento.
 *
 * Não representa um status persistido da ContaReceber.
 */
public enum SituacaoAlertaVencimento {

    VENCIDA,
    PROXIMA_DO_VENCIMENTO
}