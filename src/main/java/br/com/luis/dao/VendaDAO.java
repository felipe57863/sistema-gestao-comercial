package br.com.luis.dao;

import br.com.luis.model.Venda;
import br.com.luis.model.StatusVenda;
import br.com.luis.model.FormaPagamento;
import br.com.luis.model.StatusContaReceber;
import br.com.luis.model.TipoVenda;
import br.com.luis.viewmodel.FiltroRelatorioDescontoVenda;
import br.com.luis.viewmodel.FiltroHistoricoVenda;
import br.com.luis.viewmodel.VendaDescontoRelatorioDados;
import br.com.luis.viewmodel.VendaHistoricoListagemView;
import br.com.luis.util.ConnectionFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
 * DAO responsável pela persistência e consulta de vendas por JDBC.
 *
 * Insere os dados principais da venda, executa atualizações protegidas de status
 * e fornece consultas consolidadas utilizadas pelo Histórico de Vendas, além de
 * consultas agregadas executadas diretamente no banco para o dashboard, como a
 * contagem de vendas válidas e a soma do valor total dessas vendas em um período.
 * Também fornece dados necessários aos detalhes e às validações dos fluxos
 * existentes. Os itens associados são persistidos separadamente pelo
 * ItemVendaDAO.
 *
 * Não controla a interface nem decide regras de finalização, recebimento ou
 * estorno. Essas decisões pertencem aos Services responsáveis. Nos métodos que
 * recebem uma Connection externa, o Service chamador controla commit, rollback
 * e fechamento da conexão.
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
     * A condição combina o identificador e o status esperado, impedindo que o
     * fluxo altere uma venda que já esteja em outro estado. O retorno permite ao
     * Service confirmar se exatamente o registro esperado foi atualizado. A
     * escolha do novo status pertence ao Service responsável pelo recebimento ou
     * estorno.
     *
     * Usa a Connection informada e encerra apenas o PreparedStatement criado.
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
     * Conta as vendas válidas registradas no período informado.
     *
     * Considera somente vendas com status {@link StatusVenda#PAGA} ou
     * {@link StatusVenda#PENDENTE}. Vendas com status
     * {@link StatusVenda#ESTORNADA} são excluídas da contagem.
     *
     * O limite inicial do período é inclusivo e o limite final é exclusivo. A
     * Connection é recebida externamente e permanece sob responsabilidade da
     * camada chamadora. Este método não executa commit, rollback nem fecha a
     * Connection recebida, encerrando somente o PreparedStatement e o ResultSet
     * que cria.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param inicioInclusivo data e hora inicial inclusiva do período.
     * @param fimExclusivo data e hora final exclusiva do período.
     * @return quantidade de vendas válidas no período ou zero quando nenhuma
     *         venda atender aos filtros.
     * @throws IllegalArgumentException quando a conexão ou algum limite do
     *                                  período for nulo, ou quando o limite final
     *                                  não for posterior ao limite inicial.
     * @throws IllegalStateException quando a consulta não retornar resultado ou
     *                               quando a quantidade calculada não puder ser
     *                               representada por um {@code int} válido.
     * @throws RuntimeException quando ocorrer erro de acesso ao banco de dados.
     */
    public int contarVendasValidasNoPeriodo(
            Connection conn,
            LocalDateTime inicioInclusivo,
            LocalDateTime fimExclusivo
    ) {

        if (conn == null) {
            throw new IllegalArgumentException(
                    "Conexão não pode ser nula."
            );
        }

        if (inicioInclusivo == null) {
            throw new IllegalArgumentException(
                    "Data e hora inicial do período são obrigatórias."
            );
        }

        if (fimExclusivo == null) {
            throw new IllegalArgumentException(
                    "Data e hora final do período são obrigatórias."
            );
        }

        if (!fimExclusivo.isAfter(inicioInclusivo)) {
            throw new IllegalArgumentException(
                    "O limite final do período deve ser posterior ao limite inicial."
            );
        }

        String sql = """
                SELECT COUNT(*) AS quantidade_vendas
                FROM Venda
                WHERE data_hora >= ?
                  AND data_hora < ?
                  AND status IN (?, ?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, inicioInclusivo.toString());
            stmt.setString(2, fimExclusivo.toString());
            stmt.setString(3, StatusVenda.PAGA.name());
            stmt.setString(4, StatusVenda.PENDENTE.name());

            try (ResultSet rs = stmt.executeQuery()) {

                if (!rs.next()) {
                    throw new IllegalStateException(
                            "A consulta de quantidade de vendas não retornou resultado."
                    );
                }

                long quantidade =
                        rs.getLong("quantidade_vendas");

                if (quantidade < 0) {
                    throw new IllegalStateException(
                            "A quantidade de vendas calculada para o período é inválida."
                    );
                }

                if (quantidade > Integer.MAX_VALUE) {
                    throw new IllegalStateException(
                            "A quantidade de vendas calculada para o período excede o limite suportado."
                    );
                }

                return (int) quantidade;
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao contar vendas válidas no período.",
                    e
            );
        }
    }

    /**
     * Soma o valor total das vendas válidas registradas no período informado.
     *
     * Considera somente vendas com status {@link StatusVenda#PAGA} ou
     * {@link StatusVenda#PENDENTE}. Vendas com status
     * {@link StatusVenda#ESTORNADA} são excluídas da soma.
     *
     * O limite inicial do período é inclusivo e o limite final é exclusivo. A
     * Connection é recebida externamente e permanece sob responsabilidade da
     * camada chamadora. Este método não executa commit, rollback nem fecha a
     * Connection recebida, encerrando somente o PreparedStatement e o ResultSet
     * que cria.
     *
     * O retorno usa escala 2 e arredondamento {@link RoundingMode#HALF_UP}. O DAO
     * devolve somente o valor numérico consolidado, sem formatação visual ou
     * conversão para texto ou moeda.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param inicioInclusivo data e hora inicial inclusiva do período.
     * @param fimExclusivo data e hora final exclusiva do período.
     * @return valor total das vendas válidas no período, normalizado para escala
     *         2, ou zero quando nenhuma venda atender aos filtros.
     * @throws IllegalArgumentException quando a conexão ou algum limite do
     *                                  período for nulo, ou quando o limite final
     *                                  não for posterior ao limite inicial.
     * @throws IllegalStateException quando a consulta não retornar resultado ou
     *                               quando o valor total calculado for negativo.
     * @throws RuntimeException quando ocorrer erro de acesso ao banco de dados.
     */
    public BigDecimal somarValorVendasValidasNoPeriodo(
            Connection conn,
            LocalDateTime inicioInclusivo,
            LocalDateTime fimExclusivo
    ) {

        if (conn == null) {
            throw new IllegalArgumentException(
                    "Conexão não pode ser nula."
            );
        }

        if (inicioInclusivo == null) {
            throw new IllegalArgumentException(
                    "Data e hora inicial do período são obrigatórias."
            );
        }

        if (fimExclusivo == null) {
            throw new IllegalArgumentException(
                    "Data e hora final do período são obrigatórias."
            );
        }

        if (!fimExclusivo.isAfter(inicioInclusivo)) {
            throw new IllegalArgumentException(
                    "O limite final do período deve ser posterior ao limite inicial."
            );
        }

        String sql = """
                SELECT COALESCE(SUM(valor_total), 0) AS valor_total_vendido
                FROM Venda
                WHERE data_hora >= ?
                  AND data_hora < ?
                  AND status IN (?, ?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, inicioInclusivo.toString());
            stmt.setString(2, fimExclusivo.toString());
            stmt.setString(3, StatusVenda.PAGA.name());
            stmt.setString(4, StatusVenda.PENDENTE.name());

            try (ResultSet rs = stmt.executeQuery()) {

                if (!rs.next()) {
                    throw new IllegalStateException(
                            "A consulta do valor total vendido não retornou resultado."
                    );
                }

                BigDecimal valorTotalVendido =
                        rs.getBigDecimal("valor_total_vendido");

                if (valorTotalVendido == null) {
                    valorTotalVendido = BigDecimal.ZERO;
                }

                if (valorTotalVendido.signum() < 0) {
                    throw new IllegalStateException(
                            "O valor total vendido calculado para o período é inválido."
                    );
                }

                return valorTotalVendido.setScale(
                        2,
                        RoundingMode.HALF_UP
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao somar o valor das vendas válidas no período.",
                    e
            );
        }
    }

    /**
     * Lista as vendas válidas com desconto no período informado.
     *
     * A consulta agrega os valores históricos congelados em ItemVenda por venda,
     * associa Venda e Cliente em uma única execução e não acessa Produto ou
     * Promocao. O limite inicial é inclusivo e a data final visual é convertida
     * para o início exclusivo do dia seguinte.
     *
     * Não abre ou fecha Connection, não executa commit e não executa rollback.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param filtro fotografia dos filtros aplicados ao relatório.
     * @return projeções históricas ordenadas ou lista vazia.
     */
    public List<VendaDescontoRelatorioDados> listarParaRelatorioDescontos(
            Connection conn,
            FiltroRelatorioDescontoVenda filtro
    ) {
        if (conn == null) {
            throw new IllegalArgumentException(
                    "Conexão não pode ser nula."
            );
        }

        if (filtro == null) {
            throw new IllegalArgumentException(
                    "Filtro do relatório de descontos não pode ser nulo."
            );
        }

        filtro.validar();

        if (LocalDateTime.MAX.toLocalDate().equals(filtro.getDataFinal())) {
            throw new IllegalArgumentException(
                    "A data final informada não permite calcular "
                            + "o limite exclusivo do período."
            );
        }

        LocalDateTime inicioInclusivo =
                filtro.getDataInicial().atStartOfDay();

        LocalDateTime fimExclusivo;

        try {
            fimExclusivo = filtro.getDataFinal()
                    .plusDays(1)
                    .atStartOfDay();

        } catch (DateTimeException e) {
            throw new IllegalArgumentException(
                    "Data final inválida para consulta do relatório de descontos.",
                    e
            );
        }

        StringBuilder sql = new StringBuilder("""
                WITH itens_agregados AS (
                    SELECT
                        item.venda_id,
                        COALESCE(
                            SUM(item.quantidade * item.preco_unitario),
                            0
                        ) AS valor_bruto_itens,
                        COALESCE(
                            SUM(item.desconto_promocional),
                            0
                        ) AS desconto_promocional_itens,
                        COALESCE(
                            SUM(item.desconto_global),
                            0
                        ) AS desconto_global_itens,
                        COALESCE(
                            SUM(item.subtotal),
                            0
                        ) AS valor_liquido_itens
                    FROM ItemVenda item
                    GROUP BY item.venda_id
                )
                SELECT
                    venda.id_venda,
                    venda.data_hora,
                    venda.tipo_venda,
                    venda.status,
                    cliente.nome AS nome_cliente,
                    venda.valor_total AS valor_total_venda,
                    venda.valor_desconto_global AS desconto_global_venda,
                    itens.valor_bruto_itens,
                    itens.desconto_promocional_itens,
                    itens.desconto_global_itens,
                    itens.valor_liquido_itens
                FROM Venda venda
                INNER JOIN itens_agregados itens
                        ON itens.venda_id = venda.id_venda
                LEFT JOIN Cliente cliente
                       ON cliente.id_cliente = venda.cliente_id
                WHERE venda.data_hora >= ?
                  AND venda.data_hora < ?
                  AND venda.status IN (?, ?)
                  AND (
                      itens.desconto_promocional_itens
                      + itens.desconto_global_itens
                  ) > 0
                """);

        if (filtro.getTipoVenda() != null) {
            sql.append("""
                      AND venda.tipo_venda = ?
                    """);
        }

        sql.append("""
                ORDER BY (
                             itens.desconto_promocional_itens
                             + itens.desconto_global_itens
                         ) DESC,
                         venda.data_hora DESC,
                         venda.id_venda DESC
                """);

        List<VendaDescontoRelatorioDados> vendas = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int parametro = 1;

            stmt.setString(parametro++, inicioInclusivo.toString());
            stmt.setString(parametro++, fimExclusivo.toString());
            stmt.setString(parametro++, StatusVenda.PAGA.name());
            stmt.setString(parametro++, StatusVenda.PENDENTE.name());

            if (filtro.getTipoVenda() != null) {
                stmt.setString(parametro, filtro.getTipoVenda().name());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Integer vendaId = rs.getInt("id_venda");

                    try {
                        VendaDescontoRelatorioDados dados =
                                new VendaDescontoRelatorioDados(
                                        vendaId,
                                        LocalDateTime.parse(
                                                rs.getString("data_hora")
                                        ),
                                        TipoVenda.valueOf(
                                                rs.getString("tipo_venda")
                                        ),
                                        StatusVenda.valueOf(
                                                rs.getString("status")
                                        ),
                                        rs.getString("nome_cliente"),
                                        rs.getBigDecimal("valor_total_venda"),
                                        rs.getBigDecimal("desconto_global_venda"),
                                        rs.getBigDecimal("valor_bruto_itens"),
                                        rs.getBigDecimal(
                                                "desconto_promocional_itens"
                                        ),
                                        rs.getBigDecimal("desconto_global_itens"),
                                        rs.getBigDecimal("valor_liquido_itens")
                                );

                        vendas.add(dados);

                    } catch (DateTimeException | IllegalArgumentException e) {
                        throw new IllegalStateException(
                                "Venda " + vendaId
                                        + " possui dados históricos inválidos "
                                        + "no relatório de descontos.",
                                e
                        );
                    }
                }
            }

            return vendas;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao listar vendas para o relatório de descontos.",
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
