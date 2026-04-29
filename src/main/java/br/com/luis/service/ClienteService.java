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

        // Fail-fast: validação básica
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente é obrigatório.");
        }

        // Regra de negócio: todo cliente inicia como ATIVO
        cliente.setStatus(Cliente.StatusCliente.ATIVO);

        // Regra de negócio: limite deve existir (já validado no model, mas reforçado aqui)
        if (cliente.getLimiteCredito() == null) {
            throw new IllegalArgumentException("Limite de crédito é obrigatório.");
        }

        // NOTA:
        // - Validações estruturais estão no Model (Rich Domain Model)
        // - Duplicidade de documento é garantida pelo banco (UNIQUE) e tratada no DAO

        // Log de auditoria
        System.out.println("[LOG] Cliente enviado para persistência: " + cliente.getNome());

        // Persistência
        dao.cadastrar(cliente);
    }

    /**
     * Retorna todos os clientes cadastrados.
     */
    public List<Cliente> listarTodos() {
        return dao.listarTodos();
    }
}