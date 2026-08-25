-- Tabela: ItemEntradaEstoque
-- Registra os produtos e valores históricos de cada entrada de estoque.
CREATE TABLE IF NOT EXISTS ItemEntradaEstoque (
    id_item_entrada INTEGER PRIMARY KEY AUTOINCREMENT,
    entrada_id INTEGER NOT NULL CHECK (entrada_id > 0),
    produto_id INTEGER NOT NULL CHECK (produto_id > 0),
    descricao_produto TEXT NOT NULL CHECK (TRIM(descricao_produto) <> ''),
    quantidade_recebida INTEGER NOT NULL CHECK (quantidade_recebida > 0),
    preco_compra_unitario REAL NOT NULL CHECK (preco_compra_unitario > 0),
    subtotal REAL NOT NULL CHECK (subtotal > 0),

    UNIQUE (entrada_id, produto_id),

    -- RESTRICT preserva a entrada e o produto vinculados ao histórico do item.
    FOREIGN KEY (entrada_id)
    REFERENCES EntradaEstoque(id_entrada)
    ON DELETE RESTRICT,

    FOREIGN KEY (produto_id)
    REFERENCES Produto(id_produto)
    ON DELETE RESTRICT
    );
