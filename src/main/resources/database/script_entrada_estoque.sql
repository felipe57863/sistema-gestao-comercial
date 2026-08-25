-- Tabela: EntradaEstoque
-- Registra o cabeçalho histórico de cada reposição confirmada de estoque.
CREATE TABLE IF NOT EXISTS EntradaEstoque (
    id_entrada INTEGER PRIMARY KEY AUTOINCREMENT,
    data_hora TEXT NOT NULL CHECK (TRIM(data_hora) <> ''),
    usuario_id INTEGER NOT NULL CHECK (usuario_id > 0),
    nome_usuario TEXT NOT NULL CHECK (TRIM(nome_usuario) <> ''),
    referencia TEXT CHECK (referencia IS NULL OR TRIM(referencia) <> ''),
    observacao TEXT CHECK (observacao IS NULL OR TRIM(observacao) <> ''),

    -- RESTRICT preserva o usuário vinculado ao histórico da entrada.
    FOREIGN KEY (usuario_id)
    REFERENCES Usuario(id_usuario)
    ON DELETE RESTRICT
    );
