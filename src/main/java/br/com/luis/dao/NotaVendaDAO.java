package br.com.luis.dao;

import br.com.luis.model.FormaPagamento;
import br.com.luis.model.NotaVenda;
import br.com.luis.model.StatusNotaVenda;
import br.com.luis.model.TipoVenda;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DAO responsável pela persistência e leitura da fotografia principal da
 * Nota de Venda.
 *
 * Todos os métodos usam Connection externa. A criação da Nota deve participar
 * da transação de finalização da venda e a mudança para ESTORNADA deve participar
 * da mesma transação do estorno. O DAO não executa commit, rollback nem fecha a
 * Connection recebida.
 */
public class NotaVendaDAO {

    /**
     * Insere a fotografia principal de uma Nota de Venda usando a Connection
     * controlada pelo Service.
     *
     * @return ID gerado, que também representa o número permanente da Nota.
     */
    public int inserir(Connection conn, NotaVenda notaVenda) {
        validarConnection(conn);

        if (notaVenda == null) {
            throw new IllegalArgumentException(
                    "Nota de Venda não pode ser nula."
            );
        }

        String sql = """
                INSERT INTO NotaVenda (
                    venda_id,
                    status,
                    data_hora_venda,
                    tipo_venda,
                    forma_pagamento,
                    usuario_id,
                    nome_usuario,
                    cliente_id,
                    nome_cliente,
                    documento_cliente,
                    valor_total,
                    valor_desconto_global,
                    valor_recebido,
                    troco,
                    prazo_pagamento_id,
                    quantidade_dias_prazo,
                    data_vencimento
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
        )) {
            stmt.setInt(1, notaVenda.getVendaId());
            stmt.setString(2, notaVenda.getStatus().name());
            stmt.setString(3, notaVenda.getDataHoraVenda().toString());
            stmt.setString(4, notaVenda.getTipoVenda().name());
            stmt.setString(5, notaVenda.getFormaPagamento().name());
            stmt.setInt(6, notaVenda.getUsuarioId());
            stmt.setString(7, notaVenda.getNomeUsuario());

            definirIntegerOpcional(
                    stmt,
                    8,
                    notaVenda.getClienteId()
            );
            definirStringOpcional(
                    stmt,
                    9,
                    notaVenda.getNomeCliente()
            );
            definirStringOpcional(
                    stmt,
                    10,
                    notaVenda.getDocumentoCliente()
            );

            stmt.setBigDecimal(11, notaVenda.getValorTotal());
            stmt.setBigDecimal(
                    12,
                    notaVenda.getValorDescontoGlobal()
            );

            definirBigDecimalOpcional(
                    stmt,
                    13,
                    notaVenda.getValorRecebido()
            );
            definirBigDecimalOpcional(
                    stmt,
                    14,
                    notaVenda.getTroco()
            );
            definirIntegerOpcional(
                    stmt,
                    15,
                    notaVenda.getPrazoPagamentoId()
            );
            definirIntegerOpcional(
                    stmt,
                    16,
                    notaVenda.getQuantidadeDiasPrazo()
            );
            definirLocalDateOpcional(
                    stmt,
                    17,
                    notaVenda.getDataVencimento()
            );

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            throw new IllegalStateException(
                    "Nota de Venda inserida, mas o ID gerado não foi retornado."
            );

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao inserir Nota de Venda no banco de dados.",
                    e
            );
        }
    }

    /**
     * Busca a fotografia principal pelo número/ID da Nota.
     *
     * Os itens são carregados separadamente pelo ItemNotaVendaDAO.
     */
    public NotaVenda buscarPorId(Connection conn, Integer notaId) {
        validarConnection(conn);
        validarIdPositivo(notaId, "ID da Nota de Venda");

        String sql = sqlConsultaBase() + " WHERE id_nota = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, notaId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                return mapearNotaVenda(rs);
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao buscar Nota de Venda por ID.",
                    e
            );
        }
    }

    /**
     * Busca a fotografia principal associada a uma venda.
     *
     * Retorna null para vendas legadas que não possuam NotaVenda.
     */
    public NotaVenda buscarPorVendaId(
            Connection conn,
            Integer vendaId
    ) {
        validarConnection(conn);
        validarIdPositivo(vendaId, "ID da venda");

        String sql = sqlConsultaBase() + " WHERE venda_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, vendaId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                return mapearNotaVenda(rs);
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao buscar Nota de Venda por venda.",
                    e
            );
        }
    }

    /**
     * Marca como ESTORNADA somente a Nota atualmente ATIVA da venda informada.
     *
     * Não existe transição inversa neste DAO. O retorno permite ao
     * EstornoVendaService exigir que exatamente uma Nota aplicável tenha sido
     * alterada dentro da mesma transação do estorno.
     */
    public boolean marcarComoEstornada(
            Connection conn,
            Integer vendaId
    ) {
        validarConnection(conn);
        validarIdPositivo(vendaId, "ID da venda");

        String sql = """
                UPDATE NotaVenda
                SET status = ?
                WHERE venda_id = ?
                  AND status = ?
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, StatusNotaVenda.ESTORNADA.name());
            stmt.setInt(2, vendaId);
            stmt.setString(3, StatusNotaVenda.ATIVA.name());

            int linhasAfetadas = stmt.executeUpdate();

            if (linhasAfetadas > 1) {
                throw new IllegalStateException(
                        "Mais de uma Nota de Venda foi atualizada para a mesma venda."
                );
            }

            return linhasAfetadas == 1;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao marcar Nota de Venda como estornada.",
                    e
            );
        }
    }

    private String sqlConsultaBase() {
        return """
                SELECT
                    id_nota,
                    venda_id,
                    status,
                    data_hora_venda,
                    tipo_venda,
                    forma_pagamento,
                    usuario_id,
                    nome_usuario,
                    cliente_id,
                    nome_cliente,
                    documento_cliente,
                    valor_total,
                    valor_desconto_global,
                    valor_recebido,
                    troco,
                    prazo_pagamento_id,
                    quantidade_dias_prazo,
                    data_vencimento
                FROM NotaVenda
                """;
    }

    private NotaVenda mapearNotaVenda(ResultSet rs) throws SQLException {
        Integer notaId = rs.getInt("id_nota");

        try {
            NotaVenda notaVenda = new NotaVenda();
            notaVenda.setIdNota(notaId);
            notaVenda.setVendaId(rs.getInt("venda_id"));
            notaVenda.setStatus(
                    StatusNotaVenda.valueOf(rs.getString("status"))
            );
            notaVenda.setDataHoraVenda(
                    LocalDateTime.parse(rs.getString("data_hora_venda"))
            );
            notaVenda.setTipoVenda(
                    TipoVenda.valueOf(rs.getString("tipo_venda"))
            );
            notaVenda.setFormaPagamento(
                    FormaPagamento.valueOf(rs.getString("forma_pagamento"))
            );
            notaVenda.setUsuarioId(rs.getInt("usuario_id"));
            notaVenda.setNomeUsuario(rs.getString("nome_usuario"));

            notaVenda.setClienteId(
                    obterIntegerOpcional(rs, "cliente_id")
            );
            notaVenda.setNomeCliente(rs.getString("nome_cliente"));
            notaVenda.setDocumentoCliente(
                    rs.getString("documento_cliente")
            );

            notaVenda.setValorTotal(rs.getBigDecimal("valor_total"));
            notaVenda.setValorDescontoGlobal(
                    rs.getBigDecimal("valor_desconto_global")
            );
            notaVenda.setValorRecebido(
                    rs.getBigDecimal("valor_recebido")
            );
            notaVenda.setTroco(rs.getBigDecimal("troco"));

            notaVenda.setPrazoPagamentoId(
                    obterIntegerOpcional(rs, "prazo_pagamento_id")
            );
            notaVenda.setQuantidadeDiasPrazo(
                    obterIntegerOpcional(rs, "quantidade_dias_prazo")
            );

            String dataVencimento = rs.getString("data_vencimento");
            notaVenda.setDataVencimento(
                    dataVencimento == null
                            ? null
                            : LocalDate.parse(dataVencimento)
            );

            return notaVenda;

        } catch (DateTimeException | IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Nota de Venda " + notaId
                            + " possui fotografia histórica inválida.",
                    e
            );
        }
    }

    private void validarConnection(Connection conn) {
        if (conn == null) {
            throw new IllegalArgumentException(
                    "Conexão não pode ser nula."
            );
        }
    }

    private void validarIdPositivo(Integer id, String nomeCampo) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    nomeCampo + " deve ser maior que zero."
            );
        }
    }

    private Integer obterIntegerOpcional(
            ResultSet rs,
            String nomeColuna
    ) throws SQLException {
        int valor = rs.getInt(nomeColuna);
        return rs.wasNull() ? null : valor;
    }

    private void definirIntegerOpcional(
            PreparedStatement stmt,
            int indice,
            Integer valor
    ) throws SQLException {
        if (valor == null) {
            stmt.setNull(indice, Types.INTEGER);
            return;
        }

        stmt.setInt(indice, valor);
    }

    private void definirStringOpcional(
            PreparedStatement stmt,
            int indice,
            String valor
    ) throws SQLException {
        if (valor == null) {
            stmt.setNull(indice, Types.VARCHAR);
            return;
        }

        stmt.setString(indice, valor);
    }

    private void definirBigDecimalOpcional(
            PreparedStatement stmt,
            int indice,
            BigDecimal valor
    ) throws SQLException {
        if (valor == null) {
            stmt.setNull(indice, Types.REAL);
            return;
        }

        stmt.setBigDecimal(indice, valor);
    }

    private void definirLocalDateOpcional(
            PreparedStatement stmt,
            int indice,
            LocalDate valor
    ) throws SQLException {
        if (valor == null) {
            stmt.setNull(indice, Types.VARCHAR);
            return;
        }

        stmt.setString(indice, valor.toString());
    }
}
