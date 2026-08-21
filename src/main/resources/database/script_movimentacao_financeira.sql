-- Tabela: MovimentacaoFinanceira
-- Registra entradas e saídas financeiras sem apagar lançamentos anteriores.
CREATE TABLE IF NOT EXISTS MovimentacaoFinanceira (
    id_movimentacao INTEGER PRIMARY KEY AUTOINCREMENT,

    data_hora TEXT NOT NULL,

    -- O tipo identifica ENTRADA ou SAIDA; os valores oficiais são validados no Java.
    tipo TEXT NOT NULL,

    -- A origem identifica o fluxo que gerou o lançamento.
    -- Valores atuais: VENDA_A_VISTA, RECEBIMENTO_CONTA,
    -- ESTORNO_VENDA_A_VISTA e ESTORNO_RECEBIMENTO_CONTA.
    origem TEXT NOT NULL,

    forma_pagamento TEXT NOT NULL,

    valor REAL NOT NULL DEFAULT 0 CHECK (valor >= 0),

    -- A venda permanece vinculada também nos lançamentos compensatórios.
    venda_id INTEGER NOT NULL,

    -- A conta fica nula na venda à vista e no respectivo estorno.
    -- No recebimento integral e em sua compensação, identifica a conta correspondente.
    conta_receber_id INTEGER,

    usuario_id INTEGER NOT NULL,

    -- O estorno preserva a entrada original e registra uma nova SAIDA compensatória.

    -- Os vínculos usam RESTRICT para preservar o histórico financeiro.
    FOREIGN KEY (venda_id)
    REFERENCES Venda(id_venda)
    ON DELETE RESTRICT,

    FOREIGN KEY (conta_receber_id)
    REFERENCES ContaReceber(id_conta)
    ON DELETE RESTRICT,

    FOREIGN KEY (usuario_id)
    REFERENCES Usuario(id_usuario)
    ON DELETE RESTRICT
    );