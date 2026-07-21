package br.com.luis.service;

import br.com.luis.dao.AuditoriaEstornoVendaDAO;
import br.com.luis.dao.ClienteDAO;
import br.com.luis.dao.ContaReceberDAO;
import br.com.luis.dao.ItemVendaDAO;
import br.com.luis.dao.MovimentacaoFinanceiraDAO;
import br.com.luis.dao.UsuarioDAO;
import br.com.luis.dao.VendaDAO;
import br.com.luis.model.AuditoriaEstornoVenda;
import br.com.luis.model.Cliente;
import br.com.luis.model.ContaReceber;
import br.com.luis.model.FormaPagamento;
import br.com.luis.model.MovimentacaoFinanceira;
import br.com.luis.model.OrigemMovimentacaoFinanceira;
import br.com.luis.model.StatusContaReceber;
import br.com.luis.model.StatusVenda;
import br.com.luis.model.TipoMovimentacaoFinanceira;
import br.com.luis.model.TipoVenda;
import br.com.luis.model.Usuario;
import br.com.luis.model.Venda;
import br.com.luis.util.ConnectionFactory;
import br.com.luis.viewmodel.FiltroHistoricoVenda;
import br.com.luis.viewmodel.ItemVendaHistoricoView;
import br.com.luis.viewmodel.VendaHistoricoDetalheView;
import br.com.luis.viewmodel.VendaHistoricoListagemView;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Camada de serviço responsável pelas consultas do histórico de vendas.
 *
 * Coordena os DAOs, valida inconsistências persistidas e entrega ViewModels
 * consolidados para a futura interface.
 *
 * Não realiza estorno e não altera dados persistidos.
 */
public class HistoricoVendaService {

    private static final String CLIENTE_NAO_IDENTIFICADO =
            "Consumidor não identificado";

    private final VendaDAO vendaDAO;
    private final ItemVendaDAO itemVendaDAO;
    private final ContaReceberDAO contaReceberDAO;
    private final MovimentacaoFinanceiraDAO movimentacaoFinanceiraDAO;
    private final AuditoriaEstornoVendaDAO auditoriaEstornoVendaDAO;
    private final ClienteDAO clienteDAO;
    private final UsuarioDAO usuarioDAO;

    public HistoricoVendaService() {
        this.vendaDAO = new VendaDAO();
        this.itemVendaDAO = new ItemVendaDAO();
        this.contaReceberDAO = new ContaReceberDAO();
        this.movimentacaoFinanceiraDAO =
                new MovimentacaoFinanceiraDAO();
        this.auditoriaEstornoVendaDAO =
                new AuditoriaEstornoVendaDAO();
        this.clienteDAO = new ClienteDAO();
        this.usuarioDAO = new UsuarioDAO();
    }

    /**
     * Lista o histórico de vendas conforme os filtros informados.
     */
    public List<VendaHistoricoListagemView> listarHistorico(
            FiltroHistoricoVenda filtro
    ) {

        validarFiltroHistorico(filtro);

        try (Connection conn = ConnectionFactory.getConnection()) {

            List<VendaHistoricoListagemView> vendas =
                    vendaDAO.listarHistoricoComFiltros(
                            conn,
                            filtro
                    );

            if (vendas == null || vendas.isEmpty()) {
                return new ArrayList<>();
            }

            for (VendaHistoricoListagemView venda : vendas) {
                consolidarVendaListagem(venda);
            }

            return vendas;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao consultar o histórico de vendas.",
                    e
            );
        }
    }

    /**
     * Busca todos os detalhes disponíveis de uma venda.
     */
    public VendaHistoricoDetalheView buscarDetalhesVenda(
            Integer vendaId
    ) {

        validarVendaId(vendaId);

        try (Connection conn = ConnectionFactory.getConnection()) {

            Venda venda = vendaDAO.buscarPorId(
                    conn,
                    vendaId
            );

            DadosVendaConvertidos dadosVenda =
                    validarEConverterVenda(
                            venda,
                            vendaId
                    );

            List<ItemVendaHistoricoView> itens =
                    itemVendaDAO.listarDetalhesPorVendaId(
                            conn,
                            vendaId
                    );

            validarItensVenda(
                    itens,
                    vendaId
            );

            ContaReceber contaReceber =
                    contaReceberDAO.buscarPorVendaId(
                            conn,
                            vendaId
                    );

            validarContaReceber(
                    venda,
                    dadosVenda,
                    contaReceber
            );

            List<MovimentacaoFinanceira> movimentacoes =
                    movimentacaoFinanceiraDAO.listarPorVendaId(
                            conn,
                            vendaId
                    );

            if (movimentacoes == null) {
                movimentacoes = new ArrayList<>();
            }

            AuditoriaEstornoVenda auditoria =
                    auditoriaEstornoVendaDAO.buscarPorVendaId(
                            conn,
                            vendaId
                    );

            validarAuditoria(
                    venda,
                    contaReceber,
                    auditoria
            );

            Cliente cliente = carregarCliente(
                    conn,
                    venda
            );

            Usuario usuarioVenda =
                    carregarUsuarioObrigatorio(
                            conn,
                            venda.getUsuarioId(),
                            "responsável pela venda"
                    );

            Usuario usuarioEstorno = null;

            if (auditoria != null) {
                usuarioEstorno =
                        carregarUsuarioObrigatorio(
                                conn,
                                auditoria.getUsuarioId(),
                                "responsável pelo estorno"
                        );
            }

            DadosMovimentacoesDetalhe dadosMovimentacoes =
                    identificarMovimentacoes(
                            movimentacoes,
                            venda,
                            dadosVenda,
                            contaReceber,
                            auditoria
                    );

            validarCenarioDetalhe(
                    venda,
                    dadosVenda,
                    contaReceber,
                    auditoria,
                    dadosMovimentacoes
            );

            return montarDetalheVenda(
                    venda,
                    dadosVenda,
                    cliente,
                    usuarioVenda,
                    itens,
                    contaReceber,
                    dadosMovimentacoes,
                    auditoria,
                    usuarioEstorno
            );

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao consultar os detalhes da venda.",
                    e
            );
        }
    }

    /**
     * Valida e normaliza o filtro antes da abertura da Connection.
     */
    private void validarFiltroHistorico(
            FiltroHistoricoVenda filtro
    ) {

        if (filtro == null) {
            throw new IllegalArgumentException(
                    "Filtro do histórico é obrigatório."
            );
        }

        filtro.setClienteOuDocumento(
                filtro.getClienteOuDocumento()
        );

        filtro.validar();
    }

    /**
     * Valida os dados de uma linha e monta seu resumo financeiro.
     */
    private void consolidarVendaListagem(
            VendaHistoricoListagemView venda
    ) {

        if (venda == null) {
            throw new IllegalStateException(
                    "A consulta retornou uma venda nula."
            );
        }

        Integer vendaId = venda.getVendaId();

        if (vendaId == null || vendaId <= 0) {
            throw new IllegalStateException(
                    "A consulta retornou uma venda com ID inválido."
            );
        }

        if (venda.getDataHora() == null) {
            throw new IllegalStateException(
                    "Venda " + vendaId
                            + " não possui data e hora."
            );
        }

        if (venda.getTipoVenda() == null) {
            throw new IllegalStateException(
                    "Venda " + vendaId
                            + " não possui tipo."
            );
        }

        if (venda.getStatusVenda() == null) {
            throw new IllegalStateException(
                    "Venda " + vendaId
                            + " não possui status."
            );
        }

        if (venda.getValorTotal() == null
                || venda.getValorTotal()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalStateException(
                    "Venda " + vendaId
                            + " possui valor total inválido."
            );
        }

        if (venda.getQuantidadeItens() == null
                || venda.getQuantidadeItens() <= 0) {

            throw new IllegalStateException(
                    "Venda " + vendaId
                            + " não possui produtos registrados."
            );
        }

        venda.setNomeCliente(
                venda.getNomeCliente()
        );

        validarEstruturaFinanceiraListagem(venda);

        venda.setResumoFinanceiro(
                montarResumoFinanceiro(venda)
        );
    }

    /**
     * Valida a coerência financeira necessária para a listagem.
     */
    private void validarEstruturaFinanceiraListagem(
            VendaHistoricoListagemView venda
    ) {

        Integer vendaId = venda.getVendaId();

        int quantidadeContas =
                venda.getQuantidadeContasVinculadas();

        int quantidadeEntradas =
                venda.getQuantidadeEntradasCompativeis();

        if (quantidadeContas < 0
                || quantidadeContas > 1) {

            throw new IllegalStateException(
                    "Venda " + vendaId
                            + " possui quantidade inconsistente "
                            + "de contas a receber."
            );
        }

        if (quantidadeEntradas < 0
                || quantidadeEntradas > 1) {

            throw new IllegalStateException(
                    "Venda " + vendaId
                            + " possui mais de uma entrada "
                            + "financeira original compatível."
            );
        }

        if (venda.getTipoVenda() == TipoVenda.A_VISTA) {

            validarListagemVendaAVista(
                    venda,
                    quantidadeContas,
                    quantidadeEntradas
            );

            return;
        }

        if (venda.getTipoVenda() == TipoVenda.A_PRAZO) {

            validarListagemVendaAPrazo(
                    venda,
                    quantidadeContas,
                    quantidadeEntradas
            );

            return;
        }

        throw new IllegalStateException(
                "Venda " + vendaId
                        + " possui tipo não suportado."
        );
    }

    private void validarListagemVendaAVista(
            VendaHistoricoListagemView venda,
            int quantidadeContas,
            int quantidadeEntradas
    ) {

        Integer vendaId = venda.getVendaId();

        validarFormaPagamentoFinanceira(
                venda.getFormaPagamentoVenda(),
                "forma de pagamento da venda " + vendaId
        );

        if (quantidadeContas != 0) {
            throw new IllegalStateException(
                    "Venda à vista " + vendaId
                            + " não pode possuir conta a receber."
            );
        }

        if (venda.getStatusVenda() == StatusVenda.PENDENTE) {

            throw new IllegalStateException(
                    "Venda à vista " + vendaId
                            + " não pode estar PENDENTE."
            );
        }

        if (quantidadeEntradas != 1
                || venda.getFormaPagamentoEntrada() == null) {

            throw new IllegalStateException(
                    "Venda à vista " + vendaId
                            + " não possui uma entrada financeira "
                            + "original válida."
            );
        }

        validarFormaPagamentoFinanceira(
                venda.getFormaPagamentoEntrada(),
                "forma de pagamento da entrada da venda "
                        + vendaId
        );

        if (venda.getFormaPagamentoVenda()
                != venda.getFormaPagamentoEntrada()) {

            throw new IllegalStateException(
                    "Venda à vista " + vendaId
                            + " possui formas de pagamento "
                            + "incompatíveis."
            );
        }
    }

    private void validarListagemVendaAPrazo(
            VendaHistoricoListagemView venda,
            int quantidadeContas,
            int quantidadeEntradas
    ) {

        Integer vendaId = venda.getVendaId();

        if (venda.getFormaPagamentoVenda()
                != FormaPagamento.A_PRAZO) {

            throw new IllegalStateException(
                    "Venda a prazo " + vendaId
                            + " não possui forma de pagamento "
                            + "A_PRAZO."
            );
        }

        if (quantidadeContas != 1
                || venda.getStatusContaReceber() == null) {

            throw new IllegalStateException(
                    "Venda a prazo " + vendaId
                            + " não possui uma conta a receber "
                            + "válida."
            );
        }

        if (venda.getStatusVenda() == StatusVenda.PENDENTE) {

            if (venda.getStatusContaReceber()
                    != StatusContaReceber.PENDENTE
                    || quantidadeEntradas != 0) {

                throw new IllegalStateException(
                        "Venda a prazo pendente " + vendaId
                                + " possui situação financeira "
                                + "incompatível."
                );
            }

            return;
        }

        if (venda.getStatusVenda() == StatusVenda.PAGA) {

            if (venda.getStatusContaReceber()
                    != StatusContaReceber.PAGA
                    || quantidadeEntradas != 1
                    || venda.getFormaPagamentoEntrada() == null) {

                throw new IllegalStateException(
                        "Venda a prazo paga " + vendaId
                                + " possui situação financeira "
                                + "incompatível."
                );
            }

            validarFormaPagamentoFinanceira(
                    venda.getFormaPagamentoEntrada(),
                    "forma de recebimento da venda "
                            + vendaId
            );

            return;
        }

        if (venda.getStatusVenda() == StatusVenda.ESTORNADA) {

            if (venda.getStatusContaReceber()
                    != StatusContaReceber.CANCELADA) {

                throw new IllegalStateException(
                        "Venda a prazo estornada " + vendaId
                                + " não possui conta CANCELADA."
                );
            }

            if (quantidadeEntradas == 1) {
                validarFormaPagamentoFinanceira(
                        venda.getFormaPagamentoEntrada(),
                        "forma de recebimento da venda "
                                + vendaId
                );
            }

            return;
        }

        throw new IllegalStateException(
                "Venda a prazo " + vendaId
                        + " possui status incompatível."
        );
    }

    /**
     * Monta o texto aprovado para a coluna Financeiro.
     */
    private String montarResumoFinanceiro(
            VendaHistoricoListagemView venda
    ) {

        if (venda.getStatusVenda() == StatusVenda.ESTORNADA) {
            return "Estornada";
        }

        if (venda.getTipoVenda() == TipoVenda.A_VISTA) {

            return montarResumoPagamentoAVista(
                    venda.getFormaPagamentoVenda()
            );
        }

        if (venda.getStatusVenda() == StatusVenda.PENDENTE) {
            return "Conta a receber";
        }

        return montarResumoRecebimentoAPrazo(
                venda.getFormaPagamentoEntrada()
        );
    }

    private String montarResumoPagamentoAVista(
            FormaPagamento formaPagamento
    ) {

        switch (formaPagamento) {
            case DINHEIRO:
                return "Pago em dinheiro";

            case PIX:
                return "Pago via PIX";

            case CARTAO:
                return "Pago em cartão";

            default:
                throw new IllegalStateException(
                        "Forma de pagamento inválida "
                                + "para venda à vista."
                );
        }
    }

    private String montarResumoRecebimentoAPrazo(
            FormaPagamento formaPagamento
    ) {

        switch (formaPagamento) {
            case DINHEIRO:
                return "Recebido em dinheiro";

            case PIX:
                return "Recebido via PIX";

            case CARTAO:
                return "Recebido em cartão";

            default:
                throw new IllegalStateException(
                        "Forma de pagamento inválida "
                                + "para recebimento de conta."
                );
        }
    }

    private void validarFormaPagamentoFinanceira(
            FormaPagamento formaPagamento,
            String contexto
    ) {

        if (formaPagamento != FormaPagamento.DINHEIRO
                && formaPagamento != FormaPagamento.PIX
                && formaPagamento != FormaPagamento.CARTAO) {

            throw new IllegalStateException(
                    contexto + " é inválida."
            );
        }
    }

    private void validarVendaId(Integer vendaId) {

        if (vendaId == null || vendaId <= 0) {
            throw new IllegalArgumentException(
                    "ID da venda deve ser maior que zero."
            );
        }
    }

    /**
     * Valida a venda encontrada e converte estritamente os valores textuais.
     */
    private DadosVendaConvertidos validarEConverterVenda(
            Venda venda,
            Integer vendaIdSolicitado
    ) {

        if (venda == null) {
            throw new IllegalArgumentException(
                    "Venda não encontrada."
            );
        }

        if (venda.getIdVenda() == null
                || venda.getIdVenda() <= 0
                || !vendaIdSolicitado.equals(
                venda.getIdVenda()
        )) {

            throw new IllegalStateException(
                    "Venda encontrada possui ID inconsistente."
            );
        }

        if (venda.getDataHora() == null) {
            throw new IllegalStateException(
                    "Venda " + vendaIdSolicitado
                            + " não possui data e hora."
            );
        }

        if (venda.getValorTotal() == null
                || venda.getValorTotal()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalStateException(
                    "Venda " + vendaIdSolicitado
                            + " possui valor total inválido."
            );
        }

        if (venda.getUsuarioId() == null
                || venda.getUsuarioId() <= 0) {

            throw new IllegalStateException(
                    "Venda " + vendaIdSolicitado
                            + " possui usuário responsável inválido."
            );
        }

        TipoVenda tipoVenda =
                converterEnumPersistido(
                        venda.getTipoVenda(),
                        TipoVenda.class,
                        "tipo da venda",
                        vendaIdSolicitado
                );

        FormaPagamento formaPagamento =
                converterEnumPersistido(
                        venda.getFormaPagamento(),
                        FormaPagamento.class,
                        "forma de pagamento",
                        vendaIdSolicitado
                );

        StatusVenda statusVenda =
                converterEnumPersistido(
                        venda.getStatus(),
                        StatusVenda.class,
                        "status da venda",
                        vendaIdSolicitado
                );

        if (tipoVenda == TipoVenda.A_VISTA) {

            validarFormaPagamentoFinanceira(
                    formaPagamento,
                    "Forma de pagamento da venda "
                            + vendaIdSolicitado
            );

        } else if (tipoVenda == TipoVenda.A_PRAZO) {

            if (formaPagamento != FormaPagamento.A_PRAZO) {
                throw new IllegalStateException(
                        "Venda a prazo " + vendaIdSolicitado
                                + " não possui forma de pagamento "
                                + "A_PRAZO."
                );
            }

            if (venda.getClienteId() == null
                    || venda.getClienteId() <= 0) {

                throw new IllegalStateException(
                        "Venda a prazo " + vendaIdSolicitado
                                + " não possui cliente válido."
                );
            }
        }

        return new DadosVendaConvertidos(
                tipoVenda,
                formaPagamento,
                statusVenda
        );
    }

    private <E extends Enum<E>> E converterEnumPersistido(
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

    private void validarItensVenda(
            List<ItemVendaHistoricoView> itens,
            Integer vendaId
    ) {

        if (itens == null || itens.isEmpty()) {
            throw new IllegalStateException(
                    "Venda " + vendaId
                            + " não possui itens registrados."
            );
        }

        for (ItemVendaHistoricoView item : itens) {

            if (item == null) {
                throw new IllegalStateException(
                        "Venda " + vendaId
                                + " possui item nulo."
                );
            }

            if (item.getProdutoId() == null
                    || item.getProdutoId() <= 0) {

                throw new IllegalStateException(
                        "Venda " + vendaId
                                + " possui item com produto inválido."
                );
            }

            if (item.getDescricaoProduto() == null
                    || item.getDescricaoProduto().isBlank()) {

                throw new IllegalStateException(
                        "Venda " + vendaId
                                + " possui produto sem descrição."
                );
            }

            if (item.getQuantidade() == null
                    || item.getQuantidade() <= 0) {

                throw new IllegalStateException(
                        "Venda " + vendaId
                                + " possui item com quantidade inválida."
                );
            }

            if (item.getPrecoUnitario() == null
                    || item.getPrecoUnitario()
                    .compareTo(BigDecimal.ZERO) < 0) {

                throw new IllegalStateException(
                        "Venda " + vendaId
                                + " possui preço unitário inválido."
                );
            }

            if (item.getSubtotal() == null
                    || item.getSubtotal()
                    .compareTo(BigDecimal.ZERO) < 0) {

                throw new IllegalStateException(
                        "Venda " + vendaId
                                + " possui subtotal inválido."
                );
            }
        }
    }

    private void validarContaReceber(
            Venda venda,
            DadosVendaConvertidos dadosVenda,
            ContaReceber contaReceber
    ) {

        Integer vendaId = venda.getIdVenda();

        if (dadosVenda.tipoVenda() == TipoVenda.A_VISTA) {

            if (contaReceber != null) {
                throw new IllegalStateException(
                        "Venda à vista " + vendaId
                                + " possui conta a receber vinculada."
                );
            }

            return;
        }

        if (contaReceber == null) {
            throw new IllegalStateException(
                    "Venda a prazo " + vendaId
                            + " não possui conta a receber."
            );
        }

        if (contaReceber.getIdConta() == null
                || contaReceber.getIdConta() <= 0) {

            throw new IllegalStateException(
                    "Venda " + vendaId
                            + " possui conta a receber com ID inválido."
            );
        }

        if (!vendaId.equals(
                contaReceber.getVendaId()
        )) {

            throw new IllegalStateException(
                    "Conta a receber possui vínculo incompatível "
                            + "com a venda " + vendaId + "."
            );
        }

        if (venda.getClienteId() == null
                || !venda.getClienteId().equals(
                contaReceber.getClienteId()
        )) {

            throw new IllegalStateException(
                    "Conta a receber possui cliente incompatível "
                            + "com a venda " + vendaId + "."
            );
        }

        if (contaReceber.getStatus() == null) {
            throw new IllegalStateException(
                    "Conta a receber da venda " + vendaId
                            + " não possui status."
            );
        }

        if (contaReceber.getValor() == null
                || contaReceber.getValor()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalStateException(
                    "Conta a receber da venda " + vendaId
                            + " possui valor inválido."
            );
        }

        if (contaReceber.getValor().compareTo(
                venda.getValorTotal()
        ) != 0) {

            throw new IllegalStateException(
                    "Valor da conta a receber é incompatível "
                            + "com o total da venda " + vendaId + "."
            );
        }
    }

    private void validarAuditoria(
            Venda venda,
            ContaReceber contaReceber,
            AuditoriaEstornoVenda auditoria
    ) {

        if (auditoria == null) {
            return;
        }

        Integer vendaId = venda.getIdVenda();

        if (auditoria.getIdAuditoria() == null
                || auditoria.getIdAuditoria() <= 0) {

            throw new IllegalStateException(
                    "Auditoria da venda " + vendaId
                            + " possui ID inválido."
            );
        }

        if (!vendaId.equals(
                auditoria.getVendaId()
        )) {

            throw new IllegalStateException(
                    "Auditoria possui vínculo incompatível "
                            + "com a venda " + vendaId + "."
            );
        }

        if (auditoria.getUsuarioId() == null
                || auditoria.getUsuarioId() <= 0) {

            throw new IllegalStateException(
                    "Auditoria da venda " + vendaId
                            + " possui usuário inválido."
            );
        }

        if (auditoria.getDataHora() == null) {
            throw new IllegalStateException(
                    "Auditoria da venda " + vendaId
                            + " não possui data e hora."
            );
        }

        if (auditoria.getMotivo() == null
                || auditoria.getMotivo().isBlank()) {

            throw new IllegalStateException(
                    "Auditoria da venda " + vendaId
                            + " não possui motivo."
            );
        }

        if (auditoria.getStatusVendaAnterior() == null) {
            throw new IllegalStateException(
                    "Auditoria da venda " + vendaId
                            + " não possui status anterior."
            );
        }

        if (contaReceber == null) {

            if (auditoria.getContaReceberId() != null
                    || auditoria.getStatusContaReceberAnterior()
                    != null) {

                throw new IllegalStateException(
                        "Auditoria da venda " + vendaId
                                + " possui conta incompatível."
                );
            }

        } else {

            if (!contaReceber.getIdConta().equals(
                    auditoria.getContaReceberId()
            )) {

                throw new IllegalStateException(
                        "Auditoria possui conta incompatível "
                                + "com a venda " + vendaId + "."
                );
            }

            if (auditoria.getStatusContaReceberAnterior()
                    == null) {

                throw new IllegalStateException(
                        "Auditoria da venda " + vendaId
                                + " não possui status anterior "
                                + "da conta."
                );
            }
        }

        boolean possuiMovimentacaoOriginal =
                auditoria.getMovimentacaoOriginalId() != null;

        boolean possuiMovimentacaoSaida =
                auditoria.getMovimentacaoSaidaId() != null;

        if (possuiMovimentacaoOriginal
                != possuiMovimentacaoSaida) {

            throw new IllegalStateException(
                    "Auditoria da venda " + vendaId
                            + " possui vínculos financeiros "
                            + "incompletos."
            );
        }
    }

    private Cliente carregarCliente(
            Connection conn,
            Venda venda
    ) {

        if (venda.getClienteId() == null) {
            return null;
        }

        Cliente cliente =
                clienteDAO.buscarPorIdComPrazo(
                        conn,
                        venda.getClienteId()
                );

        if (cliente == null) {
            throw new IllegalStateException(
                    "Cliente vinculado à venda "
                            + venda.getIdVenda()
                            + " não foi encontrado."
            );
        }

        if (cliente.getIdCliente() == null
                || !venda.getClienteId().equals(
                cliente.getIdCliente()
        )) {

            throw new IllegalStateException(
                    "Cliente vinculado à venda "
                            + venda.getIdVenda()
                            + " possui ID inconsistente."
            );
        }

        return cliente;
    }

    private Usuario carregarUsuarioObrigatorio(
            Connection conn,
            Integer usuarioId,
            String contexto
    ) {

        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalStateException(
                    "Usuário " + contexto
                            + " possui ID inválido."
            );
        }

        Usuario usuario = usuarioDAO.buscarPorId(
                conn,
                usuarioId
        );

        if (usuario == null) {
            throw new IllegalStateException(
                    "Usuário " + contexto
                            + " não foi encontrado."
            );
        }

        if (usuario.getIdUsuario() == null
                || !usuarioId.equals(
                usuario.getIdUsuario()
        )) {

            throw new IllegalStateException(
                    "Usuário " + contexto
                            + " possui ID inconsistente."
            );
        }

        return usuario;
    }

    /**
     * Valida e identifica as movimentações original e de saída.
     */
    private DadosMovimentacoesDetalhe identificarMovimentacoes(
            List<MovimentacaoFinanceira> movimentacoes,
            Venda venda,
            DadosVendaConvertidos dadosVenda,
            ContaReceber contaReceber,
            AuditoriaEstornoVenda auditoria
    ) {

        List<MovimentacaoFinanceira> originais =
                new ArrayList<>();

        List<MovimentacaoFinanceira> saidas =
                new ArrayList<>();

        for (MovimentacaoFinanceira movimentacao
                : movimentacoes) {

            validarMovimentacao(
                    movimentacao,
                    venda,
                    dadosVenda,
                    contaReceber
            );

            if (ehMovimentacaoOriginal(movimentacao)) {
                originais.add(movimentacao);
            }

            if (ehMovimentacaoSaida(movimentacao)) {
                saidas.add(movimentacao);
            }
        }

        if (originais.size() > 1) {
            throw new IllegalStateException(
                    "Venda " + venda.getIdVenda()
                            + " possui mais de uma entrada "
                            + "financeira original compatível."
            );
        }

        if (saidas.size() > 1) {
            throw new IllegalStateException(
                    "Venda " + venda.getIdVenda()
                            + " possui mais de uma saída "
                            + "financeira de estorno compatível."
            );
        }

        MovimentacaoFinanceira original;
        MovimentacaoFinanceira saida;

        if (auditoria == null) {

            if (!saidas.isEmpty()) {
                throw new IllegalStateException(
                        "Venda " + venda.getIdVenda()
                                + " possui saída de estorno sem "
                                + "auditoria correspondente."
                );
            }

            original =
                    originais.isEmpty()
                            ? null
                            : originais.get(0);

            saida = null;

        } else {

            boolean auditoriaSemMovimentacoes =
                    auditoria.getMovimentacaoOriginalId() == null
                            && auditoria.getMovimentacaoSaidaId() == null;

            if (auditoriaSemMovimentacoes
                    && (!originais.isEmpty()
                    || !saidas.isEmpty())) {

                throw new IllegalStateException(
                        "Venda " + venda.getIdVenda()
                                + " possui auditoria sem vínculos "
                                + "financeiros, mas existem movimentações "
                                + "financeiras vinculadas."
                );
            }

            original = localizarMovimentacaoAuditada(
                    movimentacoes,
                    auditoria.getMovimentacaoOriginalId(),
                    true,
                    venda.getIdVenda()
            );

            saida = localizarMovimentacaoAuditada(
                    movimentacoes,
                    auditoria.getMovimentacaoSaidaId(),
                    false,
                    venda.getIdVenda()
            );
        }

        validarValoresMovimentacoes(
                venda,
                contaReceber,
                original,
                saida
        );

        return new DadosMovimentacoesDetalhe(
                original,
                saida
        );
    }

    private void validarMovimentacao(
            MovimentacaoFinanceira movimentacao,
            Venda venda,
            DadosVendaConvertidos dadosVenda,
            ContaReceber contaReceber
    ) {

        Integer vendaId = venda.getIdVenda();

        if (movimentacao == null) {
            throw new IllegalStateException(
                    "Venda " + vendaId
                            + " possui movimentação nula."
            );
        }

        if (movimentacao.getIdMovimentacao() == null
                || movimentacao.getIdMovimentacao() <= 0) {

            throw new IllegalStateException(
                    "Venda " + vendaId
                            + " possui movimentação com ID inválido."
            );
        }

        if (!vendaId.equals(
                movimentacao.getVendaId()
        )) {

            throw new IllegalStateException(
                    "Movimentação financeira possui vínculo "
                            + "incompatível com a venda "
                            + vendaId + "."
            );
        }

        if (movimentacao.getDataHora() == null
                || movimentacao.getTipo() == null
                || movimentacao.getOrigem() == null
                || movimentacao.getFormaPagamento() == null
                || movimentacao.getValor() == null) {

            throw new IllegalStateException(
                    "Movimentação financeira da venda "
                            + vendaId
                            + " possui dados obrigatórios ausentes."
            );
        }

        if (movimentacao.getValor()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalStateException(
                    "Movimentação financeira da venda "
                            + vendaId
                            + " possui valor inválido."
            );
        }

        validarFormaPagamentoFinanceira(
                movimentacao.getFormaPagamento(),
                "Forma de pagamento da movimentação "
                        + movimentacao.getIdMovimentacao()
        );

        OrigemMovimentacaoFinanceira origem =
                movimentacao.getOrigem();

        if (origem
                == OrigemMovimentacaoFinanceira.VENDA_A_VISTA) {

            if (movimentacao.getTipo()
                    != TipoMovimentacaoFinanceira.ENTRADA
                    || dadosVenda.tipoVenda()
                    != TipoVenda.A_VISTA
                    || movimentacao.getContaReceberId()
                    != null) {

                throw new IllegalStateException(
                        "Movimentação "
                                + movimentacao.getIdMovimentacao()
                                + " é incompatível com a venda "
                                + vendaId + "."
                );
            }

            return;
        }

        if (origem
                == OrigemMovimentacaoFinanceira.RECEBIMENTO_CONTA) {

            validarMovimentacaoVinculadaAConta(
                    movimentacao,
                    contaReceber,
                    vendaId,
                    TipoMovimentacaoFinanceira.ENTRADA,
                    dadosVenda.tipoVenda()
            );

            return;
        }

        if (origem
                == OrigemMovimentacaoFinanceira
                .ESTORNO_VENDA_A_VISTA) {

            if (movimentacao.getTipo()
                    != TipoMovimentacaoFinanceira.SAIDA
                    || dadosVenda.tipoVenda()
                    != TipoVenda.A_VISTA
                    || movimentacao.getContaReceberId()
                    != null) {

                throw new IllegalStateException(
                        "Movimentação de saída "
                                + movimentacao.getIdMovimentacao()
                                + " é incompatível com a venda "
                                + vendaId + "."
                );
            }

            return;
        }

        if (origem
                == OrigemMovimentacaoFinanceira
                .ESTORNO_RECEBIMENTO_CONTA) {

            validarMovimentacaoVinculadaAConta(
                    movimentacao,
                    contaReceber,
                    vendaId,
                    TipoMovimentacaoFinanceira.SAIDA,
                    dadosVenda.tipoVenda()
            );

            return;
        }

        throw new IllegalStateException(
                "Movimentação "
                        + movimentacao.getIdMovimentacao()
                        + " possui origem não suportada."
        );
    }

    private void validarMovimentacaoVinculadaAConta(
            MovimentacaoFinanceira movimentacao,
            ContaReceber contaReceber,
            Integer vendaId,
            TipoMovimentacaoFinanceira tipoEsperado,
            TipoVenda tipoVenda
    ) {

        if (movimentacao.getTipo() != tipoEsperado
                || tipoVenda != TipoVenda.A_PRAZO
                || contaReceber == null
                || movimentacao.getContaReceberId() == null
                || !contaReceber.getIdConta().equals(
                movimentacao.getContaReceberId()
        )) {

            throw new IllegalStateException(
                    "Movimentação "
                            + movimentacao.getIdMovimentacao()
                            + " possui conta incompatível "
                            + "com a venda " + vendaId + "."
            );
        }
    }

    private boolean ehMovimentacaoOriginal(
            MovimentacaoFinanceira movimentacao
    ) {
        return movimentacao.getTipo()
                == TipoMovimentacaoFinanceira.ENTRADA
                && (
                movimentacao.getOrigem()
                        == OrigemMovimentacaoFinanceira.VENDA_A_VISTA
                        || movimentacao.getOrigem()
                        == OrigemMovimentacaoFinanceira.RECEBIMENTO_CONTA
        );
    }

    private boolean ehMovimentacaoSaida(
            MovimentacaoFinanceira movimentacao
    ) {
        return movimentacao.getTipo()
                == TipoMovimentacaoFinanceira.SAIDA
                && (
                movimentacao.getOrigem()
                        == OrigemMovimentacaoFinanceira
                        .ESTORNO_VENDA_A_VISTA
                        || movimentacao.getOrigem()
                        == OrigemMovimentacaoFinanceira
                        .ESTORNO_RECEBIMENTO_CONTA
        );
    }

    private MovimentacaoFinanceira localizarMovimentacaoAuditada(
            List<MovimentacaoFinanceira> movimentacoes,
            Integer movimentacaoId,
            boolean original,
            Integer vendaId
    ) {

        if (movimentacaoId == null) {
            return null;
        }

        MovimentacaoFinanceira encontrada = null;

        for (MovimentacaoFinanceira movimentacao
                : movimentacoes) {

            if (movimentacaoId.equals(
                    movimentacao.getIdMovimentacao()
            )) {
                encontrada = movimentacao;
                break;
            }
        }

        if (encontrada == null) {
            throw new IllegalStateException(
                    "Movimentação " + movimentacaoId
                            + " registrada na auditoria da venda "
                            + vendaId
                            + " não foi encontrada."
            );
        }

        boolean compativel =
                original
                        ? ehMovimentacaoOriginal(encontrada)
                        : ehMovimentacaoSaida(encontrada);

        if (!compativel) {
            throw new IllegalStateException(
                    "Movimentação " + movimentacaoId
                            + " registrada na auditoria da venda "
                            + vendaId
                            + " possui tipo ou origem incompatível."
            );
        }

        return encontrada;
    }

    private void validarValoresMovimentacoes(
            Venda venda,
            ContaReceber contaReceber,
            MovimentacaoFinanceira original,
            MovimentacaoFinanceira saida
    ) {

        if (original != null) {

            BigDecimal valorEsperado =
                    contaReceber != null
                            ? contaReceber.getValor()
                            : venda.getValorTotal();

            if (original.getValor().compareTo(
                    valorEsperado
            ) != 0) {

                throw new IllegalStateException(
                        "Movimentação original da venda "
                                + venda.getIdVenda()
                                + " possui valor incompatível."
                );
            }
        }

        if (saida != null) {

            if (original == null) {
                throw new IllegalStateException(
                        "Venda " + venda.getIdVenda()
                                + " possui saída sem entrada "
                                + "financeira original."
                );
            }

            if (saida.getValor().compareTo(
                    original.getValor()
            ) != 0) {

                throw new IllegalStateException(
                        "Saída do estorno da venda "
                                + venda.getIdVenda()
                                + " possui valor incompatível."
                );
            }

            if (saida.getFormaPagamento()
                    != original.getFormaPagamento()) {

                throw new IllegalStateException(
                        "Saída do estorno da venda "
                                + venda.getIdVenda()
                                + " possui forma de pagamento "
                                + "incompatível."
                );
            }
        }
    }

    /**
     * Valida a combinação final entre venda, conta, auditoria e movimentos.
     */
    private void validarCenarioDetalhe(
            Venda venda,
            DadosVendaConvertidos dadosVenda,
            ContaReceber contaReceber,
            AuditoriaEstornoVenda auditoria,
            DadosMovimentacoesDetalhe movimentacoes
    ) {

        if (dadosVenda.tipoVenda() == TipoVenda.A_VISTA) {

            validarCenarioDetalheAVista(
                    venda,
                    dadosVenda,
                    contaReceber,
                    auditoria,
                    movimentacoes
            );

            return;
        }

        validarCenarioDetalheAPrazo(
                venda,
                dadosVenda,
                contaReceber,
                auditoria,
                movimentacoes
        );
    }

    private void validarCenarioDetalheAVista(
            Venda venda,
            DadosVendaConvertidos dadosVenda,
            ContaReceber contaReceber,
            AuditoriaEstornoVenda auditoria,
            DadosMovimentacoesDetalhe movimentacoes
    ) {

        Integer vendaId = venda.getIdVenda();

        if (contaReceber != null) {
            throw new IllegalStateException(
                    "Venda à vista " + vendaId
                            + " possui conta a receber."
            );
        }

        if (movimentacoes.original() != null
                && movimentacoes.original()
                .getFormaPagamento()
                != dadosVenda.formaPagamento()) {

            throw new IllegalStateException(
                    "Entrada financeira da venda à vista "
                            + vendaId
                            + " possui forma incompatível."
            );
        }

        if (dadosVenda.statusVenda() == StatusVenda.PAGA) {

            if (auditoria != null
                    || movimentacoes.original() == null
                    || movimentacoes.saida() != null) {

                throw new IllegalStateException(
                        "Venda à vista paga " + vendaId
                                + " possui situação de histórico "
                                + "incompatível."
                );
            }

            return;
        }

        if (dadosVenda.statusVenda() == StatusVenda.ESTORNADA) {

            if (auditoria == null
                    || auditoria.getStatusVendaAnterior()
                    != StatusVenda.PAGA
                    || movimentacoes.original() == null
                    || movimentacoes.saida() == null) {

                throw new IllegalStateException(
                        "Venda à vista estornada " + vendaId
                                + " possui dados de auditoria ou "
                                + "movimentações incompatíveis."
                );
            }

            return;
        }

        throw new IllegalStateException(
                "Venda à vista " + vendaId
                        + " possui status incompatível."
        );
    }

    private void validarCenarioDetalheAPrazo(
            Venda venda,
            DadosVendaConvertidos dadosVenda,
            ContaReceber contaReceber,
            AuditoriaEstornoVenda auditoria,
            DadosMovimentacoesDetalhe movimentacoes
    ) {

        Integer vendaId = venda.getIdVenda();

        if (contaReceber == null) {
            throw new IllegalStateException(
                    "Venda a prazo " + vendaId
                            + " não possui conta a receber."
            );
        }

        if (dadosVenda.statusVenda() == StatusVenda.PENDENTE) {

            if (contaReceber.getStatus()
                    != StatusContaReceber.PENDENTE
                    || auditoria != null
                    || movimentacoes.original() != null
                    || movimentacoes.saida() != null) {

                throw new IllegalStateException(
                        "Venda a prazo pendente " + vendaId
                                + " possui situação incompatível."
                );
            }

            return;
        }

        if (dadosVenda.statusVenda() == StatusVenda.PAGA) {

            if (contaReceber.getStatus()
                    != StatusContaReceber.PAGA
                    || auditoria != null
                    || movimentacoes.original() == null
                    || movimentacoes.saida() != null) {

                throw new IllegalStateException(
                        "Venda a prazo paga " + vendaId
                                + " possui situação incompatível."
                );
            }

            return;
        }

        if (dadosVenda.statusVenda() == StatusVenda.ESTORNADA) {

            if (contaReceber.getStatus()
                    != StatusContaReceber.CANCELADA
                    || auditoria == null) {

                throw new IllegalStateException(
                        "Venda a prazo estornada " + vendaId
                                + " não possui conta cancelada "
                                + "e auditoria válidas."
                );
            }

            validarCenarioAnteriorAuditoriaAPrazo(
                    vendaId,
                    auditoria,
                    movimentacoes
            );

            return;
        }

        throw new IllegalStateException(
                "Venda a prazo " + vendaId
                        + " possui status incompatível."
        );
    }

    private void validarCenarioAnteriorAuditoriaAPrazo(
            Integer vendaId,
            AuditoriaEstornoVenda auditoria,
            DadosMovimentacoesDetalhe movimentacoes
    ) {

        if (auditoria.getStatusVendaAnterior()
                == StatusVenda.PENDENTE
                && auditoria.getStatusContaReceberAnterior()
                == StatusContaReceber.PENDENTE) {

            if (movimentacoes.original() != null
                    || movimentacoes.saida() != null) {

                throw new IllegalStateException(
                        "Venda a prazo estornada " + vendaId
                                + " era pendente, mas possui "
                                + "movimentações financeiras."
                );
            }

            return;
        }

        if (auditoria.getStatusVendaAnterior()
                == StatusVenda.PAGA
                && auditoria.getStatusContaReceberAnterior()
                == StatusContaReceber.PAGA) {

            if (movimentacoes.original() == null
                    || movimentacoes.saida() == null) {

                throw new IllegalStateException(
                        "Venda a prazo estornada " + vendaId
                                + " era paga, mas não possui "
                                + "as movimentações esperadas."
                );
            }

            return;
        }

        throw new IllegalStateException(
                "Auditoria da venda a prazo " + vendaId
                        + " possui estados anteriores "
                        + "incompatíveis."
        );
    }

    /**
     * Monta o ViewModel final dos detalhes.
     */
    private VendaHistoricoDetalheView montarDetalheVenda(
            Venda venda,
            DadosVendaConvertidos dadosVenda,
            Cliente cliente,
            Usuario usuarioVenda,
            List<ItemVendaHistoricoView> itens,
            ContaReceber contaReceber,
            DadosMovimentacoesDetalhe movimentacoes,
            AuditoriaEstornoVenda auditoria,
            Usuario usuarioEstorno
    ) {

        MovimentacaoFinanceira original =
                movimentacoes.original();

        MovimentacaoFinanceira saida =
                movimentacoes.saida();

        return new VendaHistoricoDetalheView(
                venda.getIdVenda(),
                venda.getDataHora(),
                dadosVenda.tipoVenda(),
                dadosVenda.formaPagamento(),
                dadosVenda.statusVenda(),
                venda.getValorTotal(),

                cliente != null
                        ? cliente.getIdCliente()
                        : null,

                cliente != null
                        ? cliente.getNome()
                        : CLIENTE_NAO_IDENTIFICADO,

                cliente != null
                        ? cliente.getDocumento()
                        : null,

                usuarioVenda.getIdUsuario(),
                usuarioVenda.getNome(),

                itens,

                contaReceber != null
                        ? contaReceber.getIdConta()
                        : null,

                contaReceber != null
                        ? contaReceber.getStatus()
                        : null,

                contaReceber != null
                        ? contaReceber.getValor()
                        : null,

                original != null
                        ? original.getIdMovimentacao()
                        : null,

                original != null
                        ? original.getTipo()
                        : null,

                original != null
                        ? original.getOrigem()
                        : null,

                original != null
                        ? original.getFormaPagamento()
                        : null,

                original != null
                        ? original.getValor()
                        : null,

                original != null
                        ? original.getDataHora()
                        : null,

                saida != null
                        ? saida.getIdMovimentacao()
                        : null,

                saida != null
                        ? saida.getTipo()
                        : null,

                saida != null
                        ? saida.getOrigem()
                        : null,

                saida != null
                        ? saida.getFormaPagamento()
                        : null,

                saida != null
                        ? saida.getValor()
                        : null,

                saida != null
                        ? saida.getDataHora()
                        : null,

                auditoria != null
                        ? auditoria.getIdAuditoria()
                        : null,

                usuarioEstorno != null
                        ? usuarioEstorno.getIdUsuario()
                        : null,

                usuarioEstorno != null
                        ? usuarioEstorno.getNome()
                        : null,

                auditoria != null
                        ? auditoria.getDataHora()
                        : null,

                auditoria != null
                        ? auditoria.getMotivo()
                        : null,

                auditoria != null
                        ? auditoria.getStatusVendaAnterior()
                        : null,

                auditoria != null
                        ? auditoria.getStatusContaReceberAnterior()
                        : null
        );
    }

    /**
     * Valores enum convertidos da entidade Venda.
     */
    private record DadosVendaConvertidos(
            TipoVenda tipoVenda,
            FormaPagamento formaPagamento,
            StatusVenda statusVenda
    ) {
    }

    /**
     * Movimentações identificadas para os detalhes.
     */
    private record DadosMovimentacoesDetalhe(
            MovimentacaoFinanceira original,
            MovimentacaoFinanceira saida
    ) {
    }
}