package br.com.luis.service;

import br.com.luis.dao.UsuarioDAO;
import br.com.luis.dao.VendaDAO;
import br.com.luis.model.StatusVenda;
import br.com.luis.model.Usuario;
import br.com.luis.util.ConnectionFactory;
import br.com.luis.viewmodel.FiltroRelatorioDescontoVenda;
import br.com.luis.viewmodel.ResultadoRelatorioDescontoVenda;
import br.com.luis.viewmodel.VendaDescontoRelatorioDados;
import br.com.luis.viewmodel.VendaDescontoRelatorioView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Coordena a consulta protegida do relatório de descontos concedidos.
 *
 * Valida a autorização persistida, confere as invariantes históricas entre
 * Venda e ItemVenda, cria as linhas finais e totaliza somente o conjunto que o
 * DAO devolveu após todos os filtros.
 */
public class RelatorioDescontoVendaService {

    private static final int ESCALA_MONETARIA = 2;
    private static final RoundingMode ARREDONDAMENTO_MONETARIO =
            RoundingMode.HALF_UP;

    private static final String STATUS_ATIVO = "ATIVO";
    private static final String PERFIL_ADMIN = "ADMIN";
    private static final String CLIENTE_NAO_IDENTIFICADO =
            "Consumidor não identificado";

    private static final String MENSAGEM_ACESSO_NEGADO =
            "Usuário não autorizado a consultar o relatório de descontos concedidos.";

    private final VendaDAO vendaDAO;
    private final UsuarioDAO usuarioDAO;

    /**
     * Cria o Service com os DAOs utilizados pela consulta de leitura.
     */
    public RelatorioDescontoVendaService() {
        this.vendaDAO = new VendaDAO();
        this.usuarioDAO = new UsuarioDAO();
    }

    /**
     * Consulta as vendas válidas com desconto para o período e tipo informados.
     *
     * @param filtro fotografia imutável dos filtros solicitados.
     * @param usuarioId identificador do usuário que solicita a consulta.
     * @return resultado imutável consolidado sobre as linhas finais.
     */
    public ResultadoRelatorioDescontoVenda consultar(
            FiltroRelatorioDescontoVenda filtro,
            Integer usuarioId
    ) {
        if (filtro == null) {
            throw new IllegalArgumentException(
                    "Filtro do relatório de descontos não pode ser nulo."
            );
        }

        filtro.validar();
        validarUsuarioId(usuarioId);

        if (LocalDate.MAX.equals(filtro.getDataFinal())) {
            throw new IllegalArgumentException(
                    "A data final informada não permite calcular "
                            + "o limite exclusivo do período."
            );
        }

        try (Connection conn = ConnectionFactory.getConnection()) {
            Usuario usuario = usuarioDAO.buscarPorId(conn, usuarioId);
            validarAutorizacao(usuario, usuarioId);

            List<VendaDescontoRelatorioDados> dadosConsultados =
                    vendaDAO.listarParaRelatorioDescontos(conn, filtro);

            if (dadosConsultados == null) {
                throw new IllegalStateException(
                        "A consulta do relatório de descontos retornou uma lista nula."
                );
            }

            List<VendaDescontoRelatorioView> vendas = new ArrayList<>();

            for (VendaDescontoRelatorioDados dados : dadosConsultados) {
                if (dados == null) {
                    throw new IllegalStateException(
                            "A consulta do relatório de descontos retornou uma linha nula."
                    );
                }

                vendas.add(criarLinhaValidada(dados, filtro));
            }

            BigDecimal totalPromocional = criarValorMonetarioZero();
            BigDecimal totalGlobal = criarValorMonetarioZero();
            BigDecimal totalDescontos = criarValorMonetarioZero();

            for (VendaDescontoRelatorioView venda : vendas) {
                totalPromocional = totalPromocional.add(
                        venda.getDescontoPromocional()
                );
                totalGlobal = totalGlobal.add(venda.getDescontoGlobal());
                totalDescontos = totalDescontos.add(venda.getDescontoTotal());
            }

            totalPromocional = normalizarValorMonetario(totalPromocional);
            totalGlobal = normalizarValorMonetario(totalGlobal);
            totalDescontos = normalizarValorMonetario(totalDescontos);

            try {
                return new ResultadoRelatorioDescontoVenda(
                        filtro,
                        vendas,
                        vendas.size(),
                        totalPromocional,
                        totalGlobal,
                        totalDescontos
                );

            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                        "Incoerência ao consolidar o relatório de descontos concedidos.",
                        e
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao acessar o banco durante a consulta "
                            + "do relatório de descontos concedidos.",
                    e
            );
        }
    }

    private VendaDescontoRelatorioView criarLinhaValidada(
            VendaDescontoRelatorioDados dados,
            FiltroRelatorioDescontoVenda filtro
    ) {
        Integer vendaId = dados.getVendaId();

        if (dados.getStatusVenda() != StatusVenda.PAGA
                && dados.getStatusVenda() != StatusVenda.PENDENTE) {

            throw incoerenciaHistorica(
                    vendaId,
                    "status diferente de PAGA ou PENDENTE"
            );
        }

        if (filtro.getTipoVenda() != null
                && dados.getTipoVenda() != filtro.getTipoVenda()) {

            throw incoerenciaHistorica(
                    vendaId,
                    "tipo diferente do filtro aplicado"
            );
        }

        BigDecimal valorTotalVenda =
                normalizarValorMonetario(dados.getValorTotalVenda());
        BigDecimal descontoGlobalVenda =
                normalizarValorMonetario(
                        dados.getValorDescontoGlobalVenda()
                );
        BigDecimal valorBruto =
                normalizarValorMonetario(dados.getValorBrutoItens());
        BigDecimal descontoPromocional =
                normalizarValorMonetario(
                        dados.getDescontoPromocionalItens()
                );
        BigDecimal descontoGlobal =
                normalizarValorMonetario(dados.getDescontoGlobalItens());
        BigDecimal valorLiquido =
                normalizarValorMonetario(dados.getValorLiquidoItens());

        validarValorNaoNegativo(vendaId, "valor total da venda", valorTotalVenda);
        validarValorNaoNegativo(
                vendaId,
                "desconto global consolidado da venda",
                descontoGlobalVenda
        );
        validarValorNaoNegativo(vendaId, "valor bruto dos itens", valorBruto);
        validarValorNaoNegativo(
                vendaId,
                "desconto promocional dos itens",
                descontoPromocional
        );
        validarValorNaoNegativo(
                vendaId,
                "desconto global dos itens",
                descontoGlobal
        );
        validarValorNaoNegativo(
                vendaId,
                "valor líquido dos itens",
                valorLiquido
        );

        BigDecimal descontoTotal = normalizarValorMonetario(
                descontoPromocional.add(descontoGlobal)
        );

        if (descontoTotal.signum() <= 0) {
            throw incoerenciaHistorica(
                    vendaId,
                    "desconto total não positivo"
            );
        }

        BigDecimal valorLiquidoCalculado = normalizarValorMonetario(
                valorBruto.subtract(descontoTotal)
        );

        if (valorLiquidoCalculado.compareTo(valorLiquido) != 0) {
            throw incoerenciaHistorica(
                    vendaId,
                    "valor bruto menos descontos difere do líquido dos itens"
            );
        }

        if (descontoGlobal.compareTo(descontoGlobalVenda) != 0) {
            throw incoerenciaHistorica(
                    vendaId,
                    "desconto global dos itens difere do consolidado da venda"
            );
        }

        if (valorLiquido.compareTo(valorTotalVenda) != 0) {
            throw incoerenciaHistorica(
                    vendaId,
                    "valor líquido dos itens difere do total da venda"
            );
        }

        String cliente = dados.getClienteNome();

        if (cliente == null || cliente.isBlank()) {
            cliente = CLIENTE_NAO_IDENTIFICADO;
        }

        try {
            return new VendaDescontoRelatorioView(
                    vendaId,
                    dados.getDataHora(),
                    cliente,
                    dados.getTipoVenda(),
                    valorBruto,
                    descontoPromocional,
                    descontoGlobal,
                    descontoTotal,
                    valorLiquido
            );

        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Venda " + vendaId
                            + " não pôde ser representada no relatório de descontos.",
                    e
            );
        }
    }

    private void validarUsuarioId(Integer usuarioId) {
        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException(
                    "ID do usuário deve ser maior que zero."
            );
        }
    }

    private void validarAutorizacao(
            Usuario usuario,
            Integer usuarioIdSolicitado
    ) {
        boolean usuarioAutorizado =
                usuario != null
                        && usuarioIdSolicitado.equals(usuario.getIdUsuario())
                        && STATUS_ATIVO.equals(usuario.getStatus())
                        && PERFIL_ADMIN.equals(usuario.getPerfil());

        if (!usuarioAutorizado) {
            throw new SecurityException(MENSAGEM_ACESSO_NEGADO);
        }
    }

    private void validarValorNaoNegativo(
            Integer vendaId,
            String nomeCampo,
            BigDecimal valor
    ) {
        if (valor.signum() < 0) {
            throw incoerenciaHistorica(
                    vendaId,
                    nomeCampo + " negativo"
            );
        }
    }

    private IllegalStateException incoerenciaHistorica(
            Integer vendaId,
            String detalhe
    ) {
        return new IllegalStateException(
                "Inconsistência histórica na venda " + vendaId + ": "
                        + detalhe + "."
        );
    }

    private BigDecimal criarValorMonetarioZero() {
        return BigDecimal.ZERO.setScale(
                ESCALA_MONETARIA,
                ARREDONDAMENTO_MONETARIO
        );
    }

    private BigDecimal normalizarValorMonetario(BigDecimal valor) {
        if (valor == null) {
            throw new IllegalStateException(
                    "Valor monetário histórico obrigatório não foi informado."
            );
        }

        return valor.setScale(
                ESCALA_MONETARIA,
                ARREDONDAMENTO_MONETARIO
        );
    }
}
