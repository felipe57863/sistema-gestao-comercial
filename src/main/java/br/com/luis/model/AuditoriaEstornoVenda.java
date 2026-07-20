package br.com.luis.model;

import java.time.LocalDateTime;

/**
 * Entidade que representa o registro persistente de um estorno de venda.
 *
 * Preserva os dados necessários para rastrear quem realizou o estorno,
 * quando ele ocorreu, o motivo informado, os estados anteriores e os
 * vínculos financeiros envolvidos.
 *
 * O registro é imutável no fluxo normal. Não deve ser atualizado ou excluído
 * depois de persistido.
 */
public class AuditoriaEstornoVenda {

    private final Integer idAuditoria;
    private final Integer vendaId;
    private final Integer usuarioId;
    private final LocalDateTime dataHora;
    private final String motivo;
    private final StatusVenda statusVendaAnterior;
    private final Integer contaReceberId;
    private final StatusContaReceber statusContaReceberAnterior;
    private final Integer movimentacaoOriginalId;
    private final Integer movimentacaoSaidaId;

    /**
     * Cria um registro de auditoria de estorno.
     *
     * O identificador da auditoria pode ser {@code null} antes da persistência.
     *
     * Os vínculos opcionais devem respeitar os três cenários oficiais:
     *
     * - venda à vista paga: sem conta e com movimentações;
     * - venda a prazo pendente: conta PENDENTE e sem movimentações;
     * - venda a prazo paga: conta PAGA e com movimentações.
     *
     * @param idAuditoria identificador persistido da auditoria ou {@code null}
     *                    para um novo registro.
     * @param vendaId identificador da venda estornada.
     * @param usuarioId identificador do usuário responsável pelo estorno.
     * @param dataHora data e hora do estorno.
     * @param motivo motivo obrigatório do estorno.
     * @param statusVendaAnterior status da venda antes do estorno.
     * @param contaReceberId identificador da conta vinculada, quando aplicável.
     * @param statusContaReceberAnterior status anterior da conta, quando aplicável.
     * @param movimentacaoOriginalId identificador da entrada original,
     *                               quando aplicável.
     * @param movimentacaoSaidaId identificador da saída de estorno,
     *                            quando aplicável.
     */
    public AuditoriaEstornoVenda(
            Integer idAuditoria,
            Integer vendaId,
            Integer usuarioId,
            LocalDateTime dataHora,
            String motivo,
            StatusVenda statusVendaAnterior,
            Integer contaReceberId,
            StatusContaReceber statusContaReceberAnterior,
            Integer movimentacaoOriginalId,
            Integer movimentacaoSaidaId
    ) {
        validarIdOpcional(idAuditoria, "ID da auditoria");
        validarIdObrigatorio(vendaId, "ID da venda");
        validarIdObrigatorio(usuarioId, "ID do usuário");
        validarDataHora(dataHora);
        validarMotivo(motivo);
        validarStatusVendaAnterior(statusVendaAnterior);

        validarConta(
                contaReceberId,
                statusContaReceberAnterior
        );

        validarMovimentacoes(
                movimentacaoOriginalId,
                movimentacaoSaidaId
        );

        validarCenarioInterno(
                statusVendaAnterior,
                contaReceberId,
                statusContaReceberAnterior,
                movimentacaoOriginalId
        );

        this.idAuditoria = idAuditoria;
        this.vendaId = vendaId;
        this.usuarioId = usuarioId;
        this.dataHora = dataHora;
        this.motivo = motivo.trim();
        this.statusVendaAnterior = statusVendaAnterior;
        this.contaReceberId = contaReceberId;
        this.statusContaReceberAnterior = statusContaReceberAnterior;
        this.movimentacaoOriginalId = movimentacaoOriginalId;
        this.movimentacaoSaidaId = movimentacaoSaidaId;
    }

    private static void validarIdObrigatorio(
            Integer id,
            String nomeCampo
    ) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    nomeCampo + " deve ser maior que zero."
            );
        }
    }

    private static void validarIdOpcional(
            Integer id,
            String nomeCampo
    ) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException(
                    nomeCampo + " deve ser maior que zero."
            );
        }
    }

    private static void validarDataHora(LocalDateTime dataHora) {
        if (dataHora == null) {
            throw new IllegalArgumentException(
                    "Data e hora do estorno são obrigatórias."
            );
        }
    }

    private static void validarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException(
                    "Motivo do estorno é obrigatório."
            );
        }

        if (motivo.trim().length() > 500) {
            throw new IllegalArgumentException(
                    "Motivo do estorno deve possuir no máximo 500 caracteres."
            );
        }
    }

    private static void validarStatusVendaAnterior(
            StatusVenda statusVendaAnterior
    ) {
        if (statusVendaAnterior != StatusVenda.PAGA
                && statusVendaAnterior != StatusVenda.PENDENTE) {
            throw new IllegalArgumentException(
                    "Status anterior da venda deve ser PAGA ou PENDENTE."
            );
        }
    }

    private static void validarConta(
            Integer contaReceberId,
            StatusContaReceber statusContaReceberAnterior
    ) {
        boolean contaInformada = contaReceberId != null;
        boolean statusInformado = statusContaReceberAnterior != null;

        if (contaInformada != statusInformado) {
            throw new IllegalArgumentException(
                    "Conta a receber e status anterior da conta devem ser "
                            + "informados conjuntamente."
            );
        }

        validarIdOpcional(
                contaReceberId,
                "ID da conta a receber"
        );

        if (statusContaReceberAnterior != null
                && statusContaReceberAnterior != StatusContaReceber.PAGA
                && statusContaReceberAnterior != StatusContaReceber.PENDENTE) {
            throw new IllegalArgumentException(
                    "Status anterior da conta deve ser PAGA ou PENDENTE."
            );
        }
    }

    private static void validarMovimentacoes(
            Integer movimentacaoOriginalId,
            Integer movimentacaoSaidaId
    ) {
        boolean originalInformada = movimentacaoOriginalId != null;
        boolean saidaInformada = movimentacaoSaidaId != null;

        if (originalInformada != saidaInformada) {
            throw new IllegalArgumentException(
                    "Movimentação original e movimentação de saída devem ser "
                            + "informadas conjuntamente."
            );
        }

        validarIdOpcional(
                movimentacaoOriginalId,
                "ID da movimentação original"
        );

        validarIdOpcional(
                movimentacaoSaidaId,
                "ID da movimentação de saída"
        );

        if (movimentacaoOriginalId != null
                && movimentacaoOriginalId.equals(movimentacaoSaidaId)) {
            throw new IllegalArgumentException(
                    "Movimentação original e movimentação de saída devem ser diferentes."
            );
        }
    }

    /**
     * Valida a coerência entre os estados anteriores e os vínculos opcionais.
     *
     * Esta validação considera somente os campos internos da auditoria.
     * A verificação dos registros reais e das características das movimentações
     * financeiras pertence ao EstornoVendaService.
     */
    private static void validarCenarioInterno(
            StatusVenda statusVendaAnterior,
            Integer contaReceberId,
            StatusContaReceber statusContaReceberAnterior,
            Integer movimentacaoOriginalId
    ) {
        boolean contaInformada = contaReceberId != null;
        boolean movimentacoesInformadas = movimentacaoOriginalId != null;

        if (!contaInformada) {
            if (statusVendaAnterior != StatusVenda.PAGA
                    || !movimentacoesInformadas) {
                throw new IllegalArgumentException(
                        "Venda sem conta a receber deve estar PAGA e possuir "
                                + "movimentações financeiras de estorno."
                );
            }

            return;
        }

        if (statusContaReceberAnterior == StatusContaReceber.PENDENTE) {
            if (statusVendaAnterior != StatusVenda.PENDENTE
                    || movimentacoesInformadas) {
                throw new IllegalArgumentException(
                        "Conta anteriormente PENDENTE exige venda PENDENTE "
                                + "e ausência de movimentações financeiras."
                );
            }

            return;
        }

        if (statusContaReceberAnterior == StatusContaReceber.PAGA) {
            if (statusVendaAnterior != StatusVenda.PAGA
                    || !movimentacoesInformadas) {
                throw new IllegalArgumentException(
                        "Conta anteriormente PAGA exige venda PAGA e "
                                + "movimentações financeiras de estorno."
                );
            }

            return;
        }

        throw new IllegalArgumentException(
                "Combinação inválida para o cenário de auditoria do estorno."
        );
    }

    public Integer getIdAuditoria() {
        return idAuditoria;
    }

    public Integer getVendaId() {
        return vendaId;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getMotivo() {
        return motivo;
    }

    public StatusVenda getStatusVendaAnterior() {
        return statusVendaAnterior;
    }

    public Integer getContaReceberId() {
        return contaReceberId;
    }

    public StatusContaReceber getStatusContaReceberAnterior() {
        return statusContaReceberAnterior;
    }

    public Integer getMovimentacaoOriginalId() {
        return movimentacaoOriginalId;
    }

    public Integer getMovimentacaoSaidaId() {
        return movimentacaoSaidaId;
    }

    @Override
    public String toString() {
        return "AuditoriaEstornoVenda{" +
                "idAuditoria=" + idAuditoria +
                ", vendaId=" + vendaId +
                ", usuarioId=" + usuarioId +
                ", dataHora=" + dataHora +
                ", motivo='" + motivo + '\'' +
                ", statusVendaAnterior=" + statusVendaAnterior +
                ", contaReceberId=" + contaReceberId +
                ", statusContaReceberAnterior=" + statusContaReceberAnterior +
                ", movimentacaoOriginalId=" + movimentacaoOriginalId +
                ", movimentacaoSaidaId=" + movimentacaoSaidaId +
                '}';
    }
}