-- Tabela: Promocao
-- Armazena a promoção vinculada a um produto.
CREATE TABLE IF NOT EXISTS Promocao (
    id_promocao INTEGER PRIMARY KEY AUTOINCREMENT,
    tipo_desconto TEXT NOT NULL
    CHECK (tipo_desconto IN ('PERCENTUAL', 'VALOR_FIXO')),
    valor_desconto REAL NOT NULL
    CHECK (valor_desconto > 0),
    -- O SQLite representa a promoção ativa como 1 e a inativa como 0.
    ativa INTEGER NOT NULL DEFAULT 1
    CHECK (ativa IN (0,1)),
    produto_id INTEGER NOT NULL,
    -- RESTRICT preserva o histórico de promoções do produto.
    FOREIGN KEY (produto_id)
    REFERENCES Produto(id_produto)
    ON DELETE RESTRICT
    );