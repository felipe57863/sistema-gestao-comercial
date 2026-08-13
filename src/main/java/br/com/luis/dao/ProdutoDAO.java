package br.com.luis.dao;

import br.com.luis.model.Produto;
import br.com.luis.util.ConnectionFactory;
import br.com.luis.viewmodel.FiltroRelatorioEstoqueProduto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
     * Lista os dados persistidos de produtos necessários ao relatório de estoque.
     *
     * A consulta aplica somente os filtros que pertencem diretamente às colunas
     * persistidas: descrição e status cadastral. A classificação da situação de
     * estoque, seu filtro e a ordenação gerencial final pertencem ao Service.
     *
     * Usa a Connection recebida externamente e encerra somente o
     * PreparedStatement e o ResultSet criados. Não executa commit, rollback nem
     * fecha a Connection informada.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param filtro fotografia imutável dos filtros aplicados ao relatório.
     * @return produtos encontrados; lista vazia quando não houver registros.
     * @throws IllegalArgumentException quando a conexão ou o filtro for nulo.
     * @throws IllegalStateException quando um registro persistido for inválido.
     * @throws RuntimeException quando ocorrer erro de acesso ao banco de dados.
     */
    public List<Produto> listarParaRelatorioEstoque(
            Connection conn,
            FiltroRelatorioEstoqueProduto filtro
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (filtro == null) {
            throw new IllegalArgumentException(
                    "Filtro do relatório de estoque não pode ser nulo."
            );
        }

        filtro.validar();

        StringBuilder sql = new StringBuilder("""
                SELECT produto.id_produto,
                       produto.descricao,
                       produto.preco,
                       produto.quantidade_estoque,
                       produto.estoque_minimo,
                       produto.ativo
                FROM Produto produto
                WHERE 1 = 1
                """);

        if (filtro.getDescricao() != null) {
            sql.append("  AND LOWER(produto.descricao) LIKE ?\n");
        }

        if (filtro.getAtivo() != null) {
            sql.append("  AND produto.ativo = ?\n");
        }

        sql.append("""
                ORDER BY produto.descricao COLLATE NOCASE ASC,
                         produto.id_produto ASC
                """);

        List<Produto> produtos = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int indiceParametro = 1;

            if (filtro.getDescricao() != null) {
                stmt.setString(
                        indiceParametro++,
                        "%" + filtro.getDescricao().toLowerCase(Locale.ROOT) + "%"
                );
            }

            if (filtro.getAtivo() != null) {
                stmt.setInt(
                        indiceParametro,
                        Boolean.TRUE.equals(filtro.getAtivo()) ? 1 : 0
                );
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Integer produtoId = null;

                    try {
                        produtoId = rs.getInt("id_produto");
                        produtos.add(mapearProduto(rs));

                    } catch (RuntimeException e) {
                        String contextoIdentificacao =
                                produtoId != null && produtoId > 0
                                        ? " de ID " + produtoId
                                        : "";

                        throw new IllegalStateException(
                                "Dados persistidos inválidos ao mapear produto"
                                        + contextoIdentificacao
                                        + " para o relatório de estoque.",
                                e
                        );
                    }
                }
            }

            return produtos;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao listar produtos para o relatório de estoque.",
                    e
            );
        }
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
     * Busca um produto pelo ID usando uma Connection externa.
     *
     * Participa da transação de finalização da venda para permitir a consulta
     * consistente do produto pela camada Service.
     *
     * Importante:
     * - não abre nova Connection;
     * - não executa commit;
     * - não executa rollback;
     * - não fecha a Connection recebida.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param idProduto ID do produto.
     * @return produto encontrado ou null caso não exista.
     */
    public Produto buscarPorId(Connection conn, Integer idProduto) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (idProduto == null || idProduto <= 0) {
            throw new IllegalArgumentException("ID do produto inválido.");
        }

        String sql = """
            SELECT id_produto, descricao, preco, quantidade_estoque, estoque_minimo, ativo
            FROM Produto
            WHERE id_produto = ?
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idProduto);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearProduto(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar produto por ID usando conexão externa.", e);
        }

        return null;
    }

    /**
     * Baixa estoque de um produto usando uma Connection externa.
     *
     * Participa da transação de finalização coordenada pelo VendaService.
     *
     * A baixa é feita de forma segura:
     * - somente produto ativo pode ter estoque baixado;
     * - a quantidade em estoque precisa ser suficiente;
     * - o UPDATE só ocorre se a condição de estoque for satisfeita.
     *
     * Importante:
     * - não abre nova Connection;
     * - não executa commit;
     * - não executa rollback;
     * - não fecha a Connection recebida.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param idProduto ID do produto.
     * @param quantidade quantidade que será baixada do estoque.
     */
    public void baixarEstoque(Connection conn, Integer idProduto, Integer quantidade) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (idProduto == null || idProduto <= 0) {
            throw new IllegalArgumentException("ID do produto inválido para baixa de estoque.");
        }

        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade inválida para baixa de estoque.");
        }

        String sql = """
            UPDATE Produto
            SET quantidade_estoque = quantidade_estoque - ?
            WHERE id_produto = ?
              AND ativo = 1
              AND quantidade_estoque >= ?
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, quantidade);
            stmt.setInt(2, idProduto);
            stmt.setInt(3, quantidade);

            int linhasAfetadas = stmt.executeUpdate();

            if (linhasAfetadas == 0) {
                throw new RuntimeException(
                        "Não foi possível baixar o estoque. Produto inexistente, inativo ou com estoque insuficiente."
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao baixar estoque usando conexão externa.", e);
        }
    }

    /**
     * Restaura a quantidade de um produto no estoque usando uma Connection externa.
     *
     * Participa da transação de estorno coordenada pelo EstornoVendaService.
     * O produto pode estar ativo ou inativo, pois o estorno deve devolver
     * integralmente ao estoque a quantidade registrada na venda.
     *
     * Importante:
     * - não abre nova Connection;
     * - não executa commit;
     * - não executa rollback;
     * - não fecha a Connection recebida;
     * - não altera o estado ativo ou inativo do produto.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param idProduto identificador do produto.
     * @param quantidade quantidade que será devolvida ao estoque.
     */
    public void restaurarEstoque(
            Connection conn,
            Integer idProduto,
            Integer quantidade
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (idProduto == null || idProduto <= 0) {
            throw new IllegalArgumentException(
                    "ID do produto inválido para restauração de estoque."
            );
        }

        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException(
                    "Quantidade inválida para restauração de estoque."
            );
        }

        String sql = """
            UPDATE Produto
            SET quantidade_estoque = quantidade_estoque + ?
            WHERE id_produto = ?
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, quantidade);
            stmt.setInt(2, idProduto);

            int linhasAfetadas = stmt.executeUpdate();

            if (linhasAfetadas == 0) {
                throw new IllegalStateException(
                        "Não foi possível restaurar o estoque. Produto de ID "
                                + idProduto + " não encontrado."
                );
            }

            if (linhasAfetadas > 1) {
                throw new IllegalStateException(
                        "Mais de um produto foi atualizado para o ID "
                                + idProduto + "."
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao restaurar estoque usando conexão externa.",
                    e
            );
        }
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