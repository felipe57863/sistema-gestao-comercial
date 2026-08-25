package br.com.luis.dao;

import br.com.luis.model.EntradaEstoque;
import br.com.luis.viewmodel.EntradaEstoqueRelatorioView;
import br.com.luis.viewmodel.FiltroRelatorioEntradaEstoque;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Persiste entradas de estoque usando uma Connection externa.
 */
public class EntradaEstoqueDAO {

    /**
     * Insere o cabeçalho da entrada na transação controlada pela camada Service.
     * Encerra apenas o PreparedStatement e o ResultSet que cria.
     */
    public int inserir(Connection conn, EntradaEstoque entradaEstoque) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (entradaEstoque == null) {
            throw new IllegalArgumentException("Entrada de estoque não pode ser nula.");
        }

        if (entradaEstoque.getDataHora() == null) {
            throw new IllegalArgumentException("Data e hora da entrada não podem ser nulas.");
        }

        if (entradaEstoque.getUsuarioId() == null || entradaEstoque.getUsuarioId() <= 0) {
            throw new IllegalArgumentException("ID do usuário deve ser maior que zero.");
        }

        if (entradaEstoque.getNomeUsuario() == null
                || entradaEstoque.getNomeUsuario().isBlank()) {
            throw new IllegalArgumentException("Nome do usuário não pode ser vazio.");
        }

        String sql = """
                INSERT INTO EntradaEstoque (
                    data_hora,
                    usuario_id,
                    nome_usuario,
                    referencia,
                    observacao
                ) VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt =
                     conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, entradaEstoque.getDataHora().toString());
            stmt.setInt(2, entradaEstoque.getUsuarioId());
            stmt.setString(3, entradaEstoque.getNomeUsuario());

            if (entradaEstoque.getReferencia() != null) {
                stmt.setString(4, entradaEstoque.getReferencia());
            } else {
                stmt.setNull(4, Types.VARCHAR);
            }

            if (entradaEstoque.getObservacao() != null) {
                stmt.setString(5, entradaEstoque.getObservacao());
            } else {
                stmt.setNull(5, Types.VARCHAR);
            }

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
                    "Entrada de estoque inserida, mas o ID gerado não foi retornado."
            );

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao inserir entrada de estoque usando conexão externa.",
                    e
            );
        }
    }

    /**
     * Lista entradas completas para o relatório usando a Connection externa.
     *
     * O filtro de produto usa EXISTS apenas para selecionar cabeçalhos. O JOIN
     * agregado permanece com todos os itens da entrada.
     */
    public List<EntradaEstoqueRelatorioView> listarParaRelatorio(
            Connection conn,
            LocalDateTime inicioInclusivo,
            LocalDateTime fimExclusivo,
            FiltroRelatorioEntradaEstoque filtro
    ) {
        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }
        if (inicioInclusivo == null || fimExclusivo == null) {
            throw new IllegalArgumentException("Limites do período são obrigatórios.");
        }
        if (!fimExclusivo.isAfter(inicioInclusivo)) {
            throw new IllegalArgumentException(
                    "O limite final do período deve ser posterior ao limite inicial."
            );
        }
        if (filtro == null) {
            throw new IllegalArgumentException("Filtro do relatório não pode ser nulo.");
        }
        filtro.validar();

        StringBuilder sql = new StringBuilder(
                """
                SELECT entrada.id_entrada,
                       entrada.data_hora,
                       entrada.usuario_id,
                       entrada.nome_usuario,
                       entrada.referencia,
                       entrada.observacao,
                       COUNT(DISTINCT item.produto_id) AS quantidade_produtos,
                       SUM(item.quantidade_recebida) AS total_unidades,
                       SUM(item.subtotal) AS valor_total_entrada
                FROM EntradaEstoque entrada
                JOIN ItemEntradaEstoque item
                  ON item.entrada_id = entrada.id_entrada
                WHERE entrada.data_hora >= ?
                  AND entrada.data_hora < ?
                """
        );

        if (filtro.getEntradaId() != null) {
            sql.append("  AND entrada.id_entrada = ?\n");
        }
        if (filtro.getUsuarioId() != null) {
            sql.append("  AND entrada.usuario_id = ?\n");
        }
        if (filtro.getReferencia() != null) {
            sql.append(
                    "  AND LOWER(COALESCE(entrada.referencia, '')) LIKE ?\n"
            );
        }
        if (filtro.getProdutoId() != null) {
            sql.append(
                    """
                      AND EXISTS (
                          SELECT 1
                          FROM ItemEntradaEstoque item_filtro
                          WHERE item_filtro.entrada_id = entrada.id_entrada
                            AND item_filtro.produto_id = ?
                      )
                    """
            );
        }

        sql.append(
                """
                GROUP BY entrada.id_entrada,
                         entrada.data_hora,
                         entrada.usuario_id,
                         entrada.nome_usuario,
                         entrada.referencia,
                         entrada.observacao
                ORDER BY entrada.data_hora DESC,
                         entrada.id_entrada DESC
                """
        );

        List<EntradaEstoqueRelatorioView> entradas = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int indice = 1;
            stmt.setString(indice++, inicioInclusivo.toString());
            stmt.setString(indice++, fimExclusivo.toString());

            if (filtro.getEntradaId() != null) {
                stmt.setInt(indice++, filtro.getEntradaId());
            }
            if (filtro.getUsuarioId() != null) {
                stmt.setInt(indice++, filtro.getUsuarioId());
            }
            if (filtro.getReferencia() != null) {
                stmt.setString(
                        indice++,
                        "%" + filtro.getReferencia().toLowerCase(Locale.ROOT) + "%"
                );
            }
            if (filtro.getProdutoId() != null) {
                stmt.setInt(indice, filtro.getProdutoId());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Integer entradaId = null;

                    try {
                        entradaId = rs.getInt("id_entrada");

                        entradas.add(
                                new EntradaEstoqueRelatorioView(
                                        entradaId,
                                        LocalDateTime.parse(
                                                rs.getString("data_hora")
                                        ),
                                        rs.getInt("usuario_id"),
                                        rs.getString("nome_usuario"),
                                        rs.getString("referencia"),
                                        rs.getString("observacao"),
                                        rs.getInt("quantidade_produtos"),
                                        rs.getInt("total_unidades"),
                                        rs.getBigDecimal("valor_total_entrada")
                                )
                        );

                    } catch (RuntimeException e) {
                        throw new IllegalStateException(
                                "Dados inválidos na entrada de estoque"
                                        + (entradaId == null
                                        ? "."
                                        : " de ID " + entradaId + "."),
                                e
                        );
                    }
                }
            }

            return entradas;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao consultar o relatório de entradas de estoque.",
                    e
            );
        }
    }
}
