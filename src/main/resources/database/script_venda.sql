-- Tabela: Venda
-- Registra a venda finalizada e seus vínculos comerciais.
CREATE TABLE IF NOT EXISTS Venda (
    id_venda INTEGER PRIMARY KEY AUTOINCREMENT,

    -- A data/hora da finalização é definida pelo VendaService.
    data_hora TEXT NOT NULL,

    -- Tipo da venda: A_VISTA ou A_PRAZO.
    tipo_venda TEXT,

    -- À vista usa DINHEIRO, PIX ou CARTAO; venda a prazo usa A_PRAZO.
    forma_pagamento TEXT,

    -- O total é recalculado a partir dos subtotais atuais dos itens.
    valor_total REAL NOT NULL DEFAULT 0,

    -- O VendaService calcula e distribui o desconto global entre os itens elegíveis.
    valor_desconto_global REAL NOT NULL DEFAULT 0,

    -- Situação da venda: PAGA, PENDENTE ou ESTORNADA.
    status TEXT NOT NULL,

    usuario_id INTEGER NOT NULL,

    -- O cliente é opcional nas vendas sem identificação.
    cliente_id INTEGER,

    -- RESTRICT preserva o usuário referenciado por vendas históricas.
    FOREIGN KEY (usuario_id)
    REFERENCES Usuario(id_usuario)
    ON DELETE RESTRICT,

    -- SET NULL permite retirar o vínculo do cliente sem apagar a venda histórica.
    FOREIGN KEY (cliente_id)
    REFERENCES Cliente(id_cliente)
    ON DELETE SET NULL
    );