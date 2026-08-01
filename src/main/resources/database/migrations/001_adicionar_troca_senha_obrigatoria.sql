ALTER TABLE Usuario
ADD COLUMN troca_senha_obrigatoria INTEGER NOT NULL DEFAULT 1
CHECK (troca_senha_obrigatoria IN (0, 1));
