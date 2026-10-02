# Deploy e operação

Guia operacional: deploy gratuito, ingestão agendada, configuração e problemas comuns.
Para a visão geral do projeto, veja o [README](README.md).

## Arquitetura de produção

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

**Cold start:** depois de dormir, a primeira visita espera o Render religar a instância (~1 min) e a aplicação subir (~50 s) — cerca de 1 min 45 s; o frontend avisa na tela. A imagem já vem ajustada para isso: JVM para pouca memória/CPU e CDS (Class Data Sharing) gravado no build. Medido com `docker run --memory=512m --cpus=0.1`: partida de 159 s → 47 s, memória de 310 → 214 MB.

## Passo a passo

A ordem importa: a API precisa do banco, o frontend precisa da API, e a API precisa da URL do frontend (CORS).

**1. Banco (Neon).** Crie um projeto numa região do leste dos EUA, perto do Render (o [`render.yaml`](render.yaml) usa `region: virginia`; o ideal é **AWS US East (N. Virginia)**, mas Ohio também serve — custa ~10–15 ms a mais por consulta). Em *Connect*, desligue *Connection pooling* (o Flyway precisa da conexão direta). A string `postgresql://USUARIO:SENHA@HOST/neondb?sslmode=require` vira três variáveis:

| Variável | Valor |
|---|---|
| `DB_URL` | `jdbc:postgresql://HOST/neondb?sslmode=require` (com `jdbc:`, **sem** `USUARIO:SENHA@` e sem `channel_binding`) |
| `DB_USER` | `USUARIO` |
| `DB_PASSWORD` | `SENHA` |

**2. Primeira carga (antes do Render).** São ~23 mil proposições. Na instância gratuita (0,1 CPU) e com o limite de 2 h do Actions, isso levaria várias execuções — é mais rápido carregar **localmente** e copiar a base pronta para o Neon. Feito antes do primeiro deploy, o Flyway só valida o schema que já está lá:

```bash
# Carga completa na base local (~2 h; a API local precisa de uma ADMIN_API_KEY)
API_URL=http://localhost:8080 ADMIN_API_KEY=... LOTES=400 node .github/scripts/ingestao.mjs

# Cópia para o Neon, de dentro do container do Postgres (sem arquivo intermediário)
docker exec -e NEON_URL="postgresql://USUARIO:SENHA@HOST/neondb?sslmode=require" parlamento-postgres \
  sh -c 'pg_dump -U parlamento -d parlamento --no-owner --no-privileges | psql "$NEON_URL" -v ON_ERROR_STOP=1 -q'
```

Alternativa sem carga local: pule este passo (o Flyway cria as tabelas vazias) e, depois do passo 5, rode *Actions → Ingestão agendada → Run workflow* com a carga base marcada e o máximo de lotes; se passar das 2 h, uma nova execução (sem a carga base) continua de onde parou.

**3. API (Render).** *New → Blueprint* e escolha este repositório — o [`render.yaml`](render.yaml) define o serviço. Preencha `DB_URL`, `DB_USER`, `DB_PASSWORD` e, por enquanto, `CORS_ORIGINS=http://localhost:5173`; a `ADMIN_API_KEY` é gerada pelo Render. O primeiro build leva ~10 min. Confira `https://SUA-API.onrender.com/actuator/health` e copie o valor de `ADMIN_API_KEY` em *Environment*.

**4. Frontend (Vercel).** *Add New → Project*, importe o repositório com **Root Directory** `frontend` (o preset Vite é detectado) e a variável `VITE_API_URL` = URL da API, sem barra final. O [`frontend/vercel.json`](frontend/vercel.json) faz os links diretos (ex.: `/deputados/204379`) não darem 404. Se mudar a URL da API depois, refaça o deploy: a variável é embutida no build.

**5. CORS (Render de novo).** Troque `CORS_ORIGINS` pela URL de produção da Vercel — para manter o desenvolvimento local, separe por vírgula: `https://seu-site.vercel.app,http://localhost:5173`. URLs de *preview* da Vercel não ficam liberadas.

**6. GitHub Actions.** Veja [Ingestão agendada](#ingestão-agendada).

## Ingestão

A base começa vazia. A ingestão é disparada pelos endpoints `/admin`, que exigem a chave de administração no header `X-Admin-Key` (variável `ADMIN_API_KEY` no servidor — **sem chave configurada, a administração fica bloqueada**, 403). No Swagger, use o botão **Authorize**.

```bash
# Fase A — carga base: partidos → deputados → proposições por autoria
curl -X POST -H "X-Admin-Key: $ADMIN_API_KEY" http://localhost:8080/api/v1/admin/ingestao/base

# Fase B — enriquecimento: situação, data, inteiro teor e autoria completa, em lotes
curl -X POST -H "X-Admin-Key: $ADMIN_API_KEY" "http://localhost:8080/api/v1/admin/ingestao/enriquecimento?limite=500"
```

As duas fases são **idempotentes** (podem rodar de novo sem duplicar dados) e a Fase B é **retomável**: o que falha continua na fila. Cada unidade processada gera uma linha em `ingestao_log` (`SUCESSO`, `PARCIAL` ou `FALHA`).

### Ingestão agendada

Em hospedagem gratuita a API dorme quando ociosa, e um `@Scheduled` dentro dela nunca dispararia. Quem agenda é o GitHub: o workflow [`ingestao.yml`](.github/workflows/ingestao.yml) roda **todo dia às 03:00 (Brasília)**, acorda a API pelo health check e executa a carga base seguida de até 40 lotes de enriquecimento (100 proposições por lote — chamadas curtas não esbarram no limite de tempo de requisição).

Cada lote processa primeiro as proposições **nunca enriquecidas** e usa as vagas restantes para **revalidar** as que têm situação com mais de 7 dias (`parlamento.ingestao.validade-situacao`), da mais antiga para a mais recente. Em regime normal: as proposições novas do dia e ~4 mil revalidações por noite — ciclo completo da base em ~7 dias.

Para ativar, em **Settings → Secrets and variables → Actions**:

| Tipo | Nome | Valor |
|---|---|---|
| Variable | `API_URL` | URL pública da API, ex.: `https://sua-api.onrender.com` |
| Secret | `ADMIN_API_KEY` | A mesma chave configurada no servidor |

Enquanto `API_URL` não existir, o job é pulado. Dá para rodar sob demanda em **Actions → Ingestão agendada → Run workflow**. A lógica fica em [`.github/scripts/ingestao.mjs`](.github/scripts/ingestao.mjs) (Node, sem dependências) e roda igual localmente:

```bash
API_URL=http://localhost:8080 ADMIN_API_KEY=... CARGA_BASE=false LOTES=5 node .github/scripts/ingestao.mjs
```

> O GitHub desativa workflows agendados após 60 dias sem atividade no repositório; um commit ou uma execução manual reativa.

## Configuração

No `docker compose`, via `.env` (veja `.env.example`):

| Variável | Padrão | |
|---|---|---|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | `parlamento` | Banco, usuário e senha (repassados à API) |
| `POSTGRES_PORT` | `5433` | Porta do PostgreSQL no host |
| `API_PORT` | `8080` | Porta da API no host |
| `CAMARA_API_URL` | `https://dadosabertos.camara.leg.br/api/v2` | Base da API da Câmara |
| `ADMIN_API_KEY` | *(vazia = admin bloqueado)* | Chave do header `X-Admin-Key`. Gere com `openssl rand -hex 32` |
| `CORS_ORIGINS` | `http://localhost:5173` | Origens do frontend autorizadas, separadas por vírgula |

Fora do Docker, a API lê `DB_HOST` (`localhost`), `DB_PORT` (`5433`), `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT` (`8080`), `CAMARA_API_URL`, `ADMIN_API_KEY` e `CORS_ORIGINS`. Em hospedagem, **`DB_URL`** (URL JDBC completa) tem prioridade sobre host/porta/nome, e **`PORT`** (definida pela plataforma) sobre `SERVER_PORT`. O health check fica em `/actuator/health`.

O recorte de dados fica em `application.yml` — ampliá-lo é só mudar esses valores, e a documentação do Swagger acompanha:

```yaml
parlamento:
  ingestao:
    legislatura: 57
    data-apresentacao-inicio: 2023-02-01   # início da legislatura 57
    tipos-proposicao: [PL, PEC, PLP, PDL]
    validade-situacao: 7d
```

## Problemas comuns

| Sintoma | Causa provável |
|---|---|
| Site mostra "Não foi possível conectar à API" | `CORS_ORIGINS` sem a URL exata da Vercel (atenção a `https` e barra final) ou `VITE_API_URL` errada |
| Deploy no Render falha ao conectar no banco | `DB_URL` sem `jdbc:`, com usuário/senha embutidos ou com o host do *pooler* |
| Workflow falha com 401 | `ADMIN_API_KEY` do GitHub diferente da do Render |
| Workflow aparece como *skipped* | `API_URL` não criada em *Variables* (ou criada em *Secrets*) |
