package br.com.luis.service;

import br.com.luis.dao.ContaReceberDAO;
import br.com.luis.dao.MovimentacaoFinanceiraDAO;
import br.com.luis.model.ContaReceber;
import br.com.luis.model.FormaPagamento;
import br.com.luis.model.MovimentacaoFinanceira;
import br.com.luis.model.OrigemMovimentacaoFinanceira;
import br.com.luis.model.StatusContaReceber;
import br.com.luis.model.TipoMovimentacaoFinanceira;
import br.com.luis.util.ConnectionFactory;
import br.com.luis.viewmodel.ResultadoRecebimentoConta;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Camada de serviço responsável pelas regras de negócio
 * relacionadas ao recebimento de contas a receber.
 *
 * Nesta primeira versão, o serviço realiza somente recebimento integral.
 *
 * Não há:
 * - pagamento parcial;
 * - juros;
 * - multa;
 * - desconto;
 * - estorno.
 */
public class ContaReceberService {

    private final ContaReceberDAO contaReceberDAO;
    private final MovimentacaoFinanceiraDAO movimentacaoFinanceiraDAO;

    public ContaReceberService() {
        this.contaReceberDAO = new ContaReceberDAO();
        this.movimentacaoFinanceiraDAO = new MovimentacaoFinanceiraDAO();
    }

    /**
     * Recebe integralmente uma conta a receber.
     *
     * O valor recebido sempre será o valor total da ContaReceber.
     * Os dados financeiros do recebimento são registrados em MovimentacaoFinanceira.
     *
     * @param contaReceberId ID da conta a receber.
     * @param formaPagamento forma de pagamento usada no recebimento.
     * @param usuarioId ID do usuário responsável pelo recebimento.
     * @return resultado do recebimento integral da conta.
     */
    public ResultadoRecebimentoConta receberConta(
            Integer contaReceberId,
            FormaPagamento formaPagamento,
            Integer usuarioId
    ) {

        validarDadosBasicosRecebimento(
                contaReceberId,
                formaPagamento,
                usuarioId
        );

        try (Connection conn = ConnectionFactory.getConnection()) {
            boolean autoCommitAnterior = conn.getAutoCommit();

            try {
                conn.setAutoCommit(false);

                DadosRecebimentoConta dadosRecebimento = receberContaTransacional(
                        conn,
                        contaReceberId,
                        formaPagamento,
                        usuarioId
                );

                conn.commit();

                return montarResultadoRecebimentoConta(dadosRecebimento);

            } catch (RuntimeException e) {
                executarRollbackSeguro(conn);
                throw e;

            } catch (SQLException e) {
                executarRollbackSeguro(conn);
                throw new RuntimeException("Erro ao receber conta a receber.", e);

            } finally {
                restaurarAutoCommitSeguro(conn, autoCommitAnterior);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao receber conta a receber.", e);
        }
    }

    /**
     * Executa o fluxo transacional do recebimento integral.
     *
     * Este método não executa commit, rollback e não fecha a Connection.
     */
    private DadosRecebimentoConta receberContaTransacional(
            Connection conn,
            Integer contaReceberId,
            FormaPagamento formaPagamento,
            Integer usuarioId
    ) {

        ContaReceber contaReceber = buscarEValidarContaPendente(
                conn,
                contaReceberId
        );

        LocalDateTime dataHoraRecebimento = LocalDateTime.now();

        atualizarContaParaPaga(
                conn,
                contaReceber
        );

        MovimentacaoFinanceira movimentacaoFinanceira = montarMovimentacaoFinanceiraRecebimento(
                contaReceber,
                formaPagamento,
                usuarioId,
                dataHoraRecebimento
        );

        Integer movimentacaoFinanceiraId = persistirMovimentacaoFinanceira(
                conn,
                movimentacaoFinanceira
        );

        return new DadosRecebimentoConta(
                contaReceber.getIdConta(),
                movimentacaoFinanceiraId,
                contaReceber.getVendaId(),
                contaReceber.getValor(),
                formaPagamento,
                dataHoraRecebimento
        );
    }

    /**
     * Busca e valida se a conta existe e está pendente.
     *
     * Este método não altera o objeto ContaReceber em memória.
     */
    private ContaReceber buscarEValidarContaPendente(
            Connection conn,
            Integer contaReceberId
    ) {

        ContaReceber contaReceber = contaReceberDAO.buscarPorId(
                conn,
                contaReceberId
        );

        if (contaReceber == null) {
            throw new IllegalArgumentException("Conta a receber não encontrada.");
        }

        if (contaReceber.getIdConta() == null || contaReceber.getIdConta() <= 0) {
            throw new IllegalStateException("Conta a receber possui ID inválido.");
        }

        if (contaReceber.getStatus() == null) {
            throw new IllegalStateException("Conta a receber sem status.");
        }

        if (contaReceber.getStatus() == StatusContaReceber.PAGA) {
            throw new IllegalStateException("Esta conta a receber já foi paga.");
        }

        if (contaReceber.getStatus() != StatusContaReceber.PENDENTE) {
            throw new IllegalStateException("A conta a receber não está pendente.");
        }

        if (contaReceber.getValor() == null || contaReceber.getValor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Valor da conta a receber é inválido.");
        }

        if (contaReceber.getVendaId() == null || contaReceber.getVendaId() <= 0) {
            throw new IllegalStateException("Venda vinculada à conta a receber é inválida.");
        }

        return contaReceber;
    }

    /**
     * Atualiza a conta para PAGA usando proteção de estado.
     *
     * Este método não altera o objeto ContaReceber em memória.
     */
    private void atualizarContaParaPaga(
            Connection conn,
            ContaReceber contaReceber
    ) {

        boolean atualizada = contaReceberDAO.atualizarStatus(
                conn,
                contaReceber.getIdConta(),
                StatusContaReceber.PENDENTE,
                StatusContaReceber.PAGA
        );

        if (!atualizada) {
            throw new IllegalStateException(
                    "Não foi possível receber a conta. Ela pode já ter sido alterada."
            );
        }
    }

    /**
     * Monta a movimentação financeira referente ao recebimento integral.
     */
    private MovimentacaoFinanceira montarMovimentacaoFinanceiraRecebimento(
            ContaReceber contaReceber,
            FormaPagamento formaPagamento,
            Integer usuarioId,
            LocalDateTime dataHoraRecebimento
    ) {

        return new MovimentacaoFinanceira(
                null,
                dataHoraRecebimento,
                TipoMovimentacaoFinanceira.ENTRADA,
                OrigemMovimentacaoFinanceira.RECEBIMENTO_CONTA,
                formaPagamento,
                contaReceber.getValor(),
                contaReceber.getVendaId(),
                contaReceber.getIdConta(),
                usuarioId
        );
    }

    /**
     * Persiste a movimentação financeira dentro da transação.
     */
    private Integer persistirMovimentacaoFinanceira(
            Connection conn,
            MovimentacaoFinanceira movimentacaoFinanceira
    ) {

        Integer movimentacaoFinanceiraId = movimentacaoFinanceiraDAO.inserir(
                conn,
                movimentacaoFinanceira
        );

        if (movimentacaoFinanceiraId == null || movimentacaoFinanceiraId <= 0) {
            throw new IllegalStateException("Não foi possível obter o ID da movimentação financeira.");
        }

        return movimentacaoFinanceiraId;
    }

    /**
     * Monta o ViewModel de resultado após o commit da transação.
     */
    private ResultadoRecebimentoConta montarResultadoRecebimentoConta(
            DadosRecebimentoConta dados
    ) {

        return new ResultadoRecebimentoConta(
                dados.getContaReceberId(),
                dados.getMovimentacaoFinanceiraId(),
                dados.getVendaId(),
                dados.getValorRecebido(),
                dados.getFormaPagamento(),
                dados.getDataHoraRecebimento(),
                StatusContaReceber.PAGA
        );
    }

    /**
     * Valida os dados básicos necessários para iniciar
     * o recebimento integral de uma conta a receber.
     *
     * Esta validação não acessa banco de dados e não abre conexão.
     */
    private void validarDadosBasicosRecebimento(
            Integer contaReceberId,
            FormaPagamento formaPagamento,
            Integer usuarioId
    ) {

        if (contaReceberId == null || contaReceberId <= 0) {
            throw new IllegalArgumentException("Conta a receber inválida para recebimento.");
        }

        if (formaPagamento == null) {
            throw new IllegalArgumentException("Forma de pagamento é obrigatória para recebimento.");
        }

        if (formaPagamento == FormaPagamento.A_PRAZO) {
            throw new IllegalArgumentException("Forma de pagamento A_PRAZO não é permitida para recebimento.");
        }

        if (formaPagamento != FormaPagamento.DINHEIRO
                && formaPagamento != FormaPagamento.PIX
                && formaPagamento != FormaPagamento.CARTAO) {
            throw new IllegalArgumentException("Forma de pagamento inválida para recebimento.");
        }

        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException("Usuário inválido para recebimento.");
        }
    }

    /**
     * Executa rollback de forma segura.
     *
     * Caso o rollback falhe, o erro é apenas registrado para não substituir
     * a exceção original do fluxo.
     */
    private void executarRollbackSeguro(Connection conn) {

        if (conn == null) {
            return;
        }

        try {
            conn.rollback();
        } catch (SQLException e) {
            System.err.println("Erro ao executar rollback do recebimento: " + e.getMessage());
        }
    }

    /**
     * Restaura o autoCommit da conexão de forma segura.
     *
     * Caso a restauração falhe, o erro é apenas registrado para não substituir
     * a exceção original do fluxo.
     */
    private void restaurarAutoCommitSeguro(
            Connection conn,
            boolean autoCommitAnterior
    ) {

        if (conn == null) {
            return;
        }

        try {
            conn.setAutoCommit(autoCommitAnterior);
        } catch (SQLException e) {
            System.err.println("Erro ao restaurar autoCommit do recebimento: " + e.getMessage());
        }
    }

    /**
     * Estrutura auxiliar interna para transportar os dados necessários
     * para montar o resultado do recebimento após o commit.
     */
    private static class DadosRecebimentoConta {

        private final Integer contaReceberId;
        private final Integer movimentacaoFinanceiraId;
        private final Integer vendaId;
        private final BigDecimal valorRecebido;
        private final FormaPagamento formaPagamento;
        private final LocalDateTime dataHoraRecebimento;

        private DadosRecebimentoConta(
                Integer contaReceberId,
                Integer movimentacaoFinanceiraId,
                Integer vendaId,
                BigDecimal valorRecebido,
                FormaPagamento formaPagamento,
                LocalDateTime dataHoraRecebimento
        ) {
            this.contaReceberId = contaReceberId;
            this.movimentacaoFinanceiraId = movimentacaoFinanceiraId;
            this.vendaId = vendaId;
            this.valorRecebido = valorRecebido;
            this.formaPagamento = formaPagamento;
            this.dataHoraRecebimento = dataHoraRecebimento;
        }

        private Integer getContaReceberId() {
            return contaReceberId;
        }

        private Integer getMovimentacaoFinanceiraId() {
            return movimentacaoFinanceiraId;
        }

        private Integer getVendaId() {
            return vendaId;
        }

        private BigDecimal getValorRecebido() {
            return valorRecebido;
        }

        private FormaPagamento getFormaPagamento() {
            return formaPagamento;
        }

        private LocalDateTime getDataHoraRecebimento() {
            return dataHoraRecebimento;
        }
    }
}