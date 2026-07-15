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
     * Valida regras cadastrais e cadastra um novo cliente.
     *
     * @implNote Impede documento duplicado e garante que todo novo cliente seja
     * criado como ATIVO.
     */
    public void cadastrar(Cliente cliente) {

        validarCliente(cliente);

        // Regra de negócio: evitar duplicidade de documento
        validarDocumentoDuplicado(cliente.getDocumento(), null);

        // Regra de negócio: todo cliente inicia como ATIVO
        cliente.setStatus(Cliente.StatusCliente.ATIVO);

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
     * Valida regras cadastrais e atualiza os dados de um cliente existente.
     *
     * @implNote Exige ID para atualização e bloqueia documento duplicado em
     * outro cliente.
     */
    public void atualizar(Cliente cliente) {

        validarCliente(cliente);

        if (cliente.getIdCliente() == null || cliente.getIdCliente() <= 0) {
            throw new IllegalArgumentException("Não é possível atualizar um cliente sem ID válido.");
        }

        // Regra de negócio: evitar duplicidade de documento em outro cliente
        validarDocumentoDuplicado(cliente.getDocumento(), cliente.getIdCliente());

        System.out.println("[LOG] Cliente enviado para persistência (Atualização): " + cliente.getNome());

        dao.atualizar(cliente);
    }

    /**
     * Fail-fast: validação básica e reutilizável para cadastro e atualização.
     *
     * @implNote Garante que os dados mínimos do cliente estejam preenchidos
     * antes da persistência.
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

        if (cliente.getTipo() == null) {
            throw new IllegalArgumentException("Tipo de cliente é obrigatório.");
        }

        if (cliente.getLimiteCredito() == null) {
            throw new IllegalArgumentException("Limite de crédito é obrigatório.");
        }

        if (cliente.getLimiteCredito().signum() < 0) {
            throw new IllegalArgumentException("Limite de crédito não pode ser negativo.");
        }

        if (cliente.getPrazoPagamento() == null || cliente.getPrazoPagamento().getIdPrazo() == null) {
            throw new IllegalArgumentException("Prazo de pagamento é obrigatório.");
        }
    }

    /**
     * Valida se já existe outro cliente com o mesmo documento.
     *
     * @implNote Impede duplicidade lógica de documento no cadastro de clientes.
     */
    private void validarDocumentoDuplicado(String documento, Integer idAtual) {

        List<Cliente> existentes = dao.listarTodos();

        for (Cliente clienteExistente : existentes) {

            boolean mesmoDocumento = clienteExistente.getDocumento().equals(documento);
            boolean mesmoRegistro = idAtual != null && clienteExistente.getIdCliente().equals(idAtual);

            if (mesmoDocumento && !mesmoRegistro) {
                throw new RuntimeException("Já existe um cliente cadastrado com esse documento.");
            }
        }
    }
}