package br.com.luis.model;

/**
 * Entidade que representa as condições de pagamento a prazo.
 * Exemplo: "30 Dias", "15 e 30 Dias", "À Vista".
 */
public class PrazoPagamento {

    private Integer idPrazo;
    private String descricao;
    private Integer quantidadeDias;
    private boolean ativo; // No banco: 1 (true) / 0 (false)

    // Construtor vazio para uso em DAO
    public PrazoPagamento() {
    }

    // Construtor completo com validação
    public PrazoPagamento(Integer idPrazo, String descricao, Integer quantidadeDias, boolean ativo) {
        this.idPrazo = idPrazo;
        setDescricao(descricao);
        setQuantidadeDias(quantidadeDias);
        setAtivo(ativo);
    }

    public Integer getIdPrazo() {
        return idPrazo;
    }

    public void setIdPrazo(Integer idPrazo) {
        this.idPrazo = idPrazo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("Descrição do prazo é obrigatória.");
        }
        this.descricao = descricao.trim();
    }

    public Integer getQuantidadeDias() {
        return quantidadeDias;
    }

    public void setQuantidadeDias(Integer quantidadeDias) {
        if (quantidadeDias == null || quantidadeDias < 0) {
            throw new IllegalArgumentException("Quantidade de dias deve ser zero ou positiva.");
        }
        this.quantidadeDias = quantidadeDias;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    /**
     * Exibição amigável no JavaFX (ComboBox).
     */
    @Override
    public String toString() {
        if (quantidadeDias == 0) {
            return descricao + " (À vista)";
        }
        return descricao + " (" + quantidadeDias + " dias)";
    }
}