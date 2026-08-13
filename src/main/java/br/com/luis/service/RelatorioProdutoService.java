package br.com.luis.service;

import br.com.luis.dao.ProdutoDAO;
import br.com.luis.dao.PromocaoDAO;
import br.com.luis.dao.UsuarioDAO;
import br.com.luis.model.Produto;
import br.com.luis.model.Promocao;
import br.com.luis.model.Usuario;
import br.com.luis.util.ConnectionFactory;
import br.com.luis.viewmodel.FiltroRelatorioEstoqueProduto;
import br.com.luis.viewmodel.FiltroRelatorioPromocaoProduto;
import br.com.luis.viewmodel.ProdutoEstoqueRelatorioView;
import br.com.luis.viewmodel.ProdutoPromocaoRelatorioView;
import br.com.luis.viewmodel.ResultadoRelatorioEstoqueProduto;
import br.com.luis.viewmodel.ResultadoRelatorioPromocaoProduto;
import br.com.luis.viewmodel.SituacaoEstoqueProduto;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Coordena as consultas protegidas dos relatórios de produtos.
 *
 * O Service reconsulta e autoriza o usuário, solicita aos DAOs somente os dados
 * persistidos, transforma as linhas, classifica o estoque, aplica a situação e
 * consolida os totalizadores. Não acessa sessão ou componentes JavaFX e não
 * executa commit ou rollback, pois os dois fluxos são exclusivamente de leitura.
 */
public class RelatorioProdutoService {

    private static final String STATUS_ATIVO = "ATIVO";
    private static final String PERFIL_ADMIN = "ADMIN";

    private static final String MENSAGEM_ACESSO_NEGADO =
            "Usuário não autorizado a consultar relatórios de produtos.";

    private final ProdutoDAO produtoDAO;
    private final PromocaoDAO promocaoDAO;
    private final UsuarioDAO usuarioDAO;

    /**
     * Cria o Service com as dependências JDBC usadas nas consultas de produtos.
     */
    public RelatorioProdutoService() {
        this.produtoDAO = new ProdutoDAO();
        this.promocaoDAO = new PromocaoDAO();
        this.usuarioDAO = new UsuarioDAO();
    }

    /**
     * Consulta e consolida a visão de estoque para um administrador autorizado.
     *
     * @param filtro fotografia imutável dos filtros solicitados.
     * @param usuarioId identificador do usuário que solicita a consulta.
     * @return resultado imutável e consolidado da visão de estoque.
     * @throws IllegalArgumentException quando filtro ou usuário forem inválidos.
     * @throws SecurityException quando o usuário não estiver autorizado.
     * @throws IllegalStateException quando houver incoerência nos dados.
     * @throws RuntimeException quando ocorrer falha de conexão ou persistência.
     */
    public ResultadoRelatorioEstoqueProduto consultarEstoque(
            FiltroRelatorioEstoqueProduto filtro,
            Integer usuarioId
    ) {

        if (filtro == null) {
            throw new IllegalArgumentException(
                    "Filtro do relatório de estoque não pode ser nulo."
            );
        }

        filtro.validar();
        validarUsuarioId(usuarioId);

        try (Connection conn = ConnectionFactory.getConnection()) {

            Usuario usuario = usuarioDAO.buscarPorId(conn, usuarioId);
            validarAutorizacao(usuario, usuarioId);

            List<Produto> dadosPersistidos =
                    produtoDAO.listarParaRelatorioEstoque(
                            conn,
                            filtro
                    );

            if (dadosPersistidos == null) {
                throw new IllegalStateException(
                        "A consulta do relatório de estoque retornou uma lista nula."
                );
            }

            List<ProdutoEstoqueRelatorioView> produtos = new ArrayList<>();
            int quantidadeAbaixoDoMinimo = 0;
            int quantidadeNoMinimo = 0;
            int quantidadeAcimaDoMinimo = 0;

            for (Produto produto : dadosPersistidos) {
                if (produto == null) {
                    throw new IllegalStateException(
                            "A consulta do relatório de estoque retornou um produto nulo."
                    );
                }

                Integer estoqueAtual = produto.getQuantidadeEstoque();
                Integer estoqueMinimo = produto.getEstoqueMinimo();

                if (estoqueAtual == null || estoqueMinimo == null) {
                    throw new IllegalStateException(
                            "Produto de ID "
                                    + produto.getIdProduto()
                                    + " sem dados completos de estoque."
                    );
                }

                int diferenca = estoqueAtual - estoqueMinimo;
                SituacaoEstoqueProduto situacao =
                        classificarEstoque(
                                estoqueAtual,
                                estoqueMinimo
                        );

                if (filtro.getSituacao() != null
                        && filtro.getSituacao() != situacao) {
                    continue;
                }

                ProdutoEstoqueRelatorioView produtoRelatorio;

                try {
                    produtoRelatorio = new ProdutoEstoqueRelatorioView(
                            produto.getIdProduto(),
                            produto.getDescricao(),
                            estoqueAtual,
                            estoqueMinimo,
                            diferenca,
                            situacao,
                            produto.isAtivo()
                    );

                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException(
                            "Incoerência ao montar a linha do produto de ID "
                                    + produto.getIdProduto()
                                    + " para o relatório de estoque.",
                            e
                    );
                }

                produtos.add(produtoRelatorio);

                switch (situacao) {
                    case ABAIXO_DO_MINIMO -> quantidadeAbaixoDoMinimo++;
                    case NO_MINIMO -> quantidadeNoMinimo++;
                    case ACIMA_DO_MINIMO -> quantidadeAcimaDoMinimo++;
                }
            }

            produtos.sort(
                    Comparator
                            .comparingInt(
                                    (ProdutoEstoqueRelatorioView produto) ->
                                            obterOrdemSituacao(produto.getSituacao())
                            )
                            .thenComparingInt(
                                    ProdutoEstoqueRelatorioView::getDiferenca
                            )
                            .thenComparing(
                                    ProdutoEstoqueRelatorioView::getDescricao,
                                    String.CASE_INSENSITIVE_ORDER
                            )
                            .thenComparing(
                                    ProdutoEstoqueRelatorioView::getProdutoId
                            )
            );

            try {
                return new ResultadoRelatorioEstoqueProduto(
                        filtro,
                        produtos,
                        quantidadeAbaixoDoMinimo,
                        quantidadeNoMinimo,
                        quantidadeAcimaDoMinimo
                );

            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                        "Incoerência ao consolidar o resultado "
                                + "do relatório de estoque.",
                        e
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao acessar o banco de dados durante "
                            + "a consulta do relatório de estoque.",
                    e
            );
        }
    }

    /**
     * Consulta as promoções ativas e devolve uma linha para cada registro
     * encontrado, inclusive quando um produto possuir mais de uma promoção ativa.
     *
     * @param filtro fotografia imutável dos filtros solicitados.
     * @param usuarioId identificador do usuário que solicita a consulta.
     * @return resultado imutável da visão de produtos em promoção.
     * @throws IllegalArgumentException quando filtro ou usuário forem inválidos.
     * @throws SecurityException quando o usuário não estiver autorizado.
     * @throws IllegalStateException quando houver incoerência nos dados.
     * @throws RuntimeException quando ocorrer falha de conexão ou persistência.
     */
    public ResultadoRelatorioPromocaoProduto consultarPromocoes(
            FiltroRelatorioPromocaoProduto filtro,
            Integer usuarioId
    ) {

        if (filtro == null) {
            throw new IllegalArgumentException(
                    "Filtro do relatório de promoções não pode ser nulo."
            );
        }

        filtro.validar();
        validarUsuarioId(usuarioId);

        try (Connection conn = ConnectionFactory.getConnection()) {

            Usuario usuario = usuarioDAO.buscarPorId(conn, usuarioId);
            validarAutorizacao(usuario, usuarioId);

            List<Promocao> dadosPersistidos =
                    promocaoDAO.listarParaRelatorioProdutos(
                            conn,
                            filtro
                    );

            if (dadosPersistidos == null) {
                throw new IllegalStateException(
                        "A consulta do relatório de promoções retornou uma lista nula."
                );
            }

            List<ProdutoPromocaoRelatorioView> promocoes = new ArrayList<>();

            for (Promocao promocao : dadosPersistidos) {
                if (promocao == null) {
                    throw new IllegalStateException(
                            "A consulta do relatório de promoções retornou "
                                    + "uma promoção nula."
                    );
                }

                Produto produto = promocao.getProduto();

                if (produto == null) {
                    throw new IllegalStateException(
                            "Promoção de ID "
                                    + promocao.getIdPromocao()
                                    + " sem produto válido."
                    );
                }

                ProdutoPromocaoRelatorioView promocaoRelatorio;

                try {
                    promocaoRelatorio = new ProdutoPromocaoRelatorioView(
                            produto.getIdProduto(),
                            produto.getDescricao(),
                            produto.getPreco(),
                            promocao.getTipoDesconto(),
                            promocao.getValorDesconto(),
                            produto.isAtivo()
                    );

                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException(
                            "Incoerência ao montar a linha da promoção de ID "
                                    + promocao.getIdPromocao()
                                    + " para o relatório de produtos.",
                            e
                    );
                }

                promocoes.add(promocaoRelatorio);
            }

            try {
                return new ResultadoRelatorioPromocaoProduto(
                        filtro,
                        promocoes
                );

            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                        "Incoerência ao consolidar o resultado "
                                + "do relatório de promoções.",
                        e
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao acessar o banco de dados durante "
                            + "a consulta do relatório de promoções.",
                    e
            );
        }
    }

    /**
     * Classifica a situação a partir dos valores persistidos de estoque.
     */
    private SituacaoEstoqueProduto classificarEstoque(
            int estoqueAtual,
            int estoqueMinimo
    ) {
        if (estoqueAtual < estoqueMinimo) {
            return SituacaoEstoqueProduto.ABAIXO_DO_MINIMO;
        }

        if (estoqueAtual == estoqueMinimo) {
            return SituacaoEstoqueProduto.NO_MINIMO;
        }

        return SituacaoEstoqueProduto.ACIMA_DO_MINIMO;
    }

    /**
     * Define a prioridade gerencial usada na ordenação da visão de estoque.
     */
    private int obterOrdemSituacao(SituacaoEstoqueProduto situacao) {
        return switch (situacao) {
            case ABAIXO_DO_MINIMO -> 0;
            case NO_MINIMO -> 1;
            case ACIMA_DO_MINIMO -> 2;
        };
    }

    /**
     * Valida o identificador antes da abertura da Connection.
     */
    private void validarUsuarioId(Integer usuarioId) {
        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException(
                    "ID do usuário deve ser maior que zero."
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
                        && usuarioIdSolicitado.equals(usuario.getIdUsuario())
                        && STATUS_ATIVO.equals(usuario.getStatus())
                        && PERFIL_ADMIN.equals(usuario.getPerfil());

        if (!usuarioAutorizado) {
            throw new SecurityException(MENSAGEM_ACESSO_NEGADO);
        }
    }
}
