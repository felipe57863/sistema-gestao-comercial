package br.com.luis.service;

import br.com.luis.dao.PromocaoDAO;
import br.com.luis.model.Promocao;
import br.com.luis.model.Produto;
import br.com.luis.util.ConnectionFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Service responsável pelas regras de negócio e transações de promoções.
 *
 * Valida produto, tipo e valor do desconto e coordena a substituição ou a
 * inativação da promoção ativa. Nos fluxos de alteração, controla a Connection,
 * commit, rollback e restauração do autoCommit. O PromocaoDAO permanece limitado
 * às operações de persistência executadas com a conexão recebida.
 */
public class PromocaoService {

    private final PromocaoDAO promocaoDAO;

    public PromocaoService() {
        this.promocaoDAO = new PromocaoDAO();
    }

    /**
     * Valida e cadastra uma nova promoção em uma transação controlada pelo Service.
     *
     * Os dados são validados antes da abertura da conexão. Pela RN22, a mesma
     * Connection inativa as promoções anteriores do produto e persiste o novo
     * registro.
     *
     * O commit ocorre somente após as duas operações. Se ocorrer uma falha antes da
     * conclusão da transação, o Service tenta executar rollback. O autoCommit é
     * restaurado quando a transação foi concluída por commit ou rollback.
     */
    public void cadastrarPromocaoNova(Promocao promocao) {

        validarPromocao(promocao);

        // 2. Toda promoção cadastrada por este fluxo deve entrar como ativa
        promocao.setAtiva(true);

        try (Connection conn = ConnectionFactory.getConnection()) {

            boolean autoCommitOriginal = conn.getAutoCommit();
            Throwable falhaOriginal = null;
            boolean transacaoConcluida = false;

            try {
                // 3. Assume o controle manual da transação
                conn.setAutoCommit(false);

                // 4. Executa as operações em cadeia no DAO passando a MESMA conexão
                promocaoDAO.inativarPromocoesAnteriores(conn, promocao.getProduto().getIdProduto());
                promocaoDAO.cadastrar(conn, promocao);

                conn.commit();
                transacaoConcluida = true;

                System.out.println("[LOG] Promoção cadastrada para o produto ID: "
                        + promocao.getProduto().getIdProduto());

            } catch (SQLException | RuntimeException | Error e) {
                falhaOriginal = e;

                // Se a transação ainda não foi concluída, tenta executar rollback.
                if (!transacaoConcluida) {
                    try {
                        conn.rollback();
                        transacaoConcluida = true;
                    } catch (SQLException rollbackErro) {
                        e.addSuppressed(rollbackErro);
                    }
                }

                if (e instanceof Error error) {
                    throw error;
                }

                throw new RuntimeException(
                        "Erro ao aplicar a promoção. A operação foi cancelada: " + e.getMessage(),
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
                            throw new RuntimeException(
                                    "Erro ao restaurar o autoCommit da conexão após cadastro de promoção.",
                                    restauracaoErro
                            );
                        }
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao abrir conexão para cadastro de promoção.", e);
        }
    }

    /**
     * Inativa a promoção ativa de um produto em uma transação própria.
     *
     * O Service controla a Connection, o commit, o rollback e a restauração do
     * autoCommit; o DAO apenas executa a atualização solicitada.
     */
    public void inativarPromocaoAtivaDoProduto(Produto produto) {

        if (produto == null || produto.getIdProduto() == null || produto.getIdProduto() <= 0) {
            throw new IllegalArgumentException("Produto inválido para inativação da promoção.");
        }

        try (Connection conn = ConnectionFactory.getConnection()) {

            boolean autoCommitOriginal = conn.getAutoCommit();
            Throwable falhaOriginal = null;
            boolean transacaoConcluida = false;

            try {
                conn.setAutoCommit(false);

                promocaoDAO.inativarPromocoesAnteriores(conn, produto.getIdProduto());

                conn.commit();
                transacaoConcluida = true;

                System.out.println("[LOG] Promoção ativa inativada para o produto ID: "
                        + produto.getIdProduto());

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

                if (e instanceof Error error) {
                    throw error;
                }

                throw new RuntimeException(
                        "Erro ao inativar promoção ativa do produto. A operação foi cancelada: " + e.getMessage(),
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
                            throw new RuntimeException(
                                    "Erro ao restaurar o autoCommit da conexão após inativação de promoção.",
                                    restauracaoErro
                            );
                        }
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao abrir conexão para inativar promoção do produto.", e);
        }
    }

    /**
     * Busca a promoção ativa de um produto.
     * Método de consulta usado pelas telas e pela aplicação automática de
     * promoção no carrinho de vendas.
     */
    public Promocao buscarPromocaoAtivaPorProduto(Produto produto) {

        if (produto == null
                || produto.getIdProduto() == null
                || produto.getIdProduto() <= 0) {

            throw new IllegalArgumentException(
                    "Produto inválido para busca de promoção."
            );
        }

        try (Connection conn = ConnectionFactory.getConnection()) {

            return promocaoDAO.buscarPromocaoAtivaPorProduto(
                    conn,
                    produto
            );

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao abrir conexão para buscar promoção ativa do produto.",
                    e
            );
        }
    }

    /**
     * Valida tipo, valor e produto antes da aplicação da RN22.
     */
    void validarPromocao(Promocao promocao) {

        if (promocao == null) {
            throw new IllegalArgumentException("Promoção é obrigatória.");
        }

        if (promocao.getProduto() == null || promocao.getProduto().getIdProduto() == null) {
            throw new IllegalArgumentException("Produto inválido para cadastro da promoção.");
        }

        if (promocao.getProduto().getIdProduto() <= 0) {
            throw new IllegalArgumentException("ID do produto inválido para cadastro da promoção.");
        }

        if (promocao.getTipoDesconto() == null) {
            throw new IllegalArgumentException("Tipo de desconto é obrigatório.");
        }

        if (promocao.getValorDesconto() == null) {
            throw new IllegalArgumentException("Valor do desconto é obrigatório.");
        }

        if (promocao.getValorDesconto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor do desconto deve ser maior que zero.");
        }

        if (promocao.getTipoDesconto() == Promocao.TipoDesconto.PERCENTUAL) {
            validarDescontoPercentual(promocao.getValorDesconto());
        }

        if (promocao.getTipoDesconto() == Promocao.TipoDesconto.VALOR_FIXO) {
            validarDescontoValorFixo(promocao);
        }
    }

    /**
     * Valida se o desconto percentual não ultrapassa 100%.
     */
    private void validarDescontoPercentual(BigDecimal valorDesconto) {

        BigDecimal cemPorCento = new BigDecimal("100.00");

        if (valorDesconto.compareTo(cemPorCento) > 0) {
            throw new IllegalArgumentException("Desconto percentual não pode ser maior que 100%.");
        }
    }

    /**
     * Valida se o desconto fixo não ultrapassa o preço do produto.
     */
    private void validarDescontoValorFixo(Promocao promocao) {

        Produto produto = promocao.getProduto();

        if (produto.getPreco() == null) {
            throw new IllegalArgumentException("Preço do produto é obrigatório para validar desconto fixo.");
        }

        if (promocao.getValorDesconto().compareTo(produto.getPreco()) > 0) {
            throw new IllegalArgumentException("Desconto não pode ser maior que o preço do produto.");
        }

        BigDecimal valorNormalizado = promocao.getValorDesconto()
                .setScale(2, RoundingMode.HALF_UP);

        if (valorNormalizado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Valor do desconto deve ser maior que zero após o arredondamento monetário."
            );
        }

        if (valorNormalizado.compareTo(produto.getPreco()) > 0) {
            throw new IllegalArgumentException("Desconto não pode ser maior que o preço do produto.");
        }

        promocao.setValorDesconto(valorNormalizado);
    }
}