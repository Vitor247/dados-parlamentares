// Espelho dos DTOs da API pública (/api/v1). Campos que a fonte não publica chegam como null.

export interface Fonte {
  origem: string
  uri: string | null
  atualizadoEm: string | null
}

export interface Pagina<T> {
  conteudo: T[]
  pagina: number
  tamanho: number
  totalElementos: number
  totalPaginas: number
  _fonte: { origem: string; atualizadoEm: string | null }
}

export interface PartidoResumo {
  id: number
  sigla: string
  nome: string
}

export interface Partido extends PartidoResumo {
  _fonte: Fonte
}

export interface DeputadoResumo {
  id: number
  nome: string
  siglaUf: string | null
  /** Sigla como publicada pela fonte. */
  siglaPartido: string | null
  /** Partido resolvido na base; null se não pôde ser vinculado. */
  partido: PartidoResumo | null
  situacao: string | null
  urlFoto: string | null
  _fonte: Fonte
}

export interface Deputado extends DeputadoResumo {
  nomeCivil: string | null
  idLegislatura: number | null
  condicaoEleitoral: string | null
  email: string | null
  dataNascimento: string | null
  ufNascimento: string | null
  municipioNascimento: string | null
  escolaridade: string | null
  /** Contagem do que está na base, dentro do recorte. Não é métrica de produtividade. */
  totalProposicoes: number
}

export interface ProposicaoResumo {
  id: number
  identificacao: string
  siglaTipo: string
  numero: number
  ano: number
  ementa: string | null
  dataApresentacao: string | null
  situacao: string | null
  situacaoAtualizadaEm: string | null
  /** false = situação e data ainda não foram carregadas da fonte. */
  detalheCarregado: boolean
  _fonte: Fonte
}

export interface Situacao {
  descricao: string | null
  codigo: number | null
  data: string | null
  orgaoSigla: string | null
  ultimaTramitacao: string | null
  /** Quando a situação foi consultada na fonte. */
  atualizadaEm: string | null
}

export interface Proposicao {
  id: number
  identificacao: string
  siglaTipo: string
  codTipo: number | null
  descricaoTipo: string | null
  numero: number
  ano: number
  ementa: string | null
  dataApresentacao: string | null
  urlInteiroTeor: string | null
  situacao: Situacao | null
  detalheCarregado: boolean
  _fonte: Fonte
}

export interface Autor {
  nome: string
  tipo: string | null
  ordemAssinatura: number | null
  proponente: boolean | null
  /** null quando o autor não é deputado ou não está na base. */
  deputado: { id: number; nome: string; siglaPartido: string | null; siglaUf: string | null } | null
}

export interface Autores {
  proposicaoId: number
  identificacao: string
  detalheCarregado: boolean
  autores: Autor[]
  _fonte: Fonte
}

/** Situações presentes na base (GET /proposicoes/situacoes). */
export interface Situacoes {
  situacoes: { descricao: string; total: number }[]
}

/** Corpo de erro padrão da API (StandardError). */
export interface ErroPadrao {
  timestamp: string
  status: number
  erro: string
  mensagem: string
  caminho: string
}
