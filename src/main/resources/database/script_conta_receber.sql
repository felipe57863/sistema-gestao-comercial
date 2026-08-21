-- Tabela: ContaReceber
-- Registra a obrigação financeira gerada por uma venda a prazo.
CREATE TABLE IF NOT EXISTS ContaReceber (
    id_conta INTEGER PRIMARY KEY AUTOINCREMENT,
    valor REAL NOT NULL DEFAULT 0 CHECK (valor >= 0),

    -- O vencimento é a data da venda somada aos dias do prazo efetivo.
    data_vencimento TEXT NOT NULL,

    -- PENDENTE, PAGA e CANCELADA são controlados pela aplicação.
    status TEXT NOT NULL,

    -- Cada venda a prazo pode gerar no máximo uma ContaReceber.
    venda_id INTEGER NOT NULL UNIQUE,

    -- O cliente é mantido diretamente para o cálculo do limite disponível.
    cliente_id INTEGER NOT NULL,

    -- O prazo efetivo deve respeitar o prazo máximo permitido ao cliente.
    prazo_pagamento_id INTEGER NOT NULL,

    -- Os dias do prazo são congelados para preservar o histórico da venda.
    quantidade_dias_prazo INTEGER NOT NULL CHECK (quantidade_dias_prazo > 0),

    data_criacao TEXT NOT NULL,

    -- Os vínculos usam RESTRICT para preservar o histórico financeiro.
    FOREIGN KEY (venda_id)
    REFERENCES Venda(id_venda)
    ON DELETE RESTRICT,

    FOREIGN KEY (cliente_id)
    REFERENCES Cliente(id_cliente)
    ON DELETE RESTRICT,

    FOREIGN KEY (prazo_pagamento_id)
    REFERENCES PrazoPagamento(id_prazo)
    ON DELETE RESTRICT
    );