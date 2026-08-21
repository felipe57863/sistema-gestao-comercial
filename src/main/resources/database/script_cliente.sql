-- Tabela: Cliente
-- Armazena os dados cadastrais e as condições comerciais do cliente.
CREATE TABLE IF NOT EXISTS Cliente (
    id_cliente INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    -- CPF/CNPJ é persistido sem pontuação.
    documento TEXT NOT NULL UNIQUE,
    telefone TEXT,
    email TEXT,
    tipo_cliente TEXT NOT NULL CHECK (tipo_cliente IN ('PF', 'PJ')),
    limite_credito REAL NOT NULL CHECK (limite_credito >= 0),
    status TEXT NOT NULL CHECK (status IN ('ATIVO', 'BLOQUEADO')),
    prazo_pagamento_id INTEGER NOT NULL,
    FOREIGN KEY (prazo_pagamento_id)
    REFERENCES PrazoPagamento(id_prazo)
    ON DELETE RESTRICT
    );
