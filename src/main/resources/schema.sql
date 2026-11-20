CREATE TABLE IF NOT EXISTS produto (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo          TEXT    NOT NULL UNIQUE,
    nome            TEXT    NOT NULL,
    categoria       TEXT    NOT NULL,
    preco_custo     TEXT    NOT NULL,
    preco_venda     TEXT    NOT NULL,
    estoque_atual   INTEGER NOT NULL DEFAULT 0,
    estoque_minimo  INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS movimentacao (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    produto_id  INTEGER NOT NULL REFERENCES produto (id),
    tipo        TEXT    NOT NULL CHECK (tipo IN ('ENTRADA', 'SAIDA')),
    quantidade  INTEGER NOT NULL CHECK (quantidade > 0),
    data_hora   TEXT    NOT NULL,
    observacao  TEXT
);

CREATE TABLE IF NOT EXISTS venda (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    data_hora  TEXT NOT NULL,
    cliente    TEXT
);

CREATE TABLE IF NOT EXISTS item_venda (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    venda_id        INTEGER NOT NULL REFERENCES venda (id),
    produto_id      INTEGER NOT NULL REFERENCES produto (id),
    quantidade      INTEGER NOT NULL CHECK (quantidade > 0),
    preco_unitario  TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS fornecedor (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    nome                TEXT    NOT NULL,
    contato             TEXT,
    prazo_entrega_dias  INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS pedido_compra (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    fornecedor_id     INTEGER NOT NULL REFERENCES fornecedor (id),
    data_pedido       TEXT    NOT NULL,
    previsao_entrega  TEXT    NOT NULL,
    status            TEXT    NOT NULL CHECK (status IN ('ABERTO', 'RECEBIDO', 'CANCELADO')),
    data_recebimento  TEXT
);

CREATE TABLE IF NOT EXISTS item_pedido_compra (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    pedido_id       INTEGER NOT NULL REFERENCES pedido_compra (id),
    produto_id      INTEGER NOT NULL REFERENCES produto (id),
    quantidade      INTEGER NOT NULL CHECK (quantidade > 0),
    custo_unitario  TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_movimentacao_produto ON movimentacao (produto_id);
CREATE INDEX IF NOT EXISTS idx_pedido_compra_status ON pedido_compra (status);
CREATE INDEX IF NOT EXISTS idx_item_venda_venda ON item_venda (venda_id);
CREATE INDEX IF NOT EXISTS idx_venda_data ON venda (data_hora);
