package br.com.luis.service;

import br.com.luis.dao.ContaReceberDAO;
import br.com.luis.dao.UsuarioDAO;
import br.com.luis.model.StatusContaReceber;
import br.com.luis.model.Usuario;
import br.com.luis.util.ConnectionFactory;
import br.com.luis.viewmodel.ContaReceberRelatorioDados;
import br.com.luis.viewmodel.ContaReceberRelatorioView;
import br.com.luis.viewmodel.FiltroRelatorioContaReceber;
import br.com.luis.viewmodel.ResultadoRelatorioContaReceber;
import br.com.luis.viewmodel.SituacaoRelatorioContaReceber;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Coordena a consulta protegida do relatório de contas a receber.
 *
 * O Service valida os filtros, calcula os limites técnicos do período,
 * reconsulta e autoriza o usuário, captura uma única data de referência,
 * solicita ao DAO as projeções persistidas, calcula a situação gerencial de
 * cada conta e consolida os totais da fotografia retornada.
 *
 * Não acessa SessaoUsuario, componentes JavaFX ou mecanismos de formatação
 * visual. Também não altera autoCommit e não executa commit ou rollback,
 * pois o fluxo é exclusivamente de leitura.
 */
public class RelatorioContaReceberService {

    private static final int ESCALA_MONETARIA = 2;

    private static final RoundingMode ARREDONDAMENTO_MONETARIO =
            RoundingMode.HALF_UP;

    private static final String STATUS_ATIVO = "ATIVO";
    private static final String PERFIL_ADMIN = "ADMIN";

    private static final String MENSAGEM_ACESSO_NEGADO =
            "Usuário não autorizado a consultar o relatório de contas a receber.";

    private final ContaReceberDAO contaReceberDAO;
    private final UsuarioDAO usuarioDAO;

    /**
     * Cria o Service com as dependências JDBC usadas na consulta do relatório.
     */
    public RelatorioContaReceberService() {
        this.contaReceberDAO = new ContaReceberDAO();
        this.usuarioDAO = new UsuarioDAO();
    }

    /**
     * Consulta o relatório de contas a receber para os filtros informados e
     * devolve uma fotografia completa e imutável.
     *
     * O período recebido pela interface possui as duas pontas inclusivas. O
     * Service converte a data final para o limite técnico exclusivo exigido pelo
     * DAO. A mesma data de referência é utilizada em toda a consulta para
     * classificar contas pendentes como A_VENCER ou VENCIDA.
     *
     * @param filtro fotografia imutável dos filtros solicitados.
     * @param usuarioId identificador do usuário que solicita a consulta.
     * @return resultado consolidado do relatório de contas a receber.
     * @throws IllegalArgumentException quando o filtro, o usuário ou o período
     *                                  informado for inválido.
     * @throws SecurityException quando o usuário não estiver autorizado.
     * @throws IllegalStateException quando houver incoerência nos dados
     *                               persistidos ou na consolidação do resultado.
     * @throws RuntimeException quando ocorrer falha de conexão ou persistência.
     */
    public ResultadoRelatorioContaReceber consultarRelatorio(
            FiltroRelatorioContaReceber filtro,
            Integer usuarioId
    ) {

        if (filtro == null) {
            throw new IllegalArgumentException(
                    "Filtro do relatório de contas a receber não pode ser nulo."
            );
        }

        filtro.validar();

        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException(
                    "ID do usuário deve ser maior que zero."
            );
        }

        LocalDate dataFinal = filtro.getDataFinal();

        if (LocalDate.MAX.equals(dataFinal)) {
            throw new IllegalArgumentException(
                    "A data final informada não permite calcular "
                            + "o limite exclusivo do período."
            );
        }

        LocalDate inicioInclusivo = filtro.getDataInicial();
        LocalDate fimExclusivo = dataFinal.plusDays(1);

        try (Connection conn = ConnectionFactory.getConnection()) {

            Usuario usuario = usuarioDAO.buscarPorId(
                    conn,
                    usuarioId
            );

            validarAutorizacao(
                    usuario,
                    usuarioId
            );

            LocalDate dataReferencia = LocalDate.now();

            List<ContaReceberRelatorioDados> dadosPersistidos =
                    contaReceberDAO.listarParaRelatorio(
                            conn,
                            inicioInclusivo,
                            fimExclusivo,
                            filtro.getClienteTexto()
                    );

            if (dadosPersistidos == null) {
                throw new IllegalStateException(
                        "A consulta do relatório de contas a receber "
                                + "retornou uma lista nula."
                );
            }

            List<ContaReceberRelatorioView> contas =
                    new ArrayList<>();

            BigDecimal valorListado = criarValorMonetarioZero();
            BigDecimal valorPendente = criarValorMonetarioZero();
            BigDecimal valorVencido = criarValorMonetarioZero();

            for (ContaReceberRelatorioDados dados : dadosPersistidos) {

                if (dados == null) {
                    throw new IllegalStateException(
                            "A consulta do relatório de contas a receber "
                                    + "retornou uma projeção nula."
                    );
                }

                SituacaoRelatorioContaReceber situacao =
                        calcularSituacao(
                                dados,
                                dataReferencia
                        );

                if (filtro.getSituacao() != null
                        && filtro.getSituacao() != situacao) {
                    continue;
                }

                ContaReceberRelatorioView conta;

                try {
                    conta = new ContaReceberRelatorioView(
                            dados.getIdConta(),
                            dados.getVendaId(),
                            dados.getNomeCliente(),
                            dados.getValor(),
                            dados.getDataVencimento(),
                            situacao
                    );

                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException(
                            "Incoerência ao montar a linha da conta a receber"
                                    + " de ID "
                                    + dados.getIdConta()
                                    + " para o relatório.",
                            e
                    );
                }

                contas.add(conta);

                BigDecimal valorConta = conta.getValor();

                valorListado = valorListado.add(valorConta);

                if (situacao == SituacaoRelatorioContaReceber.A_VENCER
                        || situacao == SituacaoRelatorioContaReceber.VENCIDA) {

                    valorPendente = valorPendente.add(valorConta);
                }

                if (situacao == SituacaoRelatorioContaReceber.VENCIDA) {
                    valorVencido = valorVencido.add(valorConta);
                }
            }

            valorListado = normalizarValorMonetario(valorListado);
            valorPendente = normalizarValorMonetario(valorPendente);
            valorVencido = normalizarValorMonetario(valorVencido);

            try {
                return new ResultadoRelatorioContaReceber(
                        filtro,
                        dataReferencia,
                        contas,
                        valorListado,
                        valorPendente,
                        valorVencido
                );

            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                        "Incoerência ao consolidar o resultado "
                                + "do relatório de contas a receber.",
                        e
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao acessar o banco de dados durante "
                            + "a consulta do relatório de contas a receber.",
                    e
            );
        }
    }

    /**
     * Verifica a autorização persistida do usuário que solicita o relatório.
     */
    private void validarAutorizacao(
            Usuario usuario,
            Integer usuarioIdSolicitado
    ) {

        boolean usuarioAutorizado =
                usuario != null
                        && usuarioIdSolicitado.equals(
                                usuario.getIdUsuario()
                        )
                        && STATUS_ATIVO.equals(
                                usuario.getStatus()
                        )
                        && PERFIL_ADMIN.equals(
                                usuario.getPerfil()
                        );

        if (!usuarioAutorizado) {
            throw new SecurityException(
                    MENSAGEM_ACESSO_NEGADO
            );
        }
    }

    /**
     * Calcula a situação gerencial usando somente o estado persistido, o
     * vencimento e a data única de referência da consulta.
     */
    private SituacaoRelatorioContaReceber calcularSituacao(
            ContaReceberRelatorioDados dados,
            LocalDate dataReferencia
    ) {

        if (dados.getDataVencimento() == null) {
            throw new IllegalStateException(
                    "Conta a receber de ID "
                            + dados.getIdConta()
                            + " sem data de vencimento válida."
            );
        }

        StatusContaReceber statusPersistido =
                dados.getStatusPersistido();

        if (statusPersistido == null) {
            throw new IllegalStateException(
                    "Conta a receber de ID "
                            + dados.getIdConta()
                            + " sem status persistido válido."
            );
        }

        return switch (statusPersistido) {
            case PENDENTE ->
                    dados.getDataVencimento().isBefore(dataReferencia)
                            ? SituacaoRelatorioContaReceber.VENCIDA
                            : SituacaoRelatorioContaReceber.A_VENCER;

            case PAGA ->
                    SituacaoRelatorioContaReceber.PAGA;

            case CANCELADA ->
                    SituacaoRelatorioContaReceber.CANCELADA;
        };
    }

    /**
     * Cria o valor monetário inicial R$ 0,00.
     */
    private BigDecimal criarValorMonetarioZero() {
        return BigDecimal.ZERO.setScale(
                ESCALA_MONETARIA,
                ARREDONDAMENTO_MONETARIO
        );
    }

    /**
     * Normaliza valores monetários para escala 2 com HALF_UP.
     */
    private BigDecimal normalizarValorMonetario(
            BigDecimal valor
    ) {
        return valor.setScale(
                ESCALA_MONETARIA,
                ARREDONDAMENTO_MONETARIO
        );
    }
}
