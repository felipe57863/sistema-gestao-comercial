-- Criação da tabela MovimentacaoFinanceira
CREATE TABLE IF NOT EXISTS MovimentacaoFinanceira (
    -- Chave primária da movimentação financeira.
    id_movimentacao INTEGER PRIMARY KEY AUTOINCREMENT,

    -- Data e hora da movimentação.
    -- No Java: LocalDateTime.
    -- No SQLite: TEXT em formato ISO 8601.
    data_hora TEXT NOT NULL,

    -- Tipo da movimentação.
    -- Primeira entrega: ENTRADA.
    -- Não usar CHECK restritivo aqui para não dificultar fases futuras.
    tipo TEXT NOT NULL,

    -- Origem da movimentação.
    -- Primeira entrega: VENDA_A_VISTA.
    -- Futuramente poderá receber outras origens, como recebimento de conta.
    origem TEXT NOT NULL,

    -- Forma de pagamento usada na movimentação.
    -- Primeira entrega: DINHEIRO, PIX ou CARTAO.
    forma_pagamento TEXT NOT NULL,

    -- Valor da movimentação financeira.
    -- No Java, este valor deverá ser tratado com BigDecimal.
    valor REAL NOT NULL DEFAULT 0 CHECK (valor >= 0),

    -- Venda vinculada à movimentação.
    venda_id INTEGER NOT NULL,

    -- Conta a receber vinculada.
    -- Na venda à vista, fica NULL.
    -- Futuramente poderá ser usado no recebimento de conta.
    conta_receber_id INTEGER,

    -- Usuário responsável pelo lançamento.
    usuario_id INTEGER NOT NULL,

    -- Relacionamento com a venda.
    -- Restritivo para preservar histórico financeiro.
    FOREIGN KEY (venda_id)
    REFERENCES Venda(id_venda)
    ON DELETE RESTRICT,

    -- Relacionamento com conta a receber.
    -- Restritivo para preservar histórico financeiro.
    FOREIGN KEY (conta_receber_id)
    REFERENCES ContaReceber(id_conta)
    ON DELETE RESTRICT,

    -- Relacionamento com usuário.
    -- Restritivo para preservar histórico financeiro.
    FOREIGN KEY (usuario_id)
    REFERENCES Usuario(id_usuario)
    ON DELETE RESTRICT
    );