-- Criação da tabela PrazoPagamento
CREATE TABLE IF NOT EXISTS PrazoPagamento (
    id_prazo INTEGER PRIMARY KEY AUTOINCREMENT,
    -- Descrição do prazo, exemplo: "À Vista", "30 Dias", "15 e 30 Dias"
    descricao TEXT NOT NULL,
    -- Quantidade de dias para vencimento
    quantidade_dias INTEGER NOT NULL CHECK (quantidade_dias >= 0),
    -- SQLite não possui BOOLEAN, então usamos INTEGER: 1 = ativo, 0 = inativo
    ativo INTEGER NOT NULL DEFAULT 1 CHECK (ativo IN (0, 1))
    );