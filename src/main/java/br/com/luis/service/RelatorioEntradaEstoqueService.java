package br.com.luis.service;

import br.com.luis.dao.EntradaEstoqueDAO;
import br.com.luis.dao.ItemEntradaEstoqueDAO;
import br.com.luis.dao.UsuarioDAO;
import br.com.luis.model.Usuario;
import br.com.luis.util.ConnectionFactory;
import br.com.luis.viewmodel.EntradaEstoqueRelatorioView;
import br.com.luis.viewmodel.FiltroRelatorioEntradaEstoque;
import br.com.luis.viewmodel.ItemEntradaEstoqueRelatorioView;
import br.com.luis.viewmodel.ResultadoRelatorioEntradaEstoque;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Coordena consultas protegidas e exclusivamente de leitura do relatório de
 * entradas de estoque.
 */
public class RelatorioEntradaEstoqueService {

    private static final String MENSAGEM_ACESSO_NEGADO =
            "Usuário não autorizado a consultar o relatório de entradas de estoque.";

    private final EntradaEstoqueDAO entradaEstoqueDAO;
    private final ItemEntradaEstoqueDAO itemEntradaEstoqueDAO;
    private final UsuarioDAO usuarioDAO;

    public RelatorioEntradaEstoqueService() {
        this.entradaEstoqueDAO = new EntradaEstoqueDAO();
        this.itemEntradaEstoqueDAO = new ItemEntradaEstoqueDAO();
        this.usuarioDAO = new UsuarioDAO();
    }

    /**
     * Consulta entradas completas e consolida os totalizadores da fotografia.
     */
    public ResultadoRelatorioEntradaEstoque consultarRelatorio(
            FiltroRelatorioEntradaEstoque filtro,
            Integer usuarioId
    ) {
        if (filtro == null) {
            throw new IllegalArgumentException(
                    "Filtro do relatório de entradas não pode ser nulo."
            );
        }
        filtro.validar();
        validarUsuarioId(usuarioId);

        LocalDate dataFinal = filtro.getDataFinal();
        if (LocalDate.MAX.equals(dataFinal)) {
            throw new IllegalArgumentException(
                    "A data final não permite calcular o limite exclusivo do período."
            );
        }

        LocalDateTime inicioInclusivo =
                filtro.getDataInicial().atStartOfDay();
        LocalDateTime fimExclusivo =
                dataFinal.plusDays(1).atStartOfDay();

        try (Connection conn = ConnectionFactory.getConnection()) {
            validarAutorizacaoPersistida(conn, usuarioId);

            List<EntradaEstoqueRelatorioView> entradas =
                    entradaEstoqueDAO.listarParaRelatorio(
                            conn,
                            inicioInclusivo,
                            fimExclusivo,
                            filtro
                    );

            if (entradas == null) {
                throw new IllegalStateException(
                        "A consulta do relatório de entradas retornou lista nula."
                );
            }

            int totalUnidades = 0;
            BigDecimal valorTotal = BigDecimal.ZERO;

            for (EntradaEstoqueRelatorioView entrada : entradas) {
                if (entrada == null) {
                    throw new IllegalStateException(
                            "A consulta do relatório de entradas retornou linha nula."
                    );
                }

                try {
                    totalUnidades = Math.addExact(
                            totalUnidades,
                            entrada.getTotalUnidades()
                    );
                } catch (ArithmeticException e) {
                    throw new IllegalStateException(
                            "Total de unidades excede o limite suportado.",
                            e
                    );
                }

                valorTotal = valorTotal.add(
                        entrada.getValorTotal()
                );
            }

            try {
                return new ResultadoRelatorioEntradaEstoque(
                        filtro,
                        entradas,
                        entradas.size(),
                        totalUnidades,
                        valorTotal.setScale(2, RoundingMode.HALF_UP)
                );
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                        "Incoerência ao consolidar o relatório de entradas de estoque.",
                        e
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao acessar o banco durante a consulta "
                            + "do relatório de entradas de estoque.",
                    e
            );
        }
    }

    /**
     * Consulta todos os itens históricos de uma entrada.
     */
    public List<ItemEntradaEstoqueRelatorioView> consultarItensEntrada(
            Integer entradaId,
            Integer usuarioId
    ) {
        if (entradaId == null || entradaId <= 0) {
            throw new IllegalArgumentException(
                    "ID da entrada deve ser maior que zero."
            );
        }
        validarUsuarioId(usuarioId);

        try (Connection conn = ConnectionFactory.getConnection()) {
            validarAutorizacaoPersistida(conn, usuarioId);

            List<ItemEntradaEstoqueRelatorioView> itens =
                    itemEntradaEstoqueDAO
                            .listarParaRelatorioPorEntradaId(
                                    conn,
                                    entradaId
                            );

            if (itens == null) {
                throw new IllegalStateException(
                        "A consulta dos itens da entrada retornou lista nula."
                );
            }
            if (itens.isEmpty()) {
                throw new IllegalStateException(
                        "A entrada de estoque informada não possui itens históricos."
                );
            }

            return List.copyOf(itens);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Erro ao acessar o banco durante a consulta "
                            + "dos itens da entrada de estoque.",
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

    private void validarAutorizacaoPersistida(
            Connection conn,
            Integer usuarioId
    ) {
        Usuario usuario = usuarioDAO.buscarPorId(
                conn,
                usuarioId
        );

        boolean autorizado =
                usuario != null
                        && usuario.getIdUsuario() != null
                        && usuario.getIdUsuario() > 0
                        && usuarioId.equals(usuario.getIdUsuario())
                        && "ADMIN".equals(usuario.getPerfil())
                        && "ATIVO".equals(usuario.getStatus())
                        && !usuario.isTrocaSenhaObrigatoria();

        if (!autorizado) {
            throw new SecurityException(
                    MENSAGEM_ACESSO_NEGADO
            );
        }
    }
}
