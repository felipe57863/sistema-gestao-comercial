-- Tabela: ItemVenda
-- Preserva os valores efetivamente usados em cada item da venda.
CREATE TABLE IF NOT EXISTS ItemVenda (
    id_item INTEGER PRIMARY KEY AUTOINCREMENT,

    quantidade INTEGER NOT NULL,

    -- O preço unitário é congelado para preservar o histórico da venda.
    preco_unitario REAL NOT NULL,

    -- Itens com promoção não participam do desconto global.
    desconto_promocional REAL NOT NULL DEFAULT 0,

    -- Guarda a parcela do desconto global distribuída para este item elegível.
    desconto_global REAL NOT NULL DEFAULT 0,

    -- Subtotal = quantidade * preço unitário - desconto promocional - desconto global.
    -- O cálculo pertence ao domínio Java, não ao banco.
    subtotal REAL NOT NULL,

    produto_id INTEGER NOT NULL,

    venda_id INTEGER NOT NULL,

    -- RESTRICT preserva os produtos e a venda referenciados pelo histórico.
    FOREIGN KEY (produto_id)
    REFERENCES Produto(id_produto)
    ON DELETE RESTRICT,

    FOREIGN KEY (venda_id)
    REFERENCES Venda(id_venda)
    ON DELETE RESTRICT
    );