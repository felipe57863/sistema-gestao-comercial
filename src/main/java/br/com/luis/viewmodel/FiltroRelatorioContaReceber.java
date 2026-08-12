package br.com.luis.viewmodel;

import java.time.LocalDate;

/**
 * Transporta os filtros aplicados ao relatório de contas a receber.
 *
 * A classe é imutável e representa uma fotografia dos filtros informados no
 * momento em que a consulta é iniciada. Cliente nulo representa ausência de
 * filtro textual e situação nula representa a opção visual "Todas".
 *
 * Esta classe não acessa banco de dados, DAO, Service, sessão, componentes
 * JavaFX ou mecanismos de formatação visual.
 */
public final class FiltroRelatorioContaReceber {

    private final LocalDate dataInicial;
    private final LocalDate dataFinal;
    private final String clienteTexto;
    private final SituacaoRelatorioContaReceber situacao;

    /**
     * Cria uma fotografia imutável dos filtros do relatório de contas a receber.
     *
     * O texto do cliente é normalizado somente por remoção dos espaços externos.
     * Valor nulo ou em branco representa ausência do filtro. Nenhuma máscara de
     * CPF ou CNPJ é removida nesta classe, preservando o texto informado para a
     * fotografia da consulta.
     *
     * @param dataInicial data inicial inclusiva do período de vencimento.
     * @param dataFinal data final inclusiva do período de vencimento.
     * @param clienteTexto nome ou documento informado, ou null para todos.
     * @param situacao situação selecionada, ou null para todas.
     * @throws IllegalArgumentException quando alguma data obrigatória estiver
     *                                  ausente ou quando o período for inválido.
     */
    public FiltroRelatorioContaReceber(
            LocalDate dataInicial,
            LocalDate dataFinal,
            String clienteTexto,
            SituacaoRelatorioContaReceber situacao
    ) {
        this.dataInicial = dataInicial;
        this.dataFinal = dataFinal;
        this.clienteTexto = normalizarClienteTexto(clienteTexto);
        this.situacao = situacao;

        validar();
    }

    /**
     * Valida o contrato estrutural completo dos filtros.
     *
     * O método pode ser chamado novamente pelo Service antes da abertura da
     * Connection, preservando a validação definitiva fora da camada visual.
     */
    public void validar() {
        if (dataInicial == null) {
            throw new IllegalArgumentException(
                    "A data inicial do relatório de contas a receber é obrigatória."
            );
        }

        if (dataFinal == null) {
            throw new IllegalArgumentException(
                    "A data final do relatório de contas a receber é obrigatória."
            );
        }

        if (dataInicial.isAfter(dataFinal)) {
            throw new IllegalArgumentException(
                    "A data inicial não pode ser posterior à data final."
            );
        }
    }

    /**
     * Normaliza somente os espaços externos do texto informado.
     */
    private static String normalizarClienteTexto(String clienteTexto) {
        if (clienteTexto == null || clienteTexto.isBlank()) {
            return null;
        }

        return clienteTexto.trim();
    }

    public LocalDate getDataInicial() {
        return dataInicial;
    }

    public LocalDate getDataFinal() {
        return dataFinal;
    }

    public String getClienteTexto() {
        return clienteTexto;
    }

    public SituacaoRelatorioContaReceber getSituacao() {
        return situacao;
    }
}
