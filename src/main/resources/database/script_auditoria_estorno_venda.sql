-- Tabela: AuditoriaEstornoVenda
-- Registra o estorno concluído e preserva os vínculos necessários à auditoria.
CREATE TABLE IF NOT EXISTS AuditoriaEstornoVenda (
    id_auditoria INTEGER PRIMARY KEY AUTOINCREMENT,

    -- Uma venda pode possuir no máximo uma auditoria de estorno.
    venda_id INTEGER NOT NULL UNIQUE,

    usuario_id INTEGER NOT NULL,

    data_hora TEXT NOT NULL
    CHECK (
              LENGTH(TRIM(data_hora)) > 0
    ),

    motivo TEXT NOT NULL
    CHECK (
              LENGTH(TRIM(motivo)) BETWEEN 1 AND 500
    ),

    -- O estado anterior da venda só pode ser PAGA ou PENDENTE.
    status_venda_anterior TEXT NOT NULL
    CHECK (
              status_venda_anterior IN ('PAGA', 'PENDENTE')
    ),

    -- Uma conta pode participar de no máximo um estorno.
    conta_receber_id INTEGER UNIQUE,

    -- CANCELADA não pode ser o estado anterior de uma nova auditoria.
    status_conta_receber_anterior TEXT
    CHECK (
              status_conta_receber_anterior IS NULL
              OR status_conta_receber_anterior IN ('PAGA', 'PENDENTE')
    ),

    -- Cada movimentação original pode ser revertida por no máximo um estorno.
    movimentacao_original_id INTEGER UNIQUE,

    -- Cada saída compensatória pertence a no máximo uma auditoria.
    movimentacao_saida_id INTEGER UNIQUE,

    -- Conta e estado anterior são preservados sempre em conjunto.
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

    -- Movimentação original e saída compensatória são preservadas em conjunto.
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

    -- A movimentação original nunca pode ser a própria saída compensatória.
    CHECK (
              movimentacao_original_id IS NULL
              OR movimentacao_saida_id IS NULL
              OR movimentacao_original_id <> movimentacao_saida_id
          ),

    FOREIGN KEY (venda_id)
    REFERENCES Venda(id_venda)
    ON DELETE RESTRICT,

    FOREIGN KEY (usuario_id)
    REFERENCES Usuario(id_usuario)
    ON DELETE RESTRICT,

    FOREIGN KEY (conta_receber_id)
    REFERENCES ContaReceber(id_conta)
    ON DELETE RESTRICT,

    FOREIGN KEY (movimentacao_original_id)
    REFERENCES MovimentacaoFinanceira(id_movimentacao)
    ON DELETE RESTRICT,

    FOREIGN KEY (movimentacao_saida_id)
    REFERENCES MovimentacaoFinanceira(id_movimentacao)
    ON DELETE RESTRICT
    );