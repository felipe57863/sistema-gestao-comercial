package br.com.luis.service;

import br.com.luis.dao.AuditoriaEstornoVendaDAO;
import br.com.luis.dao.ContaReceberDAO;
import br.com.luis.dao.ItemVendaDAO;
import br.com.luis.dao.MovimentacaoFinanceiraDAO;
import br.com.luis.dao.ProdutoDAO;
import br.com.luis.dao.UsuarioDAO;
import br.com.luis.dao.VendaDAO;
import br.com.luis.model.AuditoriaEstornoVenda;
import br.com.luis.model.ContaReceber;
import br.com.luis.model.FormaPagamento;
import br.com.luis.model.ItemVenda;
import br.com.luis.model.MovimentacaoFinanceira;
import br.com.luis.model.OrigemMovimentacaoFinanceira;
import br.com.luis.model.StatusContaReceber;
import br.com.luis.model.StatusVenda;
import br.com.luis.model.TipoMovimentacaoFinanceira;
import br.com.luis.model.TipoVenda;
import br.com.luis.model.Usuario;
import br.com.luis.model.Venda;
import br.com.luis.util.ConnectionFactory;
import br.com.luis.viewmodel.ResultadoEstornoVenda;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Camada de serviço responsável pelo estorno total de vendas finalizadas.
 *
 * O estorno é executado em uma única transação e reverte os efeitos
 * comerciais, financeiros e de estoque da venda, além de persistir
 * o registro de auditoria correspondente.
 *
 * Regras principais:
 * - somente usuário ADMIN e ATIVO pode realizar o estorno;
 * - o estorno é sempre total;
 * - os itens da venda são integralmente devolvidos ao estoque;
 * - a venda é finalizada com status ESTORNADA;
 * - a conta a receber vinculada é cancelada, quando aplicável;
 * - movimentações financeiras anteriores permanecem imutáveis;
 * - uma nova movimentação de saída é criada quando existe entrada anterior;
 * - o registro persistente de auditoria é obrigatório;
 * - uma venda não pode ser estornada mais de uma vez.
 */
public class EstornoVendaService {

    private final VendaDAO vendaDAO;
    private final ItemVendaDAO itemVendaDAO;
    private final ProdutoDAO produtoDAO;
    private final ContaReceberDAO contaReceberDAO;
    private final MovimentacaoFinanceiraDAO movimentacaoFinanceiraDAO;
    private final UsuarioDAO usuarioDAO;
    private final AuditoriaEstornoVendaDAO auditoriaEstornoVendaDAO;

    public EstornoVendaService() {
        this.vendaDAO = new VendaDAO();
        this.itemVendaDAO = new ItemVendaDAO();
        this.produtoDAO = new ProdutoDAO();
        this.contaReceberDAO = new ContaReceberDAO();
        this.movimentacaoFinanceiraDAO =
                new MovimentacaoFinanceiraDAO();
        this.usuarioDAO = new UsuarioDAO();
        this.auditoriaEstornoVendaDAO =
                new AuditoriaEstornoVendaDAO();
    }

    /**
     * Realiza o estorno total de uma venda finalizada.
     *
     * Abre e controla uma única Connection para revalidar o usuário, a venda, os
     * itens, a conta e as movimentações persistidas. Dentro da mesma transação,
     * restaura o estoque, altera a venda para ESTORNADA, trata a conta vinculada
     * conforme o cenário, registra eventual saída compensatória e grava a
     * auditoria. O commit ocorre somente após todas as etapas; qualquer falha
     * provoca rollback integral e o estado anterior de autoCommit é restaurado.
     *
     * @param vendaId identificador da venda que será estornada.
     * @param motivo motivo obrigatório do estorno.
     * @param usuarioId identificador do usuário responsável pelo estorno.
     * @return resultado consolidado do estorno.
     */
    public ResultadoEstornoVenda estornarVenda(
            Integer vendaId,
            String motivo,
            Integer usuarioId
    ) {

        validarDadosBasicosEstorno(
                vendaId,
                motivo,
                usuarioId
        );

        DadosEstornoConcluido dados;

        try (Connection conn = ConnectionFactory.getConnection()) {
            boolean autoCommitAnterior = conn.getAutoCommit();

            try {
                conn.setAutoCommit(false);

                dados = estornarVendaTransacional(
                        conn,
                        vendaId,
                        motivo.trim(),
                        usuarioId
                );

                conn.commit();

            } catch (RuntimeException e) {
                executarRollbackSeguro(conn);
                throw e;

            } catch (SQLException e) {
                executarRollbackSeguro(conn);

                throw new RuntimeException(
                        "Erro ao realizar o estorno da venda.",
                        e
                );

            } finally {
                restaurarAutoCommitSeguro(
                        conn,
                        autoCommitAnterior
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao realizar o estorno da venda.",
                    e
            );
        }

        return montarResultadoEstorno(dados);
    }

    /**
     * Executa o fluxo completo do estorno dentro da transação.
     *
     * Usa a Connection recebida para executar as validações e alterações de venda,
     * itens, estoque, conta, movimentações e auditoria. Não abre outra conexão,
     * não executa commit ou rollback e não fecha a Connection; essas
     * responsabilidades pertencem ao método público que delimita a transação.
     *
     * @return dados internos necessários para montar o resultado
     *         depois do commit.
     */
    private DadosEstornoConcluido estornarVendaTransacional(
            Connection conn,
            Integer vendaId,
            String motivo,
            Integer usuarioId
    ) {

        buscarEValidarUsuarioAdministradorAtivo(
                conn,
                usuarioId
        );

        Venda venda = buscarEValidarVendaParaEstorno(
                conn,
                vendaId
        );

        validarAusenciaAuditoriaAnterior(
                conn,
                venda.getIdVenda()
        );

        List<ItemVenda> itensVenda =
                buscarEValidarItensVenda(
                        conn,
                        venda.getIdVenda()
                );

        ContaReceber contaReceber =
                buscarEValidarContaReceber(
                        conn,
                        venda
                );

        MovimentacaoFinanceira movimentacaoOriginal =
                buscarEValidarMovimentacaoFinanceiraOriginal(
                        conn,
                        venda,
                        contaReceber
                );

        CenarioEstornoVenda cenarioEstorno =
                classificarCenarioEstornoVenda(
                        venda,
                        contaReceber,
                        movimentacaoOriginal
                );

        TipoVenda tipoVenda =
                converterTipoVendaPersistido(
                        venda.getTipoVenda()
                );

        StatusVenda statusVendaAnterior =
                converterStatusVendaPersistido(
                        venda.getStatus()
                );

        Integer contaReceberId =
                contaReceber == null
                        ? null
                        : contaReceber.getIdConta();

        StatusContaReceber statusContaReceberAnterior =
                contaReceber == null
                        ? null
                        : contaReceber.getStatus();

        Integer movimentacaoFinanceiraOriginalId =
                movimentacaoOriginal == null
                        ? null
                        : movimentacaoOriginal.getIdMovimentacao();

        LocalDateTime dataHoraEstorno =
                LocalDateTime.now();

        atualizarVendaParaEstornada(
                conn,
                venda
        );

        cancelarContaReceberDeFormaProtegida(
                conn,
                contaReceber
        );

        DadosRestauracaoEstoque dadosRestauracaoEstoque =
                restaurarEstoqueItens(
                        conn,
                        itensVenda
                );

        BigDecimal valorSaida =
                BigDecimal.ZERO.setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        Integer movimentacaoFinanceiraSaidaId = null;

        if (cenarioEstorno
                != CenarioEstornoVenda.VENDA_A_PRAZO_PENDENTE) {

            valorSaida =
                    movimentacaoOriginal.getValor().setScale(
                            2,
                            RoundingMode.HALF_UP
                    );

            MovimentacaoFinanceira movimentacaoSaida =
                    montarMovimentacaoFinanceiraSaida(
                            cenarioEstorno,
                            venda,
                            contaReceber,
                            movimentacaoOriginal,
                            usuarioId,
                            dataHoraEstorno,
                            valorSaida
                    );

            movimentacaoFinanceiraSaidaId =
                    movimentacaoFinanceiraDAO.inserir(
                            conn,
                            movimentacaoSaida
                    );

            if (movimentacaoFinanceiraSaidaId == null
                    || movimentacaoFinanceiraSaidaId <= 0) {
                throw new IllegalStateException(
                        "Não foi possível identificar a movimentação "
                                + "financeira de saída do estorno."
                );
            }
        }

        AuditoriaEstornoVenda auditoria =
                new AuditoriaEstornoVenda(
                        null,
                        venda.getIdVenda(),
                        usuarioId,
                        dataHoraEstorno,
                        motivo,
                        statusVendaAnterior,
                        contaReceberId,
                        statusContaReceberAnterior,
                        movimentacaoFinanceiraOriginalId,
                        movimentacaoFinanceiraSaidaId
                );

        int auditoriaId =
                auditoriaEstornoVendaDAO.inserir(
                        conn,
                        auditoria
                );

        if (auditoriaId <= 0) {
            throw new IllegalStateException(
                    "Não foi possível identificar a auditoria do estorno."
            );
        }

        return new DadosEstornoConcluido(
                venda.getIdVenda(),
                tipoVenda,
                StatusVenda.ESTORNADA,
                contaReceberId,
                contaReceber == null
                        ? null
                        : StatusContaReceber.CANCELADA,
                movimentacaoFinanceiraOriginalId,
                movimentacaoFinanceiraSaidaId,
                valorSaida,
                auditoriaId,
                dataHoraEstorno,
                dadosRestauracaoEstoque
                        .quantidadeDeItensRestaurados(),
                dadosRestauracaoEstoque
                        .quantidadeTotalDeUnidadesRestauradas()
        );
    }

    /**
     * Busca e valida o usuário responsável pelo estorno.
     *
     * O usuário deve existir no banco, estar ativo e possuir perfil ADMIN.
     * A consulta participa da mesma transação do estorno.
     *
     * @param conn conexão controlada pelo Service.
     * @param usuarioId identificador do usuário responsável.
     * @return usuário administrador ativo validado.
     */
    private Usuario buscarEValidarUsuarioAdministradorAtivo(
            Connection conn,
            Integer usuarioId
    ) {

        Usuario usuario = usuarioDAO.buscarPorId(
                conn,
                usuarioId
        );

        if (usuario == null) {
            throw new IllegalArgumentException(
                    "Usuário responsável pelo estorno não encontrado."
            );
        }

        if (usuario.getIdUsuario() == null
                || usuario.getIdUsuario() <= 0
                || !usuarioId.equals(usuario.getIdUsuario())) {
            throw new IllegalStateException(
                    "Usuário responsável pelo estorno possui ID inconsistente."
            );
        }

        if (!"ATIVO".equals(usuario.getStatus())) {
            throw new IllegalStateException(
                    "Usuário inativo não pode realizar estorno de venda."
            );
        }

        if (!"ADMIN".equals(usuario.getPerfil())) {
            throw new IllegalStateException(
                    "Somente usuário com perfil ADMIN pode realizar "
                            + "estorno de venda."
            );
        }

        return usuario;
    }

    /**
     * Busca e valida os dados básicos persistidos da venda.
     *
     * Esta etapa confirma a existência, o identificador, o tipo, o status,
     * a forma de pagamento e o valor total da venda.
     *
     * @param conn conexão controlada pelo Service.
     * @param vendaId identificador da venda.
     * @return venda encontrada e validada.
     */
    private Venda buscarEValidarVendaParaEstorno(
            Connection conn,
            Integer vendaId
    ) {

        Venda venda = vendaDAO.buscarPorId(
                conn,
                vendaId
        );

        if (venda == null) {
            throw new IllegalArgumentException(
                    "Venda não encontrada para estorno."
            );
        }

        if (venda.getIdVenda() == null
                || venda.getIdVenda() <= 0
                || !vendaId.equals(venda.getIdVenda())) {
            throw new IllegalStateException(
                    "Venda encontrada possui ID inconsistente."
            );
        }

        TipoVenda tipoVenda =
                converterTipoVendaPersistido(
                        venda.getTipoVenda()
                );

        StatusVenda statusVenda =
                converterStatusVendaPersistido(
                        venda.getStatus()
                );

        FormaPagamento formaPagamento =
                converterFormaPagamentoPersistida(
                        venda.getFormaPagamento()
                );

        if (tipoVenda == TipoVenda.A_VISTA
                && formaPagamento == FormaPagamento.A_PRAZO) {
            throw new IllegalStateException(
                    "Venda à vista não pode possuir forma de pagamento "
                            + "A_PRAZO."
            );
        }

        if (tipoVenda == TipoVenda.A_PRAZO
                && formaPagamento != FormaPagamento.A_PRAZO) {
            throw new IllegalStateException(
                    "Venda a prazo deve possuir forma de pagamento A_PRAZO."
            );
        }

        if (statusVenda == StatusVenda.ESTORNADA) {
            throw new IllegalStateException(
                    "Esta venda já foi estornada."
            );
        }

        if (statusVenda != StatusVenda.PAGA
                && statusVenda != StatusVenda.PENDENTE) {
            throw new IllegalStateException(
                    "A venda não possui status permitido para estorno."
            );
        }

        if (tipoVenda == TipoVenda.A_VISTA
                && statusVenda != StatusVenda.PAGA) {
            throw new IllegalStateException(
                    "Venda à vista deve estar PAGA para ser estornada."
            );
        }

        if (venda.getValorTotal() == null
                || venda.getValorTotal().compareTo(
                BigDecimal.ZERO
        ) <= 0) {
            throw new IllegalStateException(
                    "Venda possui valor total inválido para estorno."
            );
        }

        return venda;
    }

    /**
     * Confirma que a venda ainda não possui auditoria de estorno.
     *
     * A consulta participa da mesma transação. A existência de uma auditoria
     * anterior bloqueia uma nova tentativa de estorno, mesmo que outro dado
     * persistido esteja inconsistente.
     *
     * @param conn conexão controlada pelo Service.
     * @param vendaId identificador da venda.
     */
    private void validarAusenciaAuditoriaAnterior(
            Connection conn,
            Integer vendaId
    ) {

        AuditoriaEstornoVenda auditoriaExistente =
                auditoriaEstornoVendaDAO.buscarPorVendaId(
                        conn,
                        vendaId
                );

        if (auditoriaExistente != null) {
            throw new IllegalStateException(
                    "Esta venda já possui auditoria de estorno "
                            + "e não pode ser estornada novamente."
            );
        }
    }

    /**
     * Busca e valida os itens persistidos da venda.
     *
     * Uma venda finalizada deve possuir ao menos um item válido.
     *
     * @param conn conexão controlada pelo Service.
     * @param vendaId identificador da venda.
     * @return itens persistidos e validados.
     */
    private List<ItemVenda> buscarEValidarItensVenda(
            Connection conn,
            Integer vendaId
    ) {

        List<ItemVenda> itens =
                itemVendaDAO.listarPorVendaId(
                        conn,
                        vendaId
                );

        if (itens == null || itens.isEmpty()) {
            throw new IllegalStateException(
                    "A venda não possui itens para restauração do estoque."
            );
        }

        for (ItemVenda item : itens) {

            if (item == null) {
                throw new IllegalStateException(
                        "A venda possui item nulo ou inconsistente."
                );
            }

            if (item.getIdItem() == null
                    || item.getIdItem() <= 0) {
                throw new IllegalStateException(
                        "A venda possui item com ID inválido."
                );
            }

            if (item.getVendaId() == null
                    || item.getVendaId() <= 0
                    || !vendaId.equals(item.getVendaId())) {
                throw new IllegalStateException(
                        "A venda possui item com vínculo inconsistente."
                );
            }

            if (item.getProdutoId() == null
                    || item.getProdutoId() <= 0) {
                throw new IllegalStateException(
                        "A venda possui item com produto inválido."
                );
            }

            if (item.getQuantidade() == null
                    || item.getQuantidade() <= 0) {
                throw new IllegalStateException(
                        "A venda possui item com quantidade inválida."
                );
            }
        }

        return itens;
    }

    /**
     * Busca e valida a conta a receber vinculada à venda.
     *
     * Regras:
     * - venda à vista não pode possuir conta a receber;
     * - venda a prazo deve possuir exatamente uma conta;
     * - venda PENDENTE exige conta PENDENTE;
     * - venda PAGA a prazo exige conta PAGA;
     * - valor e vínculos da conta devem ser coerentes com a venda.
     *
     * @param conn conexão controlada pelo Service.
     * @param venda venda persistida e previamente validada.
     * @return conta validada para venda a prazo ou null para venda à vista.
     */
    private ContaReceber buscarEValidarContaReceber(
            Connection conn,
            Venda venda
    ) {

        TipoVenda tipoVenda =
                converterTipoVendaPersistido(
                        venda.getTipoVenda()
                );

        StatusVenda statusVenda =
                converterStatusVendaPersistido(
                        venda.getStatus()
                );

        ContaReceber contaReceber =
                contaReceberDAO.buscarPorVendaId(
                        conn,
                        venda.getIdVenda()
                );

        if (tipoVenda == TipoVenda.A_VISTA) {

            if (contaReceber != null) {
                throw new IllegalStateException(
                        "Venda à vista possui conta a receber vinculada."
                );
            }

            return null;
        }

        if (tipoVenda != TipoVenda.A_PRAZO) {
            throw new IllegalStateException(
                    "Tipo da venda não é compatível com o estorno."
            );
        }

        if (contaReceber == null) {
            throw new IllegalStateException(
                    "Venda a prazo não possui conta a receber vinculada."
            );
        }

        if (contaReceber.getIdConta() == null
                || contaReceber.getIdConta() <= 0) {
            throw new IllegalStateException(
                    "Conta a receber vinculada possui ID inválido."
            );
        }

        if (contaReceber.getVendaId() == null
                || contaReceber.getVendaId() <= 0
                || !venda.getIdVenda().equals(
                contaReceber.getVendaId()
        )) {
            throw new IllegalStateException(
                    "Conta a receber possui vínculo inconsistente "
                            + "com a venda."
            );
        }

        if (contaReceber.getStatus() == null) {
            throw new IllegalStateException(
                    "Conta a receber vinculada não possui status."
            );
        }

        if (contaReceber.getValor() == null
                || contaReceber.getValor().compareTo(
                BigDecimal.ZERO
        ) <= 0) {
            throw new IllegalStateException(
                    "Conta a receber vinculada possui valor inválido."
            );
        }

        if (contaReceber.getValor().compareTo(
                venda.getValorTotal()
        ) != 0) {
            throw new IllegalStateException(
                    "Valor da conta a receber é diferente "
                            + "do valor total da venda."
            );
        }

        if (venda.getClienteId() == null
                || venda.getClienteId() <= 0) {
            throw new IllegalStateException(
                    "Venda a prazo não possui cliente válido."
            );
        }

        if (contaReceber.getClienteId() == null
                || contaReceber.getClienteId() <= 0
                || !venda.getClienteId().equals(
                contaReceber.getClienteId()
        )) {
            throw new IllegalStateException(
                    "Conta a receber possui cliente inconsistente "
                            + "com a venda."
            );
        }

        if (statusVenda == StatusVenda.PENDENTE
                && contaReceber.getStatus()
                != StatusContaReceber.PENDENTE) {
            throw new IllegalStateException(
                    "Venda PENDENTE deve possuir conta a receber PENDENTE."
            );
        }

        if (statusVenda == StatusVenda.PAGA
                && contaReceber.getStatus()
                != StatusContaReceber.PAGA) {
            throw new IllegalStateException(
                    "Venda a prazo PAGA deve possuir conta a receber PAGA."
            );
        }

        return contaReceber;
    }

    /**
     * Busca e valida a movimentação financeira original da venda.
     *
     * Regras:
     * - venda a prazo PENDENTE não pode possuir movimentação financeira;
     * - venda à vista PAGA deve possuir exatamente uma entrada VENDA_A_VISTA;
     * - venda a prazo PAGA deve possuir exatamente uma entrada
     *   RECEBIMENTO_CONTA;
     * - nenhuma saída ou origem de estorno anterior é permitida;
     * - valor, forma de pagamento e vínculos devem ser coerentes.
     *
     * @param conn conexão controlada pelo Service.
     * @param venda venda persistida e previamente validada.
     * @param contaReceber conta validada ou null para venda à vista.
     * @return movimentação original validada ou null para venda
     *         a prazo ainda pendente.
     */
    private MovimentacaoFinanceira
    buscarEValidarMovimentacaoFinanceiraOriginal(
            Connection conn,
            Venda venda,
            ContaReceber contaReceber
    ) {

        TipoVenda tipoVenda =
                converterTipoVendaPersistido(
                        venda.getTipoVenda()
                );

        StatusVenda statusVenda =
                converterStatusVendaPersistido(
                        venda.getStatus()
                );

        List<MovimentacaoFinanceira> movimentacoes =
                movimentacaoFinanceiraDAO.listarPorVendaId(
                        conn,
                        venda.getIdVenda()
                );

        if (movimentacoes == null) {
            throw new IllegalStateException(
                    "A consulta de movimentações financeiras "
                            + "retornou resultado nulo."
            );
        }

        for (MovimentacaoFinanceira movimentacao : movimentacoes) {

            if (movimentacao == null) {
                throw new IllegalStateException(
                        "A venda possui movimentação financeira nula."
                );
            }

            if (movimentacao.getTipo()
                    == TipoMovimentacaoFinanceira.SAIDA
                    || movimentacao.getOrigem()
                    == OrigemMovimentacaoFinanceira
                    .ESTORNO_VENDA_A_VISTA
                    || movimentacao.getOrigem()
                    == OrigemMovimentacaoFinanceira
                    .ESTORNO_RECEBIMENTO_CONTA) {

                throw new IllegalStateException(
                        "A venda já possui movimentação financeira "
                                + "de estorno."
                );
            }
        }

        if (tipoVenda == TipoVenda.A_PRAZO
                && statusVenda == StatusVenda.PENDENTE) {

            if (!movimentacoes.isEmpty()) {
                throw new IllegalStateException(
                        "Venda a prazo PENDENTE não pode possuir "
                                + "movimentação financeira."
                );
            }

            return null;
        }

        if (movimentacoes.size() != 1) {
            throw new IllegalStateException(
                    "Venda paga deve possuir exatamente uma "
                            + "movimentação financeira original."
            );
        }

        MovimentacaoFinanceira movimentacaoOriginal =
                movimentacoes.get(0);

        validarDadosBasicosMovimentacaoOriginal(
                movimentacaoOriginal,
                venda
        );

        if (tipoVenda == TipoVenda.A_VISTA) {
            validarMovimentacaoOriginalVendaAVista(
                    movimentacaoOriginal,
                    venda,
                    contaReceber
            );

            return movimentacaoOriginal;
        }

        if (tipoVenda == TipoVenda.A_PRAZO
                && statusVenda == StatusVenda.PAGA) {

            validarMovimentacaoOriginalRecebimentoConta(
                    movimentacaoOriginal,
                    venda,
                    contaReceber
            );

            return movimentacaoOriginal;
        }

        throw new IllegalStateException(
                "Cenário financeiro da venda não é compatível "
                        + "com o estorno."
        );
    }

    /**
     * Classifica o estorno em um dos três cenários oficiais.
     *
     * Qualquer combinação diferente da matriz esperada interrompe
     * integralmente a transação.
     *
     * @param venda venda persistida e validada.
     * @param contaReceber conta validada ou null para venda à vista.
     * @param movimentacaoOriginal entrada financeira original ou null
     *                             para venda a prazo ainda pendente.
     * @return cenário oficial identificado.
     */
    private CenarioEstornoVenda classificarCenarioEstornoVenda(
            Venda venda,
            ContaReceber contaReceber,
            MovimentacaoFinanceira movimentacaoOriginal
    ) {

        TipoVenda tipoVenda =
                converterTipoVendaPersistido(
                        venda.getTipoVenda()
                );

        StatusVenda statusVenda =
                converterStatusVendaPersistido(
                        venda.getStatus()
                );

        if (tipoVenda == TipoVenda.A_VISTA
                && statusVenda == StatusVenda.PAGA
                && contaReceber == null
                && movimentacaoOriginal != null) {

            return CenarioEstornoVenda.VENDA_A_VISTA_PAGA;
        }

        if (tipoVenda == TipoVenda.A_PRAZO
                && statusVenda == StatusVenda.PENDENTE
                && contaReceber != null
                && contaReceber.getStatus()
                == StatusContaReceber.PENDENTE
                && movimentacaoOriginal == null) {

            return CenarioEstornoVenda.VENDA_A_PRAZO_PENDENTE;
        }

        if (tipoVenda == TipoVenda.A_PRAZO
                && statusVenda == StatusVenda.PAGA
                && contaReceber != null
                && contaReceber.getStatus()
                == StatusContaReceber.PAGA
                && movimentacaoOriginal != null) {

            return CenarioEstornoVenda.VENDA_A_PRAZO_PAGA;
        }

        throw new IllegalStateException(
                "Os dados persistidos não correspondem a um "
                        + "cenário válido de estorno."
        );
    }

    /**
     * Atualiza a venda para ESTORNADA de forma protegida.
     *
     * A atualização somente é realizada se o status persistido ainda
     * corresponder ao status anteriormente carregado e validado.
     *
     * @param conn conexão controlada pelo Service.
     * @param venda venda persistida e previamente validada.
     */
    private void atualizarVendaParaEstornada(
            Connection conn,
            Venda venda
    ) {

        StatusVenda statusVendaAnterior =
                converterStatusVendaPersistido(
                        venda.getStatus()
                );

        boolean vendaAtualizada =
                vendaDAO.atualizarStatus(
                        conn,
                        venda.getIdVenda(),
                        statusVendaAnterior,
                        StatusVenda.ESTORNADA
                );

        if (!vendaAtualizada) {
            throw new IllegalStateException(
                    "A venda foi alterada por outra operação "
                            + "e não pôde ser marcada como ESTORNADA."
            );
        }
    }

    /**
     * Cancela de forma protegida a conta a receber vinculada à venda.
     *
     * Venda à vista não possui conta e, nesse caso, nenhuma atualização
     * é executada.
     *
     * @param conn conexão controlada pelo Service.
     * @param contaReceber conta validada ou null para venda à vista.
     */
    private void cancelarContaReceberDeFormaProtegida(
            Connection conn,
            ContaReceber contaReceber
    ) {

        if (contaReceber == null) {
            return;
        }

        StatusContaReceber statusContaAnterior =
                contaReceber.getStatus();

        boolean contaAtualizada =
                contaReceberDAO.atualizarStatus(
                        conn,
                        contaReceber.getIdConta(),
                        statusContaAnterior,
                        StatusContaReceber.CANCELADA
                );

        if (!contaAtualizada) {
            throw new IllegalStateException(
                    "A conta a receber foi alterada por outra operação "
                            + "e não pôde ser marcada como CANCELADA."
            );
        }
    }

    /**
     * Restaura no estoque todas as unidades dos itens da venda.
     *
     * A operação utiliza a mesma Connection controlada pelo Service.
     * O total de unidades é calculado antes das atualizações para que
     * eventual estouro numérico interrompa o fluxo antes da escrita.
     *
     * @param conn conexão da transação de estorno.
     * @param itensVenda itens previamente validados.
     * @return quantidades consolidadas da restauração.
     */
    private DadosRestauracaoEstoque restaurarEstoqueItens(
            Connection conn,
            List<ItemVenda> itensVenda
    ) {

        Set<Integer> produtosRestaurados =
                new HashSet<>();

        int quantidadeTotalDeUnidadesRestauradas = 0;

        try {
            for (ItemVenda item : itensVenda) {
                produtosRestaurados.add(
                        item.getProdutoId()
                );

                quantidadeTotalDeUnidadesRestauradas =
                        Math.addExact(
                                quantidadeTotalDeUnidadesRestauradas,
                                item.getQuantidade()
                        );
            }

        } catch (ArithmeticException e) {
            throw new IllegalStateException(
                    "A quantidade total de unidades para restauração "
                            + "ultrapassa o limite permitido.",
                    e
            );
        }

        for (ItemVenda item : itensVenda) {
            produtoDAO.restaurarEstoque(
                    conn,
                    item.getProdutoId(),
                    item.getQuantidade()
            );
        }

        return new DadosRestauracaoEstoque(
                produtosRestaurados.size(),
                quantidadeTotalDeUnidadesRestauradas
        );
    }

    /**
     * Monta a movimentação financeira de saída correspondente
     * ao cenário pago do estorno.
     *
     * @param cenarioEstorno cenário oficial classificado.
     * @param venda venda que está sendo estornada.
     * @param contaReceber conta vinculada ou null na venda à vista.
     * @param movimentacaoOriginal entrada financeira original.
     * @param usuarioId usuário administrador responsável.
     * @param dataHoraEstorno data e hora únicas da transação.
     * @param valorSaida valor normalizado da saída.
     * @return movimentação de saída pronta para persistência.
     */
    private MovimentacaoFinanceira montarMovimentacaoFinanceiraSaida(
            CenarioEstornoVenda cenarioEstorno,
            Venda venda,
            ContaReceber contaReceber,
            MovimentacaoFinanceira movimentacaoOriginal,
            Integer usuarioId,
            LocalDateTime dataHoraEstorno,
            BigDecimal valorSaida
    ) {

        if (movimentacaoOriginal == null) {
            throw new IllegalStateException(
                    "Cenário pago não possui movimentação "
                            + "financeira original."
            );
        }

        OrigemMovimentacaoFinanceira origemSaida;
        Integer contaReceberId;

        if (cenarioEstorno
                == CenarioEstornoVenda.VENDA_A_VISTA_PAGA) {

            origemSaida =
                    OrigemMovimentacaoFinanceira
                            .ESTORNO_VENDA_A_VISTA;

            contaReceberId = null;

        } else if (cenarioEstorno
                == CenarioEstornoVenda.VENDA_A_PRAZO_PAGA) {

            if (contaReceber == null) {
                throw new IllegalStateException(
                        "Venda a prazo paga não possui conta a receber "
                                + "para vincular à saída financeira."
                );
            }

            origemSaida =
                    OrigemMovimentacaoFinanceira
                            .ESTORNO_RECEBIMENTO_CONTA;

            contaReceberId =
                    contaReceber.getIdConta();

        } else {
            throw new IllegalStateException(
                    "Venda a prazo pendente não deve gerar "
                            + "movimentação financeira de saída."
            );
        }

        return new MovimentacaoFinanceira(
                null,
                dataHoraEstorno,
                TipoMovimentacaoFinanceira.SAIDA,
                origemSaida,
                movimentacaoOriginal.getFormaPagamento(),
                valorSaida,
                venda.getIdVenda(),
                contaReceberId,
                usuarioId
        );
    }

    /**
     * Valida os campos comuns da movimentação financeira original.
     */
    private void validarDadosBasicosMovimentacaoOriginal(
            MovimentacaoFinanceira movimentacaoOriginal,
            Venda venda
    ) {

        if (movimentacaoOriginal.getIdMovimentacao() == null
                || movimentacaoOriginal.getIdMovimentacao() <= 0) {
            throw new IllegalStateException(
                    "Movimentação financeira original possui ID inválido."
            );
        }

        if (movimentacaoOriginal.getDataHora() == null) {
            throw new IllegalStateException(
                    "Movimentação financeira original não possui "
                            + "data e hora."
            );
        }

        if (movimentacaoOriginal.getTipo()
                != TipoMovimentacaoFinanceira.ENTRADA) {
            throw new IllegalStateException(
                    "Movimentação financeira original deve ser uma ENTRADA."
            );
        }

        if (movimentacaoOriginal.getOrigem() == null) {
            throw new IllegalStateException(
                    "Movimentação financeira original não possui origem."
            );
        }

        if (movimentacaoOriginal.getFormaPagamento() == null
                || movimentacaoOriginal.getFormaPagamento()
                == FormaPagamento.A_PRAZO) {
            throw new IllegalStateException(
                    "Movimentação financeira original possui "
                            + "forma de pagamento inválida."
            );
        }

        if (movimentacaoOriginal.getValor() == null
                || movimentacaoOriginal.getValor().compareTo(
                BigDecimal.ZERO
        ) <= 0) {
            throw new IllegalStateException(
                    "Movimentação financeira original possui valor inválido."
            );
        }

        if (movimentacaoOriginal.getVendaId() == null
                || movimentacaoOriginal.getVendaId() <= 0
                || !venda.getIdVenda().equals(
                movimentacaoOriginal.getVendaId()
        )) {
            throw new IllegalStateException(
                    "Movimentação financeira original possui "
                            + "vínculo inconsistente com a venda."
            );
        }

        if (movimentacaoOriginal.getUsuarioId() == null
                || movimentacaoOriginal.getUsuarioId() <= 0) {
            throw new IllegalStateException(
                    "Movimentação financeira original possui "
                            + "usuário inválido."
            );
        }
    }

    /**
     * Valida a entrada financeira original de uma venda à vista.
     */
    private void validarMovimentacaoOriginalVendaAVista(
            MovimentacaoFinanceira movimentacaoOriginal,
            Venda venda,
            ContaReceber contaReceber
    ) {

        if (contaReceber != null) {
            throw new IllegalStateException(
                    "Venda à vista não pode possuir conta a receber."
            );
        }

        if (movimentacaoOriginal.getOrigem()
                != OrigemMovimentacaoFinanceira.VENDA_A_VISTA) {
            throw new IllegalStateException(
                    "Venda à vista possui origem financeira incompatível."
            );
        }

        if (movimentacaoOriginal.getContaReceberId() != null) {
            throw new IllegalStateException(
                    "Movimentação de venda à vista não pode estar "
                            + "vinculada a uma conta a receber."
            );
        }

        if (movimentacaoOriginal.getValor().compareTo(
                venda.getValorTotal()
        ) != 0) {
            throw new IllegalStateException(
                    "Valor da movimentação financeira é diferente "
                            + "do valor total da venda."
            );
        }

        FormaPagamento formaPagamentoVenda =
                converterFormaPagamentoPersistida(
                        venda.getFormaPagamento()
                );

        if (formaPagamentoVenda == FormaPagamento.A_PRAZO) {
            throw new IllegalStateException(
                    "Venda à vista possui forma de pagamento A_PRAZO."
            );
        }

        if (movimentacaoOriginal.getFormaPagamento()
                != formaPagamentoVenda) {
            throw new IllegalStateException(
                    "Forma de pagamento da movimentação é diferente "
                            + "da forma registrada na venda."
            );
        }
    }

    /**
     * Valida a entrada criada pelo recebimento integral de uma conta.
     */
    private void validarMovimentacaoOriginalRecebimentoConta(
            MovimentacaoFinanceira movimentacaoOriginal,
            Venda venda,
            ContaReceber contaReceber
    ) {

        if (contaReceber == null) {
            throw new IllegalStateException(
                    "Venda a prazo PAGA não possui conta a receber."
            );
        }

        if (movimentacaoOriginal.getOrigem()
                != OrigemMovimentacaoFinanceira.RECEBIMENTO_CONTA) {
            throw new IllegalStateException(
                    "Venda a prazo PAGA possui origem "
                            + "financeira incompatível."
            );
        }

        if (movimentacaoOriginal.getContaReceberId() == null
                || movimentacaoOriginal.getContaReceberId() <= 0
                || !contaReceber.getIdConta().equals(
                movimentacaoOriginal.getContaReceberId()
        )) {
            throw new IllegalStateException(
                    "Movimentação do recebimento possui vínculo "
                            + "inconsistente com a conta a receber."
            );
        }

        if (movimentacaoOriginal.getValor().compareTo(
                contaReceber.getValor()
        ) != 0) {
            throw new IllegalStateException(
                    "Valor da movimentação do recebimento é diferente "
                            + "do valor da conta a receber."
            );
        }

        if (movimentacaoOriginal.getValor().compareTo(
                venda.getValorTotal()
        ) != 0) {
            throw new IllegalStateException(
                    "Valor da movimentação do recebimento é diferente "
                            + "do valor total da venda."
            );
        }
    }

    /**
     * Converte a forma de pagamento persistida sem corrigir
     * automaticamente valores inválidos.
     */
    private FormaPagamento converterFormaPagamentoPersistida(
            String formaPagamentoPersistida
    ) {

        if (formaPagamentoPersistida == null
                || formaPagamentoPersistida.isBlank()) {
            throw new IllegalStateException(
                    "Venda não possui forma de pagamento informada."
            );
        }

        try {
            return FormaPagamento.valueOf(
                    formaPagamentoPersistida
            );

        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Venda possui forma de pagamento persistida inválida: "
                            + formaPagamentoPersistida + ".",
                    e
            );
        }
    }

    /**
     * Converte o tipo persistido sem corrigir automaticamente
     * valores inválidos.
     */
    private TipoVenda converterTipoVendaPersistido(
            String tipoVendaPersistido
    ) {

        if (tipoVendaPersistido == null
                || tipoVendaPersistido.isBlank()) {
            throw new IllegalStateException(
                    "Venda não possui tipo informado."
            );
        }

        try {
            return TipoVenda.valueOf(
                    tipoVendaPersistido
            );

        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Venda possui tipo persistido inválido: "
                            + tipoVendaPersistido + ".",
                    e
            );
        }
    }

    /**
     * Converte o status persistido sem corrigir automaticamente
     * valores inválidos.
     */
    private StatusVenda converterStatusVendaPersistido(
            String statusVendaPersistido
    ) {

        if (statusVendaPersistido == null
                || statusVendaPersistido.isBlank()) {
            throw new IllegalStateException(
                    "Venda não possui status informado."
            );
        }

        try {
            return StatusVenda.valueOf(
                    statusVendaPersistido
            );

        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Venda possui status persistido inválido: "
                            + statusVendaPersistido + ".",
                    e
            );
        }
    }

    /**
     * Monta o resultado público do estorno depois do commit da transação.
     *
     * Este método apenas transforma os dados internos em ViewModel
     * e não acessa o banco de dados.
     */
    private ResultadoEstornoVenda montarResultadoEstorno(
            DadosEstornoConcluido dados
    ) {

        return new ResultadoEstornoVenda(
                dados.vendaId(),
                dados.tipoVenda(),
                dados.statusVendaFinal(),
                dados.contaReceberId(),
                dados.statusContaReceberFinal(),
                dados.movimentacaoFinanceiraOriginalId(),
                dados.movimentacaoFinanceiraSaidaId(),
                dados.valorSaida(),
                dados.auditoriaId(),
                dados.dataHoraEstorno(),
                dados.quantidadeDeItensRestaurados(),
                dados.quantidadeTotalDeUnidadesRestauradas()
        );
    }

    /**
     * Valida os dados que não dependem de consulta ao banco.
     *
     * Esta validação ocorre antes da abertura da Connection.
     */
    private void validarDadosBasicosEstorno(
            Integer vendaId,
            String motivo,
            Integer usuarioId
    ) {

        if (vendaId == null || vendaId <= 0) {
            throw new IllegalArgumentException(
                    "Venda inválida para estorno."
            );
        }

        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException(
                    "Motivo do estorno é obrigatório."
            );
        }

        if (motivo.trim().length() > 500) {
            throw new IllegalArgumentException(
                    "Motivo do estorno deve possuir no máximo "
                            + "500 caracteres."
            );
        }

        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException(
                    "Usuário inválido para realizar o estorno."
            );
        }
    }

    /**
     * Executa rollback de forma segura.
     *
     * Uma eventual falha no rollback não substitui a exceção original.
     */
    private void executarRollbackSeguro(
            Connection conn
    ) {

        if (conn == null) {
            return;
        }

        try {
            conn.rollback();

        } catch (SQLException e) {
            System.err.println(
                    "Erro ao executar rollback do estorno: "
                            + e.getMessage()
            );
        }
    }

    /**
     * Restaura o autoCommit da conexão de forma segura.
     *
     * Uma eventual falha na restauração não substitui a exceção original.
     */
    private void restaurarAutoCommitSeguro(
            Connection conn,
            boolean autoCommitAnterior
    ) {

        if (conn == null) {
            return;
        }

        try {
            conn.setAutoCommit(
                    autoCommitAnterior
            );

        } catch (SQLException e) {
            System.err.println(
                    "Erro ao restaurar autoCommit do estorno: "
                            + e.getMessage()
            );
        }
    }

    /**
     * Quantidades consolidadas durante a restauração do estoque.
     */
    private record DadosRestauracaoEstoque(
            Integer quantidadeDeItensRestaurados,
            Integer quantidadeTotalDeUnidadesRestauradas
    ) {
    }

    /**
     * Cenários oficiais suportados pelo estorno total de venda.
     */
    private enum CenarioEstornoVenda {

        VENDA_A_VISTA_PAGA,
        VENDA_A_PRAZO_PENDENTE,
        VENDA_A_PRAZO_PAGA
    }

    /**
     * Estrutura interna usada para transportar os dados consolidados
     * pelo fluxo transacional até a montagem do resultado após o commit.
     */
    private record DadosEstornoConcluido(
            Integer vendaId,
            TipoVenda tipoVenda,
            StatusVenda statusVendaFinal,
            Integer contaReceberId,
            StatusContaReceber statusContaReceberFinal,
            Integer movimentacaoFinanceiraOriginalId,
            Integer movimentacaoFinanceiraSaidaId,
            BigDecimal valorSaida,
            Integer auditoriaId,
            LocalDateTime dataHoraEstorno,
            Integer quantidadeDeItensRestaurados,
            Integer quantidadeTotalDeUnidadesRestauradas
    ) {
    }
}