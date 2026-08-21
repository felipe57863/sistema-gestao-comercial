-- Tabela: ItemNotaVenda
-- Preserva os itens da fotografia documental usada na geração e reimpressão da Nota.
CREATE TABLE IF NOT EXISTS ItemNotaVenda (
    id_item_nota INTEGER PRIMARY KEY AUTOINCREMENT,

    nota_id INTEGER NOT NULL
    CHECK (nota_id > 0),

    -- O produto é histórico e não mantém FK viva com o cadastro atual.
    produto_id INTEGER NOT NULL
    CHECK (produto_id > 0),

    descricao_produto TEXT NOT NULL
    CHECK (LENGTH(TRIM(descricao_produto)) > 0),

    quantidade INTEGER NOT NULL
    CHECK (quantidade > 0),

    preco_unitario REAL NOT NULL
    CHECK (preco_unitario >= 0),

    desconto_promocional REAL NOT NULL DEFAULT 0
    CHECK (desconto_promocional >= 0),

    desconto_global REAL NOT NULL DEFAULT 0
    CHECK (desconto_global >= 0),

    subtotal REAL NOT NULL
    CHECK (subtotal >= 0),

    FOREIGN KEY (nota_id)
    REFERENCES NotaVenda(id_nota)
    ON DELETE RESTRICT
);
