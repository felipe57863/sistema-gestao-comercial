package br.com.luis.service;

import br.com.luis.dao.PromocaoDAO;
import br.com.luis.model.Promocao;
import br.com.luis.model.Produto;
import br.com.luis.util.ConnectionFactory;

import java.math.BigDecimal;
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
        // Inicializa o DAO que será orquestrado por este Service
        this.promocaoDAO = new PromocaoDAO();
    }

    /**
     * Valida e cadastra uma nova promoção em uma transação controlada pelo Service.
     *
     * Antes de abrir a conexão, valida os dados e define a nova promoção como
     * ativa. Na mesma Connection, inativa promoções anteriores do produto e
     * persiste o novo registro. O commit ocorre somente após as duas operações;
     * qualquer falha provoca rollback e o autoCommit é restaurado ao final.
     *
     * @implNote Implementa a RN22 - Nova Promoção
     */
    public void cadastrarPromocaoNova(Promocao promocao) {

        // 1. Fail-Fast (Defesa antes de tocar no banco)
        validarPromocao(promocao);

        // 2. Toda promoção cadastrada por este fluxo deve entrar como ativa
        promocao.setAtiva(true);

        try (Connection conn = ConnectionFactory.getConnection()) {

            try {
                // 3. Assume o controle manual da transação
                conn.setAutoCommit(false);

                // 4. Executa as operações em cadeia no DAO passando a MESMA conexão
                promocaoDAO.inativarPromocoesAnteriores(conn, promocao.getProduto().getIdProduto());
                promocaoDAO.cadastrar(conn, promocao);

                // 5. Se chegou até aqui sem erros, confirma tudo no banco
                conn.commit();

                System.out.println("[LOG] Promoção cadastrada para o produto ID: "
                        + promocao.getProduto().getIdProduto());

            } catch (Exception e) {

                // 6. Deu erro em qualquer etapa? Desfaz TUDO
                try {
                    conn.rollback();
                } catch (SQLException rollbackErro) {
                    throw new RuntimeException(
                            "Erro crítico: falha ao tentar realizar o rollback da transação.",
                            rollbackErro
                    );
                }

                throw new RuntimeException(
                        "Erro ao aplicar a promoção. A operação foi cancelada: " + e.getMessage(),
                        e
                );

            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    System.err.println("[ERRO] Não foi possível restaurar o autoCommit da conexão.");
                    e.printStackTrace();
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao abrir conexão para cadastro de promoção.", e);
        }
    }

    /**
     * Inativa a promoção ativa de um produto em uma transação própria.
     *
     * Valida o identificador do produto, abre e controla a Connection, solicita ao
     * DAO a inativação e confirma a alteração somente após o sucesso. Em falha,
     * executa rollback e restaura o autoCommit ao final. O DAO não decide se a
     * promoção deve ser inativada.
     *
     * @implNote Garante que a remoção da promoção ativa seja coordenada pela
     * camada Service dentro de uma transação.
     */
    public void inativarPromocaoAtivaDoProduto(Produto produto) {

        if (produto == null || produto.getIdProduto() == null || produto.getIdProduto() <= 0) {
            throw new IllegalArgumentException("Produto inválido para inativação da promoção.");
        }

        try (Connection conn = ConnectionFactory.getConnection()) {

            try {
                conn.setAutoCommit(false);

                promocaoDAO.inativarPromocoesAnteriores(conn, produto.getIdProduto());

                conn.commit();

                System.out.println("[LOG] Promoção ativa inativada para o produto ID: "
                        + produto.getIdProduto());

            } catch (Exception e) {

                try {
                    conn.rollback();
                } catch (SQLException rollbackErro) {
                    throw new RuntimeException(
                            "Erro crítico: falha ao tentar realizar o rollback da inativação da promoção.",
                            rollbackErro
                    );
                }

                throw new RuntimeException(
                        "Erro ao inativar promoção ativa do produto. A operação foi cancelada: " + e.getMessage(),
                        e
                );

            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    System.err.println("[ERRO] Não foi possível restaurar o autoCommit da conexão.");
                    e.printStackTrace();
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

        if (produto == null || produto.getIdProduto() == null || produto.getIdProduto() <= 0) {
            throw new IllegalArgumentException("Produto inválido para busca de promoção.");
        }

        return promocaoDAO.buscarPromocaoAtivaPorProduto(produto);
    }

    /**
     * Valida os dados necessários para cadastrar uma promoção.
     *
     * @implNote Garante tipo, valor e produto válidos antes da aplicação da RN22.
     */
    private void validarPromocao(Promocao promocao) {

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
     * Valida se o desconto percentual está dentro do limite permitido.
     *
     * @implNote O desconto percentual não pode ser maior que 100%.
     */
    private void validarDescontoPercentual(BigDecimal valorDesconto) {

        BigDecimal cemPorCento = new BigDecimal("100.00");

        if (valorDesconto.compareTo(cemPorCento) > 0) {
            throw new IllegalArgumentException("Desconto percentual não pode ser maior que 100%.");
        }
    }

    /**
     * Valida se o desconto fixo não ultrapassa o preço do produto.
     *
     * @implNote O desconto em valor fixo não pode ser maior que o preço do produto.
     */
    private void validarDescontoValorFixo(Promocao promocao) {

        Produto produto = promocao.getProduto();

        if (produto.getPreco() == null) {
            throw new IllegalArgumentException("Preço do produto é obrigatório para validar desconto fixo.");
        }

        if (promocao.getValorDesconto().compareTo(produto.getPreco()) > 0) {
            throw new IllegalArgumentException("Desconto não pode ser maior que o preço do produto.");
        }
    }
}