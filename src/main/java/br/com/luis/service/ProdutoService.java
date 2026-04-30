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
     * Valida regras de negócio e cadastra um novo produto.
     */
    public void cadastrar(Produto produto) {

        // � FAIL-FAST: defesa contra objeto nulo vindo da UI
        if (produto == null) {
            throw new IllegalArgumentException("O produto não pode ser nulo.");
        }

        // Regra de negócio: todo produto deve iniciar como ATIVO
        if (produto.getStatus() == null) {
            produto.setStatus(Produto.StatusProduto.ATIVO);
        }

        // Regra defensiva adicional (mesmo já validado no Model)
        if (produto.getPreco() == null) {
            throw new IllegalArgumentException("O preço do produto é obrigatório.");
        }

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
     */
    public void atualizar(Produto produto) {

        // FAIL-FAST: defesa contra objeto ou ID nulo
        if (produto == null || produto.getIdProduto() == null) {
            throw new IllegalArgumentException("Produto ou ID inválido para edição.");
        }

        System.out.println("[LOG] Solicitando atualização do produto: " + produto.getDescricao());

        // Delegação para o DAO (persistência)
        produtoDAO.atualizar(produto);
    }

    /**
     * Realiza a exclusão lógica do produto (altera o status para INATIVO).
     * Utilizado pelo botão "Excluir" da interface.
     */
    public void inativar(Produto produto) {

        // FAIL-FAST
        if (produto == null || produto.getIdProduto() == null) {
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
                Produto.StatusProduto.INATIVO
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
     * � Melhor prática: filtrado direto no banco (performance)
     */
    public List<Produto> listarAtivos() {
        return produtoDAO.listarAtivos();
    }

    /**
     * Retorna produtos com estoque baixo.
     * Já vem filtrado no banco com status ATIVO.
     */
    public List<Produto> listarAbaixoDoMinimo() {
        return produtoDAO.listarAbaixoDoMinimo();
    }
}