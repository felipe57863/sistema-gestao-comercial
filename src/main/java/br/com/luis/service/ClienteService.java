package br.com.luis.service;

import br.com.luis.dao.ClienteDAO;
import br.com.luis.model.Cliente;

import java.util.List;

/**
 * Camada de Serviço da entidade Cliente.
 * Responsável por aplicar regras de negócio antes da persistência.
 */
public class ClienteService {

    private final ClienteDAO dao;

    public ClienteService() {
        this.dao = new ClienteDAO();
    }

    /**
     * Valida regras de negócio e cadastra um cliente.
     */
    public void cadastrar(Cliente cliente) {

        validarCliente(cliente);

        // Regra de negócio: todo cliente inicia como ATIVO
        cliente.setStatus(Cliente.StatusCliente.ATIVO);

        // Regra de negócio: limite deve existir (já validado no model, mas reforçado aqui)
        if (cliente.getLimiteCredito() == null) {
            throw new IllegalArgumentException("Limite de crédito é obrigatório.");
        }

        System.out.println("[LOG] Cliente enviado para persistência (Cadastro): " + cliente.getNome());

        dao.cadastrar(cliente);
    }

    /**
     * Retorna todos os clientes cadastrados.
     */
    public List<Cliente> listarTodos() {
        return dao.listarTodos();
    }

    /**
     * Valida e atualiza os dados de um cliente existente.
     */
    public void atualizar(Cliente cliente) {

        validarCliente(cliente);

        if (cliente.getIdCliente() == null) {
            throw new IllegalArgumentException("Não é possível atualizar um cliente sem ID.");
        }

        System.out.println("[LOG] Cliente enviado para persistência (Atualização): " + cliente.getNome());

        dao.atualizar(cliente);
    }

    /**
     * Fail-fast: validação básica e reutilizável.
     */
    private void validarCliente(Cliente cliente) {

        if (cliente == null) {
            throw new IllegalArgumentException("Cliente é obrigatório.");
        }

        if (cliente.getNome() == null || cliente.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome é obrigatório.");
        }

        if (cliente.getDocumento() == null || cliente.getDocumento().isBlank()) {
            throw new IllegalArgumentException("Documento é obrigatório.");
        }

        if (cliente.getPrazoPagamento() == null) {
            throw new IllegalArgumentException("Prazo de pagamento é obrigatório.");
        }
    }
}