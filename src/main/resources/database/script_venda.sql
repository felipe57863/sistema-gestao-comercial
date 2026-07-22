-- Criação da tabela Venda
CREATE TABLE IF NOT EXISTS Venda (
    -- Chave primária da venda.
    -- AUTOINCREMENT garante que cada venda tenha um identificador único.
    id_venda INTEGER PRIMARY KEY AUTOINCREMENT,

    -- Data e hora da finalização da venda, definida pelo VendaService antes da persistência.
    -- No SQLite, datas são armazenadas como TEXT em formato ISO 8601.
    data_hora TEXT NOT NULL,

    -- Tipo da venda: A_VISTA ou A_PRAZO.
    -- É preenchido pelo Service antes da persistência da venda finalizada.
    tipo_venda TEXT,

    -- Forma de pagamento: DINHEIRO, PIX ou CARTAO nas vendas à vista;
    -- A_PRAZO nas vendas que geram conta a receber.
    forma_pagamento TEXT,

    -- Valor total da venda.
    -- É recalculado com base nos subtotais atuais dos itens.
    -- No Java, este valor é tratado com BigDecimal.
    valor_total REAL NOT NULL DEFAULT 0,

    -- Valor do desconto global aplicado na venda.
    -- É calculado e distribuído entre os itens elegíveis pelo VendaService.
    valor_desconto_global REAL NOT NULL DEFAULT 0,

    -- Situação da venda: PAGA, PENDENTE ou ESTORNADA após estorno total.
    status TEXT NOT NULL,

    -- Usuário responsável pela venda.
    -- Obrigatório, pois toda venda deve estar vinculada ao usuário logado.
    usuario_id INTEGER NOT NULL,

    -- Cliente associado à venda.
    -- Opcional, pois uma venda pode ser feita sem cliente identificado.
    cliente_id INTEGER,

    -- Relacionamento com a tabela Usuario.
    -- ON DELETE RESTRICT impede excluir um usuário que possua venda vinculada.
    FOREIGN KEY (usuario_id)
    REFERENCES Usuario(id_usuario)
    ON DELETE RESTRICT,

    -- Relacionamento com a tabela Cliente.
    -- ON DELETE SET NULL permite remover o vínculo do cliente
    -- sem apagar a venda histórica.
    FOREIGN KEY (cliente_id)
    REFERENCES Cliente(id_cliente)
    ON DELETE SET NULL
    );