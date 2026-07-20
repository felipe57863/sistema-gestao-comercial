-- Criação da tabela AuditoriaEstornoVenda.
CREATE TABLE IF NOT EXISTS AuditoriaEstornoVenda (
    -- Chave primária do registro de auditoria.
    id_auditoria INTEGER PRIMARY KEY AUTOINCREMENT,

    -- Venda estornada.
    -- Garante somente uma auditoria para cada venda.
    venda_id INTEGER NOT NULL UNIQUE,

    -- Usuário administrador responsável pelo estorno.
    usuario_id INTEGER NOT NULL,

    -- Data e hora em que o estorno foi concluído.
    -- O formato ISO 8601 e a geração do valor pertencem ao Java.
    data_hora TEXT NOT NULL
    CHECK (
              LENGTH(TRIM(data_hora)) > 0
    ),

    -- Motivo obrigatório do estorno, limitado a 500 caracteres.
    motivo TEXT NOT NULL
    CHECK (
              LENGTH(TRIM(motivo)) BETWEEN 1 AND 500
    ),

    -- Status da venda antes do estorno.
    -- Uma nova auditoria somente pode partir de PAGA ou PENDENTE.
    status_venda_anterior TEXT NOT NULL
    CHECK (
              status_venda_anterior IN ('PAGA', 'PENDENTE')
    ),

    -- Conta a receber vinculada à venda, quando aplicável.
    -- Uma mesma conta não pode participar de mais de um estorno.
    conta_receber_id INTEGER UNIQUE,

    -- Status da conta antes do cancelamento, quando aplicável.
    -- CANCELADA não pode ser estado anterior de uma nova auditoria.
    status_conta_receber_anterior TEXT
    CHECK (
              status_conta_receber_anterior IS NULL
              OR status_conta_receber_anterior IN ('PAGA', 'PENDENTE')
    ),

    -- Entrada financeira original revertida pelo estorno, quando aplicável.
    -- Uma movimentação original não pode ser vinculada a mais de um estorno.
    movimentacao_original_id INTEGER UNIQUE,

    -- Nova movimentação financeira de saída criada pelo estorno,
    -- quando aplicável.
    -- Uma saída não pode ser vinculada a mais de uma auditoria.
    movimentacao_saida_id INTEGER UNIQUE,

    -- Conta e status anterior devem estar ambos presentes ou ambos ausentes.
    CHECK (
(
              conta_receber_id IS NULL
              AND status_conta_receber_anterior IS NULL
)
    OR
(
    conta_receber_id IS NOT NULL
    AND status_conta_receber_anterior IS NOT NULL
)
    ),

    -- As movimentações original e de saída devem estar ambas presentes
    -- ou ambas ausentes.
    CHECK (
(
              movimentacao_original_id IS NULL
              AND movimentacao_saida_id IS NULL
)
    OR
(
    movimentacao_original_id IS NOT NULL
    AND movimentacao_saida_id IS NOT NULL
)
    ),

    -- A movimentação original não pode ser a mesma movimentação de saída.
    CHECK (
              movimentacao_original_id IS NULL
              OR movimentacao_saida_id IS NULL
              OR movimentacao_original_id <> movimentacao_saida_id
          ),

    -- Preserva a venda vinculada ao histórico do estorno.
    FOREIGN KEY (venda_id)
    REFERENCES Venda(id_venda)
    ON DELETE RESTRICT,

    -- Preserva o usuário responsável pelo estorno.
    FOREIGN KEY (usuario_id)
    REFERENCES Usuario(id_usuario)
    ON DELETE RESTRICT,

    -- Preserva a conta vinculada, quando existir.
    FOREIGN KEY (conta_receber_id)
    REFERENCES ContaReceber(id_conta)
    ON DELETE RESTRICT,

    -- Preserva a movimentação financeira original, quando existir.
    FOREIGN KEY (movimentacao_original_id)
    REFERENCES MovimentacaoFinanceira(id_movimentacao)
    ON DELETE RESTRICT,

    -- Preserva a movimentação financeira de saída, quando existir.
    FOREIGN KEY (movimentacao_saida_id)
    REFERENCES MovimentacaoFinanceira(id_movimentacao)
    ON DELETE RESTRICT
    );