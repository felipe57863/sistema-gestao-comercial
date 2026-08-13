package br.com.luis.viewmodel;

import br.com.luis.model.TipoVenda;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Representa uma linha final do relatório de descontos concedidos.
 *
 * Os valores já foram validados e consolidados pelo Service. A classe apenas
 * preserva a fotografia imutável exibida pela interface.
 */
public final class VendaDescontoRelatorioView {

    private static final int ESCALA_MONETARIA = 2;
    private static final RoundingMode ARREDONDAMENTO_MONETARIO =
            RoundingMode.HALF_UP;

    private final Integer vendaId;
    private final LocalDateTime dataHora;
    private final String cliente;
    private final TipoVenda tipoVenda;
    private final BigDecimal valorBruto;
    private final BigDecimal descontoPromocional;
    private final BigDecimal descontoGlobal;
    private final BigDecimal descontoTotal;
    private final BigDecimal valorLiquido;

    /**
     * Cria uma linha imutável correspondente às nove colunas do relatório.
     */
    public VendaDescontoRelatorioView(
            Integer vendaId,
            LocalDateTime dataHora,
            String cliente,
            TipoVenda tipoVenda,
            BigDecimal valorBruto,
            BigDecimal descontoPromocional,
            BigDecimal descontoGlobal,
            BigDecimal descontoTotal,
            BigDecimal valorLiquido
    ) {
        if (vendaId == null || vendaId <= 0) {
            throw new IllegalArgumentException(
                    "ID da venda deve ser maior que zero."
            );
        }

        if (dataHora == null) {
            throw new IllegalArgumentException(
                    "Data e hora da venda são obrigatórias."
            );
        }

        if (cliente == null || cliente.isBlank()) {
            throw new IllegalArgumentException(
                    "Identificação do cliente é obrigatória."
            );
        }

        if (tipoVenda == null) {
            throw new IllegalArgumentException(
                    "Tipo da venda é obrigatório."
            );
        }

        validarValorObrigatorio(valorBruto, "Valor bruto");
        validarValorObrigatorio(
                descontoPromocional,
                "Desconto promocional"
        );
        validarValorObrigatorio(descontoGlobal, "Desconto global");
        validarValorObrigatorio(descontoTotal, "Desconto total");
        validarValorObrigatorio(valorLiquido, "Valor líquido");

        BigDecimal brutoNormalizado = normalizarValorMonetario(valorBruto);
        BigDecimal promocionalNormalizado =
                normalizarValorMonetario(descontoPromocional);
        BigDecimal globalNormalizado = normalizarValorMonetario(descontoGlobal);
        BigDecimal totalNormalizado = normalizarValorMonetario(descontoTotal);
        BigDecimal liquidoNormalizado = normalizarValorMonetario(valorLiquido);

        if (brutoNormalizado.signum() < 0
                || promocionalNormalizado.signum() < 0
                || globalNormalizado.signum() < 0
                || liquidoNormalizado.signum() < 0) {

            throw new IllegalArgumentException(
                    "Valores monetários da venda não podem ser negativos."
            );
        }

        if (totalNormalizado.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Desconto total da venda deve ser maior que zero."
            );
        }

        BigDecimal totalCalculado = normalizarValorMonetario(
                promocionalNormalizado.add(globalNormalizado)
        );

        if (totalCalculado.compareTo(totalNormalizado) != 0) {
            throw new IllegalArgumentException(
                    "Desconto total não corresponde às origens da venda."
            );
        }

        BigDecimal liquidoCalculado = normalizarValorMonetario(
                brutoNormalizado.subtract(totalNormalizado)
        );

        if (liquidoCalculado.compareTo(liquidoNormalizado) != 0) {
            throw new IllegalArgumentException(
                    "Valor líquido não corresponde ao bruto menos os descontos."
            );
        }

        this.vendaId = vendaId;
        this.dataHora = dataHora;
        this.cliente = cliente.trim();
        this.tipoVenda = tipoVenda;
        this.valorBruto = brutoNormalizado;
        this.descontoPromocional = promocionalNormalizado;
        this.descontoGlobal = globalNormalizado;
        this.descontoTotal = totalNormalizado;
        this.valorLiquido = liquidoNormalizado;
    }

    private static void validarValorObrigatorio(
            BigDecimal valor,
            String nomeCampo
    ) {
        if (valor == null) {
            throw new IllegalArgumentException(nomeCampo + " é obrigatório.");
        }
    }

    private static BigDecimal normalizarValorMonetario(BigDecimal valor) {
        return valor.setScale(
                ESCALA_MONETARIA,
                ARREDONDAMENTO_MONETARIO
        );
    }

    public Integer getVendaId() {
        return vendaId;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getCliente() {
        return cliente;
    }

    public TipoVenda getTipoVenda() {
        return tipoVenda;
    }

    public BigDecimal getValorBruto() {
        return valorBruto;
    }

    public BigDecimal getDescontoPromocional() {
        return descontoPromocional;
    }

    public BigDecimal getDescontoGlobal() {
        return descontoGlobal;
    }

    public BigDecimal getDescontoTotal() {
        return descontoTotal;
    }

    public BigDecimal getValorLiquido() {
        return valorLiquido;
    }
}
