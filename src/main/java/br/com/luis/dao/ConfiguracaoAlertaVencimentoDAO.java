package br.com.luis.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO responsável pela persistência da configuração global dos alertas
 * automáticos de vencimento.
 *
 * A tabela possui uma única linha, identificada por id_configuracao = 1.
 * O DAO apenas consulta e persiste o valor; autorização e regras de negócio
 * pertencem ao Service.
 */
public class ConfiguracaoAlertaVencimentoDAO {

    private static final int ID_CONFIGURACAO = 1;

    /**
     * Busca a quantidade global de dias de antecedência.
     *
     * @param conn conexão externa controlada pelo Service.
     * @return quantidade persistida de dias de antecedência.
     */
    public int buscarDiasAntecedencia(Connection conn) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        String sql = """
                SELECT dias_antecedencia
                FROM ConfiguracaoAlertaVencimento
                WHERE id_configuracao = ?
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ID_CONFIGURACAO);

            try (ResultSet rs = stmt.executeQuery()) {

                if (!rs.next()) {
                    throw new IllegalStateException(
                            "Configuração dos alertas de vencimento não foi encontrada."
                    );
                }

                int diasAntecedencia = rs.getInt("dias_antecedencia");

                if (rs.wasNull()) {
                    throw new IllegalStateException(
                            "Dias de antecedência dos alertas não podem ser nulos."
                    );
                }

                if (rs.next()) {
                    throw new IllegalStateException(
                            "Mais de uma configuração de alerta de vencimento foi encontrada."
                    );
                }

                return diasAntecedencia;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erro ao consultar a configuração dos alertas de vencimento.",
                    e
            );
        }
    }

    /**
     * Atualiza a quantidade global de dias de antecedência.
     *
     * @param conn conexão externa controlada pelo Service.
     * @param diasAntecedencia novo valor informado pelo administrador.
     * @return quantidade de registros atualizados.
     */
    public int atualizarDiasAntecedencia(
            Connection conn,
            Integer diasAntecedencia
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (diasAntecedencia == null) {
            throw new IllegalArgumentException(
                    "Dias de antecedência são obrigatórios."
            );
        }

        String sql = """
                UPDATE ConfiguracaoAlertaVencimento
                SET dias_antecedencia = ?
                WHERE id_configuracao = ?
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, diasAntecedencia);
            stmt.setInt(2, ID_CONFIGURACAO);

            return stmt.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erro ao atualizar a configuração dos alertas de vencimento.",
                    e
            );
        }
    }
}