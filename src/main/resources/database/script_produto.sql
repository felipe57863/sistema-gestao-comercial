-- Tabela: Produto
-- Armazena os dados comerciais e de estoque dos produtos.
CREATE TABLE IF NOT EXISTS Produto (
    id_produto INTEGER PRIMARY KEY AUTOINCREMENT,
    descricao TEXT NOT NULL,
    preco REAL NOT NULL CHECK (preco >= 0),
    quantidade_estoque INTEGER NOT NULL CHECK (quantidade_estoque >= 0),
    estoque_minimo INTEGER NOT NULL CHECK (estoque_minimo >= 0),
    -- O SQLite representa o status ativo como 1 e o inativo como 0.
    ativo INTEGER NOT NULL DEFAULT 1 CHECK (ativo IN (0, 1))
    );