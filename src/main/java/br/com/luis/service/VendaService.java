package br.com.luis.service;

import br.com.luis.dao.ClienteDAO;
import br.com.luis.dao.ContaReceberDAO;
import br.com.luis.dao.ItemVendaDAO;
import br.com.luis.dao.MovimentacaoFinanceiraDAO;
import br.com.luis.dao.PrazoPagamentoDAO;
import br.com.luis.dao.ProdutoDAO;
import br.com.luis.dao.VendaDAO;
import br.com.luis.dao.UsuarioDAO;
import br.com.luis.dao.NotaVendaDAO;
import br.com.luis.dao.ItemNotaVendaDAO;
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
import br.com.luis.model.Usuario;
import br.com.luis.model.NotaVenda;
import br.com.luis.viewmodel.ResultadoFinalizacaoVenda;
import br.com.luis.viewmodel.SituacaoFinanceiraClienteView;
import br.com.luis.util.ConnectionFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Camada de serviço responsável pelas regras de negócio da venda.
 *
 * Gerencia o carrinho em memória, promoções, desconto global e cálculos da venda.
 * Também finaliza vendas à vista e a prazo em uma única transação, persistindo
 * a venda e seus itens, baixando o estoque, gerando a movimentação financeira
 * ou a conta a receber correspondente ao tipo da venda e registrando NotaVenda
 * e ItemNotaVenda como fotografia documental da operação comercial.
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
    private final UsuarioDAO usuarioDAO;
    private final NotaVendaDAO notaVendaDAO;
    private final ItemNotaVendaDAO itemNotaVendaDAO;

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
        this.usuarioDAO = new UsuarioDAO();
        this.notaVendaDAO = new NotaVendaDAO();
        this.itemNotaVendaDAO = new ItemNotaVendaDAO();
    }

    /**
     * Adiciona um produto ao carrinho da venda.
     *
     * Se o produto ainda não existir no carrinho, cria um novo ItemVenda.
     * Se o produto já existir, soma a nova quantidade à quantidade existente,
     * valida o estoque com base na quantidade total acumulada e recalcula
     * subtotal e total.
     *
     * Esta operação atua somente sobre o carrinho em memória. O estoque é
     * consultado para validação, mas sua baixa ocorre apenas na finalização
     * persistida da venda.
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
     * Esta operação altera somente o carrinho em memória e não baixa estoque.
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
     * Esta operação calcula e distribui o desconto somente no carrinho em memória.
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
     * Verifica se a venda possui ao menos um item elegível ao desconto global.
     *
     * Usa o mesmo critério da RN04 aplicado pelo fluxo de desconto global.
     */
    public boolean possuiItemElegivelParaDescontoGlobal(Venda venda) {
        if (venda == null || venda.getItens() == null || venda.getItens().isEmpty()) {
            return false;
        }

        return !listarItensElegiveisParaDescontoGlobal(venda).isEmpty();
    }

    /**
     * Calcula o subtotal bruto da venda antes de qualquer desconto.
     */
    public BigDecimal calcularSubtotalBruto(Venda venda) {
        BigDecimal subtotalBruto = BigDecimal.ZERO;

        if (venda == null || venda.getItens() == null) {
            return subtotalBruto.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
        }

        for (ItemVenda item : venda.getItens()) {
            if (item != null) {
                subtotalBruto = subtotalBruto.add(calcularValorBrutoItem(item));
            }
        }

        return subtotalBruto.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
    }

    /**
     * Calcula o total dos descontos promocionais dos itens da venda.
     */
    public BigDecimal calcularDescontoPromocionalTotal(Venda venda) {
        BigDecimal descontoPromocionalTotal = BigDecimal.ZERO;

        if (venda == null || venda.getItens() == null) {
            return descontoPromocionalTotal.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
        }

        for (ItemVenda item : venda.getItens()) {
            if (item != null && item.getDescontoPromocional() != null) {
                descontoPromocionalTotal = descontoPromocionalTotal.add(
                        item.getDescontoPromocional()
                );
            }
        }

        return descontoPromocionalTotal.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
    }

    /**
     * Calcula a soma dos descontos promocionais e do desconto global da venda.
     */
    public BigDecimal calcularDescontoTotal(Venda venda) {
        BigDecimal descontoGlobal = venda != null && venda.getValorDescontoGlobal() != null
                ? venda.getValorDescontoGlobal()
                : BigDecimal.ZERO;

        return calcularDescontoPromocionalTotal(venda)
                .add(descontoGlobal)
                .setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
    }

    /**
     * Remove um item do carrinho da venda.
     *
     * Após remover o item, qualquer desconto global aplicado anteriormente
     * é limpo, pois a base elegível do desconto pode ter sido alterada.
     * Esta operação não persiste alterações nem movimenta estoque.
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
     * Esta operação atua somente sobre o carrinho em memória.
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
     * Finaliza uma venda à vista ou a prazo em uma única transação.
     *
     * Após as validações básicas, abre e fecha a Connection, desabilita o
     * autoCommit durante o fluxo e delega a finalização específica usando a
     * mesma conexão. Persiste a Venda e seus ItemVenda, revalida e baixa o estoque,
     * gera uma MovimentacaoFinanceira para venda à vista ou uma ContaReceber para
     * venda a prazo e registra NotaVenda e ItemNotaVenda como fotografia documental.
     *
     * Executa commit somente depois de concluir todas essas operações. Em caso de
     * erro, executa rollback, restaura o estado anterior do autoCommit e propaga a
     * falha. A geração física do PDF não pertence a esta transação e ocorre somente
     * depois do commit, em fluxo próprio.
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
     * Valida o tipo e as formas de pagamento permitidas. Para pagamento em
     * dinheiro, exige valor recebido suficiente para cobrir o total da venda.
     *
     * Atua apenas sobre os dados em memória antes da abertura da Connection.
     * Não persiste dados, não baixa estoque e não gera movimentação financeira.
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
     * O valor calculado compõe o ResultadoFinalizacaoVenda, mas não é persistido.
     * Este método apenas calcula dados em memória e não acessa o banco de dados.
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
     * Este método não abre nem fecha a Connection, não executa commit e não
     * executa rollback. Ele orquestra as operações da venda à vista usando a
     * mesma transação controlada por finalizarVenda(...).
     *
     * Ordem do fluxo:
     * Venda -> ItensVenda -> Baixa de estoque -> MovimentacaoFinanceira -> Resultado.
     *
     * Ao final, retorna os identificadores persistidos e o troco calculado.
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

        Usuario usuario = buscarEValidarUsuarioFinalizacao(conn, usuarioId);
        Cliente cliente = buscarEValidarClienteVendaAVista(conn, clienteId);

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

        Integer notaVendaId = persistirNotaVendaComItens(
                conn,
                vendaId,
                venda,
                usuario,
                cliente,
                formaPagamento == FormaPagamento.DINHEIRO
                        ? valorRecebido
                        : null,
                formaPagamento == FormaPagamento.DINHEIRO
                        ? troco
                        : null,
                null,
                null,
                null
        );

        return montarResultadoFinalizacaoVendaAVista(
                vendaId,
                formaPagamento,
                venda.getValorTotal(),
                troco,
                movimentacaoFinanceiraId,
                notaVendaId
        );
    }

    /**
     * Calcula o limite de crédito disponível de um cliente.
     *
     * Fórmula:
     * limite disponível = limite de crédito cadastrado - total pendente em contas a receber.
     *
     * Este método abre uma conexão própria apenas para consulta.
     * Não inicia transação manual, não executa commit e não executa rollback.
     *
     * @param clienteId ID do cliente.
     * @return limite de crédito disponível com escala monetária.
     */
    public BigDecimal calcularLimiteCreditoDisponivel(Integer clienteId) {

        if (clienteId == null || clienteId <= 0) {
            throw new IllegalArgumentException("Cliente inválido para calcular limite de crédito disponível.");
        }

        try (Connection conn = ConnectionFactory.getConnection()) {

            Cliente cliente = clienteDAO.buscarPorIdComPrazo(conn, clienteId);

            if (cliente == null) {
                throw new IllegalArgumentException("Cliente não encontrado para calcular limite de crédito disponível.");
            }

            return calcularLimiteCreditoDisponivel(conn, cliente);

        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao calcular limite de crédito disponível.", e);
        }
    }

    /**
     * Valida os dados específicos de uma venda a prazo.
     *
     * Valida a combinação entre tipo e forma de pagamento e exige os IDs do
     * cliente e do prazo de pagamento.
     *
     * Atua apenas sobre os argumentos em memória antes da abertura da Connection.
     * As validações de cliente, prazo e limite de crédito ocorrem posteriormente
     * dentro da transação de finalização. Este método não gera conta a receber.
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
     * Este método não abre nem fecha a Connection, não executa commit e não
     * executa rollback. Ele orquestra as operações da venda a prazo usando a
     * mesma transação controlada por finalizarVenda(...).
     *
     * Ordem do fluxo:
     * Validações transacionais -> Venda -> ItensVenda -> Baixa de estoque -> ContaReceber -> Resultado.
     *
     * Ao final, retorna os identificadores persistidos e a data de vencimento.
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
        Cliente cliente = dadosValidados.getCliente();
        Usuario usuario = buscarEValidarUsuarioFinalizacao(conn, usuarioId);

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

        Integer notaVendaId = persistirNotaVendaComItens(
                conn,
                vendaId,
                venda,
                usuario,
                cliente,
                null,
                null,
                contaReceber.getPrazoPagamentoId(),
                contaReceber.getQuantidadeDiasPrazo(),
                contaReceber.getDataVencimento()
        );

        return montarResultadoFinalizacaoVendaAPrazo(
                vendaId,
                venda.getValorTotal(),
                contaReceber.getDataVencimento(),
                contaReceberId,
                notaVendaId
        );
    }

    /**
     * Executa rollback de forma segura.
     *
     * Se o rollback falhar e existir um erro original, a falha do rollback
     * será adicionada como erro suprimido no erro original.
     *
     * Este método não abre nem fecha a Connection e não executa commit. Ele é
     * chamado pelo controle transacional de finalizarVenda(...) após uma falha.
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
     * Este método não abre nem fecha a Connection, não executa commit e não
     * executa rollback. É chamado ao final do controle transacional para devolver
     * a Connection ao estado de autoCommit encontrado antes da finalização.
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
     * Ajusta data, tipo, forma de pagamento, status, usuário e cliente no objeto
     * Venda em memória. Não abre Connection nem persiste dados; a persistência
     * ocorre em seguida, na mesma transação da finalização.
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
     * Ajusta data, tipo, forma de pagamento, status, usuário e cliente no objeto
     * Venda em memória. Não abre Connection nem persiste dados; a persistência
     * ocorre em seguida, na mesma transação da finalização.
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
     * Valida os dados recebidos e cria a entrada financeira vinculada à venda,
     * preenchendo origem, forma de pagamento, valor, usuário e data em memória.
     * Não abre Connection nem persiste dados; a movimentação é inserida em
     * seguida, na mesma transação da finalização à vista.
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
     * Valida os dados recebidos e cria a conta pendente vinculada à venda e ao
     * cliente, calculando o vencimento conforme a quantidade de dias do prazo.
     * Não abre Connection nem persiste dados; a conta é inserida em seguida,
     * na mesma transação da finalização a prazo.
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
     * Após validar os dados, cria em memória o retorno da finalização com os IDs
     * persistidos, o total e o troco. Não abre Connection nem persiste dados.
     */
    private ResultadoFinalizacaoVenda montarResultadoFinalizacaoVendaAVista(
            Integer vendaId,
            FormaPagamento formaPagamento,
            BigDecimal valorTotal,
            BigDecimal troco,
            Integer movimentacaoFinanceiraId,
            Integer notaVendaId
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

        validarNotaVendaIdResultado(notaVendaId);

        return new ResultadoFinalizacaoVenda(
                vendaId,
                notaVendaId,
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
     * Após validar os dados, cria em memória o retorno da finalização com os IDs
     * persistidos, o total e a data de vencimento. Não abre Connection nem
     * persiste dados.
     */
    private ResultadoFinalizacaoVenda montarResultadoFinalizacaoVendaAPrazo(
            Integer vendaId,
            BigDecimal valorTotal,
            LocalDate dataVencimento,
            Integer contaReceberId,
            Integer notaVendaId
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

        validarNotaVendaIdResultado(notaVendaId);

        return new ResultadoFinalizacaoVenda(
                vendaId,
                notaVendaId,
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
     * Este método não abre nem fecha a Connection, não executa commit e não
     * executa rollback. Usa a conexão externa e participa da mesma transação
     * da finalização da venda.
     *
     * A validação definitiva de produto ativo e estoque suficiente é feita pelo
     * ProdutoDAO.baixarEstoque(...), usando UPDATE seguro no banco de dados.
     *
     * @implNote Implementa a RN01 - Não permitir venda sem estoque.
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
     * Este método não abre nem fecha a Connection, não executa commit e não
     * executa rollback. Usa a conexão externa e participa da mesma transação
     * da finalização da venda.
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
     * Este método não abre nem fecha a Connection, não executa commit e não
     * executa rollback. Usa a conexão externa e participa da mesma transação
     * da finalização à vista.
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
     * Este método não abre nem fecha a Connection, não executa commit e não
     * executa rollback. Usa a conexão externa e participa da mesma transação
     * da finalização a prazo.
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
     * Consulta uma fotografia consistente da situação financeira persistida do cliente.
     *
     * A busca do cliente e a soma das contas pendentes são executadas na mesma
     * Connection e em uma transação curta de leitura. O estado anterior de
     * autoCommit é restaurado ao final, com rollback seguro em caso de falha.
     *
     * @param clienteId ID do cliente consultado.
     * @return saldo devedor e limite disponível com escala monetária.
     */
    public SituacaoFinanceiraClienteView consultarSituacaoFinanceiraCliente(
            Integer clienteId
    ) {

        if (clienteId == null || clienteId <= 0) {
            throw new IllegalArgumentException(
                    "Cliente inválido para consultar situação financeira."
            );
        }

        try (Connection conn = ConnectionFactory.getConnection()) {

            boolean autoCommitAnterior = conn.getAutoCommit();
            Exception erroOriginal = null;

            try {
                conn.setAutoCommit(false);

                Cliente cliente = clienteDAO.buscarPorIdComPrazo(conn, clienteId);

                if (cliente == null) {
                    throw new IllegalArgumentException(
                            "Cliente não encontrado para consultar situação financeira."
                    );
                }

                SituacaoFinanceiraClienteView situacaoFinanceira =
                        consultarSituacaoFinanceiraCliente(conn, cliente);

                conn.commit();
                return situacaoFinanceira;

            } catch (Exception erro) {
                erroOriginal = erro;
                executarRollbackSeguro(conn, erroOriginal);

                if (erro instanceof IllegalArgumentException) {
                    throw (IllegalArgumentException) erro;
                }

                if (erro instanceof IllegalStateException) {
                    throw (IllegalStateException) erro;
                }

                throw new IllegalStateException(
                        "Erro ao consultar situação financeira do cliente.",
                        erro
                );

            } finally {
                restaurarAutoCommitSeguro(conn, autoCommitAnterior, erroOriginal);
            }

        } catch (SQLException erro) {
            throw new IllegalStateException(
                    "Erro ao consultar situação financeira do cliente.",
                    erro
            );
        }
    }

    private Usuario buscarEValidarUsuarioFinalizacao(
            Connection conn,
            Integer usuarioId
    ) {
        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException("Usuário é obrigatório para finalizar venda.");
        }

        Usuario usuario = usuarioDAO.buscarPorId(conn, usuarioId);

        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não encontrado para finalizar venda.");
        }

        if (!"ATIVO".equals(usuario.getStatus())) {
            throw new IllegalArgumentException("Usuário inativo não pode finalizar venda.");
        }

        return usuario;
    }

    private Cliente buscarEValidarClienteVendaAVista(
            Connection conn,
            Integer clienteId
    ) {
        if (clienteId == null) {
            return null;
        }

        if (clienteId <= 0) {
            throw new IllegalArgumentException("Cliente inválido para venda à vista.");
        }

        Cliente cliente = clienteDAO.buscarPorIdComPrazo(conn, clienteId);

        if (cliente == null) {
            throw new IllegalArgumentException("Cliente não encontrado para venda à vista.");
        }

        return cliente;
    }

    private Integer persistirNotaVendaComItens(
            Connection conn,
            Integer vendaId,
            Venda venda,
            Usuario usuario,
            Cliente cliente,
            BigDecimal valorRecebido,
            BigDecimal troco,
            Integer prazoPagamentoId,
            Integer quantidadeDiasPrazo,
            LocalDate dataVencimento
    ) {
        NotaVenda notaVenda = new NotaVenda();
        notaVenda.setVendaId(vendaId);
        notaVenda.setDataHoraVenda(venda.getDataHora());
        notaVenda.setTipoVenda(TipoVenda.valueOf(venda.getTipoVenda()));
        notaVenda.setFormaPagamento(FormaPagamento.valueOf(venda.getFormaPagamento()));
        notaVenda.setUsuarioId(usuario.getIdUsuario());
        notaVenda.setNomeUsuario(usuario.getNome());
        notaVenda.setClienteId(cliente == null ? null : cliente.getIdCliente());
        notaVenda.setNomeCliente(cliente == null ? null : cliente.getNome());
        notaVenda.setDocumentoCliente(cliente == null ? null : cliente.getDocumento());
        notaVenda.setValorTotal(venda.getValorTotal());
        notaVenda.setValorDescontoGlobal(venda.getValorDescontoGlobal());
        notaVenda.setValorRecebido(valorRecebido);
        notaVenda.setTroco(troco);
        notaVenda.setPrazoPagamentoId(prazoPagamentoId);
        notaVenda.setQuantidadeDiasPrazo(quantidadeDiasPrazo);
        notaVenda.setDataVencimento(dataVencimento);

        Integer notaVendaId = notaVendaDAO.inserir(conn, notaVenda);

        if (notaVendaId == null || notaVendaId <= 0) {
            throw new IllegalStateException("Não foi possível obter o ID da Nota de Venda persistida.");
        }

        int itensCopiados = itemNotaVendaDAO.copiarItensDaVenda(
                conn,
                notaVendaId,
                vendaId
        );

        if (itensCopiados != venda.getItens().size()) {
            throw new IllegalStateException("Quantidade de itens históricos copiados diferente da venda.");
        }

        return notaVendaId;
    }

    private void validarNotaVendaIdResultado(Integer notaVendaId) {
        if (notaVendaId == null || notaVendaId <= 0) {
            throw new IllegalStateException("ID da Nota de Venda é obrigatório no resultado da finalização.");
        }
    }

    /**
     * Busca e valida o cliente da venda a prazo dentro de uma transação.
     *
     * Este método não abre nem fecha a Connection, não executa commit e não
     * executa rollback. Consulta o cliente com a conexão externa e participa
     * da mesma transação da finalização a prazo.
     *
     * A busca já carrega o prazo máximo vinculado ao cliente.
     *
     * Também rejeita clientes inexistentes ou inativos.
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
     * Este método não abre nem fecha a Connection, não executa commit e não
     * executa rollback. Consulta o prazo com a conexão externa e participa da
     * mesma transação da finalização a prazo.
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
     * Este método não abre nem fecha Connection, não executa commit nem rollback
     * e não acessa o banco. Apenas compara os objetos já carregados durante a
     * mesma transação da finalização a prazo.
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
     * Calcula o limite de crédito disponível usando uma Connection externa.
     *
     * Fórmula:
     * limite disponível = limite de crédito cadastrado - total pendente em contas a receber.
     *
     * Este método não abre nem fecha a Connection, não inicia transação manual,
     * não executa commit e não executa rollback. Quando chamado na finalização a
     * prazo, sua consulta participa da mesma transação; também pode reutilizar a
     * conexão de uma consulta externa de limites.
     */
    private BigDecimal calcularLimiteCreditoDisponivel(
            Connection conn,
            Cliente cliente
    ) {

        return consultarSituacaoFinanceiraCliente(
                conn,
                cliente
        ).getLimiteDisponivel();
    }

    /**
     * Consulta e calcula uma fotografia financeira usando uma Connection externa.
     *
     * Este é o ponto interno comum para o saldo devedor e o limite disponível.
     * Não abre nem fecha a Connection e consulta o total pendente uma única vez.
     */
    private SituacaoFinanceiraClienteView consultarSituacaoFinanceiraCliente(
            Connection conn,
            Cliente cliente
    ) {

        if (conn == null) {
            throw new IllegalArgumentException(
                    "Conexão é obrigatória para consultar situação financeira."
            );
        }

        if (cliente == null) {
            throw new IllegalArgumentException(
                    "Cliente é obrigatório para consultar situação financeira."
            );
        }

        if (cliente.getIdCliente() == null || cliente.getIdCliente() <= 0) {
            throw new IllegalArgumentException(
                    "Cliente inválido para consultar situação financeira."
            );
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

        return new SituacaoFinanceiraClienteView(
                totalPendente,
                limiteDisponivel
        );
    }

    /**
     * Valida se o cliente possui limite de crédito disponível
     * para realizar a venda a prazo.
     *
     * Este método considera:
     * limite disponível = limite de crédito do cliente - total pendente em contas a receber.
     *
     * Este método não abre nem fecha a Connection, não executa commit e não
     * executa rollback. Consulta o total pendente pela conexão externa e participa
     * da mesma transação da finalização a prazo.
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

        BigDecimal limiteDisponivel = calcularLimiteCreditoDisponivel(conn, cliente);

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
     * Este método não abre nem fecha a Connection, não executa commit e não
     * executa rollback. Coordena as consultas e validações usando a mesma conexão
     * e a mesma transação da finalização a prazo.
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
     * Este método não abre nem fecha a Connection, não executa commit e não
     * executa rollback. Usa a conexão externa e participa da mesma transação
     * da finalização da venda.
     *
     * Antes de inserir cada item, o ID da venda é vinculado ao ItemVenda.
     *
     * Cada item é validado e vinculado ao ID da venda antes da persistência.
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
     * Valida a venda em memória, seus itens, usuário, tipo, forma de pagamento
     * e total. As regras específicas de venda à vista ou a prazo são executadas
     * separadamente após esta validação comum.
     *
     * Não abre Connection, não inicia transação e não acessa o banco de dados.
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
     * Esta validação atua somente sobre os dados do item em memória e não abre
     * Connection. A revalidação e a baixa de estoque são executadas posteriormente
     * dentro da mesma transação da finalização.
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
     * Atua somente sobre o carrinho em memória e permite que produtos repetidos
     * tenham suas quantidades somadas.
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

    /**
     * Calcula o limite de crédito disponível para uma lista de clientes.
     *
     * Este método abre uma única conexão para calcular todos os limites
     * e a fecha automaticamente ao final. Preserva a ordem recebida na lista
     * usando LinkedHashMap.
     *
     * Não inicia transação manual, não executa commit e não executa rollback.
     *
     * @param clientes lista de clientes que terão o limite disponível calculado.
     * @return mapa com ID do cliente como chave e limite disponível como valor.
     */
    public Map<Integer, BigDecimal> calcularLimitesCreditoDisponiveis(
            List<Cliente> clientes
    ) {

        if (clientes == null) {
            throw new IllegalArgumentException("Lista de clientes é obrigatória para calcular limites disponíveis.");
        }

        Map<Integer, BigDecimal> limitesDisponiveis = new LinkedHashMap<>();

        if (clientes.isEmpty()) {
            return limitesDisponiveis;
        }

        try (Connection conn = ConnectionFactory.getConnection()) {

            for (Cliente cliente : clientes) {
                if (cliente == null) {
                    throw new IllegalArgumentException("Cliente inválido para calcular limite disponível.");
                }

                if (cliente.getIdCliente() == null || cliente.getIdCliente() <= 0) {
                    throw new IllegalArgumentException("Cliente sem ID válido para calcular limite disponível.");
                }

                BigDecimal limiteDisponivel = calcularLimiteCreditoDisponivel(
                        conn,
                        cliente
                );

                limitesDisponiveis.put(
                        cliente.getIdCliente(),
                        limiteDisponivel
                );
            }

            return limitesDisponiveis;

        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao calcular limites de crédito disponíveis.", e);
        }
    }

}
