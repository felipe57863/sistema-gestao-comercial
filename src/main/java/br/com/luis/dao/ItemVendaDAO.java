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
 * Esta classe não contém regra de negócio.
 * Sua responsabilidade é apenas inserir, consultar ou atualizar dados
 * relacionados à tabela ItemVenda.
 */
public class ItemVendaDAO {

    /**
     * Insere um item de venda no banco de dados.
     *
     * Este método persiste apenas um item individual.
     * A lógica de salvar uma venda completa com vários itens será tratada
     * posteriormente pela camada Service, com controle transacional.
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
     * Este método foi preparado para participar da mesma transação
     * da finalização da venda.
     *
     * Importante:
     * - não abre nova conexão;
     * - não faz commit;
     * - não faz rollback;
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