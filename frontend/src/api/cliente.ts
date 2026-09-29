import type { ErroPadrao } from './tipos'

const API_URL = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080').replace(/\/+$/, '')

export type Parametros = Record<string, string | number | undefined | null>

/** Falha ao consultar a API, com uma mensagem pronta para mostrar a quem usa o site. */
export class ErroDaApi extends Error {
  /** Status HTTP; 0 quando nem foi possível conectar. */
  readonly status: number

  constructor(status: number, mensagem: string) {
    super(mensagem)
    this.name = 'ErroDaApi'
    this.status = status
  }

  get naoEncontrado() {
    return this.status === 404
  }
}

export async function buscar<T>(caminho: string, parametros?: Parametros, signal?: AbortSignal): Promise<T> {
  const url = new URL(API_URL + caminho)
  for (const [nome, valor] of Object.entries(parametros ?? {})) {
    if (valor !== undefined && valor !== null && valor !== '') {
      url.searchParams.set(nome, String(valor))
    }
  }

  let resposta: Response
  try {
    resposta = await fetch(url, { signal, headers: { Accept: 'application/json' } })
  } catch (erro) {
    if (erro instanceof DOMException && erro.name === 'AbortError') throw erro
    throw new ErroDaApi(0, 'Não foi possível conectar à API. Verifique sua conexão e tente novamente.')
  }

  if (!resposta.ok) {
    // A API responde erros no formato StandardError, com mensagem em português.
    const corpo = (await resposta.json().catch(() => null)) as ErroPadrao | null
    throw new ErroDaApi(resposta.status, corpo?.mensagem ?? `A API respondeu com erro ${resposta.status}.`)
  }
  return (await resposta.json()) as T
}
