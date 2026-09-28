# Dados Parlamentares

API REST pública que responde três perguntas sobre a Câmara dos Deputados:

1. Quem são os deputados federais e de que partido são?
2. Quais proposições cada deputado apresentou?
3. O que é cada proposição e em que situação ela está?

Os dados vêm da [API de Dados Abertos da Câmara dos Deputados](https://dadosabertos.camara.leg.br), são importados para um PostgreSQL próprio e servidos por esta API.

```text
API da Câmara → Ingestão → PostgreSQL → API REST própria → Cidadão
```

**Não é ferramenta de avaliação política.** Não há ranking, nota ou classificação: a API entrega o dado, a interpretação é de quem consulta.

## Princípios

- **Fonte oficial.** Nenhum dado é inventado, inferido ou completado.
- **Procedência explícita.** Toda resposta traz um bloco `_fonte` com a origem, a URI oficial do recurso na Câmara e quando ele foi atualizado na nossa base.
- **Ausência de dado é informação.** O que a fonte não publica aparece como `null` — nunca é omitido nem preenchido com zero.

## Stack

| | |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1 (Spring Framework 7, Jackson 3) |
| Persistência | Spring Data JPA, PostgreSQL 16, Flyway |
| Cliente HTTP | `RestClient` com timeout e retry com backoff exponencial (`RetryTemplate` do Spring 7) |
| Documentação | springdoc-openapi 3.1 (Swagger UI) |
| Testes | JUnit, AssertJ, MockMvc, Testcontainers, WireMock |

## Como rodar

**Pré-requisitos:** JDK 21 e Docker.

```bash
# 1. Sobe o PostgreSQL (porta 5433 no host)
docker compose up -d

# 2. Sobe a API (Flyway cria o schema no boot)
./mvnw spring-boot:run          # Linux/macOS
mvnw.cmd spring-boot:run        # Windows
```

A API fica em `http://localhost:8080` e a documentação interativa em **http://localhost:8080/swagger-ui.html**.

> A porta do Postgres no host é **5433** para não colidir com um PostgreSQL instalado localmente na 5432.
> Para mudar, copie `.env.example` para `.env` e ajuste `POSTGRES_PORT` (e `DB_PORT` ao rodar a API).

### Carregando os dados

A base começa vazia. A ingestão é disparada manualmente e roda em duas fases:

```bash
# Fase A — carga base: partidos → deputados → proposições por autoria (~3–4 min)
curl -X POST http://localhost:8080/api/v1/admin/ingestao/base

# Fase B — enriquecimento: situação, data, inteiro teor e autoria completa, em lotes
curl -X POST "http://localhost:8080/api/v1/admin/ingestao/enriquecimento?limite=500"
```

Ao fim da Fase A a API já responde tudo o que promete. A Fase B melhora as proposições aos poucos: cada lote de 500 leva ~2 min, e a resposta informa quantas ainda estão pendentes (`proposicoesPendentes`). Ela é **retomável** — o que falhar continua pendente para o próximo lote — e as duas fases são **idempotentes**: podem ser executadas de novo sem duplicar dados.

Cada unidade processada (um deputado, uma proposição) gera uma linha na tabela `ingestao_log` com status `SUCESSO`, `PARCIAL` ou `FALHA`.

### Configuração

| Variável | Padrão | |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5433` / `parlamento` | Conexão com o banco |
| `DB_USER` / `DB_PASSWORD` | `parlamento` / `parlamento` | |
| `SERVER_PORT` | `8080` | Porta da API |
| `CAMARA_API_URL` | `https://dadosabertos.camara.leg.br/api/v2` | Base da API da Câmara |

O recorte de dados fica em `application.yml`:

```yaml
parlamento:
  ingestao:
    legislatura: 57
    data-apresentacao-inicio: 2025-01-01
    tipos-proposicao: [PL, PEC, PLP, PDL]
```

O recorte existe para a primeira carga terminar em minutos. Ampliá-lo é só mudar esses valores — a documentação do Swagger acompanha automaticamente.

## Endpoints

Prefixo `/api/v1`. Nomenclatura em português: *proposição* e *ementa* não têm tradução limpa.

| Método | Rota | |
|---|---|---|
| GET | `/deputados?uf=&partido=&nome=` | Lista deputados (partido pela **sigla**) |
| GET | `/deputados/{id}` | Detalhe, com `totalProposicoes` |
| GET | `/deputados/{id}/proposicoes?ano=&tipo=` | Proposições de autoria do deputado |
| GET | `/partidos` | Lista partidos |
| GET | `/partidos/{id}` | Detalhe do partido |
| GET | `/partidos/{id}/deputados` | Deputados do partido |
| GET | `/proposicoes?ano=&tipo=&numero=&ementa=` | Busca proposições |
| GET | `/proposicoes/{id}` | Detalhe, com a situação atual |
| GET | `/proposicoes/{id}/autores` | Autoria, na ordem de assinatura |
| POST | `/admin/ingestao/base` | Fase A da ingestão |
| POST | `/admin/ingestao/enriquecimento?limite=` | Fase B da ingestão |

- **Paginação:** `page` começa em 0; `size` padrão 20, máximo 100; `sort=campo,asc|desc`.
- **Filtros de texto** (`nome`, `ementa`) não diferenciam maiúsculas e tratam `%` e `_` como texto literal.

### Exemplos

Localizar a proposição "PL 387/2025":

```bash
curl "http://localhost:8080/api/v1/proposicoes?tipo=PL&numero=387&ano=2025"
```

Deputados do PT em Minas Gerais:

```bash
curl "http://localhost:8080/api/v1/deputados?uf=MG&partido=PT"
```

Resposta paginada:

```json
{
  "conteudo": [
    {
      "id": 204379,
      "nome": "Acácio Favacho",
      "siglaUf": "AP",
      "siglaPartido": "MDB",
      "partido": { "id": 36899, "sigla": "MDB", "nome": "Movimento Democrático Brasileiro" },
      "situacao": "Exercício",
      "urlFoto": "https://www.camara.leg.br/internet/deputado/bandep/204379.jpg",
      "_fonte": {
        "origem": "Câmara dos Deputados - Dados Abertos",
        "uri": "https://dadosabertos.camara.leg.br/api/v2/deputados/204379",
        "atualizadoEm": "2026-09-27T19:10:22.726105"
      }
    }
  ],
  "pagina": 0,
  "tamanho": 20,
  "totalElementos": 648,
  "totalPaginas": 33,
  "_fonte": { "origem": "Câmara dos Deputados - Dados Abertos", "atualizadoEm": "2026-09-27T19:10:45.253844" }
}
```

Erros seguem um formato único:

```json
{
  "timestamp": "2026-09-27T20:21:35.689-03:00",
  "status": 404,
  "erro": "Não encontrado",
  "mensagem": "Deputado 999999 não encontrado",
  "caminho": "/api/v1/deputados/999999"
}
```

| Situação | Status |
|---|---|
| Recurso não encontrado | 404 |
| Parâmetro inválido, tipo errado, ordenação por campo inexistente | 400 |
| Método não suportado | 405 |
| API da Câmara indisponível durante a ingestão | 502 |
| Erro interno (sem stack trace na resposta) | 500 |

### Dois campos que pedem atenção

**`detalheCarregado`** — a Fase A importa só a identificação e a ementa das proposições. Enquanto `detalheCarregado = false`, situação, data de apresentação, inteiro teor e autoria completa (coautores, autores que não são deputados, ordem de assinatura) **ainda não foram carregados** e aparecem como `null`.

**`totalProposicoes`** — contagem das proposições de autoria do deputado **que estão na nossa base**, dentro do recorte importado. **Não é métrica de produtividade parlamentar** e não deve ser usada para comparar deputados.

## Comportamentos da fonte (e como foram tratados)

A API da Câmara tem armadilhas que só aparecem com dados reais. As principais:

| Comportamento | Tratamento |
|---|---|
| O filtro `dataInicio` de `/proposicoes` é por data de **tramitação**, e traz proposições de anos anteriores que só tramitaram no período | A ingestão usa `dataApresentacaoInicio` |
| `/partidos` sem filtro devolve só os partidos atuais, omitindo extintos/incorporados que existiram na legislatura | Partidos importados com `idLegislatura` |
| A lista `/deputados?idLegislatura=` repete o deputado uma vez por partido pelo qual passou (879 registros para 648 deputados na legislatura 57) | O registro do partido atual é escolhido pela sigla de `ultimoStatus` do detalhe |
| No detalhe do deputado, `ultimoStatus.uriPartido` vem `null` mesmo com a sigla preenchida | O vínculo com o partido vem sempre da lista; o detalhe só enriquece |
| Siglas de partido se repetem entre partidos diferentes ao longo do tempo | A chave é o id oficial; `sigla` é só coluna indexada |
| Autores de proposição não têm campo `id`, e nem sempre são deputados (Executivo, Senado, comissões) | Id extraído do final da URI; autor não-deputado gravado com `deputado_id = null`. Nunca se cria deputado a partir de dado de autoria |
| A API pode responder 429 sob carga | Retry com backoff exponencial em 429, 5xx e falhas de rede |

O CPF publicado no detalhe do deputado **não é armazenado**: é dado pessoal sem função no produto.

## Estrutura

```text
src/main/java/com/example/parlamento
├── ingestao
│   ├── client     # RestClient da API da Câmara (retry, paginação)
│   ├── dto        # formato EXTERNO — espelha o JSON da fonte
│   ├── mapper     # DTO externo → entidade
│   └── service    # orquestração das fases A e B
├── domain
│   ├── entity
│   └── repository # inclui Specifications dos filtros
├── api
│   ├── controller
│   ├── dto        # formato da NOSSA API
│   ├── mapper
│   └── service
├── exception      # StandardError e @RestControllerAdvice
└── config
```

Os DTOs da ingestão e os da API pública são conjuntos separados: se a Câmara mudar um campo, o contrato desta API não quebra junto.

## Testes

```bash
./mvnw test
```

Requer Docker em execução (Testcontainers sobe um PostgreSQL real). São três camadas:

- **Unitários** — mappers com os casos nulos da fonte, extração de id de URI, datas tolerantes, critério de retry.
- **Ingestão** — API da Câmara simulada com WireMock: deputado com `uriPartido` nulo, autor não-deputado sem URI, autor apontando para deputado fora da base, mesma proposição para dois deputados, paginação com várias páginas e 429 com retry.
- **API** — controllers com MockMvc sobre PostgreSQL: formato das respostas, filtros, paginação, erros e documentação OpenAPI.

## Fora do escopo

Votações, órgãos e comissões, histórico de tramitação, despesas, Senado Federal, estatísticas calculadas, autenticação e frontend. Votações são a evolução natural: o modelo já tem deputado e proposição.

## Licença

[MIT](LICENSE)
