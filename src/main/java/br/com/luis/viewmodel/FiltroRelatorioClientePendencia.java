package br.com.luis.viewmodel;

import br.com.luis.model.Cliente.StatusCliente;

/**
 * Transporta os filtros aplicados ao relatório de clientes com pendências.
 *
 * A classe é imutável e representa uma fotografia dos filtros informados no
 * momento em que a consulta é iniciada. Texto nulo representa ausência de
 * filtro por cliente, status nulo representa todos os status e o indicador de
 * vencidas nulo representa todas as pendências.
 *
 * Esta classe não acessa banco de dados, DAO, Service, sessão, componentes
 * JavaFX ou mecanismos de formatação visual.
 */
public final class FiltroRelatorioClientePendencia {

    private final String clienteTexto;
    private final StatusCliente statusCliente;
    private final Boolean possuiVencidas;

    /**
     * Cria uma fotografia imutável dos filtros do relatório.
     *
     * @param clienteTexto nome ou documento parcial, ou null para todos.
     * @param statusCliente status específico, ou null para todos.
     * @param possuiVencidas true para clientes com vencidas, false para clientes
     *                       sem vencidas ou null para todas as pendências.
     */
    public FiltroRelatorioClientePendencia(
            String clienteTexto,
            StatusCliente statusCliente,
            Boolean possuiVencidas
    ) {
        this.clienteTexto = normalizarClienteTexto(clienteTexto);
        this.statusCliente = statusCliente;
        this.possuiVencidas = possuiVencidas;

        validar();
    }

    /**
     * Valida o estado estrutural da fotografia de filtros.
     *
     * Todos os critérios são opcionais. O método pode ser chamado novamente pelo
     * Service antes da abertura da Connection.
     */
    public void validar() {
        if (clienteTexto != null && clienteTexto.isBlank()) {
            throw new IllegalArgumentException(
                    "Cliente não pode ser vazio quando informado."
            );
        }
    }

    private static String normalizarClienteTexto(String clienteTexto) {
        if (clienteTexto == null || clienteTexto.isBlank()) {
            return null;
        }

        return clienteTexto.trim();
    }

    public String getClienteTexto() {
        return clienteTexto;
    }

    public StatusCliente getStatusCliente() {
        return statusCliente;
    }

    public Boolean getPossuiVencidas() {
        return possuiVencidas;
    }
}
