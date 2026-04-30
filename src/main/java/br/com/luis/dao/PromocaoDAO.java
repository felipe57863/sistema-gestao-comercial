package br.com.luis.dao;

import br.com.luis.model.Produto;
import br.com.luis.model.Promocao;
import br.com.luis.model.Promocao.TipoDesconto;
import br.com.luis.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * DAO da entidade Promocao.
 * Responsável pela persistência e consulta de promoções no SQLite.
 */
public class PromocaoDAO {

    /**
     * Insere uma nova promoção no banco e recupera o ID gerado.
     */
    public void cadastrar(Promocao promocao) {

        String sql = """
            INSERT INTO Promocao (tipo_desconto, valor_desconto, ativa, produto_id)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            // Validação defensiva da FK
            if (promocao.getProduto() == null || promocao.getProduto().getIdProduto() == null) {
                throw new IllegalArgumentException("Produto inválido para promoção.");
            }

            // Enum → TEXT
            stmt.setString(1, promocao.getTipoDesconto().name());

            // BigDecimal → REAL
            stmt.setBigDecimal(2, promocao.getValorDesconto());

            // boolean → INTEGER
            stmt.setInt(3, promocao.isAtiva() ? 1 : 0);

            // FK
            stmt.setInt(4, promocao.getProduto().getIdProduto());

            stmt.executeUpdate();

            // Recupera ID gerado
            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    promocao.setIdPromocao(rs.getInt(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao cadastrar promoção.", e);
        }
    }

    /**
     * RN22: Inativa todas as promoções ativas de um produto.
     */
    public void inativarPromocoesAnteriores(Integer idProduto) {

        String sql = """
            UPDATE Promocao 
            SET ativa = 0 
            WHERE produto_id = ? AND ativa = 1
        """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idProduto);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inativar promoções anteriores.", e);
        }
    }

    /**
     * RN02: Busca a promoção ativa de um produto.
     * Retorna apenas UMA (LIMIT 1 como proteção).
     */
    public Promocao buscarPromocaoAtivaPorProduto(Produto produto) {

        String sql = """
            SELECT id_promocao, tipo_desconto, valor_desconto, ativa 
            FROM Promocao 
            WHERE produto_id = ? AND ativa = 1
            LIMIT 1
        """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, produto.getIdProduto());

            try (var rs = stmt.executeQuery()) {

                if (rs.next()) {

                    Promocao promocao = new Promocao();

                    promocao.setIdPromocao(rs.getInt("id_promocao"));

                    // TEXT → Enum
                    promocao.setTipoDesconto(
                            TipoDesconto.valueOf(rs.getString("tipo_desconto"))
                    );

                    promocao.setValorDesconto(rs.getBigDecimal("valor_desconto"));

                    // INTEGER → boolean
                    promocao.setAtiva(rs.getInt("ativa") == 1);

                    // Reutiliza objeto produto já existente
                    promocao.setProduto(produto);

                    return promocao;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar promoção ativa.", e);
        }

        return null;
    }
}