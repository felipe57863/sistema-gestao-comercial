package br.com.luis.dao;

import br.com.luis.model.Venda;
import br.com.luis.model.StatusVenda;
import br.com.luis.model.FormaPagamento;
import br.com.luis.model.StatusContaReceber;
import br.com.luis.model.TipoVenda;
import br.com.luis.viewmodel.FiltroHistoricoVenda;
import br.com.luis.viewmodel.VendaHistoricoListagemView;
import br.com.luis.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import java.time.LocalDateTime;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO responsável pela persistência da entidade Venda.
 *
 * Insere os dados principais da venda e retorna o identificador gerado. Os itens
 * associados são persistidos separadamente pelo ItemVendaDAO.
 *
 * Não contém regras de finalização, estoque ou financeiro. Essas regras e a
 * coordenação transacional pertencem ao VendaService.
 */
public class VendaDAO {

    /**
     * Insere uma nova venda no banco de dados.
     *
     * Abre e fecha sua própria Connection e usa try-with-resources para encerrar
     * o PreparedStatement e o ResultSet. Persiste apenas os dados principais;
     * os itens vinculados são inseridos separadamente pelo ItemVendaDAO.
     *
     * @param venda venda que será persistida.
     * @return ID gerado pelo banco para a venda inserida.
     */
    public int inserir(Venda venda) {
        String sql = """
                INSERT INTO Venda (
                    data_hora,
                    tipo_venda,
                    forma_pagamento,
                    valor_total,
                    valor_desconto_global,
                    status,
                    usuario_id,
                    cliente_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, venda.getDataHora().toString());
            stmt.setString(2, venda.getTipoVenda());
            stmt.setString(3, venda.getFormaPagamento());
            stmt.setBigDecimal(4, venda.getValorTotal());
            stmt.setBigDecimal(5, venda.getValorDescontoGlobal());
            stmt.setString(6, venda.getStatus());
            stmt.setInt(7, venda.getUsuarioId());

            if (venda.getClienteId() != null) {
                stmt.setInt(8, venda.getClienteId());
            } else {
                stmt.setNull(8, Types.INTEGER);
            }

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            throw new RuntimeException("Venda inserida, mas o ID gerado não foi retornado pelo banco.");

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir venda no banco de dados.", e);
        }
    }

    /**
     * Insere uma nova venda usando uma Connection externa.
     *
     * Participa da mesma transação de finalização coordenada pelo VendaService.
     * Encerra o PreparedStatement e o ResultSet que cria, mas respeita a
     * propriedade da Connection recebida.
     *
     * Importante:
     * - não abre nova Connection;
     * - não executa commit;
     * - não executa rollback;
     * - não fecha a Connection recebida.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param venda venda que será persistida.
     * @return ID gerado pelo banco para a venda inserida.
     */
    public int inserir(Connection conn, Venda venda) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (venda == null) {
            throw new IllegalArgumentException("Venda não pode ser nula.");
        }

        String sql = """
                INSERT INTO Venda (
                    data_hora,
                    tipo_venda,
                    forma_pagamento,
                    valor_total,
                    valor_desconto_global,
                    status,
                    usuario_id,
                    cliente_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, venda.getDataHora().toString());
            stmt.setString(2, venda.getTipoVenda());
            stmt.setString(3, venda.getFormaPagamento());
            stmt.setBigDecimal(4, venda.getValorTotal());
            stmt.setBigDecimal(5, venda.getValorDescontoGlobal());
            stmt.setString(6, venda.getStatus());
            stmt.setInt(7, venda.getUsuarioId());

            if (venda.getClienteId() != null) {
                stmt.setInt(8, venda.getClienteId());
            } else {
                stmt.setNull(8, Types.INTEGER);
            }

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            throw new RuntimeException("Venda inserida, mas o ID gerado não foi retornado pelo banco.");

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir venda no banco de dados usando conexão externa.", e);
        }
    }

    /**
     * Atualiza o status de uma venda somente quando o status atual persistido
     * corresponde ao status esperado.
     *
     * Usa uma Connection externa e encerra apenas o PreparedStatement criado.
     * Não executa commit, rollback nem fecha a Connection recebida.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param vendaId identificador da venda que será atualizada.
     * @param statusAtual status atual esperado para a venda.
     * @param novoStatus novo status que será persistido.
     * @return {@code true} quando exatamente uma venda for atualizada;
     *         {@code false} quando nenhuma venda corresponder ao ID e status atual.
     */
    public boolean atualizarStatus(
            Connection conn,
            Integer vendaId,
            StatusVenda statusAtual,
            StatusVenda novoStatus
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (vendaId == null || vendaId <= 0) {
            throw new IllegalArgumentException("ID da venda deve ser maior que zero.");
        }

        if (statusAtual == null) {
            throw new IllegalArgumentException("Status atual não pode ser nulo.");
        }

        if (novoStatus == null) {
            throw new IllegalArgumentException("Novo status não pode ser nulo.");
        }

        if (statusAtual == novoStatus) {
            throw new IllegalArgumentException("Status atual e novo status devem ser diferentes.");
        }

        String sql = """
                UPDATE Venda
                SET status = ?
                WHERE id_venda = ?
                  AND status = ?
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, novoStatus.name());
            stmt.setInt(2, vendaId);
            stmt.setString(3, statusAtual.name());

            int linhasAfetadas = stmt.executeUpdate();

            if (linhasAfetadas > 1) {
                throw new IllegalStateException("Mais de uma venda foi atualizada para o mesmo ID.");
            }

            return linhasAfetadas == 1;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar o status da venda no banco de dados.", e);
        }
    }
    /**
     * Busca uma venda pelo identificador usando uma Connection externa.
     *
     * Participa da transação controlada pela camada Service e encerra somente
     * o PreparedStatement e o ResultSet criados pelo método.
     *
     * Não carrega os itens vinculados à venda.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param vendaId identificador da venda pesquisada.
     * @return venda encontrada ou {@code null} quando não existir venda
     *         com o identificador informado.
     */
    public Venda buscarPorId(Connection conn, Integer vendaId) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (vendaId == null || vendaId <= 0) {
            throw new IllegalArgumentException("ID da venda deve ser maior que zero.");
        }

        String sql = """
            SELECT
                id_venda,
                data_hora,
                tipo_venda,
                forma_pagamento,
                valor_total,
                valor_desconto_global,
                status,
                usuario_id,
                cliente_id
            FROM Venda
            WHERE id_venda = ?
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, vendaId);

            try (ResultSet rs = stmt.executeQuery()) {

                if (!rs.next()) {
                    return null;
                }

                Venda venda = new Venda();

                venda.setIdVenda(rs.getInt("id_venda"));
                venda.setDataHora(LocalDateTime.parse(rs.getString("data_hora")));
                venda.setTipoVenda(rs.getString("tipo_venda"));
                venda.setFormaPagamento(rs.getString("forma_pagamento"));
                venda.setValorTotal(rs.getBigDecimal("valor_total"));
                venda.setValorDescontoGlobal(rs.getBigDecimal("valor_desconto_global"));
                venda.setStatus(rs.getString("status"));
                venda.setUsuarioId(rs.getInt("usuario_id"));

                int clienteId = rs.getInt("cliente_id");
                venda.setClienteId(rs.wasNull() ? null : clienteId);

                return venda;
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao buscar venda por ID no banco de dados.",
                    e
            );
        }
    }
    /**
     * Lista as vendas do histórico aplicando somente os filtros informados.
     *
     * Usa uma única consulta consolidada para evitar N+1, mantém vendas sem
     * cliente por meio de LEFT JOIN e retorna uma linha por venda.
     *
     * A quantidade de itens representa produtos distintos.
     *
     * Não abre ou fecha Connection, não executa commit e não executa rollback.
     */
    public List<VendaHistoricoListagemView>
    listarHistoricoComFiltros(
            Connection conn,
            FiltroHistoricoVenda filtro
    ) {

        if (conn == null) {
            throw new IllegalArgumentException(
                    "Conexão não pode ser nula."
            );
        }

        if (filtro == null) {
            throw new IllegalArgumentException(
                    "Filtro do histórico não pode ser nulo."
            );
        }

        filtro.validar();

        StringBuilder sql = new StringBuilder("""
                WITH itens_resumo AS (
                    SELECT
                        venda_id,
                        COUNT(DISTINCT produto_id) AS quantidade_itens
                    FROM ItemVenda
                    GROUP BY venda_id
                ),
                contas_resumo AS (
                    SELECT
                        venda_id,
                        COUNT(*) AS quantidade_contas,
                        MAX(status) AS status_conta
                    FROM ContaReceber
                    GROUP BY venda_id
                ),
                entradas_resumo AS (
                    SELECT
                        venda_id,
                        COUNT(*) AS quantidade_entradas,
                        MAX(forma_pagamento) AS forma_pagamento_entrada
                    FROM MovimentacaoFinanceira
                    WHERE tipo = 'ENTRADA'
                      AND origem IN (
                          'VENDA_A_VISTA',
                          'RECEBIMENTO_CONTA'
                      )
                    GROUP BY venda_id
                )
                SELECT
                    venda.id_venda,
                    venda.data_hora,
                    venda.tipo_venda,
                    venda.forma_pagamento,
                    venda.valor_total,
                    venda.status,
                    cliente.nome AS nome_cliente,
                    COALESCE(
                        itens.quantidade_itens,
                        0
                    ) AS quantidade_itens,
                    COALESCE(
                        contas.quantidade_contas,
                        0
                    ) AS quantidade_contas,
                    contas.status_conta,
                    COALESCE(
                        entradas.quantidade_entradas,
                        0
                    ) AS quantidade_entradas,
                    entradas.forma_pagamento_entrada
                FROM Venda venda
                LEFT JOIN Cliente cliente
                       ON cliente.id_cliente = venda.cliente_id
                LEFT JOIN itens_resumo itens
                       ON itens.venda_id = venda.id_venda
                LEFT JOIN contas_resumo contas
                       ON contas.venda_id = venda.id_venda
                LEFT JOIN entradas_resumo entradas
                       ON entradas.venda_id = venda.id_venda
                WHERE 1 = 1
                """);

        List<Object> parametros = new ArrayList<>();

        if (filtro.getDataInicial() != null) {
            sql.append("""
                      AND venda.data_hora >= ?
                    """);

            parametros.add(
                    filtro.getDataInicial().atStartOfDay()
            );
        }

        if (filtro.getDataFinal() != null) {

            LocalDateTime limiteSuperiorExclusivo;

            try {
                limiteSuperiorExclusivo =
                        filtro.getDataFinal()
                                .plusDays(1)
                                .atStartOfDay();

            } catch (DateTimeException e) {
                throw new IllegalArgumentException(
                        "Data final inválida para consulta.",
                        e
                );
            }

            sql.append("""
                      AND venda.data_hora < ?
                    """);

            parametros.add(limiteSuperiorExclusivo);
        }

        if (filtro.getClienteOuDocumento() != null) {

            String termo =
                    filtro.getClienteOuDocumento()
                            .trim();

            String termoDocumento =
                    termo.replaceAll("[^0-9]", "");

            if (termoDocumento.isBlank()) {
                termoDocumento = termo;
            }

            sql.append("""
                      AND (
                          LOWER(
                              COALESCE(cliente.nome, '')
                          ) LIKE ?
                          OR COALESCE(
                              cliente.documento,
                              ''
                          ) LIKE ?
                      )
                    """);

            parametros.add(
                    "%" + termo.toLowerCase() + "%"
            );

            parametros.add(
                    "%" + termoDocumento + "%"
            );
        }

        if (filtro.getVendaId() != null) {
            sql.append("""
                      AND venda.id_venda = ?
                    """);

            parametros.add(filtro.getVendaId());
        }

        if (filtro.getTipoVenda() != null) {
            sql.append("""
                      AND venda.tipo_venda = ?
                    """);

            parametros.add(
                    filtro.getTipoVenda().name()
            );
        }

        if (filtro.getStatusVenda() != null) {
            sql.append("""
                      AND venda.status = ?
                    """);

            parametros.add(
                    filtro.getStatusVenda().name()
            );
        }

        sql.append("""
                ORDER BY venda.data_hora DESC,
                         venda.id_venda DESC
                """);

        List<VendaHistoricoListagemView> vendas =
                new ArrayList<>();

        try (PreparedStatement stmt =
                     conn.prepareStatement(sql.toString())) {

            definirParametrosHistorico(
                    stmt,
                    parametros
            );

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    Integer vendaId =
                            rs.getInt("id_venda");

                    TipoVenda tipoVenda =
                            converterEnumHistoricoObrigatorio(
                                    rs.getString("tipo_venda"),
                                    TipoVenda.class,
                                    "tipo da venda",
                                    vendaId
                            );

                    StatusVenda statusVenda =
                            converterEnumHistoricoObrigatorio(
                                    rs.getString("status"),
                                    StatusVenda.class,
                                    "status da venda",
                                    vendaId
                            );

                    FormaPagamento formaPagamentoVenda =
                            converterEnumHistoricoObrigatorio(
                                    rs.getString(
                                            "forma_pagamento"
                                    ),
                                    FormaPagamento.class,
                                    "forma de pagamento da venda",
                                    vendaId
                            );

                    StatusContaReceber statusContaReceber =
                            converterEnumHistoricoOpcional(
                                    rs.getString("status_conta"),
                                    StatusContaReceber.class,
                                    "status da conta a receber",
                                    vendaId
                            );

                    FormaPagamento formaPagamentoEntrada =
                            converterEnumHistoricoOpcional(
                                    rs.getString(
                                            "forma_pagamento_entrada"
                                    ),
                                    FormaPagamento.class,
                                    "forma de pagamento da entrada",
                                    vendaId
                            );

                    VendaHistoricoListagemView vendaView =
                            new VendaHistoricoListagemView(
                                    vendaId,
                                    LocalDateTime.parse(
                                            rs.getString(
                                                    "data_hora"
                                            )
                                    ),
                                    rs.getString(
                                            "nome_cliente"
                                    ),
                                    tipoVenda,
                                    statusVenda,
                                    rs.getBigDecimal(
                                            "valor_total"
                                    ),
                                    rs.getInt(
                                            "quantidade_itens"
                                    ),
                                    null,
                                    formaPagamentoVenda,
                                    statusContaReceber,
                                    formaPagamentoEntrada,
                                    rs.getInt(
                                            "quantidade_contas"
                                    ),
                                    rs.getInt(
                                            "quantidade_entradas"
                                    )
                            );

                    vendas.add(vendaView);
                }
            }

            return vendas;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao listar o histórico de vendas.",
                    e
            );
        }
    }

    /**
     * Preenche os parâmetros dinâmicos da consulta do histórico.
     */
    private void definirParametrosHistorico(
            PreparedStatement stmt,
            List<Object> parametros
    ) throws SQLException {

        for (int indice = 0;
             indice < parametros.size();
             indice++) {

            Object parametro = parametros.get(indice);
            int posicao = indice + 1;

            if (parametro instanceof LocalDateTime dataHora) {
                stmt.setString(
                        posicao,
                        dataHora.toString()
                );
                continue;
            }

            if (parametro instanceof Integer numero) {
                stmt.setInt(
                        posicao,
                        numero
                );
                continue;
            }

            if (parametro instanceof String texto) {
                stmt.setString(
                        posicao,
                        texto
                );
                continue;
            }

            throw new IllegalStateException(
                    "Tipo de parâmetro não suportado na consulta "
                            + "do histórico de vendas."
            );
        }
    }

    /**
     * Converte um valor textual obrigatório retornado pelo banco.
     *
     * A conversão é estrita e não corrige silenciosamente valores inválidos.
     */
    private <E extends Enum<E>> E
    converterEnumHistoricoObrigatorio(
            String valor,
            Class<E> tipoEnum,
            String nomeCampo,
            Integer vendaId
    ) {

        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(
                    "Venda " + vendaId
                            + " possui " + nomeCampo
                            + " não informado."
            );
        }

        try {
            return Enum.valueOf(
                    tipoEnum,
                    valor
            );

        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Venda " + vendaId
                            + " possui " + nomeCampo
                            + " inválido: " + valor + ".",
                    e
            );
        }
    }

    /**
     * Converte um valor textual opcional retornado pelo banco.
     */
    private <E extends Enum<E>> E
    converterEnumHistoricoOpcional(
            String valor,
            Class<E> tipoEnum,
            String nomeCampo,
            Integer vendaId
    ) {

        if (valor == null) {
            return null;
        }

        if (valor.isBlank()) {
            throw new IllegalStateException(
                    "Venda " + vendaId
                            + " possui " + nomeCampo
                            + " vazio."
            );
        }

        try {
            return Enum.valueOf(
                    tipoEnum,
                    valor
            );

        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Venda " + vendaId
                            + " possui " + nomeCampo
                            + " inválido: " + valor + ".",
                    e
            );
        }
    }
}
