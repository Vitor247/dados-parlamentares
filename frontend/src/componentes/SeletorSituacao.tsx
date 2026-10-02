import type { Parametros } from '../api/cliente'
import type { Situacoes } from '../api/tipos'
import { useApi } from '../api/useApi'
import { formatarNumero } from '../formatar'

interface Props {
  /** Endpoint de situações do contexto: o geral ou o das proposições de um deputado. */
  caminho: string
  /** Os demais filtros da tela: só aparecem situações que retornam resultado com eles. */
  filtros: Parametros
  valor: string
  aoMudar: (valor: string) => void
}

/** Filtro facetado: as opções vêm da API e acompanham o contexto e os outros filtros. */
export function SeletorSituacao({ caminho, filtros, valor, aoMudar }: Props) {
  const situacoes = useApi<Situacoes>(caminho, filtros)
  const opcoes = situacoes.dados?.situacoes

  return (
    <label className="campo campo-situacao">
      <span>Situação</span>
      <select value={valor} onChange={(e) => aoMudar(e.target.value)} disabled={!opcoes}>
        <option value="">Todas</option>
        {opcoes?.map((s) => (
          <option key={s.descricao} value={s.descricao}>
            {s.descricao} ({formatarNumero(s.total)})
          </option>
        ))}
        {/* A seleção atual pode ter deixado de existir com os outros filtros (ou vir de um link
            antigo): continua visível, com 0, para a pessoa entender o resultado vazio. */}
        {valor && opcoes && !opcoes.some((s) => s.descricao === valor) && (
          <option value={valor}>{valor} (0)</option>
        )}
      </select>
    </label>
  )
}
