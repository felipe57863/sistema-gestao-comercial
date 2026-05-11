-- Criação da tabela ItemVenda
CREATE TABLE IF NOT EXISTS ItemVenda (
    -- Chave primária do item da venda.
    -- Cada produto adicionado a uma venda gera um item.
    id_item INTEGER PRIMARY KEY AUTOINCREMENT,

    -- Quantidade do produto vendida/adicionada ao carrinho.
    -- Deve ser maior que zero, mas essa validação principal
    -- será feita na camada Service.
    quantidade INTEGER NOT NULL,

    -- Preço unitário do produto no momento da venda.
    -- Importante para manter histórico mesmo que o preço do
    -- produto seja alterado futuramente.
    -- No Java, este valor deverá ser tratado com BigDecimal.
    preco_unitario REAL NOT NULL,

    -- Desconto promocional aplicado ao item.
    -- Caso não exista promoção, o valor padrão será 0.
    -- Produtos com desconto promocional não devem receber
    -- desconto global no Passo 4.3.
    desconto_promocional REAL NOT NULL DEFAULT 0,

    -- Desconto global aplicado ao item.
    -- Representa a parte do desconto global da venda atribuída
    -- especificamente a este item.
    -- Caso não exista desconto global, o valor padrão será 0.
    -- Este desconto só poderá ser aplicado em itens sem promoção.
    desconto_global REAL NOT NULL DEFAULT 0,

    -- Subtotal do item.
    -- Representa:
    -- quantidade * preco_unitario - desconto_promocional - desconto_global
    -- O cálculo será feito na entidade/serviço, não no banco.
    subtotal REAL NOT NULL,

    -- Produto associado ao item da venda.
    -- Obrigatório, pois todo item precisa representar um produto.
    produto_id INTEGER NOT NULL,

    -- Venda associada ao item.
    -- Obrigatório, pois todo item deve pertencer a uma venda.
    venda_id INTEGER NOT NULL,

    -- Relacionamento com a tabela Produto.
    -- ON DELETE RESTRICT impede excluir fisicamente um produto
    -- que já tenha sido usado em uma venda.
    FOREIGN KEY (produto_id)
    REFERENCES Produto(id_produto)
    ON DELETE RESTRICT,

    -- Relacionamento com a tabela Venda.
    -- ON DELETE RESTRICT impede apagar uma venda que possua
    -- itens vinculados, preservando o histórico.
    FOREIGN KEY (venda_id)
    REFERENCES Venda(id_venda)
    ON DELETE RESTRICT
    );