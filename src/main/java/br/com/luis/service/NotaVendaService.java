package br.com.luis.service;

import br.com.luis.dao.ItemNotaVendaDAO;
import br.com.luis.dao.NotaVendaDAO;
import br.com.luis.model.FormaPagamento;
import br.com.luis.model.ItemNotaVenda;
import br.com.luis.model.NotaVenda;
import br.com.luis.model.StatusNotaVenda;
import br.com.luis.model.TipoVenda;
import br.com.luis.util.ConnectionFactory;
import br.com.luis.util.GeradorNotaVendaPdf;
import br.com.luis.util.TipoViaNotaVendaPdf;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Service responsável pela coordenação documental da Nota de Venda.
 *
 * Carrega exclusivamente a fotografia persistida em NotaVenda e ItemNotaVenda,
 * valida sua integridade e delega a geração física do PDF. Não reconstrói dados
 * a partir de cadastros atuais, não altera banco e não participa das transações
 * comerciais de venda ou estorno.
 */
public class NotaVendaService {

    private static final int ESCALA_MONETARIA = 2;

    private final NotaVendaDAO notaVendaDAO;
    private final ItemNotaVendaDAO itemNotaVendaDAO;
    private final GeradorNotaVendaPdf geradorNotaVendaPdf;

    public NotaVendaService() {
        this.notaVendaDAO = new NotaVendaDAO();
        this.itemNotaVendaDAO = new ItemNotaVendaDAO();
        this.geradorNotaVendaPdf = new GeradorNotaVendaPdf();
    }

    /**
     * Gera o PDF a partir da fotografia documental persistida da Nota.
     *
     * Aceita Nota ATIVA ou ESTORNADA, não reconstrói dados a partir dos cadastros
     * atuais e gera o arquivo físico somente depois da transação comercial.
     *
     * @throws IllegalArgumentException se os parâmetros de geração forem inválidos.
     * @throws IllegalStateException se a Nota não existir, sua fotografia estiver
     *                               inconsistente ou a consulta ou geração falhar.
     */
    public Path gerarPdfPorNotaId(
            Integer notaId,
            TipoViaNotaVendaPdf tipoVia,
            Path destino
    ) {
        validarIdPositivo(notaId, "ID da Nota de Venda");
        validarParametrosGeracao(tipoVia, destino);

        NotaVenda notaVenda;
        List<ItemNotaVenda> itens;

        try (Connection conn = ConnectionFactory.getConnection()) {
            notaVenda = notaVendaDAO.buscarPorId(conn, notaId);

            if (notaVenda == null) {
                throw new IllegalStateException(
                        "Nota de Venda não encontrada."
                );
            }

            itens = itemNotaVendaDAO.listarPorNotaId(
                    conn,
                    notaVenda.getIdNota()
            );
        } catch (IllegalStateException e) {
            throw e;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Não foi possível fechar a consulta da Nota de Venda.",
                    e
            );
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Não foi possível carregar a Nota de Venda para geração.",
                    e
            );
        }

        return validarEGerar(
                notaVenda,
                itens,
                tipoVia,
                destino
        );
    }

    /**
     * Gera o PDF da Nota vinculada a uma venda.
     *
     * Vendas legadas podem não possuir fotografia documental. Quando a Nota existe,
     * o mesmo número e snapshot são preservados em estado ATIVO ou ESTORNADO e o
     * arquivo físico é gerado fora da transação comercial.
     *
     * @throws IllegalArgumentException se os parâmetros de geração forem inválidos.
     * @throws IllegalStateException se a venda não possuir Nota, a fotografia
     *                               estiver inconsistente ou a consulta ou geração falhar.
     */
    public Path gerarPdfPorVendaId(
            Integer vendaId,
            TipoViaNotaVendaPdf tipoVia,
            Path destino
    ) {
        validarIdPositivo(vendaId, "ID da venda");
        validarParametrosGeracao(tipoVia, destino);

        NotaVenda notaVenda;
        List<ItemNotaVenda> itens;

        try (Connection conn = ConnectionFactory.getConnection()) {
            notaVenda = notaVendaDAO.buscarPorVendaId(
                    conn,
                    vendaId
            );

            if (notaVenda == null) {
                throw new IllegalStateException(
                        "Esta venda não possui Nota de Venda disponível para geração."
                );
            }

            itens = itemNotaVendaDAO.listarPorNotaId(
                    conn,
                    notaVenda.getIdNota()
            );
        } catch (IllegalStateException e) {
            throw e;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Não foi possível fechar a consulta da Nota de Venda.",
                    e
            );
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Não foi possível carregar a Nota de Venda para geração.",
                    e
            );
        }

        return validarEGerar(
                notaVenda,
                itens,
                tipoVia,
                destino
        );
    }

    /**
     * Sugere somente o nome físico do arquivo, sem criar diretórios ou arquivos.
     *
     * @param notaId identificador permanente da Nota de Venda.
     * @param tipoVia indicação de primeira ou segunda via.
     * @return nome sugerido para o arquivo PDF.
     * @throws IllegalArgumentException se o identificador ou o tipo da via forem inválidos.
     */
    public String sugerirNomeArquivo(
            Integer notaId,
            TipoViaNotaVendaPdf tipoVia
    ) {
        validarIdPositivo(notaId, "ID da Nota de Venda");

        if (tipoVia == null) {
            throw new IllegalArgumentException(
                    "Tipo da via da Nota de Venda é obrigatório."
            );
        }

        String numeroNota = String.format("%06d", notaId);

        if (tipoVia == TipoViaNotaVendaPdf.SEGUNDA_VIA) {
            return "nota-venda-"
                    + numeroNota
                    + "-segunda-via.pdf";
        }

        return "nota-venda-"
                + numeroNota
                + ".pdf";
    }

    /**
     * Sugere o nome físico do PDF a partir da venda vinculada à Nota.
     *
     * Usa somente a fotografia documental já persistida. A ausência de Nota para
     * a venda é tratada como compatibilidade com legado e não provoca backfill.
     *
     * @param vendaId identificador da venda vinculada à Nota.
     * @param tipoVia indicação de primeira ou segunda via.
     * @return nome sugerido para o arquivo PDF da Nota persistida.
     * @throws IllegalArgumentException se o identificador ou o tipo da via forem inválidos.
     * @throws IllegalStateException se a venda não possuir Nota ou a consulta falhar.
     */
    public String sugerirNomeArquivoPorVendaId(
            Integer vendaId,
            TipoViaNotaVendaPdf tipoVia
    ) {
        validarIdPositivo(vendaId, "ID da venda");

        if (tipoVia == null) {
            throw new IllegalArgumentException(
                    "Tipo da via da Nota de Venda é obrigatório."
            );
        }

        NotaVenda notaVenda;

        try (Connection conn = ConnectionFactory.getConnection()) {
            notaVenda = notaVendaDAO.buscarPorVendaId(
                    conn,
                    vendaId
            );

            if (notaVenda == null) {
                throw new IllegalStateException(
                        "Esta venda não possui Nota de Venda disponível para geração."
                );
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Não foi possível fechar a consulta da Nota de Venda.",
                    e
            );
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Não foi possível localizar a Nota de Venda para geração.",
                    e
            );
        }

        return sugerirNomeArquivo(
                notaVenda.getIdNota(),
                tipoVia
        );
    }

    private Path validarEGerar(
            NotaVenda notaVenda,
            List<ItemNotaVenda> itens,
            TipoViaNotaVendaPdf tipoVia,
            Path destino
    ) {
        validarFotografia(notaVenda, itens);

        try {
            return geradorNotaVendaPdf.gerar(
                    notaVenda,
                    itens,
                    tipoVia,
                    destino
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Não foi possível gerar o PDF da Nota de Venda.",
                    e
            );
        }
    }

    private void validarParametrosGeracao(
            TipoViaNotaVendaPdf tipoVia,
            Path destino
    ) {
        if (tipoVia == null) {
            throw new IllegalArgumentException(
                    "Tipo da via da Nota de Venda é obrigatório."
            );
        }

        if (destino == null) {
            throw new IllegalArgumentException(
                    "Destino do PDF da Nota de Venda é obrigatório."
            );
        }
    }

    private void validarFotografia(
            NotaVenda notaVenda,
            List<ItemNotaVenda> itens
    ) {
        validarCabecalhoFotografia(notaVenda);
        validarClienteFotografia(notaVenda);
        validarPagamentoEPrazoFotografia(notaVenda);
        validarItensFotografia(notaVenda, itens);
    }

    private void validarCabecalhoFotografia(NotaVenda notaVenda) {
        if (notaVenda == null) {
            throw new IllegalStateException(
                    "Fotografia da Nota de Venda não foi carregada."
            );
        }

        if (notaVenda.getIdNota() == null
                || notaVenda.getIdNota() <= 0) {
            throw new IllegalStateException(
                    "Nota de Venda possui ID inválido."
            );
        }

        if (notaVenda.getVendaId() == null
                || notaVenda.getVendaId() <= 0) {
            throw new IllegalStateException(
                    "Nota de Venda possui vínculo inválido com a venda."
            );
        }

        if (notaVenda.getStatus() != StatusNotaVenda.ATIVA
                && notaVenda.getStatus() != StatusNotaVenda.ESTORNADA) {
            throw new IllegalStateException(
                    "Nota de Venda possui status documental inválido."
            );
        }

        if (notaVenda.getDataHoraVenda() == null) {
            throw new IllegalStateException(
                    "Nota de Venda não possui data e hora históricas."
            );
        }

        if (notaVenda.getTipoVenda() == null) {
            throw new IllegalStateException(
                    "Nota de Venda não possui tipo histórico."
            );
        }

        if (notaVenda.getFormaPagamento() == null) {
            throw new IllegalStateException(
                    "Nota de Venda não possui forma de pagamento histórica."
            );
        }

        if (notaVenda.getUsuarioId() == null
                || notaVenda.getUsuarioId() <= 0) {
            throw new IllegalStateException(
                    "Nota de Venda possui responsável histórico inválido."
            );
        }

        if (notaVenda.getNomeUsuario() == null
                || notaVenda.getNomeUsuario().isBlank()) {
            throw new IllegalStateException(
                    "Nota de Venda não possui nome histórico do responsável."
            );
        }

        validarValorPositivo(
                notaVenda.getValorTotal(),
                "Valor total da Nota de Venda"
        );

        validarValorNaoNegativo(
                notaVenda.getValorDescontoGlobal(),
                "Desconto global da Nota de Venda"
        );
    }

    private void validarClienteFotografia(NotaVenda notaVenda) {
        boolean possuiId = notaVenda.getClienteId() != null;
        boolean possuiNome = notaVenda.getNomeCliente() != null;
        boolean possuiDocumento = notaVenda.getDocumentoCliente() != null;

        if (!possuiId && !possuiNome && !possuiDocumento) {
            if (notaVenda.getTipoVenda() == TipoVenda.A_PRAZO) {
                throw new IllegalStateException(
                        "Venda a prazo não possui cliente histórico identificado."
                );
            }

            return;
        }

        if (!possuiId
                || notaVenda.getClienteId() <= 0
                || !possuiNome
                || notaVenda.getNomeCliente().isBlank()
                || !possuiDocumento
                || notaVenda.getDocumentoCliente().isBlank()) {
            throw new IllegalStateException(
                    "Nota de Venda possui fotografia histórica inconsistente do cliente."
            );
        }
    }

    private void validarPagamentoEPrazoFotografia(
            NotaVenda notaVenda
    ) {
        if (notaVenda.getTipoVenda() == TipoVenda.A_VISTA) {
            validarVendaAVistaFotografia(notaVenda);
            return;
        }

        if (notaVenda.getTipoVenda() == TipoVenda.A_PRAZO) {
            validarVendaAPrazoFotografia(notaVenda);
            return;
        }

        throw new IllegalStateException(
                "Nota de Venda possui tipo de venda não suportado."
        );
    }

    private void validarVendaAVistaFotografia(NotaVenda notaVenda) {
        FormaPagamento formaPagamento =
                notaVenda.getFormaPagamento();

        if (formaPagamento != FormaPagamento.DINHEIRO
                && formaPagamento != FormaPagamento.PIX
                && formaPagamento != FormaPagamento.CARTAO) {
            throw new IllegalStateException(
                    "Venda à vista possui forma de pagamento histórica inválida."
            );
        }

        if (notaVenda.getPrazoPagamentoId() != null
                || notaVenda.getQuantidadeDiasPrazo() != null
                || notaVenda.getDataVencimento() != null) {
            throw new IllegalStateException(
                    "Venda à vista possui dados históricos de prazo indevidos."
            );
        }

        if (formaPagamento == FormaPagamento.DINHEIRO) {
            validarDinheiroFotografia(notaVenda);
            return;
        }

        if (notaVenda.getValorRecebido() != null
                || notaVenda.getTroco() != null) {
            throw new IllegalStateException(
                    "Pagamento sem dinheiro possui valor recebido ou troco indevido."
            );
        }
    }

    private void validarDinheiroFotografia(NotaVenda notaVenda) {
        validarValorPositivo(
                notaVenda.getValorRecebido(),
                "Valor recebido da Nota de Venda"
        );

        validarValorNaoNegativo(
                notaVenda.getTroco(),
                "Troco da Nota de Venda"
        );

        if (notaVenda.getValorRecebido()
                .compareTo(notaVenda.getValorTotal()) < 0) {
            throw new IllegalStateException(
                    "Valor recebido histórico é menor que o total da Nota de Venda."
            );
        }

        BigDecimal trocoEsperado = normalizarMoeda(
                notaVenda.getValorRecebido()
                        .subtract(notaVenda.getValorTotal())
        );

        if (normalizarMoeda(notaVenda.getTroco())
                .compareTo(trocoEsperado) != 0) {
            throw new IllegalStateException(
                    "Troco histórico da Nota de Venda é inconsistente."
            );
        }
    }

    private void validarVendaAPrazoFotografia(NotaVenda notaVenda) {
        if (notaVenda.getFormaPagamento()
                != FormaPagamento.A_PRAZO) {
            throw new IllegalStateException(
                    "Venda a prazo não possui forma de pagamento A_PRAZO."
            );
        }

        if (notaVenda.getPrazoPagamentoId() == null
                || notaVenda.getPrazoPagamentoId() <= 0
                || notaVenda.getQuantidadeDiasPrazo() == null
                || notaVenda.getQuantidadeDiasPrazo() <= 0
                || notaVenda.getDataVencimento() == null) {
            throw new IllegalStateException(
                    "Venda a prazo possui fotografia histórica de prazo incompleta."
            );
        }

        if (notaVenda.getValorRecebido() != null
                || notaVenda.getTroco() != null) {
            throw new IllegalStateException(
                    "Venda a prazo possui valor recebido ou troco indevido."
            );
        }
    }

    private void validarItensFotografia(
            NotaVenda notaVenda,
            List<ItemNotaVenda> itens
    ) {
        if (itens == null || itens.isEmpty()) {
            throw new IllegalStateException(
                    "Nota de Venda não possui itens históricos."
            );
        }

        BigDecimal somaSubtotais = zeroMonetario();
        BigDecimal somaDescontosGlobais = zeroMonetario();

        for (ItemNotaVenda item : itens) {
            validarItemFotografia(notaVenda, item);

            BigDecimal subtotalEsperado = normalizarMoeda(
                    item.getPrecoUnitario()
                            .multiply(BigDecimal.valueOf(item.getQuantidade()))
                            .subtract(item.getDescontoPromocional())
                            .subtract(item.getDescontoGlobal())
            );

            if (normalizarMoeda(item.getSubtotal())
                    .compareTo(subtotalEsperado) != 0) {
                throw new IllegalStateException(
                        "Item histórico "
                                + item.getIdItemNota()
                                + " possui subtotal inconsistente."
                );
            }

            somaSubtotais = normalizarMoeda(
                    somaSubtotais.add(item.getSubtotal())
            );

            somaDescontosGlobais = normalizarMoeda(
                    somaDescontosGlobais.add(
                            item.getDescontoGlobal()
                    )
            );
        }

        if (somaSubtotais.compareTo(
                normalizarMoeda(notaVenda.getValorTotal())
        ) != 0) {
            throw new IllegalStateException(
                    "Soma dos itens não corresponde ao valor total da Nota de Venda."
            );
        }

        if (somaDescontosGlobais.compareTo(
                normalizarMoeda(
                        notaVenda.getValorDescontoGlobal()
                )
        ) != 0) {
            throw new IllegalStateException(
                    "Soma dos descontos globais dos itens não corresponde à Nota de Venda."
            );
        }
    }

    private void validarItemFotografia(
            NotaVenda notaVenda,
            ItemNotaVenda item
    ) {
        if (item == null) {
            throw new IllegalStateException(
                    "Nota de Venda possui item histórico nulo."
            );
        }

        if (item.getIdItemNota() == null
                || item.getIdItemNota() <= 0) {
            throw new IllegalStateException(
                    "Nota de Venda possui item histórico com ID inválido."
            );
        }

        if (item.getNotaId() == null
                || !item.getNotaId().equals(
                notaVenda.getIdNota()
        )) {
            throw new IllegalStateException(
                    "Item histórico possui vínculo inconsistente com a Nota de Venda."
            );
        }

        if (item.getProdutoId() == null
                || item.getProdutoId() <= 0) {
            throw new IllegalStateException(
                    "Item da Nota de Venda possui produto histórico inválido."
            );
        }

        if (item.getDescricaoProduto() == null
                || item.getDescricaoProduto().isBlank()) {
            throw new IllegalStateException(
                    "Item da Nota de Venda não possui descrição histórica."
            );
        }

        if (item.getQuantidade() == null
                || item.getQuantidade() <= 0) {
            throw new IllegalStateException(
                    "Item da Nota de Venda possui quantidade inválida."
            );
        }

        validarValorNaoNegativo(
                item.getPrecoUnitario(),
                "Preço unitário do item histórico"
        );
        validarValorNaoNegativo(
                item.getDescontoPromocional(),
                "Desconto promocional do item histórico"
        );
        validarValorNaoNegativo(
                item.getDescontoGlobal(),
                "Desconto global do item histórico"
        );
        validarValorNaoNegativo(
                item.getSubtotal(),
                "Subtotal do item histórico"
        );
    }

    private void validarValorPositivo(
            BigDecimal valor,
            String nomeCampo
    ) {
        if (valor == null
                || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                    nomeCampo + " deve ser maior que zero."
            );
        }
    }

    private void validarValorNaoNegativo(
            BigDecimal valor,
            String nomeCampo
    ) {
        if (valor == null
                || valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(
                    nomeCampo + " não pode ser negativo."
            );
        }
    }

    private void validarIdPositivo(
            Integer id,
            String nomeCampo
    ) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    nomeCampo + " deve ser maior que zero."
            );
        }
    }

    private BigDecimal normalizarMoeda(BigDecimal valor) {
        return valor.setScale(
                ESCALA_MONETARIA,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal zeroMonetario() {
        return BigDecimal.ZERO.setScale(
                ESCALA_MONETARIA,
                RoundingMode.HALF_UP
        );
    }
}
