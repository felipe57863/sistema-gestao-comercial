package br.com.luis.dao;

import br.com.luis.model.ItemVenda;
import br.com.luis.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DAO responsável pela persistência da entidade ItemVenda.
 *
 * Insere os itens já preparados e vinculados à venda pelo VendaService. Não
 * calcula estoque, promoção, descontos ou total e não contém regras de negócio.
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
}