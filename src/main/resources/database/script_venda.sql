CREATE TABLE IF NOT EXISTS venda (
    id_venda INTEGER PRIMARY KEY AUTOINCREMENT,
    -- Chave primária da venda.
    -- AUTOINCREMENT garante que cada venda tenha um identificador único.
    data_hora TEXT NOT NULL,
    -- Data e hora em que a venda/carrinho foi criado.
    -- No SQLite, datas são armazenadas como TEXT em formato ISO 8601.
    tipo_venda TEXT,
    -- AVISTA ou APRAZO.
    -- Na Fase 4 ainda pode ser nulo, pois a finalização não será implementada agora.
    forma_pagamento TEXT,
    -- DINHEIRO, PIX, CARTAO.
    -- Na Fase 4 ainda pode ser nulo.
    valor_total REAL NOT NULL DEFAULT 0,
    -- Começa com 0 e será recalculado conforme os itens forem adicionados.
    -- No Java, este valor deverá ser tratado com BigDecimal.
    valor_desconto_global REAL NOT NULL DEFAULT 0,
    -- Valor do desconto global aplicado na venda.
    -- Na Fase 4.1 permanece 0.
    status TEXT NOT NULL,
    -- Status da venda.
    -- ABERTA, FINALIZADA, CANCELADA, ESTORNADA.
    -- Na Fase 4, o status inicial esperado será algo como ABERTA.
    usuario_id INTEGER NOT NULL,
    -- Usuário responsável pela venda.
    -- Obrigatório, pois toda venda deve estar vinculada ao usuário logado.
    cliente_id INTEGER,
    -- Cliente associado à venda.
    -- Opcional, pois uma venda pode ser feita sem cliente identificado.

    -- Relacionamento com a tabela usuario.
    -- ON DELETE RESTRICT impede excluir um usuário que possua venda vinculada.
    -- ON UPDATE CASCADE atualiza a FK caso o ID do usuário seja alterado.
    FOREIGN KEY (usuario_id)
    REFERENCES usuario (id_usuario)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,

    -- Relacionamento com a tabela cliente.
    -- ON DELETE SET NULL permite excluir/inativar o vínculo do cliente
    -- sem apagar a venda histórica.
    FOREIGN KEY (cliente_id)
    REFERENCES cliente (id_cliente)
    ON DELETE SET NULL
    ON UPDATE CASCADE
    );