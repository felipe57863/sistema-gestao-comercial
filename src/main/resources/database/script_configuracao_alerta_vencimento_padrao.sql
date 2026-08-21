-- Configuração inicial dos alertas de vencimento: 7 dias de antecedência.
INSERT OR IGNORE INTO ConfiguracaoAlertaVencimento (
    id_configuracao,
    dias_antecedencia
) VALUES (1, 7);