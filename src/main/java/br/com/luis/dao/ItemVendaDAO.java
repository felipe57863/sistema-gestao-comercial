package br.com.luis.dao;

import br.com.luis.model.ItemVenda;
import br.com.luis.util.ConnectionFactory;
import br.com.luis.viewmodel.ItemVendaHistoricoView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO responsável pela persistência e consulta dos itens vinculados às vendas.
 *
 * Insere os itens preparados pelo VendaService, recupera os registros necessários
 * ao estorno e fornece os detalhes usados pelo Histórico de Vendas. As consultas
 * preservam quantidade, preço e subtotal gravados no momento da venda e podem
 * trazer a descrição do produto já associada, evitando consultas individuais na
 * montagem dos detalhes.
 *
 * Não calcula estoque, promoção, descontos ou total e não decide regras de
 * estorno. Quando recebe uma Connection externa, participa da transação
 * controlada pelo Service sem executar commit, rollback ou fechar a conexão.
 */
public class ItemVendaDAO {

    /**
     * Insere um item de venda no banco de dados.
     *
     * Abre e fecha sua própria Connection e usa try-with-resources para encerrar
     * o PreparedStatement e o ResultSet. Persiste um item individual com os
     * valores e vínculos recebidos já definidos.
     *
     * @param itemVenda item de venda que será persistido.
     * @return ID gerado pelo banco para o item inserido.
     */
    public int inserir(ItemVenda itemVenda) {
        String sql = """
                INSERT INTO ItemVenda (
                    quantidade,
                    preco_unitario,
                    desconto_promocional,
                    desconto_global,
                    subtotal,
                    produto_id,
                    venda_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, itemVenda.getQuantidade());
            stmt.setBigDecimal(2, itemVenda.getPrecoUnitario());
            stmt.setBigDecimal(3, itemVenda.getDescontoPromocional());
            stmt.setBigDecimal(4, itemVenda.getDescontoGlobal());
            stmt.setBigDecimal(5, itemVenda.getSubtotal());
            stmt.setInt(6, itemVenda.getProdutoId());
            stmt.setInt(7, itemVenda.getVendaId());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            throw new RuntimeException("Item de venda inserido, mas o ID gerado não foi retornado pelo banco.");

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir item de venda no banco de dados.", e);
        }
    }

    /**
     * Insere um item de venda usando uma Connection externa.
     *
     * Participa da mesma transação usada para persistir a Venda, coordenada pelo
     * VendaService. Encerra o PreparedStatement e o ResultSet que cria, mas
     * respeita a propriedade da Connection recebida.
     *
     * Importante:
     * - não abre nova Connection;
     * - não executa commit;
     * - não executa rollback;
     * - não fecha a Connection recebida.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param itemVenda item de venda que será persistido.
     * @return ID gerado pelo banco para o item inserido.
     */
    public int inserir(Connection conn, ItemVenda itemVenda) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (itemVenda == null) {
            throw new IllegalArgumentException("Item de venda não pode ser nulo.");
        }

        String sql = """
                INSERT INTO ItemVenda (
                    quantidade,
                    preco_unitario,
                    desconto_promocional,
                    desconto_global,
                    subtotal,
                    produto_id,
                    venda_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, itemVenda.getQuantidade());
            stmt.setBigDecimal(2, itemVenda.getPrecoUnitario());
            stmt.setBigDecimal(3, itemVenda.getDescontoPromocional());
            stmt.setBigDecimal(4, itemVenda.getDescontoGlobal());
            stmt.setBigDecimal(5, itemVenda.getSubtotal());
            stmt.setInt(6, itemVenda.getProdutoId());
            stmt.setInt(7, itemVenda.getVendaId());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            throw new RuntimeException("Item de venda inserido, mas o ID gerado não foi retornado pelo banco.");

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir item de venda no banco de dados usando conexão externa.", e);
        }
    }
    /**
     * Lista os itens vinculados a uma venda usando uma Connection externa.
     *
     * Participa da transação controlada pela camada Service e encerra somente
     * o PreparedStatement e o ResultSet criados pelo método.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param vendaId identificador da venda.
     * @return lista dos itens vinculados à venda, vazia quando nenhum item
     *         for encontrado.
     */
    public List<ItemVenda> listarPorVendaId(Connection conn, Integer vendaId) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (vendaId == null || vendaId <= 0) {
            throw new IllegalArgumentException("ID da venda deve ser maior que zero.");
        }

        String sql = """
            SELECT
                id_item,
                quantidade,
                preco_unitario,
                desconto_promocional,
                desconto_global,
                subtotal,
                produto_id,
                venda_id
            FROM ItemVenda
            WHERE venda_id = ?
            ORDER BY id_item
            """;

        List<ItemVenda> itens = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, vendaId);

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    ItemVenda item = new ItemVenda();

                    item.setIdItem(rs.getInt("id_item"));
                    item.setQuantidade(rs.getInt("quantidade"));
                    item.setPrecoUnitario(rs.getBigDecimal("preco_unitario"));
                    item.setDescontoPromocional(
                            rs.getBigDecimal("desconto_promocional")
                    );
                    item.setDescontoGlobal(rs.getBigDecimal("desconto_global"));
                    item.setSubtotal(rs.getBigDecimal("subtotal"));
                    item.setProdutoId(rs.getInt("produto_id"));
                    item.setVendaId(rs.getInt("venda_id"));

                    itens.add(item);
                }

                return itens;
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao listar os itens da venda no banco de dados.",
                    e
            );
        }
    }
    /**
     * Lista os itens históricos de uma venda juntamente com a descrição
     * do produto.
     *
     * Preserva quantidade, preço unitário e subtotal persistidos em ItemVenda.
     * Não consulta cada produto individualmente.
     *
     * Não abre ou fecha Connection, não executa commit e não executa rollback.
     */
    public List<ItemVendaHistoricoView>
    listarDetalhesPorVendaId(
            Connection conn,
            Integer vendaId
    ) {

        if (conn == null) {
            throw new IllegalArgumentException(
                    "Conexão não pode ser nula."
            );
        }

        if (vendaId == null || vendaId <= 0) {
            throw new IllegalArgumentException(
                    "ID da venda deve ser maior que zero."
            );
        }

        String sql = """
                SELECT
                    item.produto_id,
                    produto.descricao AS descricao_produto,
                    item.quantidade,
                    item.preco_unitario,
                    item.subtotal
                FROM ItemVenda item
                INNER JOIN Produto produto
                        ON produto.id_produto = item.produto_id
                WHERE item.venda_id = ?
                ORDER BY item.id_item
                """;

        List<ItemVendaHistoricoView> itens =
                new ArrayList<>();

        try (PreparedStatement stmt =
                     conn.prepareStatement(sql)) {

            stmt.setInt(
                    1,
                    vendaId
            );

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    ItemVendaHistoricoView itemView =
                            new ItemVendaHistoricoView(
                                    rs.getInt(
                                            "produto_id"
                                    ),
                                    rs.getString(
                                            "descricao_produto"
                                    ),
                                    rs.getInt(
                                            "quantidade"
                                    ),
                                    rs.getBigDecimal(
                                            "preco_unitario"
                                    ),
                                    rs.getBigDecimal(
                                            "subtotal"
                                    )
                            );

                    itens.add(itemView);
                }
            }

            return itens;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao listar os detalhes dos itens da venda.",
                    e
            );
        }
    }
}