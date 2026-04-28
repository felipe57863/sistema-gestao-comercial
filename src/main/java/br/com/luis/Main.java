package br.com.luis;

import br.com.luis.util.ConnectionFactory;
import br.com.luis.util.DatabaseInitializer;

import java.sql.Connection;

public class Main {

    public static void main(String[] args) {

        // Cria/verifica tabelas
        DatabaseInitializer.criarTabelas();

        // Teste de conexão
        try (Connection conn = ConnectionFactory.getConnection()) {

            if (conn != null) {
                System.out.println("Conexão com SQLite realizada com sucesso!");
            }

        } catch (Exception e) {
            System.err.println("Erro ao testar conexão: " + e.getMessage());
        }
    }
}