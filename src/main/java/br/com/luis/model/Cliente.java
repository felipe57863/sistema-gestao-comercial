package br.com.luis.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Entidade que representa um Cliente no sistema.
 */
public class Cliente {

    /**
     * Enum para diferenciar Pessoa Física e Jurídica.
     */
    public enum TipoCliente {
        PF("Pessoa Física"), PJ("Pessoa Jurídica");

        private final String descricao;

        TipoCliente(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    /**
     * Enum para controle de status do cliente.
     */
    public enum StatusCliente {
        ATIVO, BLOQUEADO
    }

    private Integer idCliente;
    private String nome;
    private String documento;
    private String telefone;
    private String email;
    private TipoCliente tipo;
    private BigDecimal limiteCredito;
    private StatusCliente status;
    private PrazoPagamento prazoPagamento;

    // Construtor vazio para uso no DAO
    public Cliente() {
    }

    /**
     * Construtor completo com validação.
     */
    public Cliente(Integer idCliente, String nome, String documento, TipoCliente tipo,
                   BigDecimal limiteCredito, StatusCliente status, PrazoPagamento prazoPagamento) {
        this(idCliente, nome, documento, null, null, tipo, limiteCredito, status, prazoPagamento);
    }

    /**
     * Construtor completo com telefone e e-mail opcionais.
     */
    public Cliente(Integer idCliente, String nome, String documento, String telefone, String email,
                   TipoCliente tipo, BigDecimal limiteCredito, StatusCliente status,
                   PrazoPagamento prazoPagamento) {
        setIdCliente(idCliente);
        setNome(nome);
        setDocumento(documento);
        setTelefone(telefone);
        setEmail(email);
        setTipo(tipo);
        setLimiteCredito(limiteCredito);
        setStatus(status);
        setPrazoPagamento(prazoPagamento);
    }

    public Integer getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Integer idCliente) {
        if (idCliente != null && idCliente <= 0) {
            throw new IllegalArgumentException("ID do cliente deve ser positivo.");
        }
        this.idCliente = idCliente;
    }

    public String getNome() {
        return nome;
    }

    /**
     * Valida nome do cliente.
     */
    public void setNome(String nome) {
        if (nome == null || nome.isBlank() || nome.trim().length() < 3) {
            throw new IllegalArgumentException("Nome deve ter pelo menos 3 caracteres.");
        }
        this.nome = nome.trim();
    }

    public String getDocumento() {
        return documento;
    }

    /**
     * Remove pontuação e padroniza o documento (CPF/CNPJ).
     */
    public void setDocumento(String documento) {
        if (documento == null || documento.isBlank()) {
            throw new IllegalArgumentException("Documento é obrigatório.");
        }

        // Remove tudo que não for número
        String documentoLimpo = documento.replaceAll("[^0-9]", "");

        if (documentoLimpo.isBlank()) {
            throw new IllegalArgumentException("Documento deve conter apenas números válidos.");
        }

        this.documento = documentoLimpo;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            this.telefone = null;
            return;
        }

        String telefoneLimpo = telefone.replaceAll("[^0-9]", "");

        if (telefoneLimpo.length() != 10 && telefoneLimpo.length() != 11) {
            throw new IllegalArgumentException("Telefone deve conter exatamente 10 ou 11 dígitos.");
        }

        this.telefone = telefoneLimpo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || email.isBlank()) {
            this.email = null;
            return;
        }

        String emailNormalizado = email.trim();

        if (!emailNormalizado.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("E-mail deve possuir um formato válido.");
        }

        this.email = emailNormalizado;
    }

    public TipoCliente getTipo() {
        return tipo;
    }

    public void setTipo(TipoCliente tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo de cliente é obrigatório.");
        }
        this.tipo = tipo;
    }

    public BigDecimal getLimiteCredito() {
        return limiteCredito;
    }

    /**
     * Regra financeira: valor não pode ser negativo e deve ter 2 casas decimais.
     */
    public void setLimiteCredito(BigDecimal limiteCredito) {
        if (limiteCredito == null || limiteCredito.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Limite de crédito não pode ser negativo.");
        }

        this.limiteCredito = limiteCredito.setScale(2, RoundingMode.HALF_UP);
    }

    public StatusCliente getStatus() {
        return status;
    }

    public void setStatus(StatusCliente status) {
        if (status == null) {
            throw new IllegalArgumentException("Status do cliente é obrigatório.");
        }
        this.status = status;
    }

    public PrazoPagamento getPrazoPagamento() {
        return prazoPagamento;
    }

    /**
     * Regra: cliente deve ter um prazo de pagamento vinculado.
     */
    public void setPrazoPagamento(PrazoPagamento prazoPagamento) {
        if (prazoPagamento == null) {
            throw new IllegalArgumentException("O cliente deve possuir um prazo de pagamento.");
        }
        this.prazoPagamento = prazoPagamento;
    }

    /**
     * Exibição amigável para ComboBox e listas.
     */
    @Override
    public String toString() {
        if (nome == null || documento == null) {
            return "Cliente não definido";
        }

        return nome + " - " + documento;
    }
}
