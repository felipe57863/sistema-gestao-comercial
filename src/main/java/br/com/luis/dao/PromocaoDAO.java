package br.com.luis.dao;

import br.com.luis.model.Produto;
import br.com.luis.model.Promocao;
import br.com.luis.model.Promocao.TipoDesconto;
import br.com.luis.util.ConnectionFactory;
import br.com.luis.viewmodel.FiltroRelatorioPromocaoProduto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    /**
     * Lista as promoções ativas destinadas ao relatório de produtos.
     *
     * Uma única consulta com INNER JOIN recupera cada promoção e o respectivo
     * produto, evitando consultas adicionais por linha. Promoções distintas do
     * mesmo produto permanecem separadas e ordenadas pelo ID da promoção.
     *
     * Usa a Connection recebida externamente e encerra somente o
     * PreparedStatement e o ResultSet criados. Não executa commit, rollback nem
     * fecha a Connection informada.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param filtro fotografia imutável dos filtros aplicados ao relatório.
     * @return promoções ativas encontradas; lista vazia quando não houver registros.
     * @throws IllegalArgumentException quando a conexão ou o filtro for nulo.
     * @throws IllegalStateException quando um registro persistido for inválido.
     * @throws RuntimeException quando ocorrer erro de acesso ao banco de dados.
     */
    public List<Promocao> listarParaRelatorioProdutos(
            Connection conn,
            FiltroRelatorioPromocaoProduto filtro
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (filtro == null) {
            throw new IllegalArgumentException(
                    "Filtro do relatório de promoções não pode ser nulo."
            );
        }

        filtro.validar();

        StringBuilder sql = new StringBuilder("""
                SELECT promocao.id_promocao AS promocao_id,
                       promocao.tipo_desconto,
                       promocao.valor_desconto,
                       promocao.ativa AS promocao_ativa,
                       produto.id_produto AS produto_id,
                       produto.descricao AS produto_descricao,
                       produto.preco AS produto_preco,
                       produto.quantidade_estoque AS produto_quantidade_estoque,
                       produto.estoque_minimo AS produto_estoque_minimo,
                       produto.ativo AS produto_ativo
                FROM Promocao promocao
                INNER JOIN Produto produto
                        ON produto.id_produto = promocao.produto_id
                WHERE promocao.ativa = 1
                """);

        if (filtro.getDescricao() != null) {
            sql.append("  AND LOWER(produto.descricao) LIKE ?\n");
        }

        if (filtro.getProdutoAtivo() != null) {
            sql.append("  AND produto.ativo = ?\n");
        }

        if (filtro.getTipoDesconto() != null) {
            sql.append("  AND promocao.tipo_desconto = ?\n");
        }

        sql.append("""
                ORDER BY produto.descricao COLLATE NOCASE ASC,
                         produto.id_produto ASC,
                         promocao.id_promocao ASC
                """);

        List<Promocao> promocoes = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int indiceParametro = 1;

            if (filtro.getDescricao() != null) {
                stmt.setString(
                        indiceParametro++,
                        "%" + filtro.getDescricao().toLowerCase(Locale.ROOT) + "%"
                );
            }

            if (filtro.getProdutoAtivo() != null) {
                stmt.setInt(
                        indiceParametro++,
                        Boolean.TRUE.equals(filtro.getProdutoAtivo()) ? 1 : 0
                );
            }

            if (filtro.getTipoDesconto() != null) {
                stmt.setString(
                        indiceParametro,
                        filtro.getTipoDesconto().name()
                );
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Integer promocaoId = null;

                    try {
                        promocaoId = rs.getInt("promocao_id");
                        promocoes.add(mapearPromocaoParaRelatorio(rs));

                    } catch (RuntimeException e) {
                        String contextoIdentificacao =
                                promocaoId != null && promocaoId > 0
                                        ? " de ID " + promocaoId
                                        : "";

                        throw new IllegalStateException(
                                "Dados persistidos inválidos ao mapear promoção"
                                        + contextoIdentificacao
                                        + " para o relatório de produtos.",
                                e
                        );
                    }
                }
            }

            return promocoes;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao listar promoções ativas para o relatório de produtos.",
                    e
            );
        }
    }

    /**
     * Reconstrói a promoção e o produto obtidos pela consulta JOIN do relatório.
     */
    private Promocao mapearPromocaoParaRelatorio(ResultSet rs) throws SQLException {
        Produto produto = new Produto(
                rs.getInt("produto_id"),
                rs.getString("produto_descricao"),
                rs.getBigDecimal("produto_preco"),
                rs.getInt("produto_quantidade_estoque"),
                rs.getInt("produto_estoque_minimo"),
                rs.getInt("produto_ativo") == 1
        );

        return new Promocao(
                rs.getInt("promocao_id"),
                TipoDesconto.valueOf(rs.getString("tipo_desconto")),
                rs.getBigDecimal("valor_desconto"),
                rs.getInt("promocao_ativa") == 1,
                produto
        );
    }
}