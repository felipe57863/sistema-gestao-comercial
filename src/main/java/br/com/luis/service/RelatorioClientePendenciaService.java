package br.com.luis.service;

import br.com.luis.dao.ContaReceberDAO;
import br.com.luis.dao.UsuarioDAO;
import br.com.luis.model.Usuario;
import br.com.luis.util.ConnectionFactory;
import br.com.luis.viewmodel.ClientePendenciaRelatorioView;
import br.com.luis.viewmodel.FiltroRelatorioClientePendencia;
import br.com.luis.viewmodel.ResultadoRelatorioClientePendencia;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Coordena a consulta protegida de clientes com pendências financeiras.
 *
 * O Service valida os filtros, captura uma única data de referência, reconsulta
 * o usuário persistido, solicita ao DAO a lista final já filtrada e consolida os
 * totalizadores sobre exatamente essas linhas. Não acessa SessaoUsuario ou
 * componentes JavaFX e não executa commit ou rollback, pois o fluxo é de leitura.
 */
public class RelatorioClientePendenciaService {

    private static final int ESCALA_MONETARIA = 2;

    private static final RoundingMode ARREDONDAMENTO_MONETARIO =
            RoundingMode.HALF_UP;

    private static final String STATUS_ATIVO = "ATIVO";
    private static final String PERFIL_ADMIN = "ADMIN";

    private static final String MENSAGEM_ACESSO_NEGADO =
            "Usuário não autorizado a consultar clientes com pendências financeiras.";

    private final ContaReceberDAO contaReceberDAO;
    private final UsuarioDAO usuarioDAO;

    /**
     * Cria o Service com as dependências JDBC utilizadas na consulta.
     */
    public RelatorioClientePendenciaService() {
        this.contaReceberDAO = new ContaReceberDAO();
        this.usuarioDAO = new UsuarioDAO();
    }

    /**
     * Consulta a posição atual de clientes com contas pendentes.
     *
     * @param filtro fotografia imutável dos filtros solicitados.
     * @param usuarioId identificador do usuário que solicita a consulta.
     * @return resultado imutável consolidado sobre a lista final filtrada.
     */
    public ResultadoRelatorioClientePendencia consultar(
            FiltroRelatorioClientePendencia filtro,
            Integer usuarioId
    ) {
        if (filtro == null) {
            throw new IllegalArgumentException(
                    "Filtro do relatório de pendências não pode ser nulo."
            );
        }

        filtro.validar();
        validarUsuarioId(usuarioId);

        LocalDate dataReferencia = LocalDate.now();

        try (Connection conn = ConnectionFactory.getConnection()) {
            Usuario usuario = usuarioDAO.buscarPorId(conn, usuarioId);
            validarAutorizacao(usuario, usuarioId);

            List<ClientePendenciaRelatorioView> clientes =
                    contaReceberDAO.listarClientesComPendencias(
                            conn,
                            filtro,
                            dataReferencia
                    );

            if (clientes == null) {
                throw new IllegalStateException(
                        "A consulta de clientes com pendências retornou uma lista nula."
                );
            }

            BigDecimal valorTotalPendente = criarValorMonetarioZero();
            BigDecimal valorTotalVencido = criarValorMonetarioZero();
            int quantidadeClientesComVencidas = 0;

            for (ClientePendenciaRelatorioView cliente : clientes) {
                if (cliente == null) {
                    throw new IllegalStateException(
                            "A consulta de clientes com pendências retornou uma linha nula."
                    );
                }

                valorTotalPendente =
                        valorTotalPendente.add(cliente.getValorPendente());

                valorTotalVencido =
                        valorTotalVencido.add(cliente.getValorVencido());

                if (cliente.getQuantidadeContasVencidas() > 0) {
                    quantidadeClientesComVencidas++;
                }
            }

            valorTotalPendente =
                    normalizarValorMonetario(valorTotalPendente);

            valorTotalVencido =
                    normalizarValorMonetario(valorTotalVencido);

            try {
                return new ResultadoRelatorioClientePendencia(
                        filtro,
                        dataReferencia,
                        clientes,
                        clientes.size(),
                        valorTotalPendente,
                        quantidadeClientesComVencidas,
                        valorTotalVencido
                );

            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                        "Incoerência ao consolidar o relatório de clientes com pendências.",
                        e
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao acessar o banco de dados durante a consulta "
                            + "de clientes com pendências.",
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

    private BigDecimal criarValorMonetarioZero() {
        return BigDecimal.ZERO.setScale(
                ESCALA_MONETARIA,
                ARREDONDAMENTO_MONETARIO
        );
    }

    private BigDecimal normalizarValorMonetario(BigDecimal valor) {
        return valor.setScale(
                ESCALA_MONETARIA,
                ARREDONDAMENTO_MONETARIO
        );
    }
}
