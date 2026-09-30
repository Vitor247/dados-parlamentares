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

**Pré-requisito:** Docker.

```bash
docker compose up -d --build
```

Sobe o PostgreSQL e a API (a imagem é compilada na primeira vez, o que leva alguns minutos). A API espera o banco ficar saudável, e o Flyway cria o schema no boot.

A API fica em `http://localhost:8080` e a documentação interativa em **http://localhost:8080/swagger-ui.html**.

> Portas no host: API na **8080**, PostgreSQL na **5433** (para não colidir com um PostgreSQL instalado localmente na 5432).
> Para mudar, copie `.env.example` para `.env` e ajuste `API_PORT` / `POSTGRES_PORT`.

### Desenvolvimento (API fora do Docker)

Com JDK 21 instalado, suba só o banco e rode a API pelo Maven Wrapper:

```bash
docker compose up -d postgres
./mvnw spring-boot:run          # Linux/macOS
mvnw.cmd spring-boot:run        # Windows
```

### Carregando os dados

A base começa vazia. A ingestão é disparada pelos endpoints `/admin`, que exigem a chave de administração no header `X-Admin-Key`. Defina a chave em `ADMIN_API_KEY` (no `.env`, para o Docker) — **sem chave configurada, a administração fica bloqueada** (403).

```bash
# Fase A — carga base: partidos → deputados → proposições por autoria (~3–4 min)
curl -X POST -H "X-Admin-Key: $ADMIN_API_KEY" http://localhost:8080/api/v1/admin/ingestao/base

# Fase B — enriquecimento: situação, data, inteiro teor e autoria completa, em lotes
curl -X POST -H "X-Admin-Key: $ADMIN_API_KEY" "http://localhost:8080/api/v1/admin/ingestao/enriquecimento?limite=500"
```

No Swagger, use o botão **Authorize** para informar a chave.

Ao fim da Fase A a API já responde tudo o que promete. A Fase B melhora as proposições aos poucos: cada lote de 500 leva ~2 min, e a resposta informa quantas ainda estão pendentes (`proposicoesPendentes`). Ela é **retomável** — o que falhar continua pendente para o próximo lote — e as duas fases são **idempotentes**: podem ser executadas de novo sem duplicar dados.

Cada unidade processada (um deputado, uma proposição) gera uma linha na tabela `ingestao_log` com status `SUCESSO`, `PARCIAL` ou `FALHA`.

### Ingestão agendada (GitHub Actions)

Em hospedagem gratuita a API "dorme" quando fica ociosa, e um `@Scheduled` dentro dela nunca dispararia. Por isso quem agenda é o GitHub: o workflow [`ingestao.yml`](.github/workflows/ingestao.yml) roda **todo dia às 03:00 (Brasília)**, acorda a API pelo health check e executa a carga base seguida de até 40 lotes de enriquecimento (100 proposições por lote, ~30 s cada — chamadas curtas não esbarram no limite de tempo de requisição da hospedagem).

Cada lote processa primeiro as proposições **nunca enriquecidas** e usa as vagas restantes para **revalidar** as que têm situação com mais de 7 dias (`parlamento.ingestao.validade-situacao`), da mais antiga para a mais recente — a situação muda na Câmara ao longo da tramitação. Na prática:

| Momento | O que acontece |
|---|---|
| Primeira carga | ~28 mil proposições entram pendentes. Mais rápido fazê-la localmente e copiar a base pronta (veja *Deploy*); pelo agendamento, 40 lotes/noite zeram o acúmulo em ~7 dias |
| Regime normal | Cada noite enriquece as proposições novas do dia e revalida ~4 mil situações — ciclo completo da base em ~7 dias, ~20–25 min por noite |
| Base maior (recorte ampliado) | Aumente `LOTES` no workflow ou a validade da situação |

Para ativar, no repositório do GitHub em **Settings → Secrets and variables → Actions**:

| Tipo | Nome | Valor |
|---|---|---|
| Variable | `API_URL` | URL pública da API, ex.: `https://sua-api.onrender.com` |
| Secret | `ADMIN_API_KEY` | A mesma chave configurada no servidor |

Enquanto `API_URL` não existir, o job é pulado (sem falhas diárias). Também dá para rodar sob demanda em **Actions → Ingestão agendada → Run workflow**, escolhendo se roda a carga base e quantos lotes — útil para a primeira carga, com mais lotes.

A lógica fica em [`.github/scripts/ingestao.mjs`](.github/scripts/ingestao.mjs) (Node, sem dependências) e roda igual localmente:

```bash
API_URL=http://localhost:8080 ADMIN_API_KEY=... CARGA_BASE=false LOTES=5 node .github/scripts/ingestao.mjs
```

> O GitHub desativa workflows agendados de repositórios públicos após 60 dias sem atividade no repositório; um commit ou uma execução manual reativa.

### Configuração

No `docker compose`, via `.env` (veja `.env.example`):

| Variável | Padrão | |
|---|---|---|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | `parlamento` | Banco, usuário e senha (repassados à API) |
| `POSTGRES_PORT` | `5433` | Porta do PostgreSQL no host |
| `API_PORT` | `8080` | Porta da API no host |
| `CAMARA_API_URL` | `https://dadosabertos.camara.leg.br/api/v2` | Base da API da Câmara |
| `ADMIN_API_KEY` | *(vazia = admin bloqueado)* | Chave do header `X-Admin-Key`. Gere com `openssl rand -hex 32` |
| `CORS_ORIGINS` | `http://localhost:5173` | Origens do frontend autorizadas, separadas por vírgula |

Rodando a API fora do Docker, ela lê `DB_HOST` (`localhost`), `DB_PORT` (`5433`), `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT` (`8080`), `CAMARA_API_URL`, `ADMIN_API_KEY` e `CORS_ORIGINS`.

Em hospedagem, duas variáveis têm prioridade: **`DB_URL`** (URL JDBC completa, como a fornecida por bancos gerenciados, ex.: `jdbc:postgresql://host/db?sslmode=require`) sobre host/porta/nome, e **`PORT`** (definida pela plataforma) sobre `SERVER_PORT`. O health check fica em `/actuator/health`.

O recorte de dados fica em `application.yml`:

```yaml
parlamento:
  ingestao:
    legislatura: 57
    data-apresentacao-inicio: 2023-02-01   # início da legislatura 57
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
| POST | `/admin/ingestao/base` | Fase A da ingestão (header `X-Admin-Key`) |
| POST | `/admin/ingestao/enriquecimento?limite=` | Fase B da ingestão (header `X-Admin-Key`) |

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
| Endpoint `/admin` sem a chave correta | 401 |
| Endpoint `/admin` com o servidor sem `ADMIN_API_KEY` | 403 |
| Método não suportado | 405 |
| API da Câmara indisponível durante a ingestão | 502 |
| Erro interno (sem stack trace na resposta) | 500 |

### Dois campos que pedem atenção

**`detalheCarregado`** — a Fase A importa só a identificação e a ementa das proposições. Enquanto `detalheCarregado = false`, situação, data de apresentação, inteiro teor e autoria completa (coautores, autores que não são deputados, ordem de assinatura) **ainda não foram carregados** e aparecem como `null`.

**`situacao.atualizadaEm`** — a situação é uma fotografia da tramitação: este campo diz quando ela foi consultada na fonte (revalidada a cada 7 dias). Não confunda com `_fonte.atualizadoEm`, que a carga diária renova ao sincronizar identificação e ementa.

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

## Frontend

Um site simples em [`frontend/`](frontend) para consultar a API: busca de deputados (nome, UF, partido), perfil com as proposições de autoria, busca de proposições (tipo, número, ano, palavra na ementa) e detalhe com situação e autoria. Segue os mesmos princípios da API — mostra a procedência de cada dado, diz quando algo não foi informado pela fonte e não classifica nem compara parlamentares.

React 19 + TypeScript, Vite e React Router, sem biblioteca de componentes. Os filtros ficam na URL, então toda busca é um link compartilhável.

```bash
cd frontend
cp .env.example .env    # VITE_API_URL=http://localhost:8080
npm install
npm run dev             # http://localhost:5173
```

A API já libera `http://localhost:5173` no CORS por padrão.

## Deploy

Tudo roda no plano gratuito:

```text
Vercel (frontend)  →  Render (API, Docker)  →  Neon (PostgreSQL)
                            ↑
           GitHub Actions (ingestão diária, 03:00)
```

| Serviço | Limites que importam aqui |
|---|---|
| **Neon** | 0,5 GB (a base completa ocupa ~40 MB); suspende após 5 min ocioso e volta em ~1 s |
| **Render** | 512 MB de RAM e 0,1 CPU; **dorme após 15 min sem tráfego**; requisições de até 100 min. (O Postgres gratuito do Render expira em 30 dias — por isso o banco fica no Neon.) |
| **Vercel** | Site estático, sem limitação relevante |

**O que esperar:** depois de dormir, a primeira visita espera o Render religar a instância (~1 min) e a aplicação subir (~50 s) — cerca de 1 min 45 s; o frontend avisa na tela. A imagem já vem ajustada para isso: JVM para pouca memória/CPU e CDS (Class Data Sharing) gravado no build. Medido com `docker run --memory=512m --cpus=0.1`: partida de 159 s → 47 s, memória de 310 → 214 MB.

### Passo a passo

A ordem importa: a API precisa do banco, o frontend precisa da API, e a API precisa da URL do frontend (CORS).

**1. Banco (Neon).** Crie um projeto na região **AWS US East (Ohio)** — a mesma do Render no [`render.yaml`](render.yaml) (`region: ohio`); se escolher outra, ajuste as duas. Em *Connect*, desligue *Connection pooling* (o Flyway precisa da conexão direta). A string `postgresql://USUARIO:SENHA@HOST/neondb?sslmode=require` vira três variáveis:

| Variável | Valor |
|---|---|
| `DB_URL` | `jdbc:postgresql://HOST/neondb?sslmode=require` (com `jdbc:`, **sem** `USUARIO:SENHA@` e sem `channel_binding`) |
| `DB_USER` | `USUARIO` |
| `DB_PASSWORD` | `SENHA` |

As tabelas são criadas pelo Flyway na primeira subida da API.

**2. API (Render).** *New → Blueprint* e escolha este repositório — o [`render.yaml`](render.yaml) define o serviço. Preencha `DB_URL`, `DB_USER`, `DB_PASSWORD` e, por enquanto, `CORS_ORIGINS=http://localhost:5173`; a `ADMIN_API_KEY` é gerada pelo Render. O primeiro build leva ~10 min. Confira `https://SUA-API.onrender.com/actuator/health` e copie o valor de `ADMIN_API_KEY` em *Environment*.

**3. Frontend (Vercel).** *Add New → Project*, importe o repositório com **Root Directory** `frontend` (o preset Vite é detectado) e a variável `VITE_API_URL` = URL da API, sem barra final. O [`frontend/vercel.json`](frontend/vercel.json) faz os links diretos (ex.: `/deputados/204379`) não darem 404. Se mudar a URL da API depois, refaça o deploy: a variável é embutida no build.

**4. CORS (Render de novo).** Troque `CORS_ORIGINS` pela URL de produção da Vercel — para manter o desenvolvimento local, separe por vírgula: `https://seu-site.vercel.app,http://localhost:5173`. URLs de *preview* da Vercel não ficam liberadas.

**5. GitHub Actions.** Em *Settings → Secrets and variables → Actions*: variável `API_URL` (URL da API) e secret `ADMIN_API_KEY` (o valor do passo 2).

**6. Primeira carga.** São ~28 mil proposições. Na instância gratuita (0,1 CPU) e com o limite de 2 h do Actions, isso levaria várias execuções — é mais rápido carregar **localmente** e copiar a base pronta para o Neon, **antes do primeiro deploy no Render** (o Flyway então só valida o schema que já está lá):

```bash
# 1. Carga completa na base local (~2–2h30; a API local precisa de uma ADMIN_API_KEY)
API_URL=http://localhost:8080 ADMIN_API_KEY=... LOTES=400 node .github/scripts/ingestao.mjs

# 2. Cópia para o Neon, de dentro do container do Postgres (sem arquivo intermediário)
docker exec -e NEON_URL="postgresql://USUARIO:SENHA@HOST/neondb?sslmode=require" parlamento-postgres \
  sh -c 'pg_dump -U parlamento -d parlamento --no-owner --no-privileges | psql "$NEON_URL" -v ON_ERROR_STOP=1 -q'
```

Alternativa sem carga local: depois do deploy, rode *Actions → Ingestão agendada → Run workflow* com a carga base marcada e o máximo de lotes; se passar das 2 h, uma nova execução (sem a carga base) continua de onde parou.

### Problemas comuns

| Sintoma | Causa provável |
|---|---|
| Site mostra "Não foi possível conectar à API" | `CORS_ORIGINS` sem a URL exata da Vercel (atenção a `https` e barra final) ou `VITE_API_URL` errada |
| Deploy no Render falha ao conectar no banco | `DB_URL` sem `jdbc:`, com usuário/senha embutidos ou com o host do *pooler* |
| Workflow falha com 401 | `ADMIN_API_KEY` do GitHub diferente da do Render |
| Workflow aparece como *skipped* | `API_URL` não criada em *Variables* (ou criada em *Secrets*) |

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

Os testes rodam fora da imagem Docker (o build da imagem usa `-DskipTests`, porque os testes de integração precisam do próprio Docker).

Requer Docker em execução (Testcontainers sobe um PostgreSQL real). São três camadas:

- **Unitários** — mappers com os casos nulos da fonte, extração de id de URI, datas tolerantes, critério de retry.
- **Ingestão** — API da Câmara simulada com WireMock: deputado com `uriPartido` nulo, autor não-deputado sem URI, autor apontando para deputado fora da base, mesma proposição para dois deputados, paginação com várias páginas e 429 com retry.
- **API** — controllers com MockMvc sobre PostgreSQL: formato das respostas, filtros, paginação, erros e documentação OpenAPI.

## Fora do escopo

Votações, órgãos e comissões, histórico de tramitação, despesas, Senado Federal, estatísticas calculadas e autenticação de usuários. Votações são a evolução natural: o modelo já tem deputado e proposição.

## Licença

[MIT](LICENSE)
