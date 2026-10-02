import type { Situacoes } from '../api/tipos'
import { useApi } from '../api/useApi'
import { formatarNumero } from '../formatar'

interface Props {
  valor: string
  aoMudar: (valor: string) => void
  /** Totais da base inteira; não fazem sentido num recorte (ex.: perfil de um deputado). */
  mostrarTotais?: boolean
}

/** Opções vindas da própria API: só as situações que existem na base aparecem. */
export function SeletorSituacao({ valor, aoMudar, mostrarTotais = false }: Props) {
  const situacoes = useApi<Situacoes>('/api/v1/proposicoes/situacoes')

  return (
    <label className="campo campo-situacao">
      <span>Situação</span>
      <select value={valor} onChange={(e) => aoMudar(e.target.value)} disabled={!situacoes.dados}>
        <option value="">Todas</option>
        {situacoes.dados?.situacoes.map((s) => (
          <option key={s.descricao} value={s.descricao}>
            {mostrarTotais ? `${s.descricao} (${formatarNumero(s.total)})` : s.descricao}
          </option>
        ))}
        {/* Valor vindo de um link antigo que não existe mais na base: mantém a seleção visível. */}
        {valor && situacoes.dados && !situacoes.dados.situacoes.some((s) => s.descricao === valor) && (
          <option value={valor}>{valor}</option>
        )}
      </select>
    </label>
  )
}
