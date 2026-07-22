-- Criação da tabela MovimentacaoFinanceira
CREATE TABLE IF NOT EXISTS MovimentacaoFinanceira (
    -- Chave primária da movimentação financeira.
    id_movimentacao INTEGER PRIMARY KEY AUTOINCREMENT,

    -- Data e hora da movimentação.
    -- No Java: LocalDateTime.
    -- No SQLite: TEXT em formato ISO 8601.
    data_hora TEXT NOT NULL,

    -- O tipo identifica se o lançamento representa ENTRADA ou SAIDA financeira.
    -- A validação dos valores oficiais ocorre no Java, sem CHECK restritivo no banco.
    tipo TEXT NOT NULL,

    -- A origem identifica o fluxo que gerou o lançamento.
    -- Valores atuais: VENDA_A_VISTA, RECEBIMENTO_CONTA,
    -- ESTORNO_VENDA_A_VISTA ou ESTORNO_RECEBIMENTO_CONTA.
    origem TEXT NOT NULL,

    -- Forma de pagamento usada na movimentação: DINHEIRO, PIX ou CARTAO.
    forma_pagamento TEXT NOT NULL,

    -- Valor da movimentação financeira.
    -- No Java, este valor é tratado com BigDecimal.
    valor REAL NOT NULL DEFAULT 0 CHECK (valor >= 0),

    -- Venda vinculada à movimentação, inclusive nos lançamentos compensatórios.
    venda_id INTEGER NOT NULL,

    -- Conta a receber vinculada.
    -- Fica NULL na venda à vista e no respectivo estorno. No recebimento integral
    -- e em sua compensação, identifica a conta correspondente.
    conta_receber_id INTEGER,

    -- Usuário responsável pelo lançamento.
    usuario_id INTEGER NOT NULL,

    -- O estorno preserva a movimentação original e registra uma nova SAIDA
    -- compensatória quando necessário, mantendo a rastreabilidade financeira.

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