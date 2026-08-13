package br.com.luis.viewmodel;

import br.com.luis.model.Cliente.StatusCliente;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Representa uma linha consolidada do relatório de clientes com pendências.
 *
 * Cada instância corresponde a um cliente com pelo menos uma conta pendente.
 * As quantidades e os valores já devem ter sido agregados pela consulta. Esta
 * classe não determina vencimento e não acessa Model completo, DAO, Service,
 * sessão, JavaFX ou mecanismos de persistência.
 */
public final class ClientePendenciaRelatorioView {

    private static final int ESCALA_MONETARIA = 2;

    private static final RoundingMode ARREDONDAMENTO_MONETARIO =
            RoundingMode.HALF_UP;

    private final Integer clienteId;
    private final String nome;
    private final String documento;
    private final StatusCliente statusCliente;
    private final int quantidadeContasPendentes;
    private final BigDecimal valorPendente;
    private final int quantidadeContasVencidas;
    private final BigDecimal valorVencido;

    /**
     * Cria uma linha imutável já agregada por cliente.
     *
     * @throws IllegalArgumentException quando algum dado estrutural for inválido.
     */
    public ClientePendenciaRelatorioView(
            Integer clienteId,
            String nome,
            String documento,
            StatusCliente statusCliente,
            int quantidadeContasPendentes,
            BigDecimal valorPendente,
            int quantidadeContasVencidas,
            BigDecimal valorVencido
    ) {
        if (clienteId == null || clienteId <= 0) {
            throw new IllegalArgumentException(
                    "ID do cliente deve ser maior que zero."
            );
        }

        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException(
                    "Nome do cliente é obrigatório."
            );
        }

        if (documento == null || documento.isBlank()) {
            throw new IllegalArgumentException(
                    "Documento do cliente é obrigatório."
            );
        }

        if (statusCliente == null) {
            throw new IllegalArgumentException(
                    "Status do cliente é obrigatório."
            );
        }

        if (quantidadeContasPendentes <= 0) {
            throw new IllegalArgumentException(
                    "Quantidade de contas pendentes deve ser maior que zero."
            );
        }

        if (quantidadeContasVencidas < 0) {
            throw new IllegalArgumentException(
                    "Quantidade de contas vencidas não pode ser negativa."
            );
        }

        if (quantidadeContasVencidas > quantidadeContasPendentes) {
            throw new IllegalArgumentException(
                    "Quantidade de contas vencidas não pode superar as pendentes."
            );
        }

        if (valorPendente == null) {
            throw new IllegalArgumentException(
                    "Valor pendente é obrigatório."
            );
        }

        if (valorVencido == null) {
            throw new IllegalArgumentException(
                    "Valor vencido é obrigatório."
            );
        }

        BigDecimal valorPendenteNormalizado =
                normalizarValorMonetario(valorPendente);

        BigDecimal valorVencidoNormalizado =
                normalizarValorMonetario(valorVencido);

        if (valorPendenteNormalizado.signum() < 0) {
            throw new IllegalArgumentException(
                    "Valor pendente não pode ser negativo."
            );
        }

        if (valorVencidoNormalizado.signum() < 0) {
            throw new IllegalArgumentException(
                    "Valor vencido não pode ser negativo."
            );
        }

        if (valorVencidoNormalizado.compareTo(valorPendenteNormalizado) > 0) {
            throw new IllegalArgumentException(
                    "Valor vencido não pode ser maior que o valor pendente."
            );
        }

        this.clienteId = clienteId;
        this.nome = nome.trim();
        this.documento = documento.trim();
        this.statusCliente = statusCliente;
        this.quantidadeContasPendentes = quantidadeContasPendentes;
        this.valorPendente = valorPendenteNormalizado;
        this.quantidadeContasVencidas = quantidadeContasVencidas;
        this.valorVencido = valorVencidoNormalizado;
    }

    private static BigDecimal normalizarValorMonetario(BigDecimal valor) {
        return valor.setScale(
                ESCALA_MONETARIA,
                ARREDONDAMENTO_MONETARIO
        );
    }

    public Integer getClienteId() {
        return clienteId;
    }

    public String getNome() {
        return nome;
    }

    public String getDocumento() {
        return documento;
    }

    public StatusCliente getStatusCliente() {
        return statusCliente;
    }

    public int getQuantidadeContasPendentes() {
        return quantidadeContasPendentes;
    }

    public BigDecimal getValorPendente() {
        return valorPendente;
    }

    public int getQuantidadeContasVencidas() {
        return quantidadeContasVencidas;
    }

    public BigDecimal getValorVencido() {
        return valorVencido;
    }
}
