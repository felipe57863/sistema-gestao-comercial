-- Criação da tabela Cliente
CREATE TABLE IF NOT EXISTS Cliente (
    id_cliente INTEGER PRIMARY KEY AUTOINCREMENT,
    -- Nome completo ou razão social do cliente
    nome TEXT NOT NULL,
    -- CPF ou CNPJ sem pontuação
    documento TEXT NOT NULL UNIQUE,
    -- Tipo do cliente: PF ou PJ
    tipo_cliente TEXT NOT NULL CHECK (tipo_cliente IN ('PF', 'PJ')),
    -- Limite de crédito do cliente
    limite_credito REAL NOT NULL CHECK (limite_credito >= 0),
    -- Status do cliente: ATIVO ou BLOQUEADO
    status TEXT NOT NULL CHECK (status IN ('ATIVO', 'BLOQUEADO')),
    -- FK para prazo de pagamento
    prazo_pagamento_id INTEGER NOT NULL,
    FOREIGN KEY (prazo_pagamento_id)
    REFERENCES PrazoPagamento(id_prazo)
    ON DELETE RESTRICT
    );