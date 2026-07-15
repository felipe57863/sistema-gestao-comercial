package br.com.luis.dao;

import br.com.luis.model.PrazoPagamento;
import br.com.luis.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO da entidade PrazoPagamento.
 * Responsável pelas operações de persistência no SQLite.
 */
public class PrazoPagamentoDAO {

    /**
     * Insere um novo prazo de pagamento no banco.
     */
    public void cadastrar(PrazoPagamento prazo) {

        String sql = "INSERT INTO PrazoPagamento (descricao, quantidade_dias, ativo) VALUES (?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, prazo.getDescricao());
            stmt.setInt(2, prazo.getQuantidadeDias());

            // SQLite não possui tipo boolean → usamos 1 (true) ou 0 (false)
            stmt.setInt(3, prazo.isAtivo() ? 1 : 0);

            stmt.executeUpdate();

            // Recupera o ID gerado automaticamente pelo banco
            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    prazo.setIdPrazo(rs.getInt(1));
                }
            }

            // Log de auditoria
            System.out.println("[LOG] Prazo cadastrado: " + prazo.getDescricao());

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao cadastrar prazo de pagamento.", e);
        }
    }

    /**
     * Atualiza os dados de um prazo de pagamento existente.
     */
    public void atualizar(PrazoPagamento prazo) {

        String sql = """
            UPDATE PrazoPagamento
            SET descricao = ?,
                quantidade_dias = ?,
                ativo = ?
            WHERE id_prazo = ?
        """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, prazo.getDescricao());
            stmt.setInt(2, prazo.getQuantidadeDias());
            stmt.setInt(3, prazo.isAtivo() ? 1 : 0);
            stmt.setInt(4, prazo.getIdPrazo());

            stmt.executeUpdate();

            // Log de auditoria
            System.out.println("[LOG] Prazo atualizado: " + prazo.getDescricao());

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar prazo de pagamento.", e);
        }
    }

    /**
     * Inativa um prazo de pagamento.
     * Não remove fisicamente o registro do banco.
     */
    public void inativar(Integer idPrazo) {

        String sql = """
            UPDATE PrazoPagamento
            SET ativo = 0
            WHERE id_prazo = ?
        """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idPrazo);
            stmt.executeUpdate();

            // Log de auditoria
            System.out.println("[LOG] Prazo inativado. ID: " + idPrazo);

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inativar prazo de pagamento.", e);
        }
    }

    /**
     * Lista TODOS os prazos cadastrados (ativos e inativos).
     */
    public List<PrazoPagamento> listarTodos() {

        String sql = """
            SELECT id_prazo, descricao, quantidade_dias, ativo
            FROM PrazoPagamento
            ORDER BY quantidade_dias
        """;

        List<PrazoPagamento> prazos = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {

            while (rs.next()) {

                PrazoPagamento prazo = new PrazoPagamento(
                        rs.getInt("id_prazo"),
                        rs.getString("descricao"),
                        rs.getInt("quantidade_dias"),
                        // ⚠️ Converte INTEGER (SQLite) → boolean (Java)
                        rs.getInt("ativo") == 1
                );

                prazos.add(prazo);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar prazos.", e);
        }

        return prazos;
    }

    /**
     * Lista apenas os prazos ativos.
     * Ideal para uso em ComboBox e seleção de cliente.
     */
    public List<PrazoPagamento> listarAtivos() {

        String sql = """
            SELECT id_prazo, descricao, quantidade_dias, ativo
            FROM PrazoPagamento
            WHERE ativo = 1
            ORDER BY quantidade_dias
        """;

        List<PrazoPagamento> prazos = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {

            while (rs.next()) {

                PrazoPagamento prazo = new PrazoPagamento(
                        rs.getInt("id_prazo"),
                        rs.getString("descricao"),
                        rs.getInt("quantidade_dias"),
                        rs.getInt("ativo") == 1
                );

                prazos.add(prazo);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar prazos ativos.", e);
        }

        return prazos;
    }

    /**
     * Busca um prazo de pagamento pelo ID usando uma Connection externa.
     *
     * Participa da transação de finalização da venda a prazo coordenada pelo
     * VendaService.
     *
     * Importante:
     * - não abre nova Connection;
     * - não executa commit;
     * - não executa rollback;
     * - não fecha a Connection recebida.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param idPrazo ID do prazo de pagamento.
     * @return prazo encontrado ou null caso não exista.
     */
    public PrazoPagamento buscarPorId(Connection conn, Integer idPrazo) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (idPrazo == null || idPrazo <= 0) {
            throw new IllegalArgumentException("ID do prazo de pagamento inválido.");
        }

        String sql = """
            SELECT id_prazo, descricao, quantidade_dias, ativo
            FROM PrazoPagamento
            WHERE id_prazo = ?
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idPrazo);

            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new PrazoPagamento(
                            rs.getInt("id_prazo"),
                            rs.getString("descricao"),
                            rs.getInt("quantidade_dias"),
                            rs.getInt("ativo") == 1
                    );
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar prazo de pagamento por ID usando conexão externa.", e);
        }

        return null;
    }
}