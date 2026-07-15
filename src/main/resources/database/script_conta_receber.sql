-- Criação da tabela ContaReceber
CREATE TABLE IF NOT EXISTS ContaReceber (
    -- Chave primária da conta a receber.
    id_conta INTEGER PRIMARY KEY AUTOINCREMENT,
    -- Valor da conta gerada pela venda a prazo.
    -- No Java, este valor é tratado com BigDecimal.
    valor REAL NOT NULL DEFAULT 0 CHECK (valor >= 0),

    -- Data de vencimento da conta.
    -- Regra: data da venda + quantidade de dias do prazo efetivo selecionado.
    -- No Java: LocalDate.
    -- No SQLite: TEXT em formato ISO 8601.
    data_vencimento TEXT NOT NULL,

    -- Status da conta: PENDENTE na geração e PAGA após o recebimento integral.
    -- A validação dos valores oficiais ocorre no Java, sem CHECK restritivo no banco.
    status TEXT NOT NULL,

    -- Venda que gerou esta conta.
    -- O VendaService gera a conta da venda a prazo; a restrição UNIQUE impede
    -- vincular a mesma venda a mais de uma ContaReceber.
    venda_id INTEGER NOT NULL UNIQUE,

    -- Cliente devedor da conta.
    -- Mantido diretamente para facilitar o cálculo de limite disponível.
    cliente_id INTEGER NOT NULL,

    -- Prazo efetivo escolhido na venda.
    -- Deve ser prazo cadastrado, ativo e menor ou igual ao prazo máximo do cliente.
    prazo_pagamento_id INTEGER NOT NULL,

    -- Cópia histórica da quantidade de dias do prazo efetivo.
    -- Evita perda de histórico caso o cadastro do prazo seja alterado.
    quantidade_dias_prazo INTEGER NOT NULL CHECK (quantidade_dias_prazo > 0),

    -- Data e hora de criação da conta.
    -- No Java: LocalDateTime.
    -- No SQLite: TEXT em formato ISO 8601.
    data_criacao TEXT NOT NULL,

    -- Relacionamento com a venda.
    -- Restritivo para preservar histórico financeiro.
    FOREIGN KEY (venda_id)
    REFERENCES Venda(id_venda)
    ON DELETE RESTRICT,

    -- Relacionamento com o cliente.
    -- Restritivo para preservar histórico financeiro.
    FOREIGN KEY (cliente_id)
    REFERENCES Cliente(id_cliente)
    ON DELETE RESTRICT,

    -- Relacionamento com o prazo de pagamento efetivo.
    -- Restritivo para preservar histórico financeiro.
    FOREIGN KEY (prazo_pagamento_id)
    REFERENCES PrazoPagamento(id_prazo)
    ON DELETE RESTRICT
    );