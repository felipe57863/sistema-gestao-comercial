-- Tabela: Usuario
-- Armazena usuários, autenticação e estado de acesso ao ERP.
CREATE TABLE IF NOT EXISTS Usuario (
    id_usuario INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    login TEXT NOT NULL UNIQUE,
    -- A senha é persistida somente como hash BCrypt.
    senha TEXT NOT NULL,
    -- Perfil e status oficiais são validados pela aplicação.
    perfil TEXT NOT NULL,
    status TEXT NOT NULL,
    -- 1 exige troca de senha antes do acesso normal; 0 libera o fluxo comum.
    troca_senha_obrigatoria INTEGER NOT NULL DEFAULT 1
    CHECK (troca_senha_obrigatoria IN (0, 1))
    );
