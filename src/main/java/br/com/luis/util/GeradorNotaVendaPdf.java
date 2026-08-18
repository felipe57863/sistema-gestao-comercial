package br.com.luis.util;

import br.com.luis.model.FormaPagamento;
import br.com.luis.model.ItemNotaVenda;
import br.com.luis.model.NotaVenda;
import br.com.luis.model.StatusNotaVenda;
import br.com.luis.model.TipoVenda;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Gerador físico do PDF da Nota de Venda.
 *
 * Recebe uma fotografia documental já carregada e validada. Não acessa banco,
 * DAO, JavaFX ou transações e não altera as entidades recebidas.
 */
public class GeradorNotaVendaPdf {

    private static final float MARGEM_ESQUERDA = 40f;
    private static final float MARGEM_DIREITA = 40f;
    private static final float MARGEM_SUPERIOR = 42f;
    private static final float MARGEM_INFERIOR = 42f;

    private static final float TAMANHO_TITULO = 15f;
    private static final float TAMANHO_SUBTITULO = 11f;
    private static final float TAMANHO_TEXTO = 9f;
    private static final float TAMANHO_TABELA = 7f;

    private static final float ALTURA_LINHA = 12f;
    private static final float ALTURA_LINHA_TABELA = 10f;
    private static final float ALTURA_CABECALHO_TABELA = 22f;
    private static final float PADDING_CELULA = 4f;

    private static final float[] LARGURAS_COLUNAS = {
            190f,
            35f,
            70f,
            70f,
            70f,
            80f
    };

    private static final DateTimeFormatter FORMATADOR_DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final DateTimeFormatter FORMATADOR_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PDFont fonteNormal;
    private final PDFont fonteNegrito;

    public GeradorNotaVendaPdf() {
        this.fonteNormal = new PDType1Font(
                Standard14Fonts.FontName.HELVETICA
        );
        this.fonteNegrito = new PDType1Font(
                Standard14Fonts.FontName.HELVETICA_BOLD
        );
    }

    /**
     * Gera a Nota em A4 retrato no destino informado.
     *
     * O arquivo final nunca é sobrescrito. A gravação ocorre primeiro em arquivo
     * temporário criado pelo próprio gerador no mesmo diretório e, somente após
     * o PDF ser concluído, esse arquivo é movido para o destino final.
     */
    public Path gerar(
            NotaVenda notaVenda,
            List<ItemNotaVenda> itens,
            TipoViaNotaVendaPdf tipoVia,
            Path destino
    ) throws IOException {
        if (notaVenda == null) {
            throw new IllegalArgumentException(
                    "Nota de Venda é obrigatória para gerar o PDF."
            );
        }

        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException(
                    "Itens da Nota de Venda são obrigatórios para gerar o PDF."
            );
        }

        if (tipoVia == null) {
            throw new IllegalArgumentException(
                    "Tipo da via é obrigatório para gerar o PDF."
            );
        }

        Path destinoNormalizado = validarDestino(destino);
        Path diretorio = destinoNormalizado.getParent();
        Path arquivoTemporario = null;

        try {
            arquivoTemporario = Files.createTempFile(
                    diretorio,
                    ".nota-venda-",
                    ".tmp"
            );

            gerarDocumento(
                    notaVenda,
                    itens,
                    tipoVia,
                    arquivoTemporario
            );

            Files.move(
                    arquivoTemporario,
                    destinoNormalizado
            );

            arquivoTemporario = null;
            return destinoNormalizado;

        } finally {
            if (arquivoTemporario != null) {
                Files.deleteIfExists(arquivoTemporario);
            }
        }
    }

    private Path validarDestino(Path destino) throws IOException {
        if (destino == null) {
            throw new IllegalArgumentException(
                    "Destino do PDF é obrigatório."
            );
        }

        Path destinoNormalizado =
                destino.toAbsolutePath().normalize();

        Path nomeArquivo = destinoNormalizado.getFileName();

        if (nomeArquivo == null
                || !nomeArquivo.toString()
                .toLowerCase(Locale.ROOT)
                .endsWith(".pdf")) {
            throw new IllegalArgumentException(
                    "O destino da Nota de Venda deve possuir extensão .pdf."
            );
        }

        Path diretorio = destinoNormalizado.getParent();

        if (diretorio == null
                || !Files.exists(diretorio)
                || !Files.isDirectory(diretorio)) {
            throw new IllegalArgumentException(
                    "O diretório de destino do PDF não existe."
            );
        }

        if (!Files.isWritable(diretorio)) {
            throw new IllegalArgumentException(
                    "O diretório de destino do PDF não permite gravação."
            );
        }

        if (Files.exists(destinoNormalizado)) {
            throw new FileAlreadyExistsException(
                    destinoNormalizado.toString()
            );
        }

        return destinoNormalizado;
    }

    private void gerarDocumento(
            NotaVenda notaVenda,
            List<ItemNotaVenda> itens,
            TipoViaNotaVendaPdf tipoVia,
            Path arquivoTemporario
    ) throws IOException {
        try (PDDocument documento = new PDDocument()) {
            EstadoPagina estado = criarPagina(documento);

            try {
                escreverCabecalhoCompleto(
                        estado,
                        notaVenda,
                        tipoVia
                );

                escreverCabecalhoTabela(estado);

                for (ItemNotaVenda item : itens) {
                    estado = escreverItem(
                            documento,
                            estado,
                            notaVenda,
                            tipoVia,
                            item
                    );
                }

                estado = garantirEspacoResumo(
                        documento,
                        estado,
                        notaVenda,
                        tipoVia
                );

                escreverResumoFinanceiro(
                        estado,
                        notaVenda
                );

            } finally {
                estado.fechar();
            }

            documento.save(
                    arquivoTemporario.toFile()
            );
        }
    }

    private EstadoPagina criarPagina(PDDocument documento)
            throws IOException {
        PDPage pagina = new PDPage(PDRectangle.A4);
        documento.addPage(pagina);

        PDPageContentStream conteudo =
                new PDPageContentStream(
                        documento,
                        pagina
                );

        float yInicial = pagina.getMediaBox().getHeight()
                - MARGEM_SUPERIOR;

        return new EstadoPagina(
                pagina,
                conteudo,
                yInicial
        );
    }

    private EstadoPagina criarPaginaContinuacao(
            PDDocument documento,
            EstadoPagina estadoAnterior,
            NotaVenda notaVenda,
            TipoViaNotaVendaPdf tipoVia,
            boolean repetirTabela
    ) throws IOException {
        estadoAnterior.fechar();

        EstadoPagina novoEstado = criarPagina(documento);

        escreverCabecalhoContinuacao(
                novoEstado,
                notaVenda,
                tipoVia
        );

        if (repetirTabela) {
            escreverCabecalhoTabela(novoEstado);
        }

        return novoEstado;
    }

    private void escreverCabecalhoCompleto(
            EstadoPagina estado,
            NotaVenda notaVenda,
            TipoViaNotaVendaPdf tipoVia
    ) throws IOException {
        escreverTextoCentralizado(
                estado,
                "Sistema de Gestão Comercial",
                fonteNegrito,
                TAMANHO_TITULO
        );
        estado.y -= 18f;

        escreverTextoCentralizado(
                estado,
                "Nota de Venda nº "
                        + formatarNumeroNota(
                        notaVenda.getIdNota()
                ),
                fonteNegrito,
                TAMANHO_SUBTITULO
        );
        estado.y -= 15f;

        if (tipoVia == TipoViaNotaVendaPdf.SEGUNDA_VIA) {
            escreverTextoCentralizado(
                    estado,
                    "2ª VIA — REIMPRESSÃO",
                    fonteNegrito,
                    TAMANHO_SUBTITULO
            );
            estado.y -= 15f;
        }

        escreverTextoCentralizado(
                estado,
                "Comprovante interno — não fiscal",
                fonteNormal,
                TAMANHO_TEXTO
        );
        estado.y -= 16f;

        if (notaVenda.getStatus()
                == StatusNotaVenda.ESTORNADA) {
            escreverAvisoEstorno(estado);
        }

        escreverLinhaInformacao(
                estado,
                "Venda nº " + notaVenda.getVendaId()
        );
        escreverLinhaInformacao(
                estado,
                "Data/Hora: "
                        + notaVenda.getDataHoraVenda()
                        .format(FORMATADOR_DATA_HORA)
        );
        escreverLinhaInformacao(
                estado,
                "Situação: " + notaVenda.getStatus().name()
        );
        escreverLinhaInformacao(
                estado,
                "Responsável: " + notaVenda.getNomeUsuario()
        );

        if (notaVenda.getClienteId() == null) {
            escreverLinhaInformacao(
                    estado,
                    "Cliente: Consumidor não identificado"
            );
        } else {
            escreverLinhaInformacao(
                    estado,
                    "Cliente: " + notaVenda.getNomeCliente()
            );
            escreverLinhaInformacao(
                    estado,
                    "CPF/CNPJ: "
                            + notaVenda.getDocumentoCliente()
            );
        }

        estado.y -= 6f;
    }

    private void escreverCabecalhoContinuacao(
            EstadoPagina estado,
            NotaVenda notaVenda,
            TipoViaNotaVendaPdf tipoVia
    ) throws IOException {
        escreverTextoCentralizado(
                estado,
                "Nota de Venda nº "
                        + formatarNumeroNota(
                        notaVenda.getIdNota()
                )
                        + " — continuação",
                fonteNegrito,
                TAMANHO_SUBTITULO
        );
        estado.y -= 16f;

        if (tipoVia == TipoViaNotaVendaPdf.SEGUNDA_VIA) {
            escreverTextoCentralizado(
                    estado,
                    "2ª VIA — REIMPRESSÃO",
                    fonteNegrito,
                    TAMANHO_TEXTO
            );
            estado.y -= 13f;
        }

        if (notaVenda.getStatus()
                == StatusNotaVenda.ESTORNADA) {
            escreverTextoCentralizado(
                    estado,
                    "******** ESTORNADA ********",
                    fonteNegrito,
                    TAMANHO_TEXTO
            );
            estado.y -= 14f;
        }

        estado.y -= 4f;
    }

    private void escreverAvisoEstorno(EstadoPagina estado)
            throws IOException {
        escreverTextoCentralizado(
                estado,
                "******** ESTORNADA ********",
                fonteNegrito,
                TAMANHO_SUBTITULO
        );
        estado.y -= 14f;

        escreverTextoCentralizado(
                estado,
                "DOCUMENTO HISTÓRICO",
                fonteNegrito,
                TAMANHO_TEXTO
        );
        estado.y -= 12f;

        escreverTextoCentralizado(
                estado,
                "SEM VALIDADE COMO COMPROVANTE DE VENDA ATIVA",
                fonteNegrito,
                TAMANHO_TEXTO
        );
        estado.y -= 18f;
    }

    private void escreverLinhaInformacao(
            EstadoPagina estado,
            String texto
    ) throws IOException {
        List<String> linhas = quebrarTexto(
                texto,
                fonteNormal,
                TAMANHO_TEXTO,
                larguraUtil()
        );

        for (String linha : linhas) {
            escreverTexto(
                    estado,
                    MARGEM_ESQUERDA,
                    estado.y,
                    linha,
                    fonteNormal,
                    TAMANHO_TEXTO
            );
            estado.y -= ALTURA_LINHA;
        }
    }

    private void escreverCabecalhoTabela(EstadoPagina estado)
            throws IOException {
        float topo = estado.y;
        float base = topo - ALTURA_CABECALHO_TABELA;

        desenharGradeLinha(
                estado,
                topo,
                base
        );

        String[] titulos = {
                "Produto",
                "Qtd.",
                "P. Unit.",
                "Desc. Prom.",
                "Desc. Glob.",
                "Subtotal"
        };

        float x = MARGEM_ESQUERDA;

        for (int i = 0; i < titulos.length; i++) {
            escreverTexto(
                    estado,
                    x + PADDING_CELULA,
                    topo - 14f,
                    titulos[i],
                    fonteNegrito,
                    TAMANHO_TABELA
            );
            x += LARGURAS_COLUNAS[i];
        }

        estado.y = base;
    }

    private EstadoPagina escreverItem(
            PDDocument documento,
            EstadoPagina estado,
            NotaVenda notaVenda,
            TipoViaNotaVendaPdf tipoVia,
            ItemNotaVenda item
    ) throws IOException {
        List<String> linhasProduto = quebrarTexto(
                item.getDescricaoProduto(),
                fonteNormal,
                TAMANHO_TABELA,
                LARGURAS_COLUNAS[0]
                        - (PADDING_CELULA * 2f)
        );

        if (linhasProduto.isEmpty()) {
            linhasProduto = List.of("");
        }

        int indiceLinha = 0;
        boolean primeiroTrecho = true;

        while (indiceLinha < linhasProduto.size()) {
            int linhasQueCabem = calcularLinhasTabelaQueCabem(
                    estado
            );

            if (linhasQueCabem <= 0) {
                estado = criarPaginaContinuacao(
                        documento,
                        estado,
                        notaVenda,
                        tipoVia,
                        true
                );
                continue;
            }

            int quantidadeLinhas = Math.min(
                    linhasQueCabem,
                    linhasProduto.size() - indiceLinha
            );

            float alturaLinha = Math.max(
                    18f,
                    (quantidadeLinhas
                            * ALTURA_LINHA_TABELA)
                            + (PADDING_CELULA * 2f)
            );

            float topo = estado.y;
            float base = topo - alturaLinha;

            desenharGradeLinha(
                    estado,
                    topo,
                    base
            );

            float yTexto = topo
                    - PADDING_CELULA
                    - TAMANHO_TABELA;

            for (int i = 0; i < quantidadeLinhas; i++) {
                escreverTexto(
                        estado,
                        MARGEM_ESQUERDA
                                + PADDING_CELULA,
                        yTexto,
                        linhasProduto.get(
                                indiceLinha + i
                        ),
                        fonteNormal,
                        TAMANHO_TABELA
                );
                yTexto -= ALTURA_LINHA_TABELA;
            }

            if (primeiroTrecho) {
                escreverValoresItem(
                        estado,
                        item,
                        topo
                );
                primeiroTrecho = false;
            }

            estado.y = base;
            indiceLinha += quantidadeLinhas;
        }

        return estado;
    }

    private int calcularLinhasTabelaQueCabem(
            EstadoPagina estado
    ) {
        float espacoDisponivel =
                estado.y - MARGEM_INFERIOR;

        if (espacoDisponivel < 18f) {
            return 0;
        }

        int linhas = (int) Math.floor(
                (espacoDisponivel
                        - (PADDING_CELULA * 2f))
                        / ALTURA_LINHA_TABELA
        );

        return Math.max(linhas, 1);
    }

    private void escreverValoresItem(
            EstadoPagina estado,
            ItemNotaVenda item,
            float topo
    ) throws IOException {
        String[] valores = {
                String.valueOf(item.getQuantidade()),
                formatarMoeda(item.getPrecoUnitario()),
                formatarMoeda(item.getDescontoPromocional()),
                formatarMoeda(item.getDescontoGlobal()),
                formatarMoeda(item.getSubtotal())
        };

        float x = MARGEM_ESQUERDA
                + LARGURAS_COLUNAS[0];

        for (int i = 0; i < valores.length; i++) {
            String valor = sanitizarTexto(
                    valores[i],
                    fonteNormal
            );

            float tamanhoFonte = calcularTamanhoFonteQueCabe(
                    valor,
                    fonteNormal,
                    TAMANHO_TABELA,
                    5f,
                    LARGURAS_COLUNAS[i + 1]
                            - (PADDING_CELULA * 2f)
            );

            escreverTexto(
                    estado,
                    x + PADDING_CELULA,
                    topo
                            - PADDING_CELULA
                            - tamanhoFonte,
                    valor,
                    fonteNormal,
                    tamanhoFonte
            );

            x += LARGURAS_COLUNAS[i + 1];
        }
    }

    private void desenharGradeLinha(
            EstadoPagina estado,
            float topo,
            float base
    ) throws IOException {
        float xInicial = MARGEM_ESQUERDA;
        float xFinal = MARGEM_ESQUERDA
                + somaLargurasColunas();

        estado.conteudo.moveTo(xInicial, topo);
        estado.conteudo.lineTo(xFinal, topo);
        estado.conteudo.moveTo(xInicial, base);
        estado.conteudo.lineTo(xFinal, base);

        float x = xInicial;
        estado.conteudo.moveTo(x, topo);
        estado.conteudo.lineTo(x, base);

        for (float largura : LARGURAS_COLUNAS) {
            x += largura;
            estado.conteudo.moveTo(x, topo);
            estado.conteudo.lineTo(x, base);
        }

        estado.conteudo.stroke();
    }

    private EstadoPagina garantirEspacoResumo(
            PDDocument documento,
            EstadoPagina estado,
            NotaVenda notaVenda,
            TipoViaNotaVendaPdf tipoVia
    ) throws IOException {
        float alturaNecessaria = calcularAlturaResumo(
                notaVenda
        );

        if (estado.y - alturaNecessaria
                >= MARGEM_INFERIOR) {
            estado.y -= 14f;
            return estado;
        }

        EstadoPagina novoEstado =
                criarPaginaContinuacao(
                        documento,
                        estado,
                        notaVenda,
                        tipoVia,
                        false
                );

        novoEstado.y -= 8f;
        return novoEstado;
    }

    private float calcularAlturaResumo(NotaVenda notaVenda) {
        int linhas = 4;

        if (notaVenda.getFormaPagamento()
                == FormaPagamento.DINHEIRO) {
            linhas += 2;
        }

        if (notaVenda.getTipoVenda()
                == TipoVenda.A_PRAZO) {
            linhas += 2;
        }

        return 28f + (linhas * ALTURA_LINHA);
    }

    private void escreverResumoFinanceiro(
            EstadoPagina estado,
            NotaVenda notaVenda
    ) throws IOException {
        escreverTexto(
                estado,
                MARGEM_ESQUERDA,
                estado.y,
                "Resumo financeiro",
                fonteNegrito,
                TAMANHO_SUBTITULO
        );
        estado.y -= 18f;

        escreverLinhaResumo(
                estado,
                "Tipo de venda: "
                        + formatarTipoVenda(
                        notaVenda.getTipoVenda()
                )
        );
        escreverLinhaResumo(
                estado,
                "Forma de pagamento: "
                        + formatarFormaPagamento(
                        notaVenda.getFormaPagamento()
                )
        );
        escreverLinhaResumo(
                estado,
                "Desconto global: "
                        + formatarMoeda(
                        notaVenda.getValorDescontoGlobal()
                )
        );
        escreverLinhaResumo(
                estado,
                "Valor total: "
                        + formatarMoeda(
                        notaVenda.getValorTotal()
                )
        );

        if (notaVenda.getFormaPagamento()
                == FormaPagamento.DINHEIRO) {
            escreverLinhaResumo(
                    estado,
                    "Valor recebido: "
                            + formatarMoeda(
                            notaVenda.getValorRecebido()
                    )
            );
            escreverLinhaResumo(
                    estado,
                    "Troco: "
                            + formatarMoeda(
                            notaVenda.getTroco()
                    )
            );
        }

        if (notaVenda.getTipoVenda()
                == TipoVenda.A_PRAZO) {
            escreverLinhaResumo(
                    estado,
                    "Prazo: "
                            + notaVenda.getQuantidadeDiasPrazo()
                            + " dias"
            );
            escreverLinhaResumo(
                    estado,
                    "Vencimento: "
                            + notaVenda.getDataVencimento()
                            .format(FORMATADOR_DATA)
            );
        }
    }

    private void escreverLinhaResumo(
            EstadoPagina estado,
            String texto
    ) throws IOException {
        escreverTexto(
                estado,
                MARGEM_ESQUERDA,
                estado.y,
                texto,
                fonteNormal,
                TAMANHO_TEXTO
        );
        estado.y -= ALTURA_LINHA;
    }

    private void escreverTextoCentralizado(
            EstadoPagina estado,
            String texto,
            PDFont fonte,
            float tamanhoFonte
    ) throws IOException {
        String textoSeguro = sanitizarTexto(
                texto,
                fonte
        );

        float largura = medirTexto(
                textoSeguro,
                fonte,
                tamanhoFonte
        );

        float x = estado.pagina
                .getMediaBox()
                .getWidth() / 2f
                - largura / 2f;

        escreverTexto(
                estado,
                x,
                estado.y,
                textoSeguro,
                fonte,
                tamanhoFonte
        );
    }

    private void escreverTexto(
            EstadoPagina estado,
            float x,
            float y,
            String texto,
            PDFont fonte,
            float tamanhoFonte
    ) throws IOException {
        String textoSeguro = sanitizarTexto(
                texto,
                fonte
        );

        estado.conteudo.beginText();
        estado.conteudo.setFont(
                fonte,
                tamanhoFonte
        );
        estado.conteudo.newLineAtOffset(x, y);
        estado.conteudo.showText(textoSeguro);
        estado.conteudo.endText();
    }

    private List<String> quebrarTexto(
            String texto,
            PDFont fonte,
            float tamanhoFonte,
            float larguraMaxima
    ) throws IOException {
        String textoSeguro = sanitizarTexto(
                texto == null ? "" : texto,
                fonte
        ).trim();

        List<String> linhas = new ArrayList<>();

        if (textoSeguro.isEmpty()) {
            linhas.add("");
            return linhas;
        }

        String[] palavras = textoSeguro.split("\\s+");
        StringBuilder linhaAtual = new StringBuilder();

        for (String palavra : palavras) {
            List<String> partes = quebrarPalavraLonga(
                    palavra,
                    fonte,
                    tamanhoFonte,
                    larguraMaxima
            );

            for (String parte : partes) {
                String candidata = linhaAtual.isEmpty()
                        ? parte
                        : linhaAtual + " " + parte;

                if (medirTexto(
                        candidata,
                        fonte,
                        tamanhoFonte
                ) <= larguraMaxima) {
                    linhaAtual.setLength(0);
                    linhaAtual.append(candidata);
                    continue;
                }

                if (!linhaAtual.isEmpty()) {
                    linhas.add(linhaAtual.toString());
                    linhaAtual.setLength(0);
                }

                linhaAtual.append(parte);
            }
        }

        if (!linhaAtual.isEmpty()) {
            linhas.add(linhaAtual.toString());
        }

        return linhas;
    }

    private List<String> quebrarPalavraLonga(
            String palavra,
            PDFont fonte,
            float tamanhoFonte,
            float larguraMaxima
    ) throws IOException {
        if (medirTexto(
                palavra,
                fonte,
                tamanhoFonte
        ) <= larguraMaxima) {
            return List.of(palavra);
        }

        List<String> partes = new ArrayList<>();
        StringBuilder parteAtual = new StringBuilder();

        for (int i = 0; i < palavra.length(); i++) {
            char caractere = palavra.charAt(i);
            String candidata = parteAtual.toString()
                    + caractere;

            if (!parteAtual.isEmpty()
                    && medirTexto(
                    candidata,
                    fonte,
                    tamanhoFonte
            ) > larguraMaxima) {
                partes.add(parteAtual.toString());
                parteAtual.setLength(0);
            }

            parteAtual.append(caractere);
        }

        if (!parteAtual.isEmpty()) {
            partes.add(parteAtual.toString());
        }

        return partes;
    }

    private float calcularTamanhoFonteQueCabe(
            String texto,
            PDFont fonte,
            float tamanhoInicial,
            float tamanhoMinimo,
            float larguraMaxima
    ) throws IOException {
        float tamanho = tamanhoInicial;

        while (tamanho > tamanhoMinimo
                && medirTexto(
                texto,
                fonte,
                tamanho
        ) > larguraMaxima) {
            tamanho -= 0.25f;
        }

        return Math.max(tamanho, tamanhoMinimo);
    }

    private String sanitizarTexto(
            String texto,
            PDFont fonte
    ) throws IOException {
        if (texto == null) {
            return "";
        }

        StringBuilder resultado = new StringBuilder();

        for (int i = 0; i < texto.length(); i++) {
            char caractere = texto.charAt(i);

            if (Character.isISOControl(caractere)) {
                resultado.append(' ');
                continue;
            }

            String candidato = String.valueOf(caractere);

            try {
                fonte.encode(candidato);
                resultado.append(caractere);
            } catch (IllegalArgumentException e) {
                resultado.append('?');
            }
        }

        return resultado.toString();
    }

    private float medirTexto(
            String texto,
            PDFont fonte,
            float tamanhoFonte
    ) throws IOException {
        return fonte.getStringWidth(texto)
                / 1000f
                * tamanhoFonte;
    }

    private float somaLargurasColunas() {
        float soma = 0f;

        for (float largura : LARGURAS_COLUNAS) {
            soma += largura;
        }

        return soma;
    }

    private float larguraUtil() {
        return PDRectangle.A4.getWidth()
                - MARGEM_ESQUERDA
                - MARGEM_DIREITA;
    }

    private String formatarNumeroNota(Integer notaId) {
        return String.format("%06d", notaId);
    }

    private String formatarMoeda(BigDecimal valor) {
        if (valor == null) {
            return "R$ 0,00";
        }

        String texto = String.format(
                new Locale("pt", "BR"),
                "R$ %,.2f",
                valor
        );

        return texto.replace('\u00A0', ' ');
    }

    private String formatarTipoVenda(TipoVenda tipoVenda) {
        if (tipoVenda == TipoVenda.A_PRAZO) {
            return "A prazo";
        }

        return "À vista";
    }

    private String formatarFormaPagamento(
            FormaPagamento formaPagamento
    ) {
        if (formaPagamento == FormaPagamento.DINHEIRO) {
            return "Dinheiro";
        }

        if (formaPagamento == FormaPagamento.PIX) {
            return "PIX";
        }

        if (formaPagamento == FormaPagamento.CARTAO) {
            return "Cartão";
        }

        if (formaPagamento == FormaPagamento.A_PRAZO) {
            return "A prazo";
        }

        return "-";
    }

    private static class EstadoPagina {
        private final PDPage pagina;
        private final PDPageContentStream conteudo;
        private float y;
        private boolean fechado;

        private EstadoPagina(
                PDPage pagina,
                PDPageContentStream conteudo,
                float y
        ) {
            this.pagina = pagina;
            this.conteudo = conteudo;
            this.y = y;
        }

        private void fechar() throws IOException {
            if (fechado) {
                return;
            }

            conteudo.close();
            fechado = true;
        }
    }
}
