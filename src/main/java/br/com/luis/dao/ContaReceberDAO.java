package br.com.luis.dao;

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

import java.util.ArrayList;
import java.util.List;

/**
 * DAO responsável pela persistência da entidade ContaReceber.
 *
 * Insere contas a receber, consulta contas e valores pendentes, atualiza o status
 * com proteção do estado esperado e lista pendências com dados do cliente.
 * Participa tanto da geração da conta em vendas a prazo quanto das operações de
 * consulta e atualização usadas no recebimento integral.
 *
 * Não contém regras de limite de crédito ou de recebimento. Essas decisões e o
 * controle transacional pertencem ao VendaService e ao ContaReceberService.
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
     * Busca uma conta a receber pelo ID usando uma Connection externa.
     *
     * É utilizada pelo ContaReceberService no fluxo transacional de recebimento
     * integral. O DAO apenas consulta e mapeia os dados; as validações sobre o
     * estado da conta pertencem ao Service.
     *
     * Importante:
     * - não abre nova Connection;
     * - não executa commit;
     * - não executa rollback;
     * - não fecha a Connection recebida.
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
     * Atualiza o status de uma conta a receber com proteção de estado atual.
     *
     * A condição statusAtual evita que uma conta já alterada seja atualizada
     * novamente sem que a camada Service perceba. A decisão de permitir o
     * recebimento e escolher o novo status pertence ao ContaReceberService.
     *
     * Importante:
     * - não abre nova Connection;
     * - não executa commit;
     * - não executa rollback;
     * - não fecha a Connection recebida.
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
     * Lista contas a receber pendentes com o nome do cliente vinculado.
     *
     * Usa uma Connection externa fornecida pelo ContaReceberService. Encerra os
     * recursos JDBC que cria, mas respeita a propriedade da conexão recebida.
     *
     * Importante:
     * - não abre nova Connection;
     * - não executa commit;
     * - não executa rollback;
     * - não fecha a Connection recebida;
     * - não calcula vencimento;
     * - não formata moeda;
     * - não formata data.
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

            // rs significa ResultSet e representa o conjunto de linhas
            // retornado pela consulta SELECT executada no banco de dados.
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
}