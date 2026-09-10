package br.com.luis.service;

import br.com.luis.dao.ProdutoDAO;
import br.com.luis.model.Produto;
import br.com.luis.dao.PromocaoDAO;
import br.com.luis.model.Promocao;

import br.com.luis.util.ConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.List;

/**
 * Camada de Serviço (Regras de Negócio) para Produtos.
 * Responsável por validar dados e delegar para o DAO.
 */
public class ProdutoService {

    private final ProdutoDAO produtoDAO;
    private final PromocaoDAO promocaoDAO;
    private final PromocaoService promocaoService;

    public ProdutoService() {
        this.produtoDAO = new ProdutoDAO();
        this.promocaoDAO = new PromocaoDAO();
        this.promocaoService = new PromocaoService();
    }

    /**
     * Valida e cadastra um novo produto com sua promoção inicial opcional
     * em uma única transação.
     *
     * A verificação de duplicidade, o cadastro do produto e o cadastro da
     * promoção utilizam a mesma Connection. O commit somente ocorre quando
     * toda a operação é concluída com sucesso.
     *
     * @param produto produto que será cadastrado.
     * @param promocao promoção inicial opcional do produto.
     */
    public void cadastrar(
            Produto produto,
            Promocao promocao
    ) {

        validarProduto(produto);

        // Regra de negócio: todo produto novo inicia ativo.
        produto.setAtivo(true);

        try (Connection conn = ConnectionFactory.getConnection()) {

            boolean autoCommitOriginal = conn.getAutoCommit();
            Throwable falhaOriginal = null;
            boolean transacaoConcluida = false;

            try {
                conn.setAutoCommit(false);

                if (produtoDAO.existeDescricao(
                        conn,
                        produto.getDescricao()
                )) {

                    throw new IllegalArgumentException(
                            "Já existe um produto cadastrado com esta descrição."
                    );
                }

                produtoDAO.cadastrar(
                        conn,
                        produto
                );

                if (promocao != null) {

                    /*
                     * O INSERT do produto já gerou seu ID, mas a transação
                     * ainda não foi confirmada.
                     */
                    promocao.setProduto(produto);
                    promocao.setAtiva(true);

                    promocaoService.validarPromocao(promocao);

                    promocaoDAO.cadastrar(
                            conn,
                            promocao
                    );
                }

                conn.commit();
                transacaoConcluida = true;

                System.out.println(
                        "[LOG] Produto cadastrado com sucesso: "
                                + produto.getDescricao()
                );

            } catch (SQLException | RuntimeException | Error e) {

                falhaOriginal = e;

                if (!transacaoConcluida) {
                    try {
                        conn.rollback();
                        transacaoConcluida = true;
                    } catch (SQLException rollbackErro) {
                        e.addSuppressed(rollbackErro);
                    }
                }

                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }

                if (e instanceof Error error) {
                    throw error;
                }

                throw new IllegalStateException(
                        "Erro ao cadastrar produto.",
                        e
                );

            } finally {

                if (transacaoConcluida) {
                    try {
                        conn.setAutoCommit(autoCommitOriginal);

                    } catch (SQLException restauracaoErro) {

                        if (falhaOriginal != null) {
                            falhaOriginal.addSuppressed(restauracaoErro);

                        } else {
                            throw new IllegalStateException(
                                    "Erro ao restaurar o autoCommit após o cadastro do produto.",
                                    restauracaoErro
                            );
                        }
                    }
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erro ao controlar a transação de cadastro do produto.",
                    e
            );
        }
    }

    /**
     * Retorna todos os produtos cadastrados.
     * Usado para alimentar tabelas na interface (JavaFX).
     */
    public List<Produto> listarTodos() {
        return produtoDAO.listarTodos();
    }

    /**
     * Valida e atualiza um produto existente em transação própria.
     *
     * A Connection é controlada pela camada Service para permitir que este fluxo
     * possa posteriormente participar de operações compostas com promoção.
     *
     * @param produto produto que será atualizado.
     */
    public void atualizar(Produto produto) {

        validarProduto(produto);

        if (produto.getIdProduto() == null
                || produto.getIdProduto() <= 0) {

            throw new IllegalArgumentException(
                    "Produto ou ID inválido para edição."
            );
        }

        try (Connection conn = ConnectionFactory.getConnection()) {

            boolean autoCommitOriginal = conn.getAutoCommit();
            Throwable falhaOriginal = null;
            boolean transacaoConcluida = false;

            try {
                conn.setAutoCommit(false);

                produtoDAO.atualizar(
                        conn,
                        produto
                );

                conn.commit();
                transacaoConcluida = true;

                System.out.println(
                        "[LOG] Produto atualizado com sucesso: "
                                + produto.getDescricao()
                );

            } catch (SQLException | RuntimeException | Error e) {

                falhaOriginal = e;

                if (!transacaoConcluida) {
                    try {
                        conn.rollback();
                        transacaoConcluida = true;
                    } catch (SQLException rollbackErro) {
                        e.addSuppressed(rollbackErro);
                    }
                }

                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }

                if (e instanceof Error error) {
                    throw error;
                }

                throw new IllegalStateException(
                        "Erro ao atualizar produto.",
                        e
                );

            } finally {

                if (transacaoConcluida) {
                    try {
                        conn.setAutoCommit(autoCommitOriginal);

                    } catch (SQLException restauracaoErro) {

                        if (falhaOriginal != null) {
                            falhaOriginal.addSuppressed(restauracaoErro);

                        } else {
                            throw new IllegalStateException(
                                    "Erro ao restaurar o autoCommit após a atualização do produto.",
                                    restauracaoErro
                            );
                        }
                    }
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erro ao controlar a transação de atualização do produto.",
                    e
            );
        }
    }

    /**
     * Atualiza um produto e seu estado de promoção em uma única transação.
     *
     * A promoção informada representa o estado desejado após a edição:
     * - promoção não nula: mantém ou substitui a promoção ativa conforme necessário;
     * - promoção nula: remove a promoção ativa, caso exista.
     *
     * A atualização do produto e qualquer alteração de promoção utilizam a mesma
     * Connection e somente são confirmadas juntas.
     *
     * @param produto produto com os dados atualizados.
     * @param promocaoDesejada promoção desejada após a edição ou null para nenhuma.
     */
    public void atualizarComPromocao(
            Produto produto,
            Promocao promocaoDesejada
    ) {

        validarProduto(produto);

        if (produto.getIdProduto() == null
                || produto.getIdProduto() <= 0) {

            throw new IllegalArgumentException(
                    "Produto ou ID inválido para edição."
            );
        }

        /*
         * Na edição o produto já possui ID, portanto a promoção pode ser
         * completamente validada antes de qualquer escrita no banco.
         */
        if (promocaoDesejada != null) {
            promocaoDesejada.setProduto(produto);
            promocaoDesejada.setAtiva(true);

            promocaoService.validarPromocao(
                    promocaoDesejada
            );
        }

        try (Connection conn = ConnectionFactory.getConnection()) {

            boolean autoCommitOriginal = conn.getAutoCommit();
            Throwable falhaOriginal = null;
            boolean transacaoConcluida = false;

            try {
                conn.setAutoCommit(false);

                Produto produtoPersistido = produtoDAO.buscarPorId(
                        conn,
                        produto.getIdProduto()
                );

                if (produtoPersistido == null) {
                    throw new IllegalArgumentException(
                            "Produto não encontrado para edição."
                    );
                }

                if (produtoDAO.existeDescricaoEmOutroProduto(
                        conn,
                        produto.getDescricao(),
                        produto.getIdProduto()
                )) {
                    throw new IllegalArgumentException(
                            "Já existe um produto cadastrado com esta descrição."
                    );
                }

                Promocao promocaoAtual =
                        promocaoDAO.buscarPromocaoAtivaPorProduto(
                                conn,
                                produtoPersistido
                        );

                boolean precoFoiAlterado =
                        produtoPersistido.getPreco().compareTo(
                                produto.getPreco()
                        ) != 0;

                boolean promocaoFoiAlterada =
                        promocaoAtual != null
                                && promocaoDesejada != null
                                && (
                                promocaoAtual.getTipoDesconto()
                                        != promocaoDesejada.getTipoDesconto()
                                        || promocaoAtual.getValorDesconto().compareTo(
                                        promocaoDesejada.getValorDesconto()
                                ) != 0
                        );

                boolean deveCadastrarNovaPromocao =
                        promocaoDesejada != null
                                && (
                                promocaoAtual == null
                                        || promocaoFoiAlterada
                                        || precoFoiAlterado
                        );

                boolean deveInativarPromocao =
                        promocaoDesejada == null
                                && promocaoAtual != null;

                /*
                 * A partir daqui começam as escritas.
                 * Todas utilizam a mesma Connection.
                 */
                produtoDAO.atualizar(
                        conn,
                        produto
                );

                if (deveCadastrarNovaPromocao) {

                    promocaoDAO.inativarPromocoesAnteriores(
                            conn,
                            produto.getIdProduto()
                    );

                    promocaoDAO.cadastrar(
                            conn,
                            promocaoDesejada
                    );

                } else if (deveInativarPromocao) {

                    promocaoDAO.inativarPromocoesAnteriores(
                            conn,
                            produto.getIdProduto()
                    );
                }

                conn.commit();
                transacaoConcluida = true;

                System.out.println(
                        "[LOG] Produto e promoção atualizados com sucesso: "
                                + produto.getDescricao()
                );

            } catch (SQLException | RuntimeException | Error e) {

                falhaOriginal = e;

                if (!transacaoConcluida) {
                    try {
                        conn.rollback();
                        transacaoConcluida = true;
                    } catch (SQLException rollbackErro) {
                        e.addSuppressed(rollbackErro);
                    }
                }

                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }

                if (e instanceof Error error) {
                    throw error;
                }

                throw new IllegalStateException(
                        "Erro ao atualizar produto e promoção.",
                        e
                );

            } finally {

                if (transacaoConcluida) {
                    try {
                        conn.setAutoCommit(autoCommitOriginal);

                    } catch (SQLException restauracaoErro) {

                        if (falhaOriginal != null) {
                            falhaOriginal.addSuppressed(restauracaoErro);

                        } else {
                            throw new IllegalStateException(
                                    "Erro ao restaurar o autoCommit após a atualização do produto.",
                                    restauracaoErro
                            );
                        }
                    }
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Erro ao controlar a transação de atualização do produto.",
                    e
            );
        }
    }

    /**
     * Realiza a exclusão lógica do produto, alterando o campo ativo para false.
     *
     * Em vez de excluir fisicamente o registro, inativa o produto para preservar
     * seus vínculos e o histórico existente. Exige um produto com ID válido.
     */
    public void inativar(Produto produto) {

        // FAIL-FAST
        if (produto == null || produto.getIdProduto() == null || produto.getIdProduto() <= 0) {
            throw new IllegalArgumentException("Produto inválido para inativação.");
        }

        // ISOLAMENTO DE MEMÓRIA (evita efeitos colaterais na UI)
        Produto produtoInativado = new Produto(
                produto.getIdProduto(),
                produto.getDescricao(),
                produto.getPreco(),
                produto.getQuantidadeEstoque(),
                produto.getEstoqueMinimo(),
                false
        );

        atualizar(produtoInativado);
    }

    /**
     * Busca produtos por nome/descrição.
     */
    public List<Produto> buscarPorDescricao(String termo) {
        return produtoDAO.buscarPorDescricao(termo);
    }

    /**
     * Retorna apenas produtos ativos.
     * Melhor prática: filtrado direto no banco (performance).
     */
    public List<Produto> listarAtivos() {
        return produtoDAO.listarAtivos();
    }

    /**
     * Retorna produtos com estoque baixo.
     * Já vem filtrado no banco com ativo = 1.
     */
    public List<Produto> listarAbaixoDoMinimo() {
        return produtoDAO.listarAbaixoDoMinimo();
    }

    /**
     * Busca e valida um produto pelo ID informado.
     */
    public Produto buscarPorId(Integer idProduto) {

        if (idProduto == null || idProduto <= 0) {
            throw new IllegalArgumentException("ID do produto inválido.");
        }

        Produto produto = produtoDAO.buscarPorId(idProduto);

        if (produto == null) {
            throw new IllegalArgumentException("Produto não encontrado.");
        }

        return produto;
    }

    /**
     * Valida os dados mínimos compartilhados por cadastro e atualização.
     */
    private void validarProduto(Produto produto) {

        if (produto == null) {
            throw new IllegalArgumentException("O produto não pode ser nulo.");
        }

        if (produto.getDescricao() == null || produto.getDescricao().isBlank()) {
            throw new IllegalArgumentException("A descrição do produto é obrigatória.");
        }

        if (produto.getPreco() == null) {
            throw new IllegalArgumentException("O preço do produto é obrigatório.");
        }

        if (produto.getPreco().signum() < 0) {
            throw new IllegalArgumentException("O preço do produto não pode ser negativo.");
        }

        if (produto.getQuantidadeEstoque() == null) {
            throw new IllegalArgumentException("A quantidade em estoque é obrigatória.");
        }

        if (produto.getQuantidadeEstoque() < 0) {
            throw new IllegalArgumentException("Quantidade em estoque não pode ser negativa.");
        }

        if (produto.getEstoqueMinimo() == null) {
            throw new IllegalArgumentException("O estoque mínimo é obrigatório.");
        }

        if (produto.getEstoqueMinimo() < 0) {
            throw new IllegalArgumentException("Estoque mínimo não pode ser negativo.");
        }
    }
}
