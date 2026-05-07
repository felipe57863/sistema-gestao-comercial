-- Criação da tabela Venda
CREATE TABLE IF NOT EXISTS Venda (
    -- Chave primária da venda.
    -- AUTOINCREMENT garante que cada venda tenha um identificador único.
    id_venda INTEGER PRIMARY KEY AUTOINCREMENT,

    -- Data e hora em que a venda/carrinho foi criado.
    -- No SQLite, datas são armazenadas como TEXT em formato ISO 8601.
    data_hora TEXT NOT NULL,

    -- Tipo da venda.
    -- AVISTA ou APRAZO.
    -- Na Fase 4 ainda pode ser nulo, pois a finalização não será implementada agora.
    tipo_venda TEXT,

    -- Forma de pagamento da venda.
    -- DINHEIRO, PIX, CARTAO.
    -- Na Fase 4 ainda pode ser nulo, pois pagamento não será implementado agora.
    forma_pagamento TEXT,

    -- Valor total da venda.
    -- Começa com 0 e será recalculado conforme os itens forem adicionados.
    -- No Java, este valor deverá ser tratado com BigDecimal.
    valor_total REAL NOT NULL DEFAULT 0,

    -- Valor do desconto global aplicado na venda.
    -- A regra de desconto global será implementada somente no Passo 4.3.
    valor_desconto_global REAL NOT NULL DEFAULT 0,

    -- Status da venda.
    -- Exemplo: ABERTA, FINALIZADA, CANCELADA, ESTORNADA.
    -- Na Fase 4, o status inicial esperado será ABERTA.
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