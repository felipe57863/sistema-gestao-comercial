package br.com.luis.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa uma entrada de estoque e seus itens.
 */
public class EntradaEstoque {

    private Integer idEntrada;
    private LocalDateTime dataHora;
    private Integer usuarioId;
    private String nomeUsuario;
    private String referencia;
    private String observacao;
    private List<ItemEntradaEstoque> itens;

    public EntradaEstoque() {
        this.itens = new ArrayList<>();
    }

    public EntradaEstoque(Integer idEntrada, LocalDateTime dataHora, Integer usuarioId,
                          String nomeUsuario, String referencia, String observacao,
                          List<ItemEntradaEstoque> itens) {
        setIdEntrada(idEntrada);
        setDataHora(dataHora);
        setUsuarioId(usuarioId);
        setNomeUsuario(nomeUsuario);
        setReferencia(referencia);
        setObservacao(observacao);
        setItens(itens);
    }

    public Integer getIdEntrada() {
        return idEntrada;
    }

    public void setIdEntrada(Integer idEntrada) {
        if (idEntrada != null && idEntrada <= 0) {
            throw new IllegalArgumentException("ID da entrada deve ser positivo.");
        }
        this.idEntrada = idEntrada;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        if (usuarioId != null && usuarioId <= 0) {
            throw new IllegalArgumentException("ID do usuário deve ser positivo.");
        }
        this.usuarioId = usuarioId;
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }

    public void setNomeUsuario(String nomeUsuario) {
        if (nomeUsuario != null && nomeUsuario.isBlank()) {
            throw new IllegalArgumentException("Nome do usuário não pode ser vazio.");
        }
        this.nomeUsuario = nomeUsuario != null ? nomeUsuario.trim() : null;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = normalizarTextoOpcional(referencia);
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = normalizarTextoOpcional(observacao);
    }

    public List<ItemEntradaEstoque> getItens() {
        return new ArrayList<>(itens);
    }

    public void setItens(List<ItemEntradaEstoque> itens) {
        this.itens = itens != null ? new ArrayList<>(itens) : new ArrayList<>();
    }

    private String normalizarTextoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
