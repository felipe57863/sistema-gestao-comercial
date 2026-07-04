package br.com.luis.service;

import br.com.luis.dao.ClienteDAO;
import br.com.luis.dao.ContaReceberDAO;
import br.com.luis.dao.ItemVendaDAO;
import br.com.luis.dao.MovimentacaoFinanceiraDAO;
import br.com.luis.dao.PrazoPagamentoDAO;
import br.com.luis.dao.ProdutoDAO;
import br.com.luis.dao.VendaDAO;
import br.com.luis.model.FormaPagamento;
import br.com.luis.model.ItemVenda;
import br.com.luis.model.MovimentacaoFinanceira;
import br.com.luis.model.OrigemMovimentacaoFinanceira;
import br.com.luis.model.Produto;
import br.com.luis.model.Promocao;
import br.com.luis.model.PrazoPagamento;
import br.com.luis.model.StatusVenda;
import br.com.luis.model.TipoDescontoGlobal;
import br.com.luis.model.TipoMovimentacaoFinanceira;
import br.com.luis.model.TipoVenda;
import br.com.luis.model.Venda;
import br.com.luis.model.Cliente;
import br.com.luis.model.ContaReceber;
import br.com.luis.model.StatusContaReceber;
import br.com.luis.viewmodel.ResultadoFinalizacaoVenda;
import br.com.luis.util.ConnectionFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Camada de Serviço responsável pelas regras de negócio da venda.
 *
 * Nesta fase, o serviço trabalha apenas com o carrinho em memória.
 * A persistência completa da venda, finalização, baixa de estoque e financeiro
 * serão tratados em etapas futuras.
 */
public class VendaService {

    private static final int ESCALA_MONETARIA = 2;
    private static final BigDecimal CEM = new BigDecimal("100");

    private final ProdutoService produtoService;
    private final PromocaoService promocaoService;

    private final VendaDAO vendaDAO;
    private final ItemVendaDAO itemVendaDAO;
    private final ProdutoDAO produtoDAO;
    private final ClienteDAO clienteDAO;
    private final PrazoPagamentoDAO prazoPagamentoDAO;
    private final ContaReceberDAO contaReceberDAO;
    private final MovimentacaoFinanceiraDAO movimentacaoFinanceiraDAO;

    public VendaService() {
        this.produtoService = new ProdutoService();
        this.promocaoService = new PromocaoService();

        this.vendaDAO = new VendaDAO();
        this.itemVendaDAO = new ItemVendaDAO();
        this.produtoDAO = new ProdutoDAO();
        this.clienteDAO = new ClienteDAO();
        this.prazoPagamentoDAO = new PrazoPagamentoDAO();
        this.contaReceberDAO = new ContaReceberDAO();
        this.movimentacaoFinanceiraDAO = new MovimentacaoFinanceiraDAO();
    }

    /**
     * Adiciona um produto ao carrinho da venda.
     *
     * Se o produto ainda não existir no carrinho, cria um novo ItemVenda.
     * Se o produto já existir, soma a nova quantidade à quantidade existente,
     * valida o estoque com base na quantidade total acumulada e recalcula
     * subtotal e total.
     *
     * Importante:
     * nesta fase, o estoque NÃO é baixado. Apenas validamos se existe estoque
     * suficiente para permitir a inclusão no carrinho.
     *
     * Sempre que o carrinho é alterado, o desconto global anterior é limpo,
     * pois a base elegível do desconto pode mudar.
     *
     * @implNote Implementa a RN01 - Não permitir venda sem estoque.
     * @implNote Implementa a RN02 - Aplicação automática de promoção.
     * @implNote Apoia a RN03/RN04 - Recalcular desconto global quando o carrinho muda.
     *
     * @param venda venda em memória que receberá o item.
     * @param idProduto ID do produto que será adicionado.
     * @param quantidade quantidade desejada do produto.
     */
    public void adicionarItemAoCarrinho(Venda venda, Integer idProduto, Integer quantidade) {

        if (venda == null) {
            throw new IllegalArgumentException("Venda inválida para adicionar item.");
        }

        Produto produto = produtoService.buscarPorId(idProduto);

        validarProdutoParaVenda(produto);
        validarQuantidade(quantidade);

        Promocao promocaoAtiva = promocaoService.buscarPromocaoAtivaPorProduto(produto);

        ItemVenda itemExistente = buscarItemPorProduto(venda, produto.getIdProduto());

        if (itemExistente != null) {
            Integer quantidadeTotal = itemExistente.getQuantidade() + quantidade;

            validarEstoque(produto, quantidadeTotal);

            limparDescontoGlobal(venda);

            atualizarItemExistente(itemExistente, produto, promocaoAtiva, quantidade);
            venda.recalcularTotal();
            return;
        }

        validarEstoque(produto, quantidade);

        limparDescontoGlobal(venda);

        ItemVenda novoItem = criarNovoItem(produto, promocaoAtiva, quantidade);

        venda.adicionarItem(novoItem);
    }

    /**
     * Atualiza a quantidade final de um item já existente no carrinho.
     *
     * Diferente de adicionarItemAoCarrinho(...), este método não soma quantidade.
     * Ele define a nova quantidade final do item.
     *
     * Antes de alterar o ItemVenda, todas as validações principais são executadas
     * para evitar estado parcial em caso de erro.
     *
     * Sempre que a quantidade é alterada, o desconto global anterior é limpo,
     * pois a base elegível do desconto pode mudar.
     *
     * @implNote Implementa a RN01 - Não permitir venda sem estoque.
     * @implNote Implementa a RN02 - Aplicação automática de promoção.
     * @implNote Apoia a RN03/RN04 - Recalcular desconto global quando o carrinho muda.
     *
     * @param venda venda em memória que contém o carrinho.
     * @param itemVenda item do carrinho que terá a quantidade alterada.
     * @param novaQuantidade nova quantidade final desejada para o item.
     */
    public void atualizarQuantidadeItemCarrinho(
            Venda venda,
            ItemVenda itemVenda,
            Integer novaQuantidade
    ) {

        validarDadosAtualizacaoQuantidade(venda, itemVenda, novaQuantidade);

        Produto produto = produtoService.buscarPorId(itemVenda.getProdutoId());

        validarProdutoParaVenda(produto);

        if (novaQuantidade > produto.getQuantidadeEstoque()) {
            throw new IllegalArgumentException(
                    "Quantidade indisponível em estoque. Estoque atual: "
                            + produto.getQuantidadeEstoque()
                            + " unidade(s)."
            );
        }

        Promocao promocaoAtiva = promocaoService.buscarPromocaoAtivaPorProduto(produto);

        BigDecimal descontoPromocional = calcularDescontoPromocional(
                itemVenda.getPrecoUnitario(),
                promocaoAtiva,
                novaQuantidade
        );

        limparDescontoGlobal(venda);

        itemVenda.setQuantidade(novaQuantidade);
        itemVenda.setDescontoPromocional(descontoPromocional);
        itemVenda.calcularSubtotal();

        venda.recalcularTotal();
    }

    /**
     * Aplica desconto global sobre os itens elegíveis da venda.
     *
     * Itens com desconto promocional não recebem desconto global.
     * O desconto é distribuído proporcionalmente entre os itens sem promoção.
     *
     * Antes de aplicar um novo desconto global, qualquer desconto global anterior
     * é removido para evitar acúmulo indevido.
     *
     * @implNote Implementa a RN03/RN04 - Desconto global aplicado apenas sobre itens sem promoção.
     *
     * @param venda venda em memória que receberá o desconto global.
     * @param tipoDescontoGlobal tipo do desconto: PERCENTUAL ou VALOR_FIXO.
     * @param valorDesconto valor percentual ou valor fixo do desconto.
     */
    public void aplicarDescontoGlobal(
            Venda venda,
            TipoDescontoGlobal tipoDescontoGlobal,
            BigDecimal valorDesconto
    ) {

        validarDadosBasicosDescontoGlobal(venda, tipoDescontoGlobal, valorDesconto);

        limparDescontoGlobal(venda);

        List<ItemVenda> itensElegiveis = listarItensElegiveisParaDescontoGlobal(venda);

        if (itensElegiveis.isEmpty()) {
            throw new IllegalArgumentException(
                    "[RN04] Desconto global não pode ser aplicado em itens promocionais."
            );
        }

        BigDecimal baseElegivel = calcularBaseElegivel(itensElegiveis);

        if (baseElegivel.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("[RN04] Base elegível para desconto global é inválida.");
        }

        BigDecimal valorDescontoGlobal = calcularValorDescontoGlobal(
                tipoDescontoGlobal,
                valorDesconto,
                baseElegivel
        );

        distribuirDescontoGlobalProporcionalmente(
                itensElegiveis,
                baseElegivel,
                valorDescontoGlobal
        );

        venda.setValorDescontoGlobal(valorDescontoGlobal);
        venda.recalcularTotal();
    }

    /**
     * Remove um item do carrinho da venda.
     *
     * Após remover o item, qualquer desconto global aplicado anteriormente
     * é limpo, pois a base elegível do desconto pode ter sido alterada.
     *
     * @implNote Apoia a RN03/RN04 - Recalcular desconto global quando o carrinho muda.
     *
     * @param venda venda em memória.
     * @param itemVenda item que será removido do carrinho.
     */
    public void removerItemDoCarrinho(Venda venda, ItemVenda itemVenda) {

        if (venda == null) {
            throw new IllegalArgumentException("Venda inválida para remover item.");
        }

        if (itemVenda == null) {
            throw new IllegalArgumentException("Item inválido para remoção.");
        }

        if (venda.getItens() == null || venda.getItens().isEmpty()) {
            throw new IllegalArgumentException("Venda não possui itens para remover.");
        }

        if (!venda.getItens().contains(itemVenda)) {
            throw new IllegalArgumentException("Item não encontrado no carrinho.");
        }

        venda.removerItem(itemVenda);

        limparDescontoGlobal(venda);
        venda.recalcularTotal();
    }

    /**
     * Limpa todos os itens do carrinho da venda.
     *
     * Também remove qualquer desconto global aplicado e recalcula o total
     * da venda para o estado inicial.
     *
     * @implNote Apoia a RN03/RN04 - Limpeza do carrinho e do desconto global.
     *
     * @param venda venda em memória que será limpa.
     */
    public void limparCarrinho(Venda venda) {

        if (venda == null) {
            throw new IllegalArgumentException("Venda inválida para limpar carrinho.");
        }

        if (venda.getItens() != null) {
            venda.getItens().clear();
        }

        venda.setValorDescontoGlobal(BigDecimal.ZERO);
        venda.recalcularTotal();
    }

    /**
     * Finaliza uma venda, persistindo os dados principais, itens,
     * baixa de estoque e financeiro.
     *
     * Este método ainda não possui implementação real.
     * A lógica transacional será adicionada nos próximos passos da Fase 5.
     *
     * @param venda venda em memória que será finalizada.
     * @param tipoVenda tipo da venda: A_VISTA ou A_PRAZO.
     * @param formaPagamento forma de pagamento selecionada.
     * @param valorRecebido valor recebido, usado principalmente para pagamento em dinheiro.
     * @param clienteId ID do cliente, obrigatório para venda a prazo e opcional para venda à vista.
     * @param prazoPagamentoId ID do prazo efetivo, obrigatório para venda a prazo.
     * @param usuarioId ID do usuário logado responsável pela venda.
     * @return resultado da finalização da venda.
     */
    public ResultadoFinalizacaoVenda finalizarVenda(
            Venda venda,
            TipoVenda tipoVenda,
            FormaPagamento formaPagamento,
            BigDecimal valorRecebido,
            Integer clienteId,
            Integer prazoPagamentoId,
            Integer usuarioId
    ) {
        validarDadosBasicosFinalizacao(venda, tipoVenda, formaPagamento, usuarioId);

        if (tipoVenda == TipoVenda.A_VISTA) {
            validarDadosVendaAVista(venda, tipoVenda, formaPagamento, valorRecebido);
        } else if (tipoVenda == TipoVenda.A_PRAZO) {
            validarDadosVendaAPrazo(tipoVenda, formaPagamento, clienteId, prazoPagamentoId);
        } else {
            throw new IllegalArgumentException("Tipo de venda inválido para finalização.");
        }

        try (Connection conn = ConnectionFactory.getConnection()) {
            boolean autoCommitAnterior = conn.getAutoCommit();
            Exception erroOriginal = null;

            try {
                conn.setAutoCommit(false);

                ResultadoFinalizacaoVenda resultado;

                if (tipoVenda == TipoVenda.A_VISTA) {
                    resultado = finalizarVendaAVistaTransacional(
                            conn,
                            venda,
                            formaPagamento,
                            valorRecebido,
                            clienteId,
                            usuarioId
                    );
                } else if (tipoVenda == TipoVenda.A_PRAZO) {
                    resultado = finalizarVendaAPrazoTransacional(
                            conn,
                            venda,
                            clienteId,
                            prazoPagamentoId,
                            usuarioId
                    );
                } else {
                    throw new IllegalArgumentException("Tipo de venda inválido para finalização.");
                }

                conn.commit();

                return resultado;

            } catch (Exception erro) {
                erroOriginal = erro;

                executarRollbackSeguro(conn, erroOriginal);

                if (erro instanceof IllegalArgumentException) {
                    throw (IllegalArgumentException) erro;
                }

                if (erro instanceof IllegalStateException) {
                    throw (IllegalStateException) erro;
                }

                if (erro instanceof SQLException) {
                    throw new IllegalStateException("Erro ao finalizar venda.", erro);
                }

                throw new IllegalStateException("Erro inesperado ao finalizar venda.", erro);

            } finally {
                restaurarAutoCommitSeguro(conn, autoCommitAnterior, erroOriginal);
            }

        } catch (SQLException erro) {
            throw new IllegalStateException("Erro ao finalizar venda.", erro);
        }
    }

    /**
     * Valida os dados específicos de uma venda à vista.
     *
     * Esta validação ainda não persiste venda, não baixa estoque
     * e não gera movimentação financeira.
     *
     * @implNote Apoia a preparação da finalização da venda à vista na Fase 5.
     */
    private void validarDadosVendaAVista(
            Venda venda,
            TipoVenda tipoVenda,
            FormaPagamento formaPagamento,
            BigDecimal valorRecebido
    ) {

        if (tipoVenda != TipoVenda.A_VISTA) {
            throw new IllegalArgumentException("Tipo de venda inválido para venda à vista.");
        }

        if (formaPagamento == FormaPagamento.A_PRAZO) {
            throw new IllegalArgumentException("Forma de pagamento A_PRAZO não é permitida para venda à vista.");
        }

        if (formaPagamento != FormaPagamento.DINHEIRO
                && formaPagamento != FormaPagamento.PIX
                && formaPagamento != FormaPagamento.CARTAO) {
            throw new IllegalArgumentException("Forma de pagamento inválida para venda à vista.");
        }

        if (formaPagamento == FormaPagamento.DINHEIRO) {
            if (valorRecebido == null) {
                throw new IllegalArgumentException("Valor recebido é obrigatório para pagamento em dinheiro.");
            }

            if (valorRecebido.compareTo(venda.getValorTotal()) < 0) {
                throw new IllegalArgumentException("Valor recebido não pode ser menor que o total da venda.");
            }
        }
    }

    /**
     * Calcula o troco de uma venda à vista.
     *
     * Para pagamento em dinheiro:
     * troco = valor recebido - valor total.
     *
     * Para PIX ou CARTAO:
     * o troco é zero.
     *
     * Este valor ainda não é retornado de verdade, pois a finalização
     * real será implementada nos próximos passos.
     *
     * @implNote O troco não será persistido no banco.
     */
    private BigDecimal calcularTrocoVendaAVista(
            Venda venda,
            FormaPagamento formaPagamento,
            BigDecimal valorRecebido
    ) {

        if (formaPagamento == FormaPagamento.DINHEIRO) {
            return valorRecebido
                    .subtract(venda.getValorTotal())
                    .setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
    }

    /**
     * Finaliza uma venda à vista usando uma Connection externa.
     *
     * Este método não abre conexão, não executa commit e não executa rollback.
     * Ele apenas orquestra as operações da venda à vista dentro de uma transação
     * que será controlada externamente.
     *
     * Ordem do fluxo:
     * Venda -> ItensVenda -> Baixa de estoque -> MovimentacaoFinanceira -> Resultado.
     *
     * @implNote Apoia a futura finalização transacional da venda à vista na Fase 5.
     */
    private ResultadoFinalizacaoVenda finalizarVendaAVistaTransacional(
            Connection conn,
            Venda venda,
            FormaPagamento formaPagamento,
            BigDecimal valorRecebido,
            Integer clienteId,
            Integer usuarioId
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão é obrigatória para finalizar venda à vista.");
        }

        prepararDadosVendaAVista(venda, formaPagamento, clienteId, usuarioId);

        BigDecimal troco = calcularTrocoVendaAVista(
                venda,
                formaPagamento,
                valorRecebido
        );

        Integer vendaId = persistirVenda(conn, venda);

        persistirItensVenda(conn, vendaId, venda);

        revalidarEBaixarEstoqueItens(conn, venda);

        MovimentacaoFinanceira movimentacaoFinanceira = montarMovimentacaoFinanceiraVendaAVista(
                vendaId,
                formaPagamento,
                venda.getValorTotal(),
                usuarioId
        );

        Integer movimentacaoFinanceiraId = persistirMovimentacaoFinanceira(
                conn,
                movimentacaoFinanceira
        );

        return montarResultadoFinalizacaoVendaAVista(
                vendaId,
                formaPagamento,
                venda.getValorTotal(),
                troco,
                movimentacaoFinanceiraId
        );
    }

    /**
     * Valida os dados específicos de uma venda a prazo.
     *
     * Esta validação ainda não consulta cliente no banco, não valida limite
     * de crédito e não gera conta a receber.
     *
     * @implNote Apoia a preparação da finalização da venda a prazo na Fase 5.
     */
    private void validarDadosVendaAPrazo(
            TipoVenda tipoVenda,
            FormaPagamento formaPagamento,
            Integer clienteId,
            Integer prazoPagamentoId
    ) {

        if (tipoVenda != TipoVenda.A_PRAZO) {
            throw new IllegalArgumentException("Tipo de venda inválido para venda a prazo.");
        }

        if (formaPagamento != FormaPagamento.A_PRAZO) {
            throw new IllegalArgumentException("Venda a prazo deve usar forma de pagamento A_PRAZO.");
        }

        if (clienteId == null || clienteId <= 0) {
            throw new IllegalArgumentException("Cliente é obrigatório para venda a prazo.");
        }

        if (prazoPagamentoId == null || prazoPagamentoId <= 0) {
            throw new IllegalArgumentException("Prazo de pagamento é obrigatório para venda a prazo.");
        }
    }

    /**
     * Finaliza uma venda a prazo usando uma Connection externa.
     *
     * Este método não abre conexão, não executa commit e não executa rollback.
     * Ele apenas orquestra as operações da venda a prazo dentro de uma transação
     * que será controlada externamente.
     *
     * Ordem do fluxo:
     * Validações transacionais -> Venda -> ItensVenda -> Baixa de estoque -> ContaReceber -> Resultado.
     *
     * @implNote Apoia a futura finalização transacional da venda a prazo na Fase 5.
     */
    private ResultadoFinalizacaoVenda finalizarVendaAPrazoTransacional(
            Connection conn,
            Venda venda,
            Integer clienteId,
            Integer prazoPagamentoId,
            Integer usuarioId
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão é obrigatória para finalizar venda a prazo.");
        }

        DadosValidadosVendaAPrazo dadosValidados = validarDadosTransacionaisVendaAPrazo(
                conn,
                clienteId,
                prazoPagamentoId,
                venda.getValorTotal()
        );

        PrazoPagamento prazoEfetivo = dadosValidados.getPrazoEfetivo();

        prepararDadosVendaAPrazo(venda, clienteId, usuarioId);

        Integer vendaId = persistirVenda(conn, venda);

        persistirItensVenda(conn, vendaId, venda);

        revalidarEBaixarEstoqueItens(conn, venda);

        ContaReceber contaReceber = montarContaReceberVendaAPrazo(
                vendaId,
                clienteId,
                prazoEfetivo.getIdPrazo(),
                prazoEfetivo.getQuantidadeDias(),
                venda.getValorTotal()
        );

        Integer contaReceberId = persistirContaReceber(conn, contaReceber);

        return montarResultadoFinalizacaoVendaAPrazo(
                vendaId,
                venda.getValorTotal(),
                contaReceber.getDataVencimento(),
                contaReceberId
        );
    }

    /**
     * Executa rollback de forma segura.
     *
     * Se o rollback falhar e existir um erro original, a falha do rollback
     * será adicionada como erro suprimido no erro original.
     *
     * Este método não abre conexão e não controla transação sozinho.
     *
     * @implNote Apoia a futura finalização transacional da venda na Fase 5.
     */
    private void executarRollbackSeguro(
            Connection conn,
            Exception erroOriginal
    ) {

        if (conn == null) {
            return;
        }

        try {
            conn.rollback();
        } catch (SQLException erroRollback) {
            if (erroOriginal != null) {
                erroOriginal.addSuppressed(erroRollback);
                return;
            }

            throw new IllegalStateException("Falha ao executar rollback da transação.", erroRollback);
        }
    }

    /**
     * Restaura o autoCommit da conexão de forma segura.
     *
     * Se a restauração falhar e existir um erro original, a falha da restauração
     * será adicionada como erro suprimido no erro original.
     *
     * Este método não abre conexão e não controla transação sozinho.
     *
     * @implNote Apoia a futura finalização transacional da venda na Fase 5.
     */
    private void restaurarAutoCommitSeguro(
            Connection conn,
            boolean autoCommitAnterior,
            Exception erroOriginal
    ) {

        if (conn == null) {
            return;
        }

        try {
            conn.setAutoCommit(autoCommitAnterior);
        } catch (SQLException erroAutoCommit) {
            if (erroOriginal != null) {
                erroOriginal.addSuppressed(erroAutoCommit);
                return;
            }

            throw new IllegalStateException("Falha ao restaurar autoCommit da conexão.", erroAutoCommit);
        }
    }

    /**
     * Prepara os dados finais de uma venda à vista.
     *
     * Este método apenas ajusta o objeto Venda em memória.
     * Ainda não persiste a venda no banco de dados.
     *
     * @implNote Apoia a preparação da finalização da venda à vista na Fase 5.
     */
    private void prepararDadosVendaAVista(
            Venda venda,
            FormaPagamento formaPagamento,
            Integer clienteId,
            Integer usuarioId
    ) {

        venda.setDataHora(LocalDateTime.now());
        venda.setTipoVenda(TipoVenda.A_VISTA.name());
        venda.setFormaPagamento(formaPagamento.name());
        venda.setStatus(StatusVenda.PAGA.name());
        venda.setUsuarioId(usuarioId);
        venda.setClienteId(clienteId);
    }

    /**
     * Prepara os dados finais de uma venda a prazo.
     *
     * Este método apenas ajusta o objeto Venda em memória.
     * Ainda não persiste a venda no banco de dados.
     *
     * @implNote Apoia a preparação da finalização da venda a prazo na Fase 5.
     */
    private void prepararDadosVendaAPrazo(
            Venda venda,
            Integer clienteId,
            Integer usuarioId
    ) {

        venda.setDataHora(LocalDateTime.now());
        venda.setTipoVenda(TipoVenda.A_PRAZO.name());
        venda.setFormaPagamento(FormaPagamento.A_PRAZO.name());
        venda.setStatus(StatusVenda.PENDENTE.name());
        venda.setUsuarioId(usuarioId);
        venda.setClienteId(clienteId);
    }

    /**
     * Monta uma movimentação financeira para venda à vista.
     *
     * Este método apenas cria e preenche o objeto em memória.
     * Ainda não insere a movimentação no banco de dados.
     *
     * @implNote Apoia a futura geração de MovimentacaoFinanceira
     * para venda à vista na Fase 5.
     */
    private MovimentacaoFinanceira montarMovimentacaoFinanceiraVendaAVista(
            Integer vendaId,
            FormaPagamento formaPagamento,
            BigDecimal valorTotal,
            Integer usuarioId
    ) {

        if (vendaId == null || vendaId <= 0) {
            throw new IllegalArgumentException("ID da venda é obrigatório para gerar movimentação financeira.");
        }

        if (formaPagamento == null) {
            throw new IllegalArgumentException("Forma de pagamento é obrigatória para movimentação financeira.");
        }

        if (formaPagamento == FormaPagamento.A_PRAZO) {
            throw new IllegalArgumentException("Venda a prazo não gera movimentação financeira imediata.");
        }

        if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor total inválido para movimentação financeira.");
        }

        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException("Usuário é obrigatório para movimentação financeira.");
        }

        MovimentacaoFinanceira movimentacaoFinanceira = new MovimentacaoFinanceira();
        movimentacaoFinanceira.setDataHora(LocalDateTime.now());
        movimentacaoFinanceira.setTipo(TipoMovimentacaoFinanceira.ENTRADA);
        movimentacaoFinanceira.setOrigem(OrigemMovimentacaoFinanceira.VENDA_A_VISTA);
        movimentacaoFinanceira.setFormaPagamento(formaPagamento);
        movimentacaoFinanceira.setValor(valorTotal);
        movimentacaoFinanceira.setVendaId(vendaId);
        movimentacaoFinanceira.setContaReceberId(null);
        movimentacaoFinanceira.setUsuarioId(usuarioId);

        return movimentacaoFinanceira;
    }

    /**
     * Monta uma conta a receber para venda a prazo.
     *
     * Este método apenas cria e preenche o objeto em memória.
     * Ainda não insere a conta a receber no banco de dados.
     *
     * @implNote Apoia a futura geração de ContaReceber
     * para venda a prazo na Fase 5.
     */
    private ContaReceber montarContaReceberVendaAPrazo(
            Integer vendaId,
            Integer clienteId,
            Integer prazoPagamentoId,
            Integer quantidadeDiasPrazo,
            BigDecimal valorTotal
    ) {

        if (vendaId == null || vendaId <= 0) {
            throw new IllegalArgumentException("ID da venda é obrigatório para gerar conta a receber.");
        }

        if (clienteId == null || clienteId <= 0) {
            throw new IllegalArgumentException("Cliente é obrigatório para gerar conta a receber.");
        }

        if (prazoPagamentoId == null || prazoPagamentoId <= 0) {
            throw new IllegalArgumentException("Prazo de pagamento é obrigatório para gerar conta a receber.");
        }

        if (quantidadeDiasPrazo == null || quantidadeDiasPrazo <= 0) {
            throw new IllegalArgumentException("Quantidade de dias do prazo é obrigatória para gerar conta a receber.");
        }

        if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor total inválido para gerar conta a receber.");
        }

        ContaReceber contaReceber = new ContaReceber();
        contaReceber.setValor(valorTotal);
        contaReceber.setDataVencimento(LocalDate.now().plusDays(quantidadeDiasPrazo));
        contaReceber.setStatus(StatusContaReceber.PENDENTE);
        contaReceber.setVendaId(vendaId);
        contaReceber.setClienteId(clienteId);
        contaReceber.setPrazoPagamentoId(prazoPagamentoId);
        contaReceber.setQuantidadeDiasPrazo(quantidadeDiasPrazo);
        contaReceber.setDataCriacao(LocalDateTime.now());

        return contaReceber;
    }

    /**
     * Monta o resultado da finalização de uma venda à vista.
     *
     * Este método apenas cria o objeto de retorno em memória.
     * Ainda não é chamado pelo finalizarVenda(...).
     *
     * @implNote Apoia o futuro retorno da finalização da venda à vista na Fase 5.
     */
    private ResultadoFinalizacaoVenda montarResultadoFinalizacaoVendaAVista(
            Integer vendaId,
            FormaPagamento formaPagamento,
            BigDecimal valorTotal,
            BigDecimal troco,
            Integer movimentacaoFinanceiraId
    ) {

        if (vendaId == null || vendaId <= 0) {
            throw new IllegalArgumentException("ID da venda é obrigatório para montar o resultado.");
        }

        if (formaPagamento == null) {
            throw new IllegalArgumentException("Forma de pagamento é obrigatória para montar o resultado.");
        }

        if (formaPagamento == FormaPagamento.A_PRAZO) {
            throw new IllegalArgumentException("Forma de pagamento inválida para resultado de venda à vista.");
        }

        if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor total inválido para montar o resultado.");
        }

        if (movimentacaoFinanceiraId == null || movimentacaoFinanceiraId <= 0) {
            throw new IllegalArgumentException("ID da movimentação financeira é obrigatório para venda à vista.");
        }

        return new ResultadoFinalizacaoVenda(
                vendaId,
                TipoVenda.A_VISTA,
                StatusVenda.PAGA,
                formaPagamento,
                valorTotal,
                troco,
                null,
                null,
                movimentacaoFinanceiraId
        );
    }

    /**
     * Monta o resultado da finalização de uma venda a prazo.
     *
     * Este método apenas cria o objeto de retorno em memória.
     * Ainda não é chamado pelo finalizarVenda(...).
     *
     * @implNote Apoia o futuro retorno da finalização da venda a prazo na Fase 5.
     */
    private ResultadoFinalizacaoVenda montarResultadoFinalizacaoVendaAPrazo(
            Integer vendaId,
            BigDecimal valorTotal,
            LocalDate dataVencimento,
            Integer contaReceberId
    ) {

        if (vendaId == null || vendaId <= 0) {
            throw new IllegalArgumentException("ID da venda é obrigatório para montar o resultado.");
        }

        if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor total inválido para montar o resultado.");
        }

        if (dataVencimento == null) {
            throw new IllegalArgumentException("Data de vencimento é obrigatória para venda a prazo.");
        }

        if (contaReceberId == null || contaReceberId <= 0) {
            throw new IllegalArgumentException("ID da conta a receber é obrigatório para venda a prazo.");
        }

        return new ResultadoFinalizacaoVenda(
                vendaId,
                TipoVenda.A_PRAZO,
                StatusVenda.PENDENTE,
                FormaPagamento.A_PRAZO,
                valorTotal,
                BigDecimal.ZERO,
                dataVencimento,
                contaReceberId,
                null
        );
    }

    /**
     * Revalida e baixa o estoque dos itens da venda dentro de uma transação.
     *
     * Este método não abre transação, não executa commit e não executa rollback.
     * Ele apenas usa a Connection externa recebida.
     *
     * A validação definitiva de produto ativo e estoque suficiente é feita pelo
     * ProdutoDAO.baixarEstoque(...), usando UPDATE seguro no banco de dados.
     *
     * @implNote Apoia a RN01 - Não permitir venda sem estoque.
     * @implNote Apoia a futura finalização transacional da venda na Fase 5.
     */
    private void revalidarEBaixarEstoqueItens(Connection conn, Venda venda) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão é obrigatória para baixar estoque.");
        }

        if (venda == null) {
            throw new IllegalArgumentException("Venda inválida para baixar estoque.");
        }

        if (venda.getItens() == null || venda.getItens().isEmpty()) {
            throw new IllegalArgumentException("Venda não possui itens para baixar estoque.");
        }

        for (ItemVenda item : venda.getItens()) {
            if (item == null) {
                throw new IllegalArgumentException("Item inválido para baixar estoque.");
            }

            if (item.getProdutoId() == null || item.getProdutoId() <= 0) {
                throw new IllegalArgumentException("Item da venda sem produto válido para baixar estoque.");
            }

            if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
                throw new IllegalArgumentException("Item da venda com quantidade inválida para baixar estoque.");
            }

            produtoDAO.baixarEstoque(
                    conn,
                    item.getProdutoId(),
                    item.getQuantidade()
            );
        }
    }

    /**
     * Persiste a venda dentro de uma transação.
     *
     * Este método não abre transação, não executa commit e não executa rollback.
     * Ele apenas usa a Connection externa recebida.
     *
     * @implNote Apoia a futura finalização transacional da venda na Fase 5.
     */
    private Integer persistirVenda(Connection conn, Venda venda) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão é obrigatória para persistir a venda.");
        }

        if (venda == null) {
            throw new IllegalArgumentException("Venda inválida para persistir.");
        }

        Integer vendaId = vendaDAO.inserir(conn, venda);

        if (vendaId == null || vendaId <= 0) {
            throw new IllegalStateException("Não foi possível obter o ID da venda persistida.");
        }

        return vendaId;
    }

    /**
     * Persiste a movimentação financeira dentro de uma transação.
     *
     * Este método não abre transação, não executa commit e não executa rollback.
     * Ele apenas usa a Connection externa recebida.
     *
     * @implNote Apoia a futura finalização transacional da venda à vista na Fase 5.
     */
    private Integer persistirMovimentacaoFinanceira(
            Connection conn,
            MovimentacaoFinanceira movimentacaoFinanceira
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão é obrigatória para persistir movimentação financeira.");
        }

        if (movimentacaoFinanceira == null) {
            throw new IllegalArgumentException("Movimentação financeira inválida para persistir.");
        }

        Integer movimentacaoFinanceiraId = movimentacaoFinanceiraDAO.inserir(conn, movimentacaoFinanceira);

        if (movimentacaoFinanceiraId == null || movimentacaoFinanceiraId <= 0) {
            throw new IllegalStateException("Não foi possível obter o ID da movimentação financeira persistida.");
        }

        return movimentacaoFinanceiraId;
    }

    /**
     * Persiste a conta a receber dentro de uma transação.
     *
     * Este método não abre transação, não executa commit e não executa rollback.
     * Ele apenas usa a Connection externa recebida.
     *
     * @implNote Apoia a futura finalização transacional da venda a prazo na Fase 5.
     */
    private Integer persistirContaReceber(
            Connection conn,
            ContaReceber contaReceber
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão é obrigatória para persistir conta a receber.");
        }

        if (contaReceber == null) {
            throw new IllegalArgumentException("Conta a receber inválida para persistir.");
        }

        Integer contaReceberId = contaReceberDAO.inserir(conn, contaReceber);

        if (contaReceberId == null || contaReceberId <= 0) {
            throw new IllegalStateException("Não foi possível obter o ID da conta a receber persistida.");
        }

        return contaReceberId;
    }

    /**
     * Busca e valida o cliente da venda a prazo dentro de uma transação.
     *
     * Este método não abre transação, não executa commit e não executa rollback.
     * Ele apenas usa a Connection externa recebida.
     *
     * A busca já carrega o prazo máximo vinculado ao cliente.
     *
     * @implNote Apoia a futura finalização transacional da venda a prazo na Fase 5.
     */
    private Cliente buscarEValidarClienteVendaAPrazo(
            Connection conn,
            Integer clienteId
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão é obrigatória para validar cliente da venda a prazo.");
        }

        if (clienteId == null || clienteId <= 0) {
            throw new IllegalArgumentException("Cliente é obrigatório para venda a prazo.");
        }

        Cliente cliente = clienteDAO.buscarPorIdComPrazo(conn, clienteId);

        if (cliente == null) {
            throw new IllegalArgumentException("Cliente não encontrado para venda a prazo.");
        }

        if (cliente.getStatus() != Cliente.StatusCliente.ATIVO) {
            throw new IllegalArgumentException("Cliente inativo não pode realizar venda a prazo.");
        }

        return cliente;
    }

    /**
     * Busca e valida o prazo efetivo da venda a prazo dentro de uma transação.
     *
     * Este método não abre transação, não executa commit e não executa rollback.
     * Ele apenas usa a Connection externa recebida.
     *
     * @implNote Apoia a futura finalização transacional da venda a prazo na Fase 5.
     */
    private PrazoPagamento buscarEValidarPrazoEfetivoVendaAPrazo(
            Connection conn,
            Integer prazoPagamentoId
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão é obrigatória para validar prazo da venda a prazo.");
        }

        if (prazoPagamentoId == null || prazoPagamentoId <= 0) {
            throw new IllegalArgumentException("Prazo de pagamento é obrigatório para venda a prazo.");
        }

        PrazoPagamento prazoPagamento = prazoPagamentoDAO.buscarPorId(conn, prazoPagamentoId);

        if (prazoPagamento == null) {
            throw new IllegalArgumentException("Prazo de pagamento não encontrado para venda a prazo.");
        }

        if (!prazoPagamento.isAtivo()) {
            throw new IllegalArgumentException("Prazo de pagamento inativo não pode ser usado na venda a prazo.");
        }

        if (prazoPagamento.getQuantidadeDias() == null || prazoPagamento.getQuantidadeDias() <= 0) {
            throw new IllegalArgumentException("Prazo de pagamento deve possuir quantidade de dias maior que zero.");
        }

        return prazoPagamento;
    }

    /**
     * Valida se o prazo efetivo escolhido na venda a prazo está dentro
     * do prazo máximo autorizado para o cliente.
     *
     * Este método não abre transação, não executa commit e não executa rollback.
     * Ele apenas compara os objetos já carregados.
     *
     * @implNote Apoia a futura finalização transacional da venda a prazo na Fase 5.
     */
    private void validarPrazoEfetivoDentroDoLimiteDoCliente(
            Cliente cliente,
            PrazoPagamento prazoEfetivo
    ) {

        if (cliente == null) {
            throw new IllegalArgumentException("Cliente é obrigatório para validar prazo da venda a prazo.");
        }

        if (prazoEfetivo == null) {
            throw new IllegalArgumentException("Prazo efetivo é obrigatório para validar venda a prazo.");
        }

        PrazoPagamento prazoMaximoCliente = cliente.getPrazoPagamento();

        if (prazoMaximoCliente == null) {
            throw new IllegalArgumentException("Cliente não possui prazo máximo autorizado.");
        }

        if (prazoMaximoCliente.getQuantidadeDias() == null || prazoMaximoCliente.getQuantidadeDias() <= 0) {
            throw new IllegalArgumentException("Prazo máximo do cliente é inválido.");
        }

        if (prazoEfetivo.getQuantidadeDias() == null || prazoEfetivo.getQuantidadeDias() <= 0) {
            throw new IllegalArgumentException("Prazo efetivo da venda é inválido.");
        }

        if (prazoEfetivo.getQuantidadeDias() > prazoMaximoCliente.getQuantidadeDias()) {
            throw new IllegalArgumentException(
                    "O prazo escolhido ultrapassa o prazo máximo autorizado para este cliente."
            );
        }
    }

    /**
     * Valida se o cliente possui limite de crédito disponível
     * para realizar a venda a prazo.
     *
     * Este método considera:
     * limite disponível = limite de crédito do cliente - total pendente em contas a receber.
     *
     * Este método não abre transação, não executa commit e não executa rollback.
     * Ele apenas usa a Connection externa recebida.
     *
     * @implNote Apoia a futura finalização transacional da venda a prazo na Fase 5.
     */
    private void validarLimiteCreditoDisponivelVendaAPrazo(
            Connection conn,
            Cliente cliente,
            BigDecimal valorVenda
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão é obrigatória para validar limite de crédito.");
        }

        if (cliente == null) {
            throw new IllegalArgumentException("Cliente é obrigatório para validar limite de crédito.");
        }

        if (cliente.getIdCliente() == null || cliente.getIdCliente() <= 0) {
            throw new IllegalArgumentException("Cliente inválido para validar limite de crédito.");
        }

        if (valorVenda == null || valorVenda.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor da venda é obrigatório para validar limite de crédito.");
        }

        BigDecimal limiteCredito = cliente.getLimiteCredito() != null
                ? cliente.getLimiteCredito()
                : BigDecimal.ZERO;

        limiteCredito = limiteCredito.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);

        BigDecimal totalPendente = contaReceberDAO.somarTotalPendentePorCliente(
                conn,
                cliente.getIdCliente()
        );

        if (totalPendente == null) {
            totalPendente = BigDecimal.ZERO;
        }

        totalPendente = totalPendente.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);

        BigDecimal limiteDisponivel = limiteCredito
                .subtract(totalPendente)
                .setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);

        BigDecimal valorVendaAjustado = valorVenda.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);

        if (valorVendaAjustado.compareTo(limiteDisponivel) > 0) {
            throw new IllegalArgumentException("Limite de crédito insuficiente para venda a prazo.");
        }
    }

    /**
     * Valida os dados transacionais necessários para uma venda a prazo.
     *
     * Este método agrupa as validações de cliente, prazo efetivo,
     * prazo máximo autorizado e limite de crédito.
     *
     * Este método não abre transação, não executa commit e não executa rollback.
     * Ele apenas usa a Connection externa recebida.
     *
     * @implNote Apoia a futura finalização transacional da venda a prazo na Fase 5.
     */
    private DadosValidadosVendaAPrazo validarDadosTransacionaisVendaAPrazo(
            Connection conn,
            Integer clienteId,
            Integer prazoPagamentoId,
            BigDecimal valorVenda
    ) {

        Cliente cliente = buscarEValidarClienteVendaAPrazo(conn, clienteId);

        PrazoPagamento prazoEfetivo = buscarEValidarPrazoEfetivoVendaAPrazo(
                conn,
                prazoPagamentoId
        );

        validarPrazoEfetivoDentroDoLimiteDoCliente(cliente, prazoEfetivo);

        validarLimiteCreditoDisponivelVendaAPrazo(conn, cliente, valorVenda);

        return new DadosValidadosVendaAPrazo(cliente, prazoEfetivo);
    }

    /**
     * Persiste os itens da venda dentro de uma transação.
     *
     * Este método não abre transação, não executa commit e não executa rollback.
     * Ele apenas usa a Connection externa recebida.
     *
     * Antes de inserir cada item, o ID da venda é vinculado ao ItemVenda.
     *
     * @implNote Apoia a futura finalização transacional da venda na Fase 5.
     */
    private void persistirItensVenda(
            Connection conn,
            Integer vendaId,
            Venda venda
    ) {

        if (conn == null) {
            throw new IllegalArgumentException("Conexão é obrigatória para persistir itens da venda.");
        }

        if (vendaId == null || vendaId <= 0) {
            throw new IllegalArgumentException("ID da venda é obrigatório para persistir itens da venda.");
        }

        if (venda == null) {
            throw new IllegalArgumentException("Venda inválida para persistir itens.");
        }

        if (venda.getItens() == null || venda.getItens().isEmpty()) {
            throw new IllegalArgumentException("Venda não possui itens para persistir.");
        }

        for (ItemVenda item : venda.getItens()) {
            validarItemParaFinalizacao(item);

            item.setVendaId(vendaId);

            itemVendaDAO.inserir(conn, item);
        }
    }

    /**
     * Valida os dados básicos comuns para qualquer tipo de finalização de venda.
     *
     * Esta validação ainda não executa regras específicas de venda à vista
     * ou venda a prazo.
     *
     * Também não abre transação e não acessa o banco de dados.
     *
     * @implNote Apoia a preparação da finalização da venda na Fase 5.
     */
    private void validarDadosBasicosFinalizacao(
            Venda venda,
            TipoVenda tipoVenda,
            FormaPagamento formaPagamento,
            Integer usuarioId
    ) {

        if (venda == null) {
            throw new IllegalArgumentException("Venda inválida para finalização.");
        }

        if (venda.getItens() == null || venda.getItens().isEmpty()) {
            throw new IllegalArgumentException("Não é possível finalizar uma venda sem itens.");
        }

        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException("Usuário inválido para finalização da venda.");
        }

        if (tipoVenda == null) {
            throw new IllegalArgumentException("Tipo de venda é obrigatório.");
        }

        if (formaPagamento == null) {
            throw new IllegalArgumentException("Forma de pagamento é obrigatória.");
        }

        if (venda.getValorTotal() == null || venda.getValorTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor total da venda deve ser maior que zero.");
        }

        for (ItemVenda item : venda.getItens()) {
            validarItemParaFinalizacao(item);
        }
    }

    /**
     * Valida os dados mínimos de um item antes da finalização.
     *
     * Esta validação não revalida estoque no banco.
     * A revalidação de estoque será feita futuramente dentro da transação.
     */
    private void validarItemParaFinalizacao(ItemVenda item) {

        if (item == null) {
            throw new IllegalArgumentException("Item inválido na venda.");
        }

        if (item.getProdutoId() == null || item.getProdutoId() <= 0) {
            throw new IllegalArgumentException("Item da venda sem produto válido.");
        }

        if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
            throw new IllegalArgumentException("Item da venda com quantidade inválida.");
        }

        if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Item da venda com preço unitário inválido.");
        }

        if (item.getSubtotal() == null || item.getSubtotal().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Item da venda com subtotal inválido.");
        }
    }

    /**
     * Limpa qualquer desconto global já aplicado na venda.
     *
     * Essa limpeza evita que o desconto seja acumulado indevidamente quando
     * o usuário aplica outro desconto global no mesmo carrinho.
     *
     * @implNote Implementa o fluxo obrigatório da RN03/RN04 - Recalcular desconto global do zero.
     */
    private void limparDescontoGlobal(Venda venda) {

        for (ItemVenda item : venda.getItens()) {
            item.setDescontoGlobal(BigDecimal.ZERO);
            item.calcularSubtotal();
        }

        venda.setValorDescontoGlobal(BigDecimal.ZERO);
        venda.recalcularTotal();
    }

    /**
     * Valida os dados necessários para atualizar a quantidade de um item do carrinho.
     *
     * @implNote Apoia a RN01 - Não permitir venda sem estoque.
     */
    private void validarDadosAtualizacaoQuantidade(
            Venda venda,
            ItemVenda itemVenda,
            Integer novaQuantidade
    ) {

        if (venda == null) {
            throw new IllegalArgumentException("Venda inválida para atualizar quantidade.");
        }

        if (venda.getItens() == null || venda.getItens().isEmpty()) {
            throw new IllegalArgumentException("Venda não possui itens para atualizar.");
        }

        if (itemVenda == null) {
            throw new IllegalArgumentException("Item inválido para atualizar quantidade.");
        }

        if (!venda.getItens().contains(itemVenda)) {
            throw new IllegalArgumentException("Item não encontrado no carrinho.");
        }

        if (itemVenda.getProdutoId() == null) {
            throw new IllegalArgumentException("Item sem produto vinculado.");
        }

        validarQuantidade(novaQuantidade);
    }

    /**
     * Valida os dados básicos necessários para aplicar desconto global.
     *
     * @implNote Implementa validações da RN03/RN04 - Desconto global.
     */
    private void validarDadosBasicosDescontoGlobal(
            Venda venda,
            TipoDescontoGlobal tipoDescontoGlobal,
            BigDecimal valorDesconto
    ) {

        if (venda == null) {
            throw new IllegalArgumentException("[RN03] Venda inválida para aplicar desconto global.");
        }

        if (venda.getItens() == null || venda.getItens().isEmpty()) {
            throw new IllegalArgumentException("[RN03] Venda não possui itens para aplicar desconto global.");
        }

        if (tipoDescontoGlobal == null) {
            throw new IllegalArgumentException("[RN03] Tipo de desconto global é obrigatório.");
        }

        if (valorDesconto == null) {
            throw new IllegalArgumentException("[RN03] Valor do desconto global é obrigatório.");
        }

        if (valorDesconto.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("[RN04] Desconto global não pode ser negativo.");
        }

        if (tipoDescontoGlobal == TipoDescontoGlobal.PERCENTUAL
                && valorDesconto.compareTo(CEM) > 0) {
            throw new IllegalArgumentException("[RN04] Desconto percentual não pode ser maior que 100%.");
        }
    }

    /**
     * Retorna apenas os itens que podem receber desconto global.
     *
     * Um item é considerado promocional quando possui descontoPromocional maior que zero.
     *
     * @implNote Implementa a RN04 - Itens promocionais não recebem desconto global.
     */
    private List<ItemVenda> listarItensElegiveisParaDescontoGlobal(Venda venda) {

        List<ItemVenda> itensElegiveis = new ArrayList<>();

        for (ItemVenda item : venda.getItens()) {
            if (!itemPossuiPromocao(item)) {
                itensElegiveis.add(item);
            }
        }

        return itensElegiveis;
    }

    /**
     * Verifica se o item possui desconto promocional.
     *
     * @implNote Implementa o critério da RN04 - descontoPromocional > 0 identifica item promocional.
     */
    private boolean itemPossuiPromocao(ItemVenda item) {

        return item.getDescontoPromocional() != null
                && item.getDescontoPromocional().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Calcula a base elegível para desconto global.
     *
     * A base é formada pela soma do valor bruto dos itens sem promoção:
     * quantidade * preço unitário.
     *
     * @implNote Implementa a RN04 - Base elegível considera apenas itens sem promoção.
     */
    private BigDecimal calcularBaseElegivel(List<ItemVenda> itensElegiveis) {

        BigDecimal baseElegivel = BigDecimal.ZERO;

        for (ItemVenda item : itensElegiveis) {
            baseElegivel = baseElegivel.add(calcularValorBrutoItem(item));
        }

        return baseElegivel.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
    }

    /**
     * Calcula o valor bruto de um item.
     *
     * Fórmula:
     * valorBruto = quantidade * precoUnitario
     *
     * @implNote Apoia a RN04 - Cálculo da base elegível do desconto global.
     */
    private BigDecimal calcularValorBrutoItem(ItemVenda item) {

        Integer quantidade = item.getQuantidade() != null ? item.getQuantidade() : 0;
        BigDecimal precoUnitario = item.getPrecoUnitario() != null
                ? item.getPrecoUnitario()
                : BigDecimal.ZERO;

        return precoUnitario
                .multiply(BigDecimal.valueOf(quantidade))
                .setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
    }

    /**
     * Calcula o valor monetário total do desconto global.
     *
     * Para percentual, calcula sobre a base elegível.
     * Para valor fixo, valida se o desconto não ultrapassa a base elegível.
     *
     * @implNote Implementa a RN03/RN04 - Cálculo do desconto global.
     */
    private BigDecimal calcularValorDescontoGlobal(
            TipoDescontoGlobal tipoDescontoGlobal,
            BigDecimal valorDesconto,
            BigDecimal baseElegivel
    ) {

        return switch (tipoDescontoGlobal) {
            case PERCENTUAL -> baseElegivel
                    .multiply(valorDesconto)
                    .divide(CEM, ESCALA_MONETARIA, RoundingMode.HALF_UP);

            case VALOR_FIXO -> calcularDescontoGlobalFixo(valorDesconto, baseElegivel);
        };
    }

    /**
     * Valida e retorna o desconto global de valor fixo.
     *
     * @implNote Implementa a RN04 - Desconto global fixo não pode ser maior que a base elegível.
     */
    private BigDecimal calcularDescontoGlobalFixo(
            BigDecimal valorDesconto,
            BigDecimal baseElegivel
    ) {

        BigDecimal desconto = valorDesconto.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);

        if (desconto.compareTo(baseElegivel) > 0) {
            throw new IllegalArgumentException("[RN04] Desconto global maior que a base elegível.");
        }

        return desconto;
    }

    /**
     * Distribui o desconto global proporcionalmente entre os itens elegíveis.
     *
     * A diferença de centavos causada por arredondamento é aplicada no último
     * item elegível para garantir que a soma dos descontos por item seja
     * exatamente igual ao desconto global total da venda.
     *
     * @implNote Implementa a RN04 - Distribuição proporcional do desconto global.
     */
    private void distribuirDescontoGlobalProporcionalmente(
            List<ItemVenda> itensElegiveis,
            BigDecimal baseElegivel,
            BigDecimal valorDescontoGlobal
    ) {

        BigDecimal totalDistribuido = BigDecimal.ZERO;

        for (int i = 0; i < itensElegiveis.size(); i++) {
            ItemVenda item = itensElegiveis.get(i);

            BigDecimal descontoDoItem;

            boolean ultimoItemElegivel = i == itensElegiveis.size() - 1;

            if (ultimoItemElegivel) {
                descontoDoItem = valorDescontoGlobal
                        .subtract(totalDistribuido)
                        .setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
            } else {
                BigDecimal valorBrutoItem = calcularValorBrutoItem(item);

                BigDecimal proporcao = valorBrutoItem.divide(
                        baseElegivel,
                        10,
                        RoundingMode.HALF_UP
                );

                descontoDoItem = valorDescontoGlobal
                        .multiply(proporcao)
                        .setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);

                totalDistribuido = totalDistribuido.add(descontoDoItem);
            }

            item.setDescontoGlobal(descontoDoItem);
            item.calcularSubtotal();
        }
    }

    /**
     * Busca no carrinho um item que já represente o produto informado.
     *
     * @implNote Apoia a regra do carrinho da Fase 4.2:
     * produtos repetidos devem ter suas quantidades somadas.
     */
    private ItemVenda buscarItemPorProduto(Venda venda, Integer idProduto) {

        for (ItemVenda item : venda.getItens()) {
            if (item.getProdutoId() != null && item.getProdutoId().equals(idProduto)) {
                return item;
            }
        }

        return null;
    }

    /**
     * Atualiza um item já existente no carrinho, somando a nova quantidade,
     * validando o estoque acumulado e recalculando desconto e subtotal.
     *
     * @implNote Implementa a RN01 - Não permitir venda sem estoque.
     * @implNote Implementa a RN02 - Aplicação automática de promoção.
     */
    private void atualizarItemExistente(
            ItemVenda itemExistente,
            Produto produto,
            Promocao promocaoAtiva,
            Integer quantidadeNova
    ) {

        Integer quantidadeTotal = itemExistente.getQuantidade() + quantidadeNova;

        validarEstoque(produto, quantidadeTotal);

        itemExistente.setQuantidade(quantidadeTotal);

        BigDecimal descontoPromocional = calcularDescontoPromocional(
                itemExistente.getPrecoUnitario(),
                promocaoAtiva,
                quantidadeTotal
        );

        itemExistente.setDescontoPromocional(descontoPromocional);
        itemExistente.calcularSubtotal();
    }

    /**
     * Cria um novo item de venda para produto que ainda não existe no carrinho.
     *
     * @implNote Implementa a RN02 - Aplicação automática de promoção.
     */
    private ItemVenda criarNovoItem(
            Produto produto,
            Promocao promocaoAtiva,
            Integer quantidade
    ) {

        ItemVenda item = new ItemVenda(
                produto.getIdProduto(),
                quantidade,
                produto.getPreco()
        );

        BigDecimal descontoPromocional = calcularDescontoPromocional(
                produto.getPreco(),
                promocaoAtiva,
                quantidade
        );

        item.setDescontoPromocional(descontoPromocional);
        item.calcularSubtotal();

        return item;
    }

    /**
     * Valida se o produto pode ser usado em uma venda.
     *
     * @implNote Implementa validação de apoio à RN01 - Não permitir venda sem estoque.
     */
    private void validarProdutoParaVenda(Produto produto) {

        if (produto == null) {
            throw new IllegalArgumentException("Produto não encontrado.");
        }

        if (!produto.isAtivo()) {
            throw new IllegalArgumentException("Produto inativo não pode ser vendido.");
        }

        if (produto.getPreco() == null) {
            throw new IllegalArgumentException("Produto sem preço cadastrado.");
        }

        if (produto.getQuantidadeEstoque() == null) {
            throw new IllegalArgumentException("Produto sem quantidade de estoque cadastrada.");
        }
    }

    /**
     * Valida se a quantidade informada é permitida.
     *
     * @implNote Implementa validação de apoio à RN01 - Não permitir venda sem estoque.
     */
    private void validarQuantidade(Integer quantidade) {

        if (quantidade == null) {
            throw new IllegalArgumentException("Quantidade do item é obrigatória.");
        }

        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade do item deve ser maior que zero.");
        }
    }

    /**
     * Valida se existe estoque suficiente para adicionar o item ao carrinho.
     *
     * @implNote Implementa a RN01 - Não permitir venda sem estoque.
     */
    private void validarEstoque(Produto produto, Integer quantidade) {

        if (produto.getQuantidadeEstoque() <= 0) {
            throw new IllegalArgumentException("Produto sem estoque disponível.");
        }

        if (quantidade > produto.getQuantidadeEstoque()) {
            throw new IllegalArgumentException(
                    "Estoque insuficiente. Disponível: " + produto.getQuantidadeEstoque()
            );
        }
    }

    /**
     * Calcula o desconto promocional aplicado ao item.
     *
     * Para desconto percentual, calcula sobre o valor bruto do item:
     * quantidade * preço unitário.
     *
     * Para desconto de valor fixo, considera o desconto por unidade
     * multiplicado pela quantidade.
     *
     * Caso não exista promoção ativa, retorna zero.
     *
     * @implNote Implementa a RN02 - Aplicação automática de promoção.
     */
    private BigDecimal calcularDescontoPromocional(
            BigDecimal precoUnitario,
            Promocao promocao,
            Integer quantidade
    ) {

        if (promocao == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal quantidadeBigDecimal = BigDecimal.valueOf(quantidade);
        BigDecimal valorBruto = precoUnitario.multiply(quantidadeBigDecimal);

        BigDecimal desconto;

        if (promocao.getTipoDesconto() == Promocao.TipoDesconto.PERCENTUAL) {
            desconto = valorBruto
                    .multiply(promocao.getValorDesconto())
                    .divide(CEM, ESCALA_MONETARIA, RoundingMode.HALF_UP);
        } else if (promocao.getTipoDesconto() == Promocao.TipoDesconto.VALOR_FIXO) {
            desconto = promocao.getValorDesconto().multiply(quantidadeBigDecimal);
        } else {
            desconto = BigDecimal.ZERO;
        }

        if (desconto.compareTo(valorBruto) > 0) {
            return valorBruto;
        }

        return desconto.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
    }

    /**
     * Estrutura auxiliar interna para transportar os dados validados
     * da venda a prazo dentro do VendaService.
     *
     * Esta classe não é model, não é viewmodel e não deve sair desta classe.
     */
    private static class DadosValidadosVendaAPrazo {

        private final Cliente cliente;
        private final PrazoPagamento prazoEfetivo;

        private DadosValidadosVendaAPrazo(
                Cliente cliente,
                PrazoPagamento prazoEfetivo
        ) {
            this.cliente = cliente;
            this.prazoEfetivo = prazoEfetivo;
        }

        private Cliente getCliente() {
            return cliente;
        }

        private PrazoPagamento getPrazoEfetivo() {
            return prazoEfetivo;
        }
    }

}