package br.com.luis.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Fábrica de conexões JDBC para o banco SQLite local da aplicação.
 *
 * O arquivo database.db é localizado de forma relativa ao diretório de execução.
 * Cada chamada cria uma nova Connection e ativa as chaves estrangeiras com
 * {@code PRAGMA foreign_keys = ON}. A classe não mantém pool de conexões.
 */
public class ConnectionFactory {

    private static final String DB_FILE = "database.db";
    private static final String URL = "jdbc:sqlite:" + DB_FILE;

    private static boolean caminhoBancoExibido = false;

    /**
     * Cria e retorna uma nova conexão ativa com o banco SQLite local.
     *
     * A conexão é devolvida com as chaves estrangeiras habilitadas. O chamador é
     * responsável pelo fechamento, preferencialmente com try-with-resources, e
     * pode controlar commit, rollback e autoCommit quando delimitar uma transação.
     *
     * @return nova conexão JDBC com o banco database.db.
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