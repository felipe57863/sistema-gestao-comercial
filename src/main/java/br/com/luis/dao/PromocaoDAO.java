package br.com.luis.dao;

import br.com.luis.model.Produto;
import br.com.luis.model.Promocao;
import br.com.luis.model.Promocao.TipoDesconto;
import br.com.luis.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DAO da entidade Promocao.
 * Responsável pela persistência e consulta de promoções no SQLite.
 */
public class PromocaoDAO {

    /**
     * Insere uma nova promoção no banco.
     * Recebe a Connection para participar de uma transação controlada pelo Service.
     */
    public void cadastrar(Connection conn, Promocao promocao) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        String sql = """
            INSERT INTO Promocao (tipo_desconto, valor_desconto, ativa, produto_id)
            VALUES (?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Validação defensiva
            if (promocao.getProduto() == null || promocao.getProduto().getIdProduto() == null) {
                throw new IllegalArgumentException("Produto inválido para cadastro da promoção.");
            }

            stmt.setString(1, promocao.getTipoDesconto().name());
            stmt.setBigDecimal(2, promocao.getValorDesconto());
            stmt.setInt(3, promocao.isAtiva() ? 1 : 0);
            stmt.setInt(4, promocao.getProduto().getIdProduto());

            stmt.executeUpdate();

            // Sincroniza ID gerado
            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    promocao.setIdPromocao(rs.getInt(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao cadastrar promoção no banco de dados.", e);
        }
    }

    /**
     * Inativa todas as promoções ativas de um produto.
     * Participa da mesma transação do cadastro.
     */
    public void inativarPromocoesAnteriores(Connection conn, Integer idProduto) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (idProduto == null || idProduto <= 0) {
            throw new IllegalArgumentException("ID do produto inválido.");
        }

        String sql = """
            UPDATE Promocao
            SET ativa = 0
            WHERE produto_id = ? AND ativa = 1
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idProduto);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inativar promoções anteriores.", e);
        }
    }

    /**
     * Busca a promoção ativa de um produto.
     * Como é apenas uma consulta, o próprio DAO pode abrir e fechar a conexão.
     */
    public Promocao buscarPromocaoAtivaPorProduto(Produto produto) {

        if (produto == null || produto.getIdProduto() == null) {
            throw new IllegalArgumentException("Produto inválido para busca de promoção.");
        }

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

                    // Reutiliza o produto da memória
                    promocao.setProduto(produto);

                    return promocao;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar promoção ativa do produto.", e);
        }

        return null;
    }
}