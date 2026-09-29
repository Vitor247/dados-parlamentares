const data = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })
const dataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
const numero = new Intl.NumberFormat('pt-BR')

/**
 * Datas da API chegam sem fuso ("2025-04-09" ou "2025-04-09T18:16:00") e representam o
 * horário de Brasília. Montar pelo construtor local evita que "2025-04-09" vire 08/04 em UTC.
 */
function paraDate(iso: string): Date {
  const [d, t = '00:00:00'] = iso.split('T')
  const [ano, mes, dia] = d.split('-').map(Number)
  const [h, min, s] = t.split(':').map((p) => Number.parseFloat(p))
  return new Date(ano, mes - 1, dia, h || 0, min || 0, s || 0)
}

export function formatarData(iso: string | null | undefined): string | null {
  return iso ? data.format(paraDate(iso)) : null
}

export function formatarDataHora(iso: string | null | undefined): string | null {
  return iso ? dataHora.format(paraDate(iso)) : null
}

export function formatarNumero(valor: number): string {
  return numero.format(valor)
}
