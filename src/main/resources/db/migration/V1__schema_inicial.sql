-- Schema inicial do MVP: partidos, deputados, proposições e autoria.
-- Regra de nullability: só é NOT NULL o que a fonte (API da Câmara) garante.

CREATE TABLE partido (
    id             BIGINT PRIMARY KEY,          -- id oficial da Câmara
    sigla          VARCHAR(50)  NOT NULL,
    nome           VARCHAR(255) NOT NULL,
    uri            VARCHAR(255),
    atualizado_em  TIMESTAMP    NOT NULL
);
-- Siglas se repetem entre partidos ao longo do tempo: índice, nunca chave.
CREATE INDEX idx_partido_sigla ON partido (sigla);

CREATE TABLE deputado (
    id                    BIGINT PRIMARY KEY,    -- id oficial da Câmara
    nome                  VARCHAR(255) NOT NULL,
    nome_civil            VARCHAR(255),
    sigla_uf              VARCHAR(2),
    id_legislatura        INTEGER,
    url_foto              VARCHAR(500),
    email                 VARCHAR(255),
    data_nascimento       DATE,
    uf_nascimento         VARCHAR(2),
    municipio_nascimento  VARCHAR(255),
    escolaridade          VARCHAR(255),
    situacao              VARCHAR(100),
    condicao_eleitoral    VARCHAR(100),
    partido_id            BIGINT REFERENCES partido (id),
    sigla_partido         VARCHAR(50),           -- retrato; resolve quando partido_id é null
    atualizado_em         TIMESTAMP NOT NULL
);
CREATE INDEX idx_deputado_uf      ON deputado (sigla_uf);
CREATE INDEX idx_deputado_partido ON deputado (partido_id);
CREATE INDEX idx_deputado_nome    ON deputado (nome);

CREATE TABLE proposicao (
    id                    BIGINT PRIMARY KEY,
    sigla_tipo            VARCHAR(20)  NOT NULL,
    cod_tipo              INTEGER,
    descricao_tipo        VARCHAR(255),
    numero                INTEGER      NOT NULL,
    ano                   INTEGER      NOT NULL,
    ementa                TEXT,
    data_apresentacao     TIMESTAMP,
    url_inteiro_teor      VARCHAR(500),
    situacao_descricao    VARCHAR(255),
    situacao_cod          INTEGER,
    situacao_data         TIMESTAMP,
    situacao_orgao_sigla  VARCHAR(50),
    tramitacao_descricao  VARCHAR(255),
    detalhe_carregado     BOOLEAN NOT NULL DEFAULT FALSE,
    atualizado_em         TIMESTAMP NOT NULL,
    -- Identificador que o cidadão usa: "PL 1234/2026".
    CONSTRAINT uk_proposicao_identificacao UNIQUE (sigla_tipo, numero, ano)
);
CREATE INDEX idx_proposicao_ano      ON proposicao (ano);
CREATE INDEX idx_proposicao_tipo     ON proposicao (sigla_tipo);
CREATE INDEX idx_proposicao_pendente ON proposicao (detalhe_carregado) WHERE detalhe_carregado = FALSE;

CREATE TABLE proposicao_autor (
    id                BIGSERIAL PRIMARY KEY,
    proposicao_id     BIGINT NOT NULL REFERENCES proposicao (id),
    deputado_id       BIGINT REFERENCES deputado (id),   -- null quando o autor não é deputado
    nome              VARCHAR(255) NOT NULL,
    tipo              VARCHAR(100),
    cod_tipo          INTEGER,
    ordem_assinatura  INTEGER,
    proponente        INTEGER,
    -- NULL não conflita com NULL: o enriquecimento apaga e regrava os autores da proposição.
    CONSTRAINT uk_autor_deputado UNIQUE (proposicao_id, deputado_id)
);
CREATE INDEX idx_autor_proposicao ON proposicao_autor (proposicao_id);
CREATE INDEX idx_autor_deputado   ON proposicao_autor (deputado_id);

CREATE TABLE ingestao_log (
    id             BIGSERIAL PRIMARY KEY,
    recurso        VARCHAR(50) NOT NULL,   -- partidos | deputados | proposicoes | enriquecimento
    referencia     VARCHAR(100),           -- id do deputado, id da proposição, etc.
    status         VARCHAR(20) NOT NULL,   -- SUCESSO | FALHA | PARCIAL
    registros      INTEGER,
    mensagem_erro  TEXT,
    iniciado_em    TIMESTAMP NOT NULL,
    finalizado_em  TIMESTAMP
);
