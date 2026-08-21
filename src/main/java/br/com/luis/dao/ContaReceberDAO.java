package br.com.luis.dao;

import br.com.luis.model.Cliente;
import br.com.luis.model.ContaReceber;
import br.com.luis.model.StatusContaReceber;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.time.LocalDate;
import java.time.LocalDateTime;

import br.com.luis.viewmodel.ContaReceberListagemView;
import br.com.luis.viewmodel.ContaReceberRelatorioDados;
import br.com.luis.viewmodel.ClientePendenciaRelatorioView;
import br.com.luis.viewmodel.FiltroRelatorioClientePendencia;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO responsável pela persistência e consulta de contas a receber por JDBC.
 *
 * Insere as contas geradas por vendas a prazo, consulta valores pendentes usados
 * na validação do limite de crédito e fornece dados para listagens e detalhes.
 * Também executa atualizações protegidas de status solicitadas pelos fluxos de
 * recebimento e estorno.
 *
 * Não escolhe o novo estado, não cria movimentações financeiras e não controla
 * transações de negócio. Essas decisões pertencem aos Services responsáveis,
 * que também controlam commit, rollback e fechamento da Connection recebida.
 */
public class ContaReceberDAO {

    /**
     * Insere uma conta a receber usando uma Connection externa.
     *
     * Participa da transação de finalização da venda a prazo coordenada pelo
     * VendaService. Encerra o PreparedStatement e o ResultSet que cria, mas
     * respeita a propriedade da Connection recebida.
     *
     * Importante:
     * - não abre nova Connection;
     * - não executa commit;
     * - não executa rollback;
     * - não fecha a Connection recebida.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param contaReceber conta a receber que será persistida.
     * @return ID gerado pelo banco para a conta inserida.
     */
    public int inserir(Connection conn, ContaReceber contaReceber) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (contaReceber == null) {
            throw new IllegalArgumentException("Conta a receber não pode ser nula.");
        }

        String sql = """
                INSERT INTO ContaReceber (
                    valor,
                    data_vencimento,
                    status,
                    venda_id,
                    cliente_id,
                    prazo_pagamento_id,
                    quantidade_dias_prazo,
                    data_criacao
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setBigDecimal(1, contaReceber.getValor());
            stmt.setString(2, contaReceber.getDataVencimento().toString());
            stmt.setString(3, contaReceber.getStatus().name());
            stmt.setInt(4, contaReceber.getVendaId());
            stmt.setInt(5, contaReceber.getClienteId());
            stmt.setInt(6, contaReceber.getPrazoPagamentoId());
            stmt.setInt(7, contaReceber.getQuantidadeDiasPrazo());
            stmt.setString(8, contaReceber.getDataCriacao().toString());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            throw new RuntimeException("Conta a receber inserida, mas o ID gerado não foi retornado pelo banco.");

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir conta a receber no banco de dados.", e);
        }
    }

    /**
     * Soma o total pendente de contas a receber de um cliente.
     *
     * A consulta soma somente contas com status PENDENTE. O VendaService utiliza
     * esse valor no cálculo do limite disponível; o DAO não calcula nem valida
     * o limite de crédito.
     *
     * Importante:
     * - não abre nova Connection;
     * - não executa commit;
     * - não executa rollback;
     * - não fecha a Connection recebida.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param clienteId ID do cliente.
     * @return soma dos valores pendentes do cliente.
     */
    public BigDecimal somarTotalPendentePorCliente(Connection conn, Integer clienteId) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (clienteId == null || clienteId <= 0) {
            throw new IllegalArgumentException("ID do cliente inválido.");
        }

        String sql = """
                SELECT COALESCE(SUM(valor), 0) AS total_pendente
                FROM ContaReceber
                WHERE cliente_id = ?
                  AND status = ?
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, clienteId);
            stmt.setString(2, StatusContaReceber.PENDENTE.name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal totalPendente = rs.getBigDecimal("total_pendente");

                    if (totalPendente == null) {
                        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    }

                    return totalPendente.setScale(2, RoundingMode.HALF_UP);
                }
            }

            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao somar total pendente do cliente.", e);
        }
    }
    /**
     * Busca uma conta pelo ID durante o fluxo transacional de recebimento.
     *
     * O DAO apenas consulta e mapeia os dados; a validação do estado pertence ao
     * ContaReceberService. A Connection permanece sob controle do Service.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param idConta ID da conta a receber.
     * @return conta encontrada ou null caso não exista.
     */
    public ContaReceber buscarPorId(
            Connection conn,
            Integer idConta
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (idConta == null || idConta <= 0) {
            throw new IllegalArgumentException("ID da conta a receber inválido.");
        }

        String sql = """
            SELECT id_conta,
                   valor,
                   data_vencimento,
                   status,
                   venda_id,
                   cliente_id,
                   prazo_pagamento_id,
                   quantidade_dias_prazo,
                   data_criacao
            FROM ContaReceber
            WHERE id_conta = ?
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idConta);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new ContaReceber(
                            rs.getInt("id_conta"),
                            rs.getBigDecimal("valor"),
                            LocalDate.parse(rs.getString("data_vencimento")),
                            StatusContaReceber.valueOf(rs.getString("status")),
                            rs.getInt("venda_id"),
                            rs.getInt("cliente_id"),
                            rs.getInt("prazo_pagamento_id"),
                            rs.getInt("quantidade_dias_prazo"),
                            LocalDateTime.parse(rs.getString("data_criacao"))
                    );
                }
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar conta a receber por ID.", e);
        }
    }
    /**
     * Busca a conta a receber vinculada a uma venda usando uma Connection externa.
     *
     * Participa da transação controlada pela camada Service e encerra somente
     * o PreparedStatement e o ResultSet criados pelo método.
     *
     * A relação esperada é de no máximo uma conta a receber por venda.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param vendaId identificador da venda.
     * @return conta vinculada à venda ou {@code null} quando não existir.
     * @throws IllegalStateException quando mais de uma conta estiver vinculada
     *                               à mesma venda.
     */
    public ContaReceber buscarPorVendaId(
            Connection conn,
            Integer vendaId
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (vendaId == null || vendaId <= 0) {
            throw new IllegalArgumentException("ID da venda deve ser maior que zero.");
        }

        String sql = """
            SELECT id_conta,
                   valor,
                   data_vencimento,
                   status,
                   venda_id,
                   cliente_id,
                   prazo_pagamento_id,
                   quantidade_dias_prazo,
                   data_criacao
            FROM ContaReceber
            WHERE venda_id = ?
            ORDER BY id_conta
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, vendaId);

            try (ResultSet rs = stmt.executeQuery()) {

                if (!rs.next()) {
                    return null;
                }

                ContaReceber contaReceber = new ContaReceber(
                        rs.getInt("id_conta"),
                        rs.getBigDecimal("valor"),
                        LocalDate.parse(rs.getString("data_vencimento")),
                        StatusContaReceber.valueOf(rs.getString("status")),
                        rs.getInt("venda_id"),
                        rs.getInt("cliente_id"),
                        rs.getInt("prazo_pagamento_id"),
                        rs.getInt("quantidade_dias_prazo"),
                        LocalDateTime.parse(rs.getString("data_criacao"))
                );

                if (rs.next()) {
                    throw new IllegalStateException(
                            "Mais de uma conta a receber está vinculada à venda de ID "
                                    + vendaId + "."
                    );
                }

                return contaReceber;
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao buscar conta a receber pela venda.",
                    e
            );
        }
    }
    /**
     * Atualiza o status somente se a conta ainda estiver no estado esperado.
     *
     * A condição por ID e status protege o fluxo contra atualização repetida ou
     * concorrente. A escolha do novo estado pertence ao Service. A Connection
     * continua sob controle do chamador.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param idConta ID da conta a receber.
     * @param statusAtual status esperado antes da atualização.
     * @param novoStatus novo status que será gravado.
     * @return true se exatamente uma linha foi atualizada; false se nenhuma linha correspondeu.
     */
    public boolean atualizarStatus(
            Connection conn,
            Integer idConta,
            StatusContaReceber statusAtual,
            StatusContaReceber novoStatus
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (idConta == null || idConta <= 0) {
            throw new IllegalArgumentException("ID da conta a receber inválido.");
        }

        if (statusAtual == null) {
            throw new IllegalArgumentException("Status atual da conta a receber é obrigatório.");
        }

        if (novoStatus == null) {
            throw new IllegalArgumentException("Novo status da conta a receber é obrigatório.");
        }

        String sql = """
            UPDATE ContaReceber
            SET status = ?
            WHERE id_conta = ?
              AND status = ?
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, novoStatus.name());
            stmt.setInt(2, idConta);
            stmt.setString(3, statusAtual.name());

            int linhasAfetadas = stmt.executeUpdate();

            if (linhasAfetadas > 1) {
                throw new IllegalStateException(
                        "Mais de uma conta a receber foi atualizada para o ID "
                                + idConta + "."
                );
            }

            return linhasAfetadas == 1;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar status da conta a receber.", e);
        }
    }
    /**
     * Lista contas pendentes com os dados básicos do cliente vinculado.
     *
     * O DAO apenas consulta e mapeia as linhas; não calcula vencimento nem aplica
     * formatação de moeda ou data. A Connection permanece sob controle do Service.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @return lista de contas pendentes com dados básicos do cliente.
     */
    public List<ContaReceberListagemView> listarPendentesComCliente(
            Connection conn
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        String sql = """
            SELECT conta.id_conta,
                   conta.cliente_id,
                   cliente.nome AS nome_cliente,
                   conta.venda_id,
                   conta.valor,
                   conta.data_vencimento,
                   conta.status
            FROM ContaReceber conta
            INNER JOIN Cliente cliente
                    ON cliente.id_cliente = conta.cliente_id
            WHERE conta.status = ?
            ORDER BY conta.data_vencimento ASC,
                     conta.id_conta ASC
            """;

        List<ContaReceberListagemView> contasPendentes = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, StatusContaReceber.PENDENTE.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ContaReceberListagemView contaView = new ContaReceberListagemView(
                            rs.getInt("id_conta"),
                            rs.getInt("cliente_id"),
                            rs.getString("nome_cliente"),
                            rs.getInt("venda_id"),
                            rs.getBigDecimal("valor"),
                            LocalDate.parse(rs.getString("data_vencimento")),
                            StatusContaReceber.valueOf(rs.getString("status")),
                            false
                    );

                    contasPendentes.add(contaView);
                }
            }

            return contasPendentes;

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar contas a receber pendentes.", e);
        }
    }

    /**
     * Lista contas a receber para o relatório histórico no intervalo informado.
     *
     * Usa uma Connection externa e resolve ContaReceber e Cliente em uma única
     * consulta. O período considera a data de vencimento no intervalo
     * [inicioInclusivo, fimExclusivo). O filtro de cliente é opcional e pesquisa
     * parcialmente pelo nome, sem diferenciar maiúsculas e minúsculas, ou pelo
     * documento persistido sem pontuação.
     *
     * O método retorna somente a projeção persistida necessária ao relatório.
     * Não calcula A_VENCER ou VENCIDA, não consolida totais e não executa regras
     * de autorização; essas responsabilidades pertencem ao Service.
     *
     * @param conn conexão externa controlada pela camada Service.
     * @param inicioInclusivo primeira data de vencimento incluída no período.
     * @param fimExclusivo primeira data de vencimento não incluída no período.
     * @param clienteTexto texto opcional para pesquisa por nome ou documento.
     * @return lista de projeções persistidas em ordem crescente de vencimento e ID.
     * @throws IllegalArgumentException quando a conexão ou os limites do período
     *                                  forem inválidos.
     * @throws IllegalStateException quando um registro persistido não puder ser
     *                               convertido para a projeção do relatório.
     * @throws RuntimeException quando ocorrer erro de acesso ao banco de dados.
     */
    public List<ContaReceberRelatorioDados> listarParaRelatorio(
            Connection conn,
            LocalDate inicioInclusivo,
            LocalDate fimExclusivo,
            String clienteTexto
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (inicioInclusivo == null) {
            throw new IllegalArgumentException(
                    "Data inicial do período é obrigatória."
            );
        }

        if (fimExclusivo == null) {
            throw new IllegalArgumentException(
                    "Data final exclusiva do período é obrigatória."
            );
        }

        if (!fimExclusivo.isAfter(inicioInclusivo)) {
            throw new IllegalArgumentException(
                    "O limite final do período deve ser posterior ao limite inicial."
            );
        }

        String termoCliente =
                clienteTexto == null || clienteTexto.isBlank()
                        ? null
                        : clienteTexto.trim();

        String termoDocumento = null;

        if (termoCliente != null) {
            String documentoNormalizado =
                    termoCliente.replaceAll("[^0-9]", "");

            if (!documentoNormalizado.isBlank()) {
                termoDocumento = documentoNormalizado;
            }
        }

        StringBuilder sql = new StringBuilder("""
                SELECT conta.id_conta,
                       conta.venda_id,
                       cliente.nome AS nome_cliente,
                       conta.valor,
                       conta.data_vencimento,
                       conta.status
                FROM ContaReceber conta
                INNER JOIN Cliente cliente
                        ON cliente.id_cliente = conta.cliente_id
                WHERE conta.data_vencimento >= ?
                  AND conta.data_vencimento < ?
                """);

        if (termoCliente != null) {
            if (termoDocumento != null) {
                sql.append("""
                          AND (
                              LOWER(cliente.nome) LIKE ?
                              OR cliente.documento LIKE ?
                          )
                        """);
            } else {
                sql.append("""
                          AND LOWER(cliente.nome) LIKE ?
                        """);
            }
        }

        sql.append("""
                ORDER BY conta.data_vencimento ASC,
                         conta.id_conta ASC
                """);

        List<ContaReceberRelatorioDados> contas = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int indiceParametro = 1;

            stmt.setString(
                    indiceParametro++,
                    inicioInclusivo.toString()
            );

            stmt.setString(
                    indiceParametro++,
                    fimExclusivo.toString()
            );

            if (termoCliente != null) {
                stmt.setString(
                        indiceParametro++,
                        "%" + termoCliente.toLowerCase() + "%"
                );

                if (termoDocumento != null) {
                    stmt.setString(
                            indiceParametro++,
                            "%" + termoDocumento + "%"
                    );
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Integer idConta = null;

                    try {
                        idConta = rs.getInt("id_conta");

                        ContaReceberRelatorioDados contaDados =
                                new ContaReceberRelatorioDados(
                                        idConta,
                                        rs.getInt("venda_id"),
                                        rs.getString("nome_cliente"),
                                        rs.getBigDecimal("valor"),
                                        LocalDate.parse(
                                                rs.getString("data_vencimento")
                                        ),
                                        StatusContaReceber.valueOf(
                                                rs.getString("status")
                                        )
                                );

                        contas.add(contaDados);

                    } catch (RuntimeException e) {
                        String contextoIdentificacao =
                                idConta != null && idConta > 0
                                        ? " de ID " + idConta
                                        : "";

                        throw new IllegalStateException(
                                "Dados persistidos inválidos ao mapear conta a receber"
                                        + contextoIdentificacao
                                        + " para o relatório.",
                                e
                        );
                    }
                }
            }

            return contas;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao listar contas a receber para o relatório.",
                    e
            );
        }
    }

    /**
     * Lista, em uma única consulta agregada, os clientes que possuem contas
     * pendentes conforme os filtros informados.
     *
     * Cada linha corresponde a um cliente. PAGA e CANCELADA não participam da
     * agregação. Uma conta é considerada vencida somente quando sua data de
     * vencimento é anterior à data única de referência recebida. O filtro de
     * vencidas é aplicado no HAVING e a ordenação prioriza criticidade financeira.
     *
     * O método usa e preserva a Connection externa: não abre conexão, não executa
     * commit ou rollback e não fecha a conexão recebida.
     *
     * @param conn conexão externa controlada pelo Service.
     * @param filtro fotografia dos filtros aplicados.
     * @param dataReferencia data única usada para identificar contas vencidas.
     * @return linhas agregadas e ordenadas, ou lista vazia quando não houver dados.
     */
    public List<ClientePendenciaRelatorioView> listarClientesComPendencias(
            Connection conn,
            FiltroRelatorioClientePendencia filtro,
            LocalDate dataReferencia
    ) {
        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (filtro == null) {
            throw new IllegalArgumentException(
                    "Filtro do relatório de pendências não pode ser nulo."
            );
        }

        filtro.validar();

        if (dataReferencia == null) {
            throw new IllegalArgumentException(
                    "Data de referência do relatório de pendências é obrigatória."
            );
        }

        String termoCliente = filtro.getClienteTexto();
        String termoDocumento = null;

        if (termoCliente != null) {
            String documentoNormalizado =
                    termoCliente.replaceAll("[^0-9]", "");

            if (!documentoNormalizado.isBlank()) {
                termoDocumento = documentoNormalizado;
            }
        }

        StringBuilder sql = new StringBuilder("""
                SELECT cliente.id_cliente,
                       cliente.nome,
                       cliente.documento,
                       cliente.status AS status_cliente,
                       COUNT(conta.id_conta) AS quantidade_contas_pendentes,
                       COALESCE(SUM(conta.valor), 0) AS valor_pendente,
                       SUM(
                           CASE
                               WHEN conta.data_vencimento < ? THEN 1
                               ELSE 0
                           END
                       ) AS quantidade_contas_vencidas,
                       COALESCE(
                           SUM(
                               CASE
                                   WHEN conta.data_vencimento < ? THEN conta.valor
                                   ELSE 0
                               END
                           ),
                           0
                       ) AS valor_vencido
                FROM ContaReceber conta
                INNER JOIN Cliente cliente
                        ON cliente.id_cliente = conta.cliente_id
                WHERE conta.status = ?
                """);

        if (termoCliente != null) {
            if (termoDocumento != null) {
                sql.append("""
                          AND (
                              LOWER(cliente.nome) LIKE ?
                              OR cliente.documento LIKE ?
                          )
                        """);
            } else {
                sql.append("""
                          AND LOWER(cliente.nome) LIKE ?
                        """);
            }
        }

        if (filtro.getStatusCliente() != null) {
            sql.append("""
                      AND cliente.status = ?
                    """);
        }

        sql.append("""
                GROUP BY cliente.id_cliente,
                         cliente.nome,
                         cliente.documento,
                         cliente.status
                """);

        if (Boolean.TRUE.equals(filtro.getPossuiVencidas())) {
            sql.append("""
                    HAVING quantidade_contas_vencidas > 0
                    """);

        } else if (Boolean.FALSE.equals(filtro.getPossuiVencidas())) {
            sql.append("""
                    HAVING quantidade_contas_vencidas = 0
                    """);
        }

        sql.append("""
                ORDER BY CASE
                             WHEN quantidade_contas_vencidas > 0 THEN 1
                             ELSE 0
                         END DESC,
                         valor_vencido DESC,
                         valor_pendente DESC,
                         cliente.nome COLLATE NOCASE ASC,
                         cliente.id_cliente ASC
                """);

        List<ClientePendenciaRelatorioView> clientes = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int indiceParametro = 1;

            stmt.setString(indiceParametro++, dataReferencia.toString());
            stmt.setString(indiceParametro++, dataReferencia.toString());
            stmt.setString(indiceParametro++, StatusContaReceber.PENDENTE.name());

            if (termoCliente != null) {
                stmt.setString(
                        indiceParametro++,
                        "%" + termoCliente.toLowerCase() + "%"
                );

                if (termoDocumento != null) {
                    stmt.setString(
                            indiceParametro++,
                            "%" + termoDocumento + "%"
                    );
                }
            }

            if (filtro.getStatusCliente() != null) {
                stmt.setString(
                        indiceParametro,
                        filtro.getStatusCliente().name()
                );
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Integer clienteId = null;

                    try {
                        clienteId = rs.getInt("id_cliente");

                        ClientePendenciaRelatorioView cliente =
                                new ClientePendenciaRelatorioView(
                                        clienteId,
                                        rs.getString("nome"),
                                        rs.getString("documento"),
                                        Cliente.StatusCliente.valueOf(
                                                rs.getString("status_cliente")
                                        ),
                                        rs.getInt("quantidade_contas_pendentes"),
                                        rs.getBigDecimal("valor_pendente"),
                                        rs.getInt("quantidade_contas_vencidas"),
                                        rs.getBigDecimal("valor_vencido")
                                );

                        clientes.add(cliente);

                    } catch (RuntimeException e) {
                        String contextoIdentificacao =
                                clienteId != null && clienteId > 0
                                        ? " de ID " + clienteId
                                        : "";

                        throw new IllegalStateException(
                                "Dados persistidos inválidos ao mapear cliente"
                                        + contextoIdentificacao
                                        + " para o relatório de pendências.",
                                e
                        );
                    }
                }
            }

            return clientes;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao listar clientes com pendências financeiras.",
                    e
            );
        }
    }
    /**
     * Lista contas pendentes cujo vencimento não ultrapassa o limite informado.
     *
     * A consulta inclui tanto contas já vencidas quanto contas ainda a vencer
     * dentro da janela do alerta. Não existe limite inferior, portanto dívidas
     * antigas continuam sendo retornadas.
     *
     * A classificação entre VENCIDA e PROXIMA_DO_VENCIMENTO pertence ao Service.
     *
     * @param conn conexão externa controlada pelo Service.
     * @param limiteInclusivo última data de vencimento incluída na janela.
     * @return contas pendentes ordenadas por vencimento e ID.
     */
    public List<ContaReceberRelatorioDados> listarPendentesAteVencimento(
            Connection conn,
            LocalDate limiteInclusivo
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão não pode ser nula.");
        }

        if (limiteInclusivo == null) {
            throw new IllegalArgumentException(
                    "Limite de vencimento do alerta é obrigatório."
            );
        }

        String sql = """
                SELECT conta.id_conta,
                       conta.venda_id,
                       cliente.nome AS nome_cliente,
                       conta.valor,
                       conta.data_vencimento,
                       conta.status
                FROM ContaReceber conta
                INNER JOIN Cliente cliente
                        ON cliente.id_cliente = conta.cliente_id
                WHERE conta.status = ?
                  AND conta.data_vencimento <= ?
                ORDER BY conta.data_vencimento ASC,
                         conta.id_conta ASC
                """;

        List<ContaReceberRelatorioDados> contas = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, StatusContaReceber.PENDENTE.name());
            stmt.setString(2, limiteInclusivo.toString());

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    Integer idConta = null;

                    try {
                        idConta = rs.getInt("id_conta");

                        contas.add(
                                new ContaReceberRelatorioDados(
                                        idConta,
                                        rs.getInt("venda_id"),
                                        rs.getString("nome_cliente"),
                                        rs.getBigDecimal("valor"),
                                        LocalDate.parse(
                                                rs.getString("data_vencimento")
                                        ),
                                        StatusContaReceber.valueOf(
                                                rs.getString("status")
                                        )
                                )
                        );

                    } catch (RuntimeException e) {

                        String contexto =
                                idConta != null && idConta > 0
                                        ? " de ID " + idConta
                                        : "";

                        throw new IllegalStateException(
                                "Dados persistidos inválidos ao mapear conta"
                                        + contexto
                                        + " para os alertas de vencimento.",
                                e
                        );
                    }
                }
            }

            return contas;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao listar contas para os alertas de vencimento.",
                    e
            );
        }
    }
}
