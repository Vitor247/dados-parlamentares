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

/** Recorte importado pela API (parlamento.ingestao.data-apresentacao-inicio): início da legislatura 57. */
const ANO_INICIAL = 2023

/** Descrição do recorte para os textos do site. */
export const DESCRICAO_RECORTE =
  'proposições dos tipos PL, PEC, PLP e PDL apresentadas desde fevereiro de 2023, início da legislatura 57'

/** Anos cobertos pelo recorte, do mais recente para o mais antigo. */
export const ANOS = Array.from({ length: new Date().getFullYear() - ANO_INICIAL + 1 }, (_, i) => ANO_INICIAL + i).reverse()

export function paginaDoDeputadoNaCamara(id: number) {
  return `https://www.camara.leg.br/deputados/${id}`
}

export function paginaDaProposicaoNaCamara(id: number) {
  return `https://www.camara.leg.br/propostas-legislativas/${id}`
}
