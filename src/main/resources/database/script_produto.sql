-- Criação da tabela Produto
CREATE TABLE IF NOT EXISTS Produto (
    id_produto INTEGER PRIMARY KEY AUTOINCREMENT,
    -- Nome do produto (obrigatório)
    descricao TEXT NOT NULL,
    -- Preço do produto (não pode ser negativo)
    preco REAL NOT NULL CHECK (preco >= 0),
    -- Quantidade disponível em estoque
    quantidade_estoque INTEGER NOT NULL CHECK (quantidade_estoque >= 0),
    -- Quantidade mínima para alerta de reposição
    estoque_minimo INTEGER NOT NULL CHECK (estoque_minimo >= 0),
    -- Indica se o produto está ativo no sistema
    -- SQLite não possui BOOLEAN, então usamos INTEGER: 1 = ativo, 0 = inativo
    ativo INTEGER NOT NULL DEFAULT 1 CHECK (ativo IN (0, 1))
    );