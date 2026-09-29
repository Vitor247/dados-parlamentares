export const UFS = [
  'AC', 'AL', 'AM', 'AP', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MG', 'MS', 'MT', 'PA',
  'PB', 'PE', 'PI', 'PR', 'RJ', 'RN', 'RO', 'RR', 'RS', 'SC', 'SE', 'SP', 'TO',
] as const

/** Tipos de proposição no recorte importado (parlamento.ingestao.tipos-proposicao na API). */
export const TIPOS_PROPOSICAO = [
  { sigla: 'PL', nome: 'Projeto de Lei' },
  { sigla: 'PEC', nome: 'Proposta de Emenda à Constituição' },
  { sigla: 'PLP', nome: 'Projeto de Lei Complementar' },
  { sigla: 'PDL', nome: 'Projeto de Decreto Legislativo' },
] as const

/** Anos cobertos pelo recorte: proposições apresentadas desde 2025 até hoje. */
export const ANOS = Array.from({ length: new Date().getFullYear() - 2025 + 1 }, (_, i) => 2025 + i).reverse()

export function paginaDoDeputadoNaCamara(id: number) {
  return `https://www.camara.leg.br/deputados/${id}`
}

export function paginaDaProposicaoNaCamara(id: number) {
  return `https://www.camara.leg.br/propostas-legislativas/${id}`
}
