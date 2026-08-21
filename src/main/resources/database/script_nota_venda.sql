-- Tabela: NotaVenda
-- Preserva a fotografia documental de uma venda finalizada.
CREATE TABLE IF NOT EXISTS NotaVenda (
    id_nota INTEGER PRIMARY KEY AUTOINCREMENT,

    -- Uma venda possui no máximo uma Nota de Venda.
    venda_id INTEGER NOT NULL UNIQUE
    CHECK (venda_id > 0),

    -- O único fluxo documental permitido é ATIVA -> ESTORNADA.
    status TEXT NOT NULL DEFAULT 'ATIVA'
    CHECK (status IN ('ATIVA', 'ESTORNADA')),

    data_hora_venda TEXT NOT NULL
    CHECK (LENGTH(TRIM(data_hora_venda)) > 0),

    tipo_venda TEXT NOT NULL
    CHECK (tipo_venda IN ('A_VISTA', 'A_PRAZO')),

    forma_pagamento TEXT NOT NULL
    CHECK (
        forma_pagamento IN (
            'DINHEIRO',
            'PIX',
            'CARTAO',
            'A_PRAZO'
        )
    ),

    -- O usuário é histórico e não mantém FK viva com o cadastro atual.
    usuario_id INTEGER NOT NULL
    CHECK (usuario_id > 0),

    nome_usuario TEXT NOT NULL
    CHECK (LENGTH(TRIM(nome_usuario)) > 0),

    -- O cliente é histórico; vendas à vista podem não possuir cliente identificado.
    -- ID, nome e documento ficam todos ausentes ou todos preenchidos.
    cliente_id INTEGER
    CHECK (cliente_id IS NULL OR cliente_id > 0),

    nome_cliente TEXT,
    documento_cliente TEXT,

    valor_total REAL NOT NULL
    CHECK (valor_total > 0),

    valor_desconto_global REAL NOT NULL DEFAULT 0
    CHECK (valor_desconto_global >= 0),

    -- Valor recebido e troco existem somente para pagamento em DINHEIRO.
    valor_recebido REAL
    CHECK (valor_recebido IS NULL OR valor_recebido >= 0),

    troco REAL
    CHECK (troco IS NULL OR troco >= 0),

    -- O prazo efetivo é histórico e não mantém dependência viva do cadastro atual.
    prazo_pagamento_id INTEGER
    CHECK (prazo_pagamento_id IS NULL OR prazo_pagamento_id > 0),

    quantidade_dias_prazo INTEGER
    CHECK (
        quantidade_dias_prazo IS NULL
        OR quantidade_dias_prazo > 0
    ),

    data_vencimento TEXT,

    CHECK (
        (
            cliente_id IS NULL
            AND nome_cliente IS NULL
            AND documento_cliente IS NULL
        )
        OR
        (
            cliente_id IS NOT NULL
            AND nome_cliente IS NOT NULL
            AND LENGTH(TRIM(nome_cliente)) > 0
            AND documento_cliente IS NOT NULL
            AND LENGTH(TRIM(documento_cliente)) > 0
        )
    ),

    -- Venda a prazo exige cliente identificado.
    CHECK (
        tipo_venda <> 'A_PRAZO'
        OR cliente_id IS NOT NULL
    ),

    -- Tipo de venda e forma de pagamento devem permanecer coerentes.
    CHECK (
        (
            tipo_venda = 'A_VISTA'
            AND forma_pagamento IN (
                'DINHEIRO',
                'PIX',
                'CARTAO'
            )
        )
        OR
        (
            tipo_venda = 'A_PRAZO'
            AND forma_pagamento = 'A_PRAZO'
        )
    ),

    -- A igualdade exata do troco é validada pelo Service com BigDecimal.
    CHECK (
        (
            forma_pagamento = 'DINHEIRO'
            AND valor_recebido IS NOT NULL
            AND valor_recebido >= valor_total
            AND troco IS NOT NULL
            AND troco >= 0
        )
        OR
        (
            forma_pagamento <> 'DINHEIRO'
            AND valor_recebido IS NULL
            AND troco IS NULL
        )
    ),

    -- Venda a prazo congela prazo e vencimento; venda à vista não usa esses campos.
    CHECK (
        (
            tipo_venda = 'A_PRAZO'
            AND prazo_pagamento_id IS NOT NULL
            AND quantidade_dias_prazo IS NOT NULL
            AND data_vencimento IS NOT NULL
            AND LENGTH(TRIM(data_vencimento)) > 0
        )
        OR
        (
            tipo_venda = 'A_VISTA'
            AND prazo_pagamento_id IS NULL
            AND quantidade_dias_prazo IS NULL
            AND data_vencimento IS NULL
        )
    ),

    -- A Venda permanece como o vínculo estrutural vivo da Nota histórica.
    FOREIGN KEY (venda_id)
    REFERENCES Venda(id_venda)
    ON DELETE RESTRICT
);
