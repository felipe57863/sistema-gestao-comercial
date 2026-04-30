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
    -- Status do produto (alinhado com Enum do Java)
    status TEXT NOT NULL DEFAULT 'ATIVO'
    );