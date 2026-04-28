package br.com.luis.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Classe responsável por inicializar e construir as tabelas do SQLite.
 */
public class DatabaseBuilder {

    public static void buildTables() {

        String sqlUsuario = """
                CREATE TABLE IF NOT EXISTS Usuario (
                    id_usuario INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome TEXT NOT NULL,
                    login TEXT NOT NULL UNIQUE,
                    senha TEXT NOT NULL,
                    perfil TEXT NOT NULL,
                    status TEXT NOT NULL
                );
                """;

        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sqlUsuario);

            System.out.println("[LOG] Tabela 'Usuario' verificada/criada com sucesso.");

        } catch (SQLException e) {
            System.err.println("[ERRO CRÍTICO] Falha ao construir a estrutura do banco.");
            throw new RuntimeException("Erro ao criar a tabela Usuario", e);
        }
    }
}
