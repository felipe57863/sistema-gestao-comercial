package br.com.luis.viewmodel;

import br.com.luis.model.StatusVenda;
import br.com.luis.model.TipoVenda;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Projeção técnica dos dados históricos agregados de uma venda.
 *
 * Transporta do DAO para o Service os valores congelados em Venda e ItemVenda.
 * Não calcula descontos, não consulta entidades atuais e não possui dependência
 * de JavaFX.
 */
public final class VendaDescontoRelatorioDados {

    private static final int ESCALA_MONETARIA = 2;
    private static final RoundingMode ARREDONDAMENTO_MONETARIO =
            RoundingMode.HALF_UP;

    private final Integer vendaId;
    private final LocalDateTime dataHora;
    private final TipoVenda tipoVenda;
    private final StatusVenda statusVenda;
    private final String clienteNome;
    private final BigDecimal valorTotalVenda;
    private final BigDecimal valorDescontoGlobalVenda;
    private final BigDecimal valorBrutoItens;
    private final BigDecimal descontoPromocionalItens;
    private final BigDecimal descontoGlobalItens;
    private final BigDecimal valorLiquidoItens;

    /**
     * Cria a projeção imutável com os valores retornados pela consulta agregada.
     */
    public VendaDescontoRelatorioDados(
            Integer vendaId,
            LocalDateTime dataHora,
            TipoVenda tipoVenda,
            StatusVenda statusVenda,
            String clienteNome,
            BigDecimal valorTotalVenda,
            BigDecimal valorDescontoGlobalVenda,
            BigDecimal valorBrutoItens,
            BigDecimal descontoPromocionalItens,
            BigDecimal descontoGlobalItens,
            BigDecimal valorLiquidoItens
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

        if (tipoVenda == null) {
            throw new IllegalArgumentException(
                    "Tipo da venda é obrigatório."
            );
        }

        if (statusVenda == null) {
            throw new IllegalArgumentException(
                    "Status da venda é obrigatório."
            );
        }

        validarValorObrigatorio(valorTotalVenda, "Valor total da venda");
        validarValorObrigatorio(
                valorDescontoGlobalVenda,
                "Desconto global consolidado da venda"
        );
        validarValorObrigatorio(valorBrutoItens, "Valor bruto dos itens");
        validarValorObrigatorio(
                descontoPromocionalItens,
                "Desconto promocional dos itens"
        );
        validarValorObrigatorio(
                descontoGlobalItens,
                "Desconto global dos itens"
        );
        validarValorObrigatorio(valorLiquidoItens, "Valor líquido dos itens");

        this.vendaId = vendaId;
        this.dataHora = dataHora;
        this.tipoVenda = tipoVenda;
        this.statusVenda = statusVenda;
        this.clienteNome = normalizarClienteNome(clienteNome);
        this.valorTotalVenda = normalizarValorMonetario(valorTotalVenda);
        this.valorDescontoGlobalVenda =
                normalizarValorMonetario(valorDescontoGlobalVenda);
        this.valorBrutoItens = normalizarValorMonetario(valorBrutoItens);
        this.descontoPromocionalItens =
                normalizarValorMonetario(descontoPromocionalItens);
        this.descontoGlobalItens =
                normalizarValorMonetario(descontoGlobalItens);
        this.valorLiquidoItens = normalizarValorMonetario(valorLiquidoItens);
    }

    private static void validarValorObrigatorio(
            BigDecimal valor,
            String nomeCampo
    ) {
        if (valor == null) {
            throw new IllegalArgumentException(nomeCampo + " é obrigatório.");
        }
    }

    private static String normalizarClienteNome(String clienteNome) {
        if (clienteNome == null || clienteNome.isBlank()) {
            return null;
        }

        return clienteNome.trim();
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

    public TipoVenda getTipoVenda() {
        return tipoVenda;
    }

    public StatusVenda getStatusVenda() {
        return statusVenda;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public BigDecimal getValorTotalVenda() {
        return valorTotalVenda;
    }

    public BigDecimal getValorDescontoGlobalVenda() {
        return valorDescontoGlobalVenda;
    }

    public BigDecimal getValorBrutoItens() {
        return valorBrutoItens;
    }

    public BigDecimal getDescontoPromocionalItens() {
        return descontoPromocionalItens;
    }

    public BigDecimal getDescontoGlobalItens() {
        return descontoGlobalItens;
    }

    public BigDecimal getValorLiquidoItens() {
        return valorLiquidoItens;
    }
}
