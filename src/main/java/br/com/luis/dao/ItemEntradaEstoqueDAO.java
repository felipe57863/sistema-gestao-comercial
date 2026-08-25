package br.com.luis.dao;

import br.com.luis.model.ItemEntradaEstoque;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Persiste itens de entrada de estoque usando uma Connection externa.
 */
public class ItemEntradaEstoqueDAO {

    /**
     * Insere um item da entrada na transação controlada pela camada Service.
     * Encerra apenas o PreparedStatement e o ResultSet que cria.
     */
    public int inserir(Connection conn, ItemEntradaEstoque itemEntradaEstoque) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (itemEntradaEstoque == null) {
            throw new IllegalArgumentException("Item da entrada de estoque não pode ser nulo.");
        }

        if (itemEntradaEstoque.getEntradaId() == null
                || itemEntradaEstoque.getEntradaId() <= 0) {
            throw new IllegalArgumentException("ID da entrada deve ser maior que zero.");
        }

        if (itemEntradaEstoque.getProdutoId() == null
                || itemEntradaEstoque.getProdutoId() <= 0) {
            throw new IllegalArgumentException("ID do produto deve ser maior que zero.");
        }

        if (itemEntradaEstoque.getDescricaoProduto() == null
                || itemEntradaEstoque.getDescricaoProduto().isBlank()) {
            throw new IllegalArgumentException("Descrição do produto não pode ser vazia.");
        }

        if (itemEntradaEstoque.getQuantidadeRecebida() == null
                || itemEntradaEstoque.getQuantidadeRecebida() <= 0) {
            throw new IllegalArgumentException("Quantidade recebida deve ser maior que zero.");
        }

        BigDecimal precoCompraUnitario = itemEntradaEstoque.getPrecoCompraUnitario();
        if (precoCompraUnitario == null
                || precoCompraUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Preço de compra unitário deve ser maior que zero."
            );
        }

        BigDecimal subtotal = itemEntradaEstoque.getSubtotal();
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Subtotal deve ser maior que zero.");
        }

        String sql = """
                INSERT INTO ItemEntradaEstoque (
                    entrada_id,
                    produto_id,
                    descricao_produto,
                    quantidade_recebida,
                    preco_compra_unitario,
                    subtotal
                ) VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt =
                     conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, itemEntradaEstoque.getEntradaId());
            stmt.setInt(2, itemEntradaEstoque.getProdutoId());
            stmt.setString(3, itemEntradaEstoque.getDescricaoProduto());
            stmt.setInt(4, itemEntradaEstoque.getQuantidadeRecebida());
            stmt.setBigDecimal(5, precoCompraUnitario);
            stmt.setBigDecimal(6, subtotal);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int idGerado = rs.getInt(1);

                    if (idGerado > 0) {
                        return idGerado;
                    }
                }
            }

            throw new IllegalStateException(
                    "Item da entrada de estoque inserido, mas o ID gerado não foi retornado."
            );

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao inserir item da entrada de estoque usando conexão externa.",
                    e
            );
        }
    }
}
