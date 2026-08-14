-- Configuração global dos alertas automáticos de vencimento.
CREATE TABLE IF NOT EXISTS ConfiguracaoAlertaVencimento (
    -- Tabela singleton: o sistema possui uma única configuração global.
    id_configuracao INTEGER PRIMARY KEY
    CHECK (id_configuracao = 1),

    -- Quantidade de dias corridos de antecedência.
    -- 0 significa: vencidas + contas que vencem hoje.
    dias_antecedencia INTEGER NOT NULL
    CHECK (dias_antecedencia BETWEEN 0 AND 365)
    );