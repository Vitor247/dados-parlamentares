# Dados Parlamentares

Plataforma que responde três perguntas sobre a Câmara dos Deputados, com dados oficiais da [API de Dados Abertos da Câmara](https://dadosabertos.camara.leg.br): **quem são os deputados federais e de que partido são, o que cada um propôs e em que situação está cada proposição**.

<!-- Links de produção: **[Site](https://...)** · **[Documentação da API (Swagger)](https://.../swagger-ui.html)** -->

**Não é ferramenta de avaliação política:** não há ranking, nota ou classificação. O sistema entrega o dado; a interpretação é de quem consulta.

## Princípios

- **Fonte oficial.** Nenhum dado é inventado, inferido ou completado.
- **Procedência explícita.** Toda resposta traz um bloco `_fonte` com a origem, a URI oficial na Câmara e quando o registro foi atualizado.
- **Ausência de dado é informação.** O que a fonte não publica aparece como `null`: nunca é omitido nem vira zero.

## Stack

| | |
|---|---|
| **Backend** | Java 21, Spring Boot 4.1 (Spring Framework 7, Jackson 3), Spring Data JPA |
| **Banco** | PostgreSQL 16, Flyway |
| **Frontend** | React 19 + TypeScript, Vite, React Router |
| **Testes** | JUnit, AssertJ, MockMvc, Testcontainers, WireMock |
| **Infra** | Docker (multi-stage, CDS), GitHub Actions, Render, Vercel, Neon |
| **Docs** | springdoc-openapi 3.1 (Swagger UI) |

## Arquitetura

```text
API da Câmara ──► Ingestão ──► PostgreSQL ──► API REST (/api/v1) ──► Frontend React
                     ▲
        GitHub Actions (todo dia, 03:00)
```

A ingestão roda em duas fases: a **carga base** importa partidos, deputados e as proposições de cada deputado; o **enriquecimento** completa cada proposição com situação, data, inteiro teor e autoria completa, e revalida periodicamente as situações, que mudam ao longo da tramitação.

### Modelo de dados

![Modelo conceitual do banco](https://raw.githubusercontent.com/Vitor247/assets/main/dados-parlamentares/model.png)

Um partido tem vários deputados. Deputados e proposições se relacionam muitos-para-muitos pela autoria, que guarda a ordem de assinatura e se o autor é proponente. O autor nem sempre é deputado (Poder Executivo, Senado, comissões), por isso o vínculo com o deputado é opcional. O log de ingestão é independente e registra cada unidade processada.

## Destaques técnicos

- **Dados reais têm armadilhas.** A API da Câmara repete o deputado uma vez por partido pelo qual passou, devolve `uriPartido` nulo no detalhe, filtra por data de *tramitação* quando parece filtrar por apresentação e tem autores sem `id` (nem sempre deputados). Cada caso foi tratado e coberto por teste.
- **Ingestão resiliente.** `RestClient` com timeout e retry com backoff exponencial em 429/5xx; cada unidade em transação própria, com log em `ingestao_log`; idempotente (upsert pelo id oficial) e retomável.
- **Atualidade explícita.** A situação de cada proposição é revalidada a cada 7 dias e a API informa quando ela foi consultada, separado da data de sincronização do resto do registro.
- **Cold start medido.** Em 512 MB e 0,1 CPU (hospedagem gratuita), JVM ajustada e CDS reduziram a partida de 159 s para 47 s e a memória de 310 para 214 MB.
- **Contratos separados.** DTOs da fonte externa e da API pública são conjuntos distintos: se a Câmara mudar um campo, o contrato desta API não quebra junto.
- **Erros padronizados.** `@RestControllerAdvice` com um formato único (`StandardError`), sem stack trace na resposta.

## Como rodar

**Pré-requisito:** Docker.

```bash
docker compose up -d --build
```

API em `http://localhost:8080` e documentação em **http://localhost:8080/swagger-ui.html**. O Flyway cria o schema no boot; para carregar os dados, veja [Ingestão](DEPLOY.md#ingestão).

Frontend:

```bash
cd frontend
cp .env.example .env    # VITE_API_URL=http://localhost:8080
npm install
npm run dev             # http://localhost:5173
```

## Endpoints

Prefixo `/api/v1`. Detalhes, parâmetros e modelos no Swagger.

| Método | Rota | |
|---|---|---|
| GET | `/deputados?uf=&partido=&nome=` | Lista deputados |
| GET | `/deputados/{id}` | Detalhe do deputado |
| GET | `/deputados/{id}/proposicoes?ano=&tipo=&situacao=` | Proposições de autoria do deputado |
| GET | `/partidos` · `/partidos/{id}` · `/partidos/{id}/deputados` | Partidos e seus deputados |
| GET | `/proposicoes?ano=&tipo=&numero=&ementa=&situacao=` | Busca proposições |
| GET | `/proposicoes/situacoes` | Situações presentes na base, com totais |
| GET | `/proposicoes/{id}` · `/proposicoes/{id}/autores` | Detalhe, situação e autoria |
| POST | `/admin/ingestao/base` · `/admin/ingestao/enriquecimento` | Ingestão (header `X-Admin-Key`) |

Paginação com `page` (a partir de 0) e `size` (padrão 20, máximo 100).

## Testes

```bash
./mvnw test    # requer Docker (Testcontainers)
```

- **Unitários:** mappers com os casos nulos da fonte, datas tolerantes, critério de retry.
- **Ingestão:** API da Câmara simulada com WireMock, cobrindo os casos que quebram ingestão na vida real, paginação e 429 com retry.
- **API:** controllers com MockMvc sobre PostgreSQL real, verificando respostas, filtros, paginação, erros e documentação.

## Deploy

Frontend na Vercel, API no Render, banco no Neon e ingestão diária no GitHub Actions, tudo no plano gratuito. Passo a passo, configuração e problemas comuns em [**DEPLOY.md**](DEPLOY.md).

## Licença

[MIT](LICENSE)
