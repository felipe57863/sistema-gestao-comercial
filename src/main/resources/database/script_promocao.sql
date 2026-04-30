-- Criação da tabela Promocao
CREATE TABLE IF NOT EXISTS Promocao (
    id_promocao INTEGER PRIMARY KEY AUTOINCREMENT,
    -- Enum do Java salvo como TEXT (validação garantida via CHECK)
    tipo_desconto TEXT NOT NULL
    CHECK (tipo_desconto IN ('PERCENTUAL', 'VALOR_FIXO')),
    -- Valor do desconto (deve ser maior que zero)
    valor_desconto REAL NOT NULL
    CHECK (valor_desconto > 0),
    -- Boolean no SQLite (1 = ativa, 0 = inativa)
    ativa INTEGER NOT NULL DEFAULT 1
    CHECK (ativa IN (0,1)),
    -- FK para Produto
    produto_id INTEGER NOT NULL,
    -- Regra: não permitir deletar produto com histórico de promoção
    FOREIGN KEY (produto_id)
    REFERENCES Produto(id_produto)
    ON DELETE RESTRICT
    );