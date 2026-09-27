# Plataforma de Dados Parlamentares — MVP

> Versão 3. Escopo reduzido: **deputados, partidos e proposições**.
> Votações e votos ficam fora desta versão.
> Este documento é a fonte de verdade para a implementação.

---

## 1. O que o sistema faz

Uma API REST pública que responde três perguntas:

1. Quem são os deputados federais e de que partido são?
2. Quais proposições cada deputado apresentou?
3. O que é cada proposição e em que situação ela está?

Os dados vêm da API de Dados Abertos da Câmara dos Deputados, são importados para um PostgreSQL próprio e servidos pela nossa API.

```text
API da Câmara → Ingestão → PostgreSQL → API REST própria → Cidadão
```

**Não** é ferramenta de avaliação política. Sem ranking, nota ou classificação. A API entrega o dado; a interpretação é do cidadão.

## 2. Objetivos técnicos

Java, Spring Boot, Spring Data JPA, PostgreSQL, Maven, REST, Docker, integração com API externa, paginação, filtros, tratamento de erros, testes, OpenAPI.

## 3. Princípios

**Fonte oficial.** Nenhum dado é inventado, inferido ou completado.

**Separar dado de interpretação.** Dado da fonte e dado normalizado são armazenados; informação calculada (percentuais, médias, comparações) fica fora do MVP. A única contagem exposta é `totalProposicoes`, e ela é apresentada como contagem do que está na base, não como métrica de produtividade.

**Ausência de dado é informação.** Quando a fonte não publica algo, a API diz isso — nunca preenche com zero nem omite o campo.

Todo DTO de resposta carrega a procedência:

```json
"_fonte": {
  "origem": "Câmara dos Deputados - Dados Abertos",
  "uri": "https://dadosabertos.camara.leg.br/api/v2/deputados/204379",
  "atualizadoEm": "2026-09-27T18:00:00"
}
```

---

## 4. Stack

| Item | Versão | Observação |
|---|---|---|
| Java | 21 | baseline do Boot 4 é 17; fixar 21 |
| Spring Boot | **4.1.x** | Boot 3.5 chegou a EOL open-source em 30/06/2026 |
| Spring Framework | 7.0.x | vem junto |
| PostgreSQL | 16+ | |
| Flyway | — | migrations com o DDL da seção 7 |
| springdoc-openapi | **3.1.x** | linha 3.x é a compatível com Boot 4; a 2.8.x **não sobe** |
| Testcontainers | — | testes de integração |

⚠ **Spring Boot 4 usa Jackson 3.** `databind` e `core` mudaram de pacote (`com.fasterxml.jackson.*` → `tools.jackson.*`). Praticamente todo exemplo de Spring na internet é Jackson 2 — **não assumir o import por reflexo, conferir na doc de migração do Boot 4.**

---

## 5. Fonte de dados

Base: `https://dadosabertos.camara.leg.br/api/v2` — sem autenticação.
Envelope de toda resposta: `{ "dados": ..., "links": [...] }`.
Paginação: `?pagina=1&itens=100` — **começa em 1**, máximo 100 itens.
Pode retornar **429** sob carga.

### Endpoints consumidos

| Endpoint | Uso |
|---|---|
| `GET /partidos?itens=100` | lista de partidos |
| `GET /deputados?idLegislatura=57&itens=100` | lista de deputados |
| `GET /deputados/{id}` | detalhe do deputado |
| `GET /proposicoes?idDeputadoAutor={id}&dataInicio={d}&itens=100` | **proposições de autoria de um deputado** |
| `GET /proposicoes/{id}` | detalhe (situação, inteiro teor) — fase de enriquecimento |
| `GET /proposicoes/{id}/autores` | autoria completa — fase de enriquecimento |

`idDeputadoAutor` é o parâmetro correto (o antigo `idAutor` foi substituído). É ele que torna a ingestão viável: 513 deputados × poucas páginas, em vez de varrer o acervo inteiro de proposições e chamar `/autores` uma por uma.

### Recorte do MVP

- Legislatura **57**
- Proposições com `dataInicio=2025-01-01`
- Tipos: **PL, PEC, PLP, PDL** (definidos em configuração, não hardcoded)

O recorte existe para a primeira carga terminar em minutos, não em horas. Ampliar depois é mudar um `application.yml`.

---

## 6. Contratos reais da fonte

Payloads verificados. As armadilhas estão marcadas com ⚠.

### 6.1 Deputado — lista (`/deputados`)

```json
{"id":204379,"uri":"...","nome":"Acácio Favacho","siglaPartido":"MDB",
 "uriPartido":"https://.../partidos/36899","siglaUf":"AP","idLegislatura":57,
 "urlFoto":"...","email":"dep.acaciofavacho@camara.leg.br"}
```

### 6.2 Deputado — detalhe (`/deputados/{id}`)

```json
{"id":92776,"nomeCivil":"STEFANO AGUIAR DOS SANTOS",
 "ultimoStatus":{"nome":"Stefano Aguiar","siglaPartido":"PSD","uriPartido":null,
   "siglaUf":"MG","idLegislatura":57,"urlFoto":"...","email":null,
   "situacao":"Exercício","condicaoEleitoral":"Titular"},
 "cpf":"...","sexo":"M","dataNascimento":"1976-03-03","dataFalecimento":null,
 "ufNascimento":"MG","municipioNascimento":"Belo Horizonte","escolaridade":"Superior"}
```

⚠ `ultimoStatus.uriPartido` vem **null** mesmo com `siglaPartido` preenchida.
→ O vínculo com Partido é resolvido pela **lista**, nunca pelo detalhe. O detalhe só enriquece.
⚠ `cpf` **não é armazenado** — é PII sem função no produto.

### 6.3 Partido (`/partidos`)

`id` (ex.: 36899), `sigla`, `nome`, `uri`.

⚠ Siglas se repetem entre partidos diferentes ao longo do tempo. **PK é o `id` numérico.** `sigla` é coluna indexada, jamais chave.

### 6.4 Proposição — lista (`/proposicoes`)

A lista é enxuta: `id`, `uri`, `siglaTipo`, `codTipo`, `numero`, `ano`, `ementa`.
Não traz situação nem data de apresentação — por isso existe a fase de enriquecimento (seção 8).

### 6.5 Proposição — detalhe (`/proposicoes/{id}`)

```json
{"id":2277677,"siglaTipo":"REQ","codTipo":147,"numero":16,"ano":2021,
 "ementa":"...","dataApresentacao":"2021-04-09T18:16",
 "descricaoTipo":"Requerimento","urlInteiroTeor":"https://...",
 "statusProposicao":{"dataHora":"2021-04-27T00:00","siglaOrgao":"CSPCCO",
   "descricaoTramitacao":"Arquivamento","descricaoSituacao":"Arquivada",
   "codSituacao":923,"despacho":"Arquivada","ambito":"Regimental"}}
```

⚠ `statusProposicao` é **achatado em colunas**. O MVP mostra a situação atual, não o histórico de tramitação.
⚠ O identificador que o cidadão usa é `siglaTipo + numero + ano` ("PL 1234/2026"), não o `id`. Precisa de índice único e de busca por essa tripla — ninguém procura por `2277677`.

### 6.6 Autores (`/proposicoes/{id}/autores`)

```json
[{"uri":".../deputados/204531","nome":"Guilherme Derrite",
  "codTipo":10000,"tipo":"Deputado(a)","ordemAssinatura":1,"proponente":1}]
```

⚠ **Não existe campo `id`** — o id é extraído do final da URI.
⚠ O autor nem sempre é deputado: pode ser Poder Executivo, Senado, comissão, órgão. Nesses casos a URI pode vir null.
→ `deputado_id` é FK **nullable**, preenchida só quando `tipo` indica deputado e a URI resolve. `nome` e `tipo` são sempre gravados.

---

## 7. Modelo de dados

```text
Partido 1 ─────── N Deputado
Deputado 1 ────── N ProposicaoAutor ────── N 1 Proposicao
```

Três entidades e uma tabela associativa. A relação deputado↔proposição é **N:N** — uma proposição pode ter vários autores, e um deputado assina várias.

```sql
CREATE TABLE partido (
    id             BIGINT PRIMARY KEY,          -- id oficial da Câmara
    sigla          VARCHAR(50)  NOT NULL,
    nome           VARCHAR(255) NOT NULL,
    uri            VARCHAR(255),
    atualizado_em  TIMESTAMP    NOT NULL
);
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
    CONSTRAINT uk_proposicao_identificacao UNIQUE (sigla_tipo, numero, ano)
);
CREATE INDEX idx_proposicao_ano     ON proposicao (ano);
CREATE INDEX idx_proposicao_tipo    ON proposicao (sigla_tipo);
CREATE INDEX idx_proposicao_pendente ON proposicao (detalhe_carregado) WHERE detalhe_carregado = FALSE;

CREATE TABLE proposicao_autor (
    id                BIGSERIAL PRIMARY KEY,
    proposicao_id     BIGINT NOT NULL REFERENCES proposicao (id),
    deputado_id       BIGINT REFERENCES deputado (id),   -- null quando não é deputado
    nome              VARCHAR(255) NOT NULL,
    tipo              VARCHAR(100),
    cod_tipo          INTEGER,
    ordem_assinatura  INTEGER,
    proponente        INTEGER,
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
```

**Regra de nullability:** só é `NOT NULL` o que a fonte garante — identificadores e os campos marcados acima. Toda coluna descritiva é nullable. Falha de ingestão por `NOT NULL` indevido é o erro mais comum neste tipo de projeto.

⚠ A constraint `uk_autor_deputado` com `deputado_id` nullable: no PostgreSQL, `NULL` não conflita com `NULL` em unique. Autores não-deputados podem duplicar se a mesma proposição for enriquecida duas vezes — o passo de enriquecimento deve **apagar os autores da proposição e regravar**, não fazer upsert linha a linha.

---

## 8. Ingestão

### Fase A — carga base (entrega o produto)

```text
1. GET /partidos                          → grava partido
2. GET /deputados?idLegislatura=57        → grava deputado + vínculo com partido
3. GET /deputados/{id} para cada deputado → enriquece (nome civil, nascimento, escolaridade)
4. Para cada deputado:
     GET /proposicoes?idDeputadoAutor={id}&dataInicio=2025-01-01&itens=100
     → grava proposicao (campos da lista, detalhe_carregado = false)
     → grava proposicao_autor (deputado_id conhecido, ordem/proponente null)
```

Ao fim da Fase A a API já responde tudo o que a seção 1 promete. Volume: ~513 deputados, poucas centenas de requisições.

### Fase B — enriquecimento (melhora, não bloqueia)

```text
Para cada proposicao WHERE detalhe_carregado = false:
     GET /proposicoes/{id}           → situação, data de apresentação, inteiro teor
     GET /proposicoes/{id}/autores   → autoria completa (coautores, não-deputados)
     → detalhe_carregado = true
```

Rodável em lotes, retomável, interrompível. Uma proposição que falha não afeta as outras.

### Regras

- **Upsert por id oficial**, sempre. `findById` + update, ou `ON CONFLICT DO UPDATE`. Nunca `deleteAll` + reinsert.
- `atualizado_em` preenchido em toda gravação.
- Cada deputado (Fase A) e cada proposição (Fase B) em **transação própria**, com linha em `ingestao_log`. Falha em um não aborta o lote.
- `RestClient` com timeout explícito e retry com backoff exponencial em **429 e 5xx**.
- Autor com URI nula ou `tipo` não-deputado: grava com `deputado_id = null`. Nunca criar deputado a partir de dado de autoria.
- Deputado referenciado em autoria que não existe na base: grava com `deputado_id = null` e loga. Acontece com ex-deputados de legislaturas anteriores.

### Disparo

```http
POST /api/v1/admin/ingestao/base
POST /api/v1/admin/ingestao/enriquecimento?limite=500
```

Síncrono, sem autenticação no MVP. `@Scheduled` fica para depois.

---

## 9. API pública

Prefixo `/api/v1`. Nomenclatura em **português** — `proposição` e `ementa` não têm tradução limpa, e traduzir cria ambiguidade permanente entre a fonte e o domínio.

```http
GET /api/v1/deputados?uf=MG&partido=PT&nome=silva&page=0&size=20
GET /api/v1/deputados/{id}
GET /api/v1/deputados/{id}/proposicoes?ano=&tipo=&page=&size=

GET /api/v1/partidos?page=&size=
GET /api/v1/partidos/{id}
GET /api/v1/partidos/{id}/deputados?page=&size=

GET /api/v1/proposicoes?ano=2026&tipo=PL&numero=1234&ementa=saude&page=&size=
GET /api/v1/proposicoes/{id}
GET /api/v1/proposicoes/{id}/autores
```

- Paginação Spring padrão: `page` começa em **0**, `size` default 20, máximo 100.
- Filtro `partido` aceita a **sigla** (é o que o usuário conhece), resolvida internamente para `partido_id`.
- Filtro `nome` e `ementa`: `ILIKE %valor%`. Busca textual de verdade fica para depois.
- `GET /deputados/{id}` retorna também `totalProposicoes` — contagem simples na base, rotulada como tal.
- Proposição com `detalheCarregado = false` expõe o campo, para o consumidor saber que situação e data ainda não foram carregadas.

### Resposta paginada

```json
{
  "conteudo": [ ... ],
  "pagina": 0,
  "tamanho": 20,
  "totalElementos": 513,
  "totalPaginas": 26,
  "_fonte": { "origem": "Câmara dos Deputados - Dados Abertos", "atualizadoEm": "..." }
}
```

---

## 10. Estrutura do projeto

```text
src/main/java/com/example/parlamento
├── ingestao
│   ├── client        # RestClient para a API da Câmara
│   ├── dto           # DTOs do formato EXTERNO (espelham o JSON da fonte)
│   ├── mapper        # DTO externo → Entity
│   └── service       # orquestração das fases A e B
├── domain
│   ├── entity
│   └── repository
├── api
│   ├── controller
│   ├── dto           # DTOs da NOSSA API
│   └── mapper
├── exception
└── config
```

**Os DTOs da ingestão e os da API pública são conjuntos separados.** Se a Câmara mudar um campo, o contrato da nossa API não quebra junto. Não reutilizar.

---

## 11. Tratamento de erros

```json
{ "timestamp": "...", "status": 404, "erro": "Não encontrado",
  "mensagem": "Deputado 999999 não encontrado", "caminho": "/api/v1/deputados/999999" }
```

`@RestControllerAdvice` cobrindo:

| Situação | Status |
|---|---|
| Recurso não encontrado | 404 |
| Parâmetro inválido / tipo errado no path | 400 |
| Falha de validação | 400 |
| Fonte externa indisponível durante ingestão | 502 |
| Erro interno | 500 |

---

## 12. Testes

**Unitários** — mappers (com os casos nulos da seção 6), extração de id a partir de URI, resolução de autor não-deputado.

**Integração** — controllers com MockMvc + Testcontainers (PostgreSQL).

**Ingestão** — cliente da Câmara com WireMock, cobrindo obrigatoriamente:

- deputado com `uriPartido` null no detalhe
- autor sem `uri`, com `tipo` diferente de deputado
- autor apontando para deputado inexistente na base
- proposição retornada para dois deputados diferentes (não pode duplicar)
- resposta paginada com mais de uma página
- 429 da fonte, com retry

São os cinco casos que quebram ingestão na vida real, mais a paginação. São os testes que importam.

---

## 13. Documentação

Springdoc em `/swagger-ui.html`. Deve descrever endpoints, parâmetros, paginação, filtros, códigos HTTP e modelos, e deixar explícito:

- que a origem dos dados é a API pública da Câmara dos Deputados;
- o recorte de dados vigente (legislatura, período, tipos de proposição);
- o significado de `detalheCarregado`;
- que `totalProposicoes` é contagem do que está na base, não métrica de produtividade parlamentar.

---

## 14. Docker

```text
docker-compose.yml
├── api       (Spring Boot)
└── postgres  (com volume nomeado)
```

Configuração por variável de ambiente. Flyway roda as migrations no boot.

---

## 15. Checklist

```text
[ ] docker-compose com Postgres subindo
[ ] Projeto Spring Boot 4.1 + Flyway com o DDL da seção 7
[ ] Entidades e repositories
[ ] RestClient da Câmara com timeout, retry e paginação
[ ] Ingestão: partidos
[ ] Ingestão: deputados (vínculo de partido pela LISTA) + detalhe
[ ] GET /partidos e /deputados funcionando ponta a ponta
[ ] Ingestão Fase A: proposições por idDeputadoAutor + vínculo de autoria
[ ] GET /deputados/{id}/proposicoes funcionando
[ ] Ingestão Fase B: enriquecimento (detalhe + autores)
[ ] Endpoints restantes da seção 9
[ ] Paginação e filtros
[ ] StandardError e @RestControllerAdvice
[ ] Testes da seção 12
[ ] OpenAPI/Swagger
[ ] README
[ ] Dockerizar
```

Os sete primeiros itens já entregam um sistema real rodando. É a partir daí que o resto fica concreto.

---

## 16. Fora do escopo

Votações, votos individuais, orientações de bancada, órgãos/comissões, eventos, histórico de tramitações, discursos, despesas (CEAP), frentes parlamentares, presença, temas, blocos partidários, histórico de troca de partido, Senado Federal, estatísticas calculadas, ranking, notas, frontend, cache, Elasticsearch, filas, microserviços, autenticação.

**Votações são a evolução natural** depois que este MVP estiver rodando: o modelo já tem deputado e proposição, e basta acrescentar as entidades de votação. Fica registrado que, quando chegar a hora, `/votacoes/{id}/votos` não publica votos individuais de votações recentes — e que o id de votação é String alfanumérica (`"2277677-4"`), não número.
