-- Criação da tabela Usuario
CREATE TABLE IF NOT EXISTS Usuario (
    id_usuario INTEGER PRIMARY KEY AUTOINCREMENT,
    -- Nome completo do usuário
    nome TEXT NOT NULL,
    -- Login único para acesso ao sistema
    login TEXT NOT NULL UNIQUE,
    -- Hash BCrypt da senha do usuário
    senha TEXT NOT NULL,
    -- Perfil do usuário: exemplo ADMIN ou VENDEDOR
    perfil TEXT NOT NULL,
    -- Status do usuário: exemplo ATIVO ou INATIVO
    status TEXT NOT NULL
    );