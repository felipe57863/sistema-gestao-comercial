-- Tabela: ConfiguracaoAlertaVencimento
-- Mantém a configuração global usada pelos alertas automáticos de vencimento.
CREATE TABLE IF NOT EXISTS ConfiguracaoAlertaVencimento (
    -- O ID fixo em 1 mantém esta tabela como singleton.
    id_configuracao INTEGER PRIMARY KEY
    CHECK (id_configuracao = 1),

    -- Zero considera apenas contas vencidas e as que vencem hoje.
    dias_antecedencia INTEGER NOT NULL
    CHECK (dias_antecedencia BETWEEN 0 AND 365)
    );