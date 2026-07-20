package br.com.luis.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Classe responsável por inicializar e construir as tabelas do SQLite.
 *
 * Esta classe executa os scripts SQL armazenados em:
 * src/main/resources/database
 *
 * O objetivo é manter o DDL fora do Java e centralizar aqui apenas
 * a execução dos scripts na ordem correta.
 */
public class DatabaseBuilder {

    private static final List<String> SCRIPTS = List.of(
            "database/script_usuario.sql",
            "database/script_prazo_pagamento.sql",
            "database/script_cliente.sql",
            "database/script_produto.sql",
            "database/script_promocao.sql",
            "database/script_venda.sql",
            "database/script_item_venda.sql",
            "database/script_conta_receber.sql",
            "database/script_movimentacao_financeira.sql",
            "database/script_auditoria_estorno_venda.sql"
    );

    /**
     * Executa todos os scripts necessários para criar/verificar
     * as tabelas básicas do sistema.
     */
    public static void buildTables() {

        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement()) {

            for (String script : SCRIPTS) {
                executarScript(stmt, script);
            }

            System.out.println("[LOG] Estrutura do banco verificada/criada com sucesso.");

        } catch (SQLException e) {
            System.err.println("[ERRO CRÍTICO] Falha ao construir a estrutura do banco.");
            throw new RuntimeException("Erro ao criar/verificar as tabelas do banco.", e);
        }
    }

    /**
     * Lê um arquivo SQL da pasta resources e executa seu conteúdo.
     */
    private static void executarScript(Statement stmt, String caminhoScript) throws SQLException {

        String sql = carregarScript(caminhoScript);

        if (sql.isBlank()) {
            throw new RuntimeException("Script SQL vazio: " + caminhoScript);
        }

        stmt.execute(sql);

        System.out.println("[LOG] Script executado: " + caminhoScript);
    }

    /**
     * Carrega um script SQL a partir do classpath.
     */
    private static String carregarScript(String caminhoScript) {

        try (InputStream inputStream = DatabaseBuilder.class
                .getClassLoader()
                .getResourceAsStream(caminhoScript)) {

            if (inputStream == null) {
                throw new RuntimeException("Script SQL não encontrado: " + caminhoScript);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler script SQL: " + caminhoScript, e);
        }
    }
}