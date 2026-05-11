package br.com.luis.dao;

import br.com.luis.model.Produto;
import br.com.luis.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO da entidade Produto.
 * Responsável pela comunicação direta com o banco SQLite.
 */
public class ProdutoDAO {

    /**
     * Insere um novo produto no banco de dados.
     * O ID é gerado automaticamente pelo SQLite (AUTOINCREMENT).
     */
    public void cadastrar(Produto produto) {

        String sql = """
            INSERT INTO Produto (descricao, preco, quantidade_estoque, estoque_minimo, ativo)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Preenchimento dos parâmetros
            stmt.setString(1, produto.getDescricao());
            stmt.setBigDecimal(2, produto.getPreco());
            stmt.setInt(3, produto.getQuantidadeEstoque());
            stmt.setInt(4, produto.getEstoqueMinimo());

            // SQLite não possui BOOLEAN → usamos 1 (true) ou 0 (false)
            stmt.setInt(5, produto.isAtivo() ? 1 : 0);

            stmt.executeUpdate();

            // Recupera o ID gerado pelo banco e sincroniza com o objeto Java
            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    produto.setIdProduto(rs.getInt(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao cadastrar produto no banco de dados.", e);
        }
    }

    /**
     * Retorna todos os produtos cadastrados.
     */
    public List<Produto> listarTodos() {

        String sql = """
            SELECT id_produto, descricao, preco, quantidade_estoque, estoque_minimo, ativo
            FROM Produto
            ORDER BY descricao COLLATE NOCASE
        """;

        List<Produto> produtos = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {

            while (rs.next()) {
                produtos.add(mapearProduto(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar produtos.", e);
        }

        return produtos;
    }

    /**
     * Retorna apenas os produtos ativos.
     * Otimizado diretamente no banco de dados.
     */
    public List<Produto> listarAtivos() {

        String sql = """
            SELECT id_produto, descricao, preco, quantidade_estoque, estoque_minimo, ativo
            FROM Produto
            WHERE ativo = 1
            ORDER BY descricao COLLATE NOCASE
        """;

        List<Produto> produtos = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {

            while (rs.next()) {
                produtos.add(mapearProduto(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar produtos ativos.", e);
        }

        return produtos;
    }

    /**
     * Atualiza os dados de um produto existente.
     * Pode ser utilizado tanto para edição quanto para exclusão lógica (ativo = false).
     */
    public void atualizar(Produto produto) {

        // FAIL-FAST: validação obrigatória de objeto e ID
        if (produto == null || produto.getIdProduto() == null) {
            throw new IllegalArgumentException("Produto ou ID inválido para atualização.");
        }

        String sql = """
            UPDATE Produto
            SET descricao = ?,
                preco = ?,
                quantidade_estoque = ?,
                estoque_minimo = ?,
                ativo = ?
            WHERE id_produto = ?
        """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Preenchimento dos parâmetros com base no objeto
            stmt.setString(1, produto.getDescricao());
            stmt.setBigDecimal(2, produto.getPreco());
            stmt.setInt(3, produto.getQuantidadeEstoque());
            stmt.setInt(4, produto.getEstoqueMinimo());

            // SQLite não possui BOOLEAN → usamos 1 (true) ou 0 (false)
            stmt.setInt(5, produto.isAtivo() ? 1 : 0);

            // WHERE id_produto = ?
            stmt.setInt(6, produto.getIdProduto());

            int linhasAfetadas = stmt.executeUpdate();

            // Verificação de integridade: se nenhuma linha foi alterada, o ID pode não existir
            if (linhasAfetadas == 0) {
                throw new RuntimeException(
                        "Nenhum produto foi atualizado. O ID " + produto.getIdProduto() + " pode não existir."
                );
            }

            System.out.println("[LOG] Produto atualizado: " + produto.getDescricao());

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar o produto no banco de dados.", e);
        }
    }

    /**
     * Busca produtos por parte da descrição (case-insensitive).
     */
    public List<Produto> buscarPorDescricao(String termo) {

        // Comportamento padrão: se vazio, retorna tudo
        if (termo == null || termo.trim().isEmpty()) {
            return listarTodos();
        }

        String sql = """
            SELECT id_produto, descricao, preco, quantidade_estoque, estoque_minimo, ativo
            FROM Produto
            WHERE LOWER(descricao) LIKE ?
            ORDER BY descricao COLLATE NOCASE
        """;

        List<Produto> produtos = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + termo.trim().toLowerCase() + "%");

            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    produtos.add(mapearProduto(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar produtos por descrição.", e);
        }

        return produtos;
    }

    /**
     * Retorna produtos ativos com estoque abaixo ou igual ao mínimo.
     */
    public List<Produto> listarAbaixoDoMinimo() {

        String sql = """
            SELECT id_produto, descricao, preco, quantidade_estoque, estoque_minimo, ativo
            FROM Produto
            WHERE quantidade_estoque <= estoque_minimo
              AND ativo = 1
            ORDER BY quantidade_estoque ASC
        """;

        List<Produto> produtos = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {

            while (rs.next()) {
                produtos.add(mapearProduto(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar produtos com estoque baixo.", e);
        }

        return produtos;
    }

    /**
     * Busca um produto pelo ID.
     *
     * Retorna null caso nenhum produto seja encontrado.
     * A validação de existência deve ser tratada na camada Service.
     */
    public Produto buscarPorId(Integer idProduto) {

        String sql = """
        SELECT id_produto, descricao, preco, quantidade_estoque, estoque_minimo, ativo
        FROM Produto
        WHERE id_produto = ?
    """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idProduto);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearProduto(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar produto por ID.", e);
        }

        return null;
    }

    /**
     * Método utilitário para mapear ResultSet → Produto.
     * Evita duplicação de código e centraliza conversões.
     */
    private Produto mapearProduto(ResultSet rs) throws SQLException {
        Produto produto = new Produto();
        produto.setIdProduto(rs.getInt("id_produto"));
        produto.setDescricao(rs.getString("descricao"));
        produto.setPreco(rs.getBigDecimal("preco"));
        produto.setQuantidadeEstoque(rs.getInt("quantidade_estoque"));
        produto.setEstoqueMinimo(rs.getInt("estoque_minimo"));

        // SQLite INTEGER → Java boolean
        produto.setAtivo(rs.getInt("ativo") == 1);

        return produto;
    }
}