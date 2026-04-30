package br.com.luis.dao;

import br.com.luis.model.Produto;
import br.com.luis.model.Produto.StatusProduto;
import br.com.luis.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
            INSERT INTO Produto (descricao, preco, quantidade_estoque, estoque_minimo, status)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            // Preenchimento dos parâmetros
            stmt.setString(1, produto.getDescricao());
            stmt.setBigDecimal(2, produto.getPreco());
            stmt.setInt(3, produto.getQuantidadeEstoque());
            stmt.setInt(4, produto.getEstoqueMinimo());

            // Enum → String (SQLite TEXT)
            stmt.setString(5, produto.getStatus().name());

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
            SELECT id_produto, descricao, preco, quantidade_estoque, estoque_minimo, status
            FROM Produto
            ORDER BY descricao
        """;

        List<Produto> produtos = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {

            while (rs.next()) {

                Produto produto = new Produto();

                produto.setIdProduto(rs.getInt("id_produto"));
                produto.setDescricao(rs.getString("descricao"));
                produto.setPreco(rs.getBigDecimal("preco"));
                produto.setQuantidadeEstoque(rs.getInt("quantidade_estoque"));
                produto.setEstoqueMinimo(rs.getInt("estoque_minimo"));

                // SQLite TEXT → Enum Java
                produto.setStatus(StatusProduto.valueOf(rs.getString("status")));

                produtos.add(produto);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar produtos.", e);
        }

        return produtos;
    }
    /**
     * Atualiza os dados de um produto existente.
     * Pode ser utilizado tanto para edição quanto para exclusão lógica (status = INATIVO).
     */
    public void atualizar(Produto produto) {

        // FAIL-FAST: validação obrigatória de objeto e ID
        if (produto == null || produto.getIdProduto() == null) {
            throw new IllegalArgumentException("Produto ou ID inválido para atualização.");
        }

        // Defesa adicional: status não pode ser nulo
        if (produto.getStatus() == null) {
            throw new IllegalArgumentException("Status do produto é obrigatório.");
        }

        String sql = """
        UPDATE Produto 
        SET descricao = ?, preco = ?, quantidade_estoque = ?, estoque_minimo = ?, status = ?
        WHERE id_produto = ?
    """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Preenchimento dos parâmetros com base no objeto
            stmt.setString(1, produto.getDescricao());
            stmt.setBigDecimal(2, produto.getPreco());
            stmt.setInt(3, produto.getQuantidadeEstoque());
            stmt.setInt(4, produto.getEstoqueMinimo());

            // Conversão Enum → String (SQLite armazena como TEXT)
            stmt.setString(5, produto.getStatus().name());

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
}