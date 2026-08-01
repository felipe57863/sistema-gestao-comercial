package br.com.luis.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Responsável por criar ou verificar a estrutura de tabelas do banco SQLite.
 *
 * Executa os scripts SQL armazenados em {@code src/main/resources/database} na
 * ordem definida pela lista interna. Essa ordem respeita as dependências entre
 * chaves estrangeiras, garantindo que as tabelas referenciadas sejam preparadas
 * antes das tabelas dependentes.
 *
 * Os scripts de criação usam criação condicional para preservar estruturas já
 * existentes. Depois dessa etapa, migrações incrementais versionadas evoluem os
 * bancos anteriores sob controle transacional. O DDL permanece nos recursos SQL;
 * esta classe centraliza o carregamento, a ordem e o controle da versão suportada.
 */
public class DatabaseBuilder {

    private static final int VERSAO_BANCO_SUPORTADA = 1;

    private static final String COLUNA_TROCA_SENHA_OBRIGATORIA =
            "troca_senha_obrigatoria";

    private static final String MIGRACAO_VERSAO_1 =
            "database/migrations/001_adicionar_troca_senha_obrigatoria.sql";

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
     * Executa os scripts necessários para criar ou verificar as tabelas do sistema.
     *
     * A sequência da lista é obrigatória por causa das chaves estrangeiras. Cada
     * script é carregado do classpath e executado na mesma ordem em que foi
     * declarado, permitindo que instruções de criação condicional mantenham as
     * tabelas existentes.
     */
    public static void buildTables() {

        try (Connection conn = ConnectionFactory.getConnection()) {

            executarScriptsCriacao(conn);
            executarMigracoes(conn);

            System.out.println("[LOG] Estrutura do banco verificada/criada com sucesso.");

        } catch (SQLException e) {
            System.err.println("[ERRO CRÍTICO] Falha ao construir a estrutura do banco.");
            throw new RuntimeException("Erro ao criar/verificar as tabelas do banco.", e);
        }
    }

    /**
     * Executa os scripts que representam a estrutura completa de um banco novo.
     */
    private static void executarScriptsCriacao(Connection conn) throws SQLException {

        try (Statement stmt = conn.createStatement()) {
            for (String script : SCRIPTS) {
                executarScript(stmt, script);
            }
        }
    }

    /**
     * Aplica as evoluções pendentes e registra a versão somente após o sucesso.
     */
    private static void executarMigracoes(Connection conn) throws SQLException {

        boolean autoCommitOriginal = conn.getAutoCommit();
        Throwable falhaOriginal = null;

        try {
            conn.setAutoCommit(false);

            int versaoAtual = obterVersaoBanco(conn);

            if (versaoAtual < 0 || versaoAtual > VERSAO_BANCO_SUPORTADA) {
                throw new IllegalStateException(
                        "Versão do banco não suportada pela aplicação. "
                                + "Versão encontrada: " + versaoAtual
                                + "; versão suportada: " + VERSAO_BANCO_SUPORTADA + "."
                );
            }

            if (versaoAtual == 0) {
                migrarParaVersao1(conn);
            } else if (versaoAtual == 1) {
                validarColunaTrocaSenhaObrigatoria(conn);
            }

            conn.commit();

        } catch (SQLException | RuntimeException e) {
            falhaOriginal = e;
            fazerRollbackSeguro(conn, e);
            throw e;

        } finally {
            restaurarAutoCommit(conn, autoCommitOriginal, falhaOriginal);
        }
    }

    /**
     * Executa a primeira evolução ou apenas reconhece um banco novo já atualizado.
     */
    private static void migrarParaVersao1(Connection conn) throws SQLException {

        if (!colunaTrocaSenhaObrigatoriaExiste(conn)) {
            try (Statement stmt = conn.createStatement()) {
                executarScript(stmt, MIGRACAO_VERSAO_1);
            }
        }

        validarColunaTrocaSenhaObrigatoria(conn);
        definirVersaoBanco(conn, 1);

        int versaoRegistrada = obterVersaoBanco(conn);

        if (versaoRegistrada != 1) {
            throw new IllegalStateException(
                    "Não foi possível registrar a versão 1 do banco de dados."
            );
        }

        System.out.println("[LOG] Banco atualizado para a versão 1.");
    }

    /**
     * Lê o marcador de versão mantido pelo SQLite.
     */
    private static int obterVersaoBanco(Connection conn) throws SQLException {

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA user_version")) {

            if (!rs.next()) {
                throw new IllegalStateException(
                        "Não foi possível consultar a versão do banco de dados."
                );
            }

            return rs.getInt(1);
        }
    }

    /**
     * Atualiza o marcador somente com uma versão conhecida pela aplicação.
     */
    private static void definirVersaoBanco(Connection conn, int versao) throws SQLException {

        if (versao < 0 || versao > VERSAO_BANCO_SUPORTADA) {
            throw new IllegalArgumentException(
                    "Versão inválida para registro no banco: " + versao + "."
            );
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA user_version = " + versao);
        }
    }

    /**
     * Confirma se a coluna adicionada pela primeira evolução já está disponível.
     */
    private static boolean colunaTrocaSenhaObrigatoriaExiste(Connection conn)
            throws SQLException {

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(Usuario)")) {

            while (rs.next()) {
                if (COLUNA_TROCA_SENHA_OBRIGATORIA.equals(rs.getString("name"))) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Valida em execução a existência, o tipo, a obrigatoriedade, o valor padrão
     * e os dados persistidos da coluna adicionada pela primeira evolução.
     *
     * A presença do CHECK permanece garantida estaticamente pelos scripts SQL.
     * Este método não inspeciona o SQL armazenado em sqlite_master; o comportamento
     * real do CHECK deve ser comprovado posteriormente em teste controlado.
     */
    private static void validarColunaTrocaSenhaObrigatoria(Connection conn)
            throws SQLException {

        boolean colunaEncontrada = false;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(Usuario)")) {

            while (rs.next()) {
                String nomeColuna = rs.getString("name");

                if (!COLUNA_TROCA_SENHA_OBRIGATORIA.equals(nomeColuna)) {
                    continue;
                }

                colunaEncontrada = true;
                validarTipoColuna(rs.getString("type"));
                validarRestricaoNotNull(rs.getInt("notnull"));
                validarValorPadrao(rs.getString("dflt_value"));
                break;
            }
        }

        if (!colunaEncontrada) {
            throw new IllegalStateException(
                    "Estrutura do banco inconsistente: a coluna "
                            + COLUNA_TROCA_SENHA_OBRIGATORIA
                            + " não foi encontrada na tabela Usuario."
            );
        }

        validarDadosTrocaSenhaObrigatoria(conn);
    }

    /**
     * Exige que o tipo declarado seja exatamente INTEGER, ignorando apenas caixa
     * e espaços externos.
     */
    private static void validarTipoColuna(String tipoDeclarado) {

        if (tipoDeclarado == null
                || !"INTEGER".equalsIgnoreCase(tipoDeclarado.trim())) {
            throw new IllegalStateException(
                    "Estrutura do banco inconsistente: a coluna "
                            + COLUNA_TROCA_SENHA_OBRIGATORIA
                            + " deve possuir tipo INTEGER. Tipo encontrado: "
                            + String.valueOf(tipoDeclarado) + "."
            );
        }
    }

    /**
     * Exige a marcação NOT NULL informada pelo PRAGMA table_info.
     */
    private static void validarRestricaoNotNull(int notNull) {

        if (notNull != 1) {
            throw new IllegalStateException(
                    "Estrutura do banco inconsistente: a coluna "
                            + COLUNA_TROCA_SENHA_OBRIGATORIA
                            + " deve possuir NOT NULL. Valor encontrado: "
                            + notNull + "."
            );
        }
    }

    /**
     * Aceita somente representações controladas e inequívocas do valor padrão 1.
     */
    private static void validarValorPadrao(String valorPadrao) {

        if (!valorPadraoEquivaleAUm(valorPadrao)) {
            throw new IllegalStateException(
                    "Estrutura do banco inconsistente: a coluna "
                            + COLUNA_TROCA_SENHA_OBRIGATORIA
                            + " deve possuir DEFAULT equivalente a 1. Valor encontrado: "
                            + String.valueOf(valorPadrao) + "."
            );
        }
    }

    /**
     * Normaliza no máximo um par externo de parênteses e um par de aspas simples
     * ou duplas. Outras expressões e conversões permanecem rejeitadas.
     */
    private static boolean valorPadraoEquivaleAUm(String valorPadrao) {

        if (valorPadrao == null) {
            return false;
        }

        String valorNormalizado = valorPadrao.trim();

        if (valorNormalizado.isEmpty()) {
            return false;
        }

        if (valorNormalizado.length() >= 2
                && valorNormalizado.startsWith("(")
                && valorNormalizado.endsWith(")")) {
            valorNormalizado = valorNormalizado
                    .substring(1, valorNormalizado.length() - 1)
                    .trim();
        }

        boolean possuiAspasSimples = valorNormalizado.length() >= 2
                && valorNormalizado.startsWith("'")
                && valorNormalizado.endsWith("'");
        boolean possuiAspasDuplas = valorNormalizado.length() >= 2
                && valorNormalizado.startsWith("\"")
                && valorNormalizado.endsWith("\"");

        if (possuiAspasSimples || possuiAspasDuplas) {
            valorNormalizado = valorNormalizado.substring(
                    1,
                    valorNormalizado.length() - 1
            );
        }

        return "1".equals(valorNormalizado);
    }

    /**
     * Rejeita valores nulos ou fora do domínio binário antes do avanço da versão.
     */
    private static void validarDadosTrocaSenhaObrigatoria(Connection conn)
            throws SQLException {

        String sql = """
            SELECT COUNT(*)
            FROM Usuario
            WHERE troca_senha_obrigatoria IS NULL
               OR troca_senha_obrigatoria NOT IN (0, 1)
            """;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (!rs.next()) {
                throw new IllegalStateException(
                        "Não foi possível validar os valores da coluna "
                                + COLUNA_TROCA_SENHA_OBRIGATORIA + "."
                );
            }

            long quantidadeInvalida = rs.getLong(1);

            if (quantidadeInvalida > 0) {
                throw new IllegalStateException(
                        "Dados do banco inconsistentes: foram encontrados "
                                + quantidadeInvalida
                                + " usuário(s) com valor inválido na coluna "
                                + COLUNA_TROCA_SENHA_OBRIGATORIA + "."
                );
            }
        }
    }

    /**
     * Tenta desfazer a evolução sem ocultar a falha que originou o rollback.
     */
    private static void fazerRollbackSeguro(Connection conn, Throwable falhaOriginal) {

        try {
            if (!conn.getAutoCommit()) {
                conn.rollback();
            }
        } catch (SQLException e) {
            falhaOriginal.addSuppressed(e);
        }
    }

    /**
     * Restaura o estado recebido da Connection e preserva a falha principal.
     */
    private static void restaurarAutoCommit(
            Connection conn,
            boolean autoCommitOriginal,
            Throwable falhaOriginal
    ) throws SQLException {

        try {
            conn.setAutoCommit(autoCommitOriginal);
        } catch (SQLException e) {
            if (falhaOriginal != null) {
                falhaOriginal.addSuppressed(e);
                return;
            }

            throw e;
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
