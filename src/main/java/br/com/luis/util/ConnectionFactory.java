package br.com.luis.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Fábrica de Conexões para o SQLite.
 * Centraliza o acesso ao banco de dados do sistema.
 */
public class ConnectionFactory {

    private static final String DB_FILE = "database.db";
    private static final String URL = "jdbc:sqlite:" + DB_FILE;

    private static boolean caminhoBancoExibido = false;

    /**
     * Retorna uma conexão ativa com o banco SQLite.
     */
    public static Connection getConnection() {
        try {
            Class.forName("org.sqlite.JDBC");

            exibirCaminhoBancoUmaVez();

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

    /**
     * Exibe uma única vez o caminho absoluto do banco usado pela aplicação.
     * Útil para evitar confusão com bancos SQLite criados em diretórios diferentes.
     */
    private static void exibirCaminhoBancoUmaVez() {
        if (!caminhoBancoExibido) {
            File arquivoBanco = new File(DB_FILE);
            System.out.println("[INFO] Banco SQLite: " + arquivoBanco.getAbsolutePath());
            caminhoBancoExibido = true;
        }
    }
}