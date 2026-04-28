package br.com.luis.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Fábrica de Conexões para o SQLite.
 * Centraliza o acesso ao banco de dados do sistema.
 */
public class ConnectionFactory {

    private static final String URL = "jdbc:sqlite:erp_tcc.db";

    /**
     * Retorna uma conexão ativa com o banco SQLite.
     */
    public static Connection getConnection() {
        try {
            Class.forName("org.sqlite.JDBC");

            Connection conn = DriverManager.getConnection(URL);

            // ATIVA FOREIGN KEYS (OBRIGATÓRIO NO SQLITE)
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }

            return conn;

        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver SQLite não encontrado.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao conectar com o banco SQLite.", e);
        }
    }
}