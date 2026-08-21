-- Tabela: PrazoPagamento
-- Define os prazos disponíveis para vendas e limites comerciais.
CREATE TABLE IF NOT EXISTS PrazoPagamento (
    id_prazo INTEGER PRIMARY KEY AUTOINCREMENT,
    descricao TEXT NOT NULL,
    quantidade_dias INTEGER NOT NULL CHECK (quantidade_dias >= 0),
    -- O SQLite representa o status ativo como 1 e o inativo como 0.
    ativo INTEGER NOT NULL DEFAULT 1 CHECK (ativo IN (0, 1))
    );