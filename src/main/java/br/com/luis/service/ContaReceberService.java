package br.com.luis.service;

import br.com.luis.dao.ContaReceberDAO;
import br.com.luis.dao.MovimentacaoFinanceiraDAO;
import br.com.luis.dao.UsuarioDAO;
import br.com.luis.dao.VendaDAO;
import br.com.luis.model.ContaReceber;
import br.com.luis.model.FormaPagamento;
import br.com.luis.model.MovimentacaoFinanceira;
import br.com.luis.model.OrigemMovimentacaoFinanceira;
import br.com.luis.model.StatusContaReceber;
import br.com.luis.model.StatusVenda;
import br.com.luis.model.TipoMovimentacaoFinanceira;
import br.com.luis.model.Usuario;
import br.com.luis.util.ConnectionFactory;
import br.com.luis.viewmodel.ResultadoRecebimentoConta;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

import br.com.luis.viewmodel.ContaReceberListagemView;

import java.time.LocalDate;
import java.util.List;

/**
 * Service responsável pelo recebimento integral de contas a receber.
 *
 * Valida os dados de entrada e, em uma única transação, reconsulta e revalida o
 * usuário executor persistido na mesma Connection antes de qualquer mutação.
 * Somente após a autorização, busca e revalida a conta, altera a conta de
 * PENDENTE para PAGA, atualiza a venda vinculada de PENDENTE para PAGA e registra
 * a movimentação financeira de entrada. Qualquer falha provoca rollback integral
 * do fluxo.
 *
 * O recebimento usa o valor integral persistido na conta e registra o usuário e
 * a forma de pagamento informados. Não oferece pagamento parcial, juros, multa,
 * desconto financeiro ou parcelamento. As regras permanecem neste Service; o
 * Controller coordena a interface e os DAOs executam somente a persistência.
 */
public class ContaReceberService {

    private final ContaReceberDAO contaReceberDAO;
    private final MovimentacaoFinanceiraDAO movimentacaoFinanceiraDAO;
    private final UsuarioDAO usuarioDAO;
    private final VendaDAO vendaDAO;

    public ContaReceberService() {
        this.contaReceberDAO = new ContaReceberDAO();
        this.movimentacaoFinanceiraDAO = new MovimentacaoFinanceiraDAO();
        this.usuarioDAO = new UsuarioDAO();
        this.vendaDAO = new VendaDAO();
    }

    /**
     * Recebe integralmente uma conta PENDENTE em uma única transação.
     *
     * Revalida no banco o administrador executor antes da primeira mutação, usa o
     * valor integral persistido na conta, atualiza conta e venda com proteção de
     * estado e registra a movimentação financeira de entrada.
     *
     * O resultado é devolvido somente após o commit. Falhas provocam rollback
     * integral e o autoCommit anterior é restaurado.
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
     * Executa as validações e mutações usando a Connection da transação.
     *
     * Revalida o administrador antes da primeira escrita, atualiza conta e venda
     * com proteção de estado e registra a entrada financeira. Não executa commit,
     * rollback nem fecha a Connection recebida.
     */
    private DadosRecebimentoConta receberContaTransacional(
            Connection conn,
            Integer contaReceberId,
            FormaPagamento formaPagamento,
            Integer usuarioId
    ) {

        buscarEValidarUsuarioAdministradorAtivo(
                conn,
                usuarioId
        );

        ContaReceber contaReceber = buscarEValidarContaPendente(
                conn,
                contaReceberId
        );

        LocalDateTime dataHoraRecebimento = LocalDateTime.now();

        atualizarContaParaPaga(
                conn,
                contaReceber
        );

        boolean vendaAtualizada = vendaDAO.atualizarStatus(
                conn,
                contaReceber.getVendaId(),
                StatusVenda.PENDENTE,
                StatusVenda.PAGA
        );

        if (!vendaAtualizada) {
            throw new IllegalStateException(
                    "Não foi possível atualizar a venda vinculada à conta a receber."
            );
        }

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
     * Busca e revalida o executor persistido antes de qualquer mutação.
     */
    private void buscarEValidarUsuarioAdministradorAtivo(
            Connection conn,
            Integer usuarioId
    ) {

        Usuario usuario = usuarioDAO.buscarPorId(
                conn,
                usuarioId
        );

        if (usuario == null
                || usuario.getIdUsuario() == null
                || usuario.getIdUsuario() <= 0
                || !usuarioId.equals(usuario.getIdUsuario())
                || !"ADMIN".equals(usuario.getPerfil())
                || !"ATIVO".equals(usuario.getStatus())
                || usuario.isTrocaSenhaObrigatoria()) {

            throw new IllegalStateException(
                    "Usuário não autorizado a receber contas a receber."
            );
        }
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
            throw new IllegalStateException(
                    "Usuário não autorizado a receber contas a receber."
            );
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
    /**
     * Lista as contas a receber pendentes para exibição na interface.
     *
     * Este método não inicia transação, pois realiza apenas consulta.
     * A indicação de conta vencida é calculada fora do DAO.
     *
     * @return lista de contas pendentes com indicação visual de vencimento.
     */
    public List<ContaReceberListagemView> listarContasPendentes() {

        try (Connection conn = ConnectionFactory.getConnection()) {

            List<ContaReceberListagemView> contasPendentes =
                    contaReceberDAO.listarPendentesComCliente(conn);

            LocalDate dataAtual = LocalDate.now();

            for (ContaReceberListagemView contaPendente : contasPendentes) {

                boolean vencida =
                        contaPendente.getDataVencimento() != null
                                && contaPendente.getDataVencimento().isBefore(dataAtual)
                                && contaPendente.getStatus() == StatusContaReceber.PENDENTE;

                contaPendente.setVencida(vencida);
            }

            return contasPendentes;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao listar contas a receber pendentes.",
                    e
            );
        }
    }
}
