#!/usr/bin/env node
/**
 * Dispara a ingestão na API (carga base + lotes de enriquecimento).
 * Usado pelo workflow agendado, mas roda igual localmente:
 *
 *   API_URL=http://localhost:8080 ADMIN_API_KEY=... node .github/scripts/ingestao.mjs
 *
 * Variáveis:
 *   API_URL        (obrigatória) raiz da API, sem barra final
 *   ADMIN_API_KEY  (obrigatória) chave do header X-Admin-Key
 *   CARGA_BASE     "true" (padrão) | "false" — executa a Fase A antes do enriquecimento
 *   LOTES          quantos lotes de enriquecimento no máximo (padrão 40; 0 = nenhum)
 *   TAMANHO_LOTE   proposições por lote (padrão 100, ~30 s cada: chamadas curtas não
 *                  esbarram em limite de tempo de requisição da hospedagem)
 */
import { appendFileSync } from 'node:fs';
import http from 'node:http';
import https from 'node:https';

const API_URL = obrigatoria('API_URL').replace(/\/+$/, '');
const ADMIN_API_KEY = obrigatoria('ADMIN_API_KEY');
const CARGA_BASE = (process.env.CARGA_BASE ?? 'true') === 'true';
const LOTES = inteiro('LOTES', 40);
const TAMANHO_LOTE = inteiro('TAMANHO_LOTE', 100);

const MINUTO = 60_000;

const resumo = ['## Ingestão', '', '| Etapa | Processados | Falhas |', '|---|---:|---:|'];

try {
  await acordar();

  if (CARGA_BASE) {
    console.log('Fase A: carga base...');
    const r = await post('/api/v1/admin/ingestao/base', 20 * MINUTO);
    for (const e of r.etapas) {
      console.log(`  ${e.recurso}: ${e.processados} processados, ${e.falhas} falhas`);
      resumo.push(`| ${e.recurso} | ${e.processados} | ${e.falhas} |`);
    }
  }

  // Cada lote processa primeiro as nunca enriquecidas e depois revalida as de situação antiga.
  let processados = 0;
  let falhas = 0;
  let pendentes = null;
  let desatualizadas = null;
  for (let lote = 1; lote <= LOTES; lote++) {
    const r = await post(`/api/v1/admin/ingestao/enriquecimento?limite=${TAMANHO_LOTE}`, 10 * MINUTO);
    const etapa = r.etapas[0];
    processados += etapa.processados;
    falhas += etapa.falhas;
    pendentes = r.proposicoesPendentes;
    desatualizadas = r.proposicoesDesatualizadas;
    console.log(`Fase B, lote ${lote}/${LOTES}: ${etapa.processados} processadas, ${etapa.falhas} falhas, `
      + `${pendentes} pendentes, ${desatualizadas} com situação desatualizada`);
    // Lote sem nenhum avanço: só restam proposições que estão falhando; não adianta insistir hoje.
    if (pendentes + desatualizadas === 0 || etapa.processados === 0) break;
  }
  if (LOTES > 0) {
    resumo.push(`| enriquecimento | ${processados} | ${falhas} |`);
    resumo.push('', `Nunca enriquecidas: **${pendentes}** · Situação desatualizada: **${desatualizadas}**`);
  }
  if (falhas > 0) {
    // Falhas por unidade não derrubam o job: continuam na fila e são retentadas na próxima execução.
    console.log(`::warning::${falhas} proposições falharam no enriquecimento (detalhes em ingestao_log)`);
  }
} catch (erro) {
  resumo.push('', `**Falhou:** ${erro.message}`);
  console.error(`::error::${erro.message}`);
  process.exitCode = 1;
} finally {
  if (process.env.GITHUB_STEP_SUMMARY) {
    appendFileSync(process.env.GITHUB_STEP_SUMMARY, resumo.join('\n') + '\n');
  }
}

/** Em hospedagem gratuita a API dorme quando ociosa; a primeira chamada a acorda. */
async function acordar() {
  const limite = Date.now() + 5 * MINUTO;
  console.log(`Acordando a API em ${API_URL}...`);
  while (Date.now() < limite) {
    try {
      const r = await fetch(`${API_URL}/actuator/health`, { signal: AbortSignal.timeout(15_000) });
      if (r.ok) {
        console.log('API no ar.');
        return;
      }
    } catch {
      // ainda subindo
    }
    await new Promise((ok) => setTimeout(ok, 10_000));
  }
  throw new Error('A API não respondeu ao health check em 5 minutos');
}

async function post(caminho, timeoutMs) {
  const { status, corpo } = await postSemLimiteOculto(API_URL + caminho, timeoutMs);
  if (status < 200 || status >= 300) {
    // StandardError da API: a mensagem já explica (401 chave errada, 502 Câmara fora...).
    let mensagem = corpo;
    try { mensagem = JSON.parse(corpo).mensagem ?? corpo; } catch { /* corpo não-JSON */ }
    throw new Error(`POST ${caminho} respondeu ${status}: ${mensagem}`);
  }
  return JSON.parse(corpo);
}

/**
 * POST com node:http(s) em vez de fetch. O fetch do Node (undici) tem um limite próprio de
 * 5 minutos para receber os cabeçalhos da resposta (headersTimeout), que ignora o AbortSignal:
 * a carga base leva mais que isso na instância gratuita, e a conexão caía com "fetch failed".
 * Aqui o único limite é o timeoutMs informado.
 */
function postSemLimiteOculto(url, timeoutMs) {
  const cliente = url.startsWith('https:') ? https : http;
  return new Promise((resolve, reject) => {
    const req = cliente.request(url, { method: 'POST', headers: { 'X-Admin-Key': ADMIN_API_KEY } }, (res) => {
      const partes = [];
      res.on('data', (parte) => partes.push(parte));
      res.on('end', () => resolve({ status: res.statusCode ?? 0, corpo: Buffer.concat(partes).toString('utf8') }));
      res.on('error', reject);
    });
    const limite = setTimeout(
      () => req.destroy(new Error(`sem resposta em ${Math.round(timeoutMs / MINUTO)} min`)),
      timeoutMs,
    );
    req.on('close', () => clearTimeout(limite));
    req.on('error', reject);
    req.end();
  });
}

function obrigatoria(nome) {
  const valor = process.env[nome];
  if (!valor) {
    console.error(`::error::Variável ${nome} não definida`);
    process.exit(1);
  }
  return valor;
}

function inteiro(nome, padrao) {
  const valor = Number.parseInt(process.env[nome] ?? `${padrao}`, 10);
  if (!Number.isInteger(valor) || valor < 0) {
    console.error(`::error::${nome} deve ser um inteiro >= 0`);
    process.exit(1);
  }
  return valor;
}
