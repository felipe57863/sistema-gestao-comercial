package br.com.luis.dao;

import br.com.luis.model.ItemNotaVenda;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO responsável pelos itens históricos vinculados à Nota de Venda.
 *
 * A criação copia ItemVenda e a descrição do Produto usando uma única operação
 * SQL dentro da Connection da finalização. Depois da cópia, geração e
 * reimpressão devem consultar apenas ItemNotaVenda, sem reconstruir descrições
 * ou valores usando os cadastros atuais.
 */
public class ItemNotaVendaDAO {

    /**
     * Copia para a Nota os itens persistidos da venda e a descrição atual do
     * Produto observada na mesma transação.
     *
     * @return quantidade de itens efetivamente copiados.
     */
    public int copiarItensDaVenda(
            Connection conn,
            Integer notaId,
            Integer vendaId
    ) {
        validarConnection(conn);
        validarIdPositivo(notaId, "ID da Nota de Venda");
        validarIdPositivo(vendaId, "ID da venda");

        String sql = """
                INSERT INTO ItemNotaVenda (
                    nota_id,
                    produto_id,
                    descricao_produto,
                    quantidade,
                    preco_unitario,
                    desconto_promocional,
                    desconto_global,
                    subtotal
                )
                SELECT
                    ?,
                    item.produto_id,
                    produto.descricao,
                    item.quantidade,
                    item.preco_unitario,
                    item.desconto_promocional,
                    item.desconto_global,
                    item.subtotal
                FROM ItemVenda item
                INNER JOIN Produto produto
                        ON produto.id_produto = item.produto_id
                WHERE item.venda_id = ?
                ORDER BY item.id_item
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, notaId);
            stmt.setInt(2, vendaId);

            return stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao copiar itens históricos para a Nota de Venda.",
                    e
            );
        }
    }

    /**
     * Lista a fotografia histórica dos itens de uma Nota sem consultar Produto.
     */
    public List<ItemNotaVenda> listarPorNotaId(
            Connection conn,
            Integer notaId
    ) {
        validarConnection(conn);
        validarIdPositivo(notaId, "ID da Nota de Venda");

        String sql = """
                SELECT
                    id_item_nota,
                    nota_id,
                    produto_id,
                    descricao_produto,
                    quantidade,
                    preco_unitario,
                    desconto_promocional,
                    desconto_global,
                    subtotal
                FROM ItemNotaVenda
                WHERE nota_id = ?
                ORDER BY id_item_nota
                """;

        List<ItemNotaVenda> itens = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, notaId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    itens.add(mapearItemNotaVenda(rs));
                }
            }

            return itens;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao listar itens históricos da Nota de Venda.",
                    e
            );
        }
    }

    private ItemNotaVenda mapearItemNotaVenda(
            ResultSet rs
    ) throws SQLException {
        Integer itemNotaId = rs.getInt("id_item_nota");

        try {
            return new ItemNotaVenda(
                    itemNotaId,
                    rs.getInt("nota_id"),
                    rs.getInt("produto_id"),
                    rs.getString("descricao_produto"),
                    rs.getInt("quantidade"),
                    rs.getBigDecimal("preco_unitario"),
                    rs.getBigDecimal("desconto_promocional"),
                    rs.getBigDecimal("desconto_global"),
                    rs.getBigDecimal("subtotal")
            );

        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Item histórico " + itemNotaId
                            + " da Nota de Venda possui dados inválidos.",
                    e
            );
        }
    }

    private void validarConnection(Connection conn) {
        if (conn == null) {
            throw new IllegalArgumentException(
                    "Conexão não pode ser nula."
            );
        }
    }

    private void validarIdPositivo(Integer id, String nomeCampo) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    nomeCampo + " deve ser maior que zero."
            );
        }
    }
}
