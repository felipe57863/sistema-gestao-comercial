package br.com.luis.service;

import br.com.luis.dao.ConfiguracaoAlertaVencimentoDAO;
import br.com.luis.dao.ContaReceberDAO;
import br.com.luis.dao.UsuarioDAO;
import br.com.luis.model.StatusContaReceber;
import br.com.luis.model.Usuario;
import br.com.luis.util.ConnectionFactory;
import br.com.luis.viewmodel.ContaAlertaVencimentoView;
import br.com.luis.viewmodel.ContaReceberRelatorioDados;
import br.com.luis.viewmodel.ResultadoAlertaVencimento;
import br.com.luis.viewmodel.SituacaoAlertaVencimento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Coordena a consulta e a configuração dos alertas automáticos de vencimento.
 *
 * O Service revalida o administrador persistido, lê a configuração global,
 * captura uma única data de referência, consulta somente contas pendentes
 * elegíveis e classifica cada conta sem alterar seu status persistido.
 */
public class AlertaVencimentoService {

    public static final int DIAS_ANTECEDENCIA_MINIMO = 0;
    public static final int DIAS_ANTECEDENCIA_MAXIMO = 365;

    private static final int ESCALA_MONETARIA = 2;

    private static final RoundingMode ARREDONDAMENTO_MONETARIO =
            RoundingMode.HALF_UP;

    private static final String STATUS_USUARIO_ATIVO = "ATIVO";
    private static final String PERFIL_ADMIN = "ADMIN";

    private static final String MENSAGEM_ACESSO_NEGADO =
            "Usuário não autorizado a consultar ou configurar alertas de vencimento.";

    private final ConfiguracaoAlertaVencimentoDAO configuracaoDAO;
    private final ContaReceberDAO contaReceberDAO;
    private final UsuarioDAO usuarioDAO;

    public AlertaVencimentoService() {
        this.configuracaoDAO = new ConfiguracaoAlertaVencimentoDAO();
        this.contaReceberDAO = new ContaReceberDAO();
        this.usuarioDAO = new UsuarioDAO();
    }

    /**
     * Consulta a fotografia atual dos alertas de vencimento.
     * O executor é reconsultado e deve ser um administrador ATIVO. O resultado
     * reúne as contas pendentes vencidas e as que vencem até o limite inclusivo
     * calculado com a antecedência persistida.
     *
     * @param usuarioId identificador do administrador executor.
     * @return fotografia consolidada dos alertas na data da consulta.
     * @throws IllegalArgumentException se o identificador do executor for inválido.
     * @throws SecurityException se o executor persistido não estiver autorizado.
     * @throws IllegalStateException se a configuração ou os dados consultados
     *                               estiverem inconsistentes.
     * @throws RuntimeException se ocorrer falha de acesso ao banco.
     */
    public ResultadoAlertaVencimento consultar(Integer usuarioId) {

        validarUsuarioId(usuarioId);

        try (Connection conn = ConnectionFactory.getConnection()) {

            Usuario usuario = usuarioDAO.buscarPorId(conn, usuarioId);
            validarAutorizacao(usuario, usuarioId);

            int diasAntecedencia =
                    configuracaoDAO.buscarDiasAntecedencia(conn);

            validarDiasAntecedenciaPersistidos(diasAntecedencia);

            LocalDate dataReferencia = LocalDate.now();
            LocalDate limiteInclusivo =
                    calcularLimiteInclusivo(
                            dataReferencia,
                            diasAntecedencia
                    );

            List<ContaReceberRelatorioDados> dadosConsultados =
                    contaReceberDAO.listarPendentesAteVencimento(
                            conn,
                            limiteInclusivo
                    );

            if (dadosConsultados == null) {
                throw new IllegalStateException(
                        "A consulta dos alertas retornou uma lista nula."
                );
            }

            List<ContaAlertaVencimentoView> contas = new ArrayList<>();

            BigDecimal valorTotalVencido = criarZeroMonetario();
            BigDecimal valorTotalProximo = criarZeroMonetario();

            int quantidadeVencidas = 0;
            int quantidadeProximas = 0;

            for (ContaReceberRelatorioDados dados : dadosConsultados) {

                if (dados == null) {
                    throw new IllegalStateException(
                            "A consulta dos alertas retornou uma conta nula."
                    );
                }

                validarDadosPersistidos(
                        dados,
                        limiteInclusivo
                );

                SituacaoAlertaVencimento situacao;

                if (dados.getDataVencimento().isBefore(dataReferencia)) {
                    situacao = SituacaoAlertaVencimento.VENCIDA;
                    quantidadeVencidas++;
                    valorTotalVencido =
                            valorTotalVencido.add(dados.getValor());

                } else {
                    situacao =
                            SituacaoAlertaVencimento.PROXIMA_DO_VENCIMENTO;
                    quantidadeProximas++;
                    valorTotalProximo =
                            valorTotalProximo.add(dados.getValor());
                }

                contas.add(
                        new ContaAlertaVencimentoView(
                                dados.getIdConta(),
                                dados.getVendaId(),
                                dados.getNomeCliente(),
                                dados.getValor(),
                                dados.getDataVencimento(),
                                situacao
                        )
                );
            }

            valorTotalVencido =
                    normalizarValorMonetario(valorTotalVencido);

            valorTotalProximo =
                    normalizarValorMonetario(valorTotalProximo);

            try {
                return new ResultadoAlertaVencimento(
                        diasAntecedencia,
                        dataReferencia,
                        limiteInclusivo,
                        contas,
                        quantidadeVencidas,
                        valorTotalVencido,
                        quantidadeProximas,
                        valorTotalProximo
                );

            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                        "Incoerência ao consolidar os alertas de vencimento.",
                        e
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao acessar o banco durante a consulta dos alertas de vencimento.",
                    e
            );
        }
    }

    /**
     * Altera a configuração global de antecedência dos alertas.
     * O valor aceito está no intervalo inclusivo de 0 a 365 dias, e o executor
     * reconsultado deve ser um administrador ATIVO.
     *
     * @param diasAntecedencia quantidade global de dias de antecedência, de 0 a 365.
     * @param usuarioId identificador do administrador executor.
     * @throws IllegalArgumentException se a antecedência ou o identificador forem inválidos.
     * @throws SecurityException se o executor persistido não estiver autorizado.
     * @throws IllegalStateException se a atualização não afetar exatamente um registro.
     * @throws RuntimeException se ocorrer falha de acesso ao banco.
     */
    public void atualizarDiasAntecedencia(
            Integer diasAntecedencia,
            Integer usuarioId
    ) {

        validarDiasAntecedenciaInformados(diasAntecedencia);
        validarUsuarioId(usuarioId);

        try (Connection conn = ConnectionFactory.getConnection()) {

            Usuario usuario = usuarioDAO.buscarPorId(conn, usuarioId);
            validarAutorizacao(usuario, usuarioId);

            int linhasAfetadas =
                    configuracaoDAO.atualizarDiasAntecedencia(
                            conn,
                            diasAntecedencia
                    );

            if (linhasAfetadas != 1) {
                throw new IllegalStateException(
                        "A configuração dos alertas não pôde ser atualizada."
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao acessar o banco durante a atualização "
                            + "da configuração dos alertas.",
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

        boolean autorizado =
                usuario != null
                        && usuarioIdSolicitado.equals(
                        usuario.getIdUsuario()
                )
                        && STATUS_USUARIO_ATIVO.equals(
                        usuario.getStatus()
                )
                        && PERFIL_ADMIN.equals(
                        usuario.getPerfil()
                );

        if (!autorizado) {
            throw new SecurityException(MENSAGEM_ACESSO_NEGADO);
        }
    }

    private void validarDiasAntecedenciaInformados(
            Integer diasAntecedencia
    ) {

        if (diasAntecedencia == null) {
            throw new IllegalArgumentException(
                    "Dias de antecedência são obrigatórios."
            );
        }

        if (diasAntecedencia < DIAS_ANTECEDENCIA_MINIMO
                || diasAntecedencia > DIAS_ANTECEDENCIA_MAXIMO) {

            throw new IllegalArgumentException(
                    "Dias de antecedência devem estar entre "
                            + DIAS_ANTECEDENCIA_MINIMO
                            + " e "
                            + DIAS_ANTECEDENCIA_MAXIMO
                            + "."
            );
        }
    }

    private void validarDiasAntecedenciaPersistidos(
            int diasAntecedencia
    ) {

        if (diasAntecedencia < DIAS_ANTECEDENCIA_MINIMO
                || diasAntecedencia > DIAS_ANTECEDENCIA_MAXIMO) {

            throw new IllegalStateException(
                    "Configuração persistida dos alertas é inválida."
            );
        }
    }

    private LocalDate calcularLimiteInclusivo(
            LocalDate dataReferencia,
            int diasAntecedencia
    ) {

        try {
            return dataReferencia.plusDays(diasAntecedencia);

        } catch (DateTimeException e) {
            throw new IllegalStateException(
                    "Não foi possível calcular a janela dos alertas.",
                    e
            );
        }
    }

    private void validarDadosPersistidos(
            ContaReceberRelatorioDados dados,
            LocalDate limiteInclusivo
    ) {

        if (dados.getStatusPersistido()
                != StatusContaReceber.PENDENTE) {

            throw new IllegalStateException(
                    "Conta " + dados.getIdConta()
                            + " possui status incompatível com os alertas."
            );
        }

        if (dados.getDataVencimento().isAfter(limiteInclusivo)) {
            throw new IllegalStateException(
                    "Conta " + dados.getIdConta()
                            + " está fora da janela dos alertas."
            );
        }

        if (dados.getValor().signum() < 0) {
            throw new IllegalStateException(
                    "Conta " + dados.getIdConta()
                            + " possui valor negativo."
            );
        }
    }

    private BigDecimal criarZeroMonetario() {
        return BigDecimal.ZERO.setScale(
                ESCALA_MONETARIA,
                ARREDONDAMENTO_MONETARIO
        );
    }

    private BigDecimal normalizarValorMonetario(
            BigDecimal valor
    ) {

        return valor.setScale(
                ESCALA_MONETARIA,
                ARREDONDAMENTO_MONETARIO
        );
    }
}
