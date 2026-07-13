package br.com.luis.viewmodel;

import br.com.luis.model.StatusContaReceber;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * ViewModel usada para representar uma linha da futura TableView
 * de contas a receber pendentes.
 *
 * Esta classe não representa uma entidade persistida no banco de dados.
 * Ela não acessa DAO, Service, SQL ou qualquer recurso de persistência.
 *
 * Responsabilidades:
 * - transportar dados brutos da conta a receber para a interface;
 * - manter dados combinados de ContaReceber e Cliente para exibição;
 * - informar se a conta deve ser tratada visualmente como vencida.
 *
 * Não realiza:
 * - formatação de moeda;
 * - formatação de data;
 * - definição de cores;
 * - definição de estilos;
 * - cálculo de vencimento.
 */
public class ContaReceberListagemView {

    private Integer contaReceberId;
    private Integer clienteId;
    private String nomeCliente;
    private Integer vendaId;
    private BigDecimal valor;
    private LocalDate dataVencimento;
    private StatusContaReceber status;
    private boolean vencida;

    /**
     * Construtor padrão.
     *
     * Define valores iniciais seguros para criação gradual do objeto.
     */
    public ContaReceberListagemView() {
        this.valor = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.vencida = false;
    }

    /**
     * Construtor completo.
     *
     * @param contaReceberId ID da conta a receber.
     * @param clienteId ID do cliente devedor.
     * @param nomeCliente nome do cliente devedor.
     * @param vendaId ID da venda que gerou a conta.
     * @param valor valor bruto da conta.
     * @param dataVencimento data de vencimento da conta.
     * @param status status atual da conta.
     * @param vencida indicação visual de conta vencida.
     */
    public ContaReceberListagemView(
            Integer contaReceberId,
            Integer clienteId,
            String nomeCliente,
            Integer vendaId,
            BigDecimal valor,
            LocalDate dataVencimento,
            StatusContaReceber status,
            boolean vencida
    ) {
        this.contaReceberId = contaReceberId;
        this.clienteId = clienteId;
        this.nomeCliente = nomeCliente;
        this.vendaId = vendaId;
        setValor(valor);
        this.dataVencimento = dataVencimento;
        this.status = status;
        this.vencida = vencida;
    }

    public Integer getContaReceberId() {
        return contaReceberId;
    }

    public void setContaReceberId(Integer contaReceberId) {
        this.contaReceberId = contaReceberId;
    }

    public Integer getClienteId() {
        return clienteId;
    }

    public void setClienteId(Integer clienteId) {
        this.clienteId = clienteId;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public void setNomeCliente(String nomeCliente) {
        this.nomeCliente = nomeCliente;
    }

    public Integer getVendaId() {
        return vendaId;
    }

    public void setVendaId(Integer vendaId) {
        this.vendaId = vendaId;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        if (valor == null) {
            this.valor = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return;
        }

        this.valor = valor.setScale(2, RoundingMode.HALF_UP);
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }

    public StatusContaReceber getStatus() {
        return status;
    }

    public void setStatus(StatusContaReceber status) {
        this.status = status;
    }

    public boolean isVencida() {
        return vencida;
    }

    public void setVencida(boolean vencida) {
        this.vencida = vencida;
    }

    @Override
    public String toString() {
        return "ContaReceberListagemView{" +
                "contaReceberId=" + contaReceberId +
                ", clienteId=" + clienteId +
                ", nomeCliente='" + nomeCliente + '\'' +
                ", vendaId=" + vendaId +
                ", valor=" + valor +
                ", dataVencimento=" + dataVencimento +
                ", status=" + status +
                ", vencida=" + vencida +
                '}';
    }
}