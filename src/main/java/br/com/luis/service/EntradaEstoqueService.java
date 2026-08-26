package br.com.luis.service;

import br.com.luis.dao.EntradaEstoqueDAO;
import br.com.luis.dao.ItemEntradaEstoqueDAO;
import br.com.luis.dao.ProdutoDAO;
import br.com.luis.dao.UsuarioDAO;
import br.com.luis.model.EntradaEstoque;
import br.com.luis.model.ItemEntradaEstoque;
import br.com.luis.model.Produto;
import br.com.luis.model.Usuario;
import br.com.luis.util.ConnectionFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Confirma entradas de estoque em uma única transação.
 */
public class EntradaEstoqueService {

    private final EntradaEstoqueDAO entradaEstoqueDAO;
    private final ItemEntradaEstoqueDAO itemEntradaEstoqueDAO;
    private final ProdutoDAO produtoDAO;
    private final UsuarioDAO usuarioDAO;

    public EntradaEstoqueService() {
        this.entradaEstoqueDAO = new EntradaEstoqueDAO();
        this.itemEntradaEstoqueDAO = new ItemEntradaEstoqueDAO();
        this.produtoDAO = new ProdutoDAO();
        this.usuarioDAO = new UsuarioDAO();
    }

    /**
     * Retorna uma fotografia imutável do último preço de compra dos produtos que
     * possuem histórico de entrada de estoque.
     */
    public Map<Integer, BigDecimal> buscarUltimosPrecosCompra() {

        try (Connection conn = ConnectionFactory.getConnection()) {
            Map<Integer, BigDecimal> ultimosPrecosCompra =
                    itemEntradaEstoqueDAO.buscarUltimosPrecosCompra(conn);

            if (ultimosPrecosCompra == null) {
                throw new IllegalStateException(
                        "A consulta de últimos preços de compra retornou nulo."
                );
            }

            return Map.copyOf(ultimosPrecosCompra);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao consultar os últimos preços de compra.",
                    e
            );
        }
    }

    /**
     * Confirma um rascunho de Entrada de Estoque e retorna uma nova instância
     * sanitizada somente após o commit.
     */
    public EntradaEstoque confirmarEntrada(
            EntradaEstoque entradaEstoque,
            Integer usuarioId
    ) {

        List<ItemEntradaEstoque> itensRascunho = validarDadosBasicos(
                entradaEstoque,
                usuarioId
        );

        try (Connection conn = ConnectionFactory.getConnection()) {
            boolean autoCommitAnterior = conn.getAutoCommit();

            try {
                conn.setAutoCommit(false);

                EntradaEstoque entradaConfirmada = confirmarEntradaTransacional(
                        conn,
                        entradaEstoque,
                        itensRascunho,
                        usuarioId
                );

                conn.commit();

                return entradaConfirmada;

            } catch (RuntimeException e) {
                executarRollbackSeguro(conn);
                throw e;

            } catch (SQLException e) {
                executarRollbackSeguro(conn);
                throw new RuntimeException(
                        "Erro ao confirmar entrada de estoque.",
                        e
                );

            } finally {
                restaurarAutoCommitSeguro(
                        conn,
                        autoCommitAnterior
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao confirmar entrada de estoque.",
                    e
            );
        }
    }

    /**
     * Valida o rascunho sem acessar o banco de dados.
     */
    private List<ItemEntradaEstoque> validarDadosBasicos(
            EntradaEstoque entradaEstoque,
            Integer usuarioId
    ) {

        if (entradaEstoque == null) {
            throw new IllegalArgumentException(
                    "Entrada de estoque não pode ser nula."
            );
        }

        if (entradaEstoque.getIdEntrada() != null) {
            throw new IllegalArgumentException(
                    "O rascunho da entrada não pode possuir ID."
            );
        }

        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException(
                    "ID do usuário deve ser maior que zero."
            );
        }

        List<ItemEntradaEstoque> itensRascunho = entradaEstoque.getItens();

        if (itensRascunho == null || itensRascunho.isEmpty()) {
            throw new IllegalArgumentException(
                    "A entrada de estoque deve possuir ao menos um item."
            );
        }

        Set<Integer> produtosInformados = new HashSet<>();

        for (ItemEntradaEstoque item : itensRascunho) {
            validarItemRascunho(item);

            if (!produtosInformados.add(item.getProdutoId())) {
                throw new IllegalArgumentException(
                        "Produto repetido na mesma entrada de estoque."
                );
            }
        }

        return itensRascunho;
    }

    /**
     * Valida somente os dados do item que podem ser fornecidos pelo rascunho.
     */
    private void validarItemRascunho(ItemEntradaEstoque item) {

        if (item == null) {
            throw new IllegalArgumentException(
                    "Item da entrada de estoque não pode ser nulo."
            );
        }

        if (item.getIdItemEntrada() != null) {
            throw new IllegalArgumentException(
                    "O item do rascunho não pode possuir ID."
            );
        }

        if (item.getEntradaId() != null) {
            throw new IllegalArgumentException(
                    "O item do rascunho não pode estar vinculado a uma entrada persistida."
            );
        }

        if (item.getProdutoId() == null || item.getProdutoId() <= 0) {
            throw new IllegalArgumentException(
                    "ID do produto deve ser maior que zero."
            );
        }

        if (item.getQuantidadeRecebida() == null
                || item.getQuantidadeRecebida() <= 0) {
            throw new IllegalArgumentException(
                    "Quantidade recebida deve ser maior que zero."
            );
        }

        BigDecimal precoCompraUnitario = item.getPrecoCompraUnitario();

        if (precoCompraUnitario == null
                || precoCompraUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Preço de compra unitário deve ser maior que zero."
            );
        }
    }

    /**
     * Revalida os dados persistidos, cria os snapshots e executa todas as
     * mutações usando a Connection controlada pelo método público.
     */
    private EntradaEstoque confirmarEntradaTransacional(
            Connection conn,
            EntradaEstoque entradaRascunho,
            List<ItemEntradaEstoque> itensRascunho,
            Integer usuarioId
    ) {

        Usuario usuarioPersistido = buscarEValidarUsuarioAdministradorAtivo(
                conn,
                usuarioId
        );

        List<ItemEntradaEstoque> itensSanitizados =
                revalidarEPrepararTodosOsItens(
                        conn,
                        itensRascunho
                );

        EntradaEstoque entradaSanitizada = new EntradaEstoque(
                null,
                LocalDateTime.now(),
                usuarioPersistido.getIdUsuario(),
                usuarioPersistido.getNome(),
                entradaRascunho.getReferencia(),
                entradaRascunho.getObservacao(),
                itensSanitizados
        );

        int entradaId = entradaEstoqueDAO.inserir(
                conn,
                entradaSanitizada
        );

        if (entradaId <= 0) {
            throw new IllegalStateException(
                    "A entrada de estoque não recebeu um ID válido."
            );
        }

        entradaSanitizada.setIdEntrada(entradaId);

        for (ItemEntradaEstoque item : itensSanitizados) {
            item.setEntradaId(entradaId);

            int itemId = itemEntradaEstoqueDAO.inserir(
                    conn,
                    item
            );

            if (itemId <= 0) {
                throw new IllegalStateException(
                        "O item da entrada de estoque não recebeu um ID válido."
                );
            }

            item.setIdItemEntrada(itemId);

            produtoDAO.incrementarEstoqueEntrada(
                    conn,
                    item.getProdutoId(),
                    item.getQuantidadeRecebida()
            );
        }

        return entradaSanitizada;
    }

    /**
     * Busca e revalida o administrador persistido antes da primeira mutação.
     */
    private Usuario buscarEValidarUsuarioAdministradorAtivo(
            Connection conn,
            Integer usuarioId
    ) {

        Usuario usuario = usuarioDAO.buscarPorId(
                conn,
                usuarioId
        );

        if (usuario == null
                || usuario.getIdUsuario() == null
                || usuario.getIdUsuario() <= 0
                || !usuarioId.equals(usuario.getIdUsuario())
                || !"ADMIN".equals(usuario.getPerfil())
                || !"ATIVO".equals(usuario.getStatus())
                || usuario.isTrocaSenhaObrigatoria()
                || usuario.getNome() == null
                || usuario.getNome().isBlank()) {

            throw new IllegalStateException(
                    "Usuário não autorizado a registrar entrada de estoque."
            );
        }

        return usuario;
    }

    /**
     * Revalida todos os produtos e prepara todos os itens antes de qualquer escrita.
     */
    private List<ItemEntradaEstoque> revalidarEPrepararTodosOsItens(
            Connection conn,
            List<ItemEntradaEstoque> itensRascunho
    ) {

        List<ItemEntradaEstoque> itensSanitizados = new ArrayList<>();

        for (ItemEntradaEstoque itemRascunho : itensRascunho) {
            Integer produtoId = itemRascunho.getProdutoId();
            Produto produtoPersistido = produtoDAO.buscarPorId(
                    conn,
                    produtoId
            );

            if (produtoPersistido == null) {
                throw new IllegalArgumentException(
                        "Produto não encontrado para entrada de estoque."
                );
            }

            if (produtoPersistido.getIdProduto() == null
                    || produtoPersistido.getIdProduto() <= 0
                    || !produtoId.equals(produtoPersistido.getIdProduto())) {

                throw new IllegalStateException(
                        "Produto persistido possui ID inconsistente."
                );
            }

            if (!produtoPersistido.isAtivo()) {
                throw new IllegalStateException(
                        "Produto inativo não pode receber entrada de estoque."
                );
            }

            if (produtoPersistido.getDescricao() == null
                    || produtoPersistido.getDescricao().isBlank()) {

                throw new IllegalStateException(
                        "Produto persistido possui descrição inválida."
                );
            }

            ItemEntradaEstoque itemSanitizado = new ItemEntradaEstoque(
                    null,
                    null,
                    produtoPersistido.getIdProduto(),
                    produtoPersistido.getDescricao(),
                    itemRascunho.getQuantidadeRecebida(),
                    itemRascunho.getPrecoCompraUnitario()
            );

            itensSanitizados.add(itemSanitizado);
        }

        return itensSanitizados;
    }

    /**
     * Executa rollback sem substituir a exceção original do fluxo.
     */
    private void executarRollbackSeguro(Connection conn) {

        if (conn == null) {
            return;
        }

        try {
            conn.rollback();
        } catch (SQLException e) {
            System.err.println(
                    "Erro ao executar rollback da entrada de estoque: "
                            + e.getMessage()
            );
        }
    }

    /**
     * Restaura o autoCommit sem mascarar uma exceção anterior.
     */
    private void restaurarAutoCommitSeguro(
            Connection conn,
            boolean autoCommitAnterior
    ) {

        if (conn == null) {
            return;
        }

        try {
            conn.setAutoCommit(autoCommitAnterior);
        } catch (SQLException e) {
            System.err.println(
                    "Erro ao restaurar autoCommit da entrada de estoque: "
                            + e.getMessage()
            );
        }
    }
}
