package br.com.luis.service;

import br.com.luis.dao.ProdutoDAO;
import br.com.luis.model.Produto;

import java.util.List;

/**
 * Camada de Serviço (Regras de Negócio) para Produtos.
 * Responsável por validar dados e delegar para o DAO.
 */
public class ProdutoService {

    private final ProdutoDAO produtoDAO;

    public ProdutoService() {
        this.produtoDAO = new ProdutoDAO();
    }

    /**
     * Valida regras cadastrais e cadastra um novo produto.
     *
     * @implNote Validação cadastral da Fase 3:
     * garante dados mínimos válidos e define todo novo produto como ativo.
     */
    public void cadastrar(Produto produto) {

        validarProduto(produto);

        // Regra de negócio: todo produto deve iniciar como ativo
        produto.setAtivo(true);

        System.out.println("[LOG] Produto enviado para persistência: " + produto.getDescricao());

        // Delega para o DAO
        produtoDAO.cadastrar(produto);
    }

    /**
     * Retorna todos os produtos cadastrados.
     * Usado para alimentar tabelas na interface (JavaFX).
     */
    public List<Produto> listarTodos() {
        return produtoDAO.listarTodos();
    }

    /**
     * Valida e envia o produto modificado para atualização no banco.
     * Utilizado pelo botão "Editar" da interface.
     *
     * @implNote Validação cadastral da Fase 3:
     * exige produto com ID válido antes de permitir atualização.
     */
    public void atualizar(Produto produto) {

        validarProduto(produto);

        // FAIL-FAST: defesa contra ID nulo ou inválido
        if (produto.getIdProduto() == null || produto.getIdProduto() <= 0) {
            throw new IllegalArgumentException("Produto ou ID inválido para edição.");
        }

        System.out.println("[LOG] Solicitando atualização do produto: " + produto.getDescricao());

        // Delegação para o DAO (persistência)
        produtoDAO.atualizar(produto);
    }

    /**
     * Realiza a exclusão lógica do produto, alterando o campo ativo para false.
     *
     * @implNote Regra crítica do Bloco 2:
     * produto com histórico não deve ser excluído fisicamente, apenas inativado.
     */
    public void inativar(Produto produto) {

        // FAIL-FAST
        if (produto == null || produto.getIdProduto() == null || produto.getIdProduto() <= 0) {
            throw new IllegalArgumentException("Produto inválido para inativação.");
        }

        System.out.println("[LOG] Realizando exclusão lógica do produto ID: " + produto.getIdProduto());

        // ISOLAMENTO DE MEMÓRIA (evita efeitos colaterais na UI)
        Produto produtoInativado = new Produto(
                produto.getIdProduto(),
                produto.getDescricao(),
                produto.getPreco(),
                produto.getQuantidadeEstoque(),
                produto.getEstoqueMinimo(),
                false
        );

        // Reaproveita o método de atualização
        produtoDAO.atualizar(produtoInativado);
    }

    /**
     * Busca produtos por nome/descrição.
     */
    public List<Produto> buscarPorDescricao(String termo) {
        return produtoDAO.buscarPorDescricao(termo);
    }

    /**
     * Retorna apenas produtos ativos.
     * Melhor prática: filtrado direto no banco (performance).
     */
    public List<Produto> listarAtivos() {
        return produtoDAO.listarAtivos();
    }

    /**
     * Retorna produtos com estoque baixo.
     * Já vem filtrado no banco com ativo = 1.
     */
    public List<Produto> listarAbaixoDoMinimo() {
        return produtoDAO.listarAbaixoDoMinimo();
    }

    /**
     * Fail-fast: validação básica e reutilizável para cadastro e atualização.
     *
     * @implNote Validação cadastral da Fase 3:
     * garante que os dados mínimos do produto estejam preenchidos antes da persistência.
     */
    private void validarProduto(Produto produto) {

        if (produto == null) {
            throw new IllegalArgumentException("O produto não pode ser nulo.");
        }

        if (produto.getDescricao() == null || produto.getDescricao().isBlank()) {
            throw new IllegalArgumentException("A descrição do produto é obrigatória.");
        }

        if (produto.getPreco() == null) {
            throw new IllegalArgumentException("O preço do produto é obrigatório.");
        }

        if (produto.getPreco().signum() < 0) {
            throw new IllegalArgumentException("O preço do produto não pode ser negativo.");
        }

        if (produto.getQuantidadeEstoque() == null) {
            throw new IllegalArgumentException("A quantidade em estoque é obrigatória.");
        }

        if (produto.getQuantidadeEstoque() < 0) {
            throw new IllegalArgumentException("Quantidade em estoque não pode ser negativa.");
        }

        if (produto.getEstoqueMinimo() == null) {
            throw new IllegalArgumentException("O estoque mínimo é obrigatório.");
        }

        if (produto.getEstoqueMinimo() < 0) {
            throw new IllegalArgumentException("Estoque mínimo não pode ser negativo.");
        }
    }
}