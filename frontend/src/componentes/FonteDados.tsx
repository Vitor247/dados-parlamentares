import type { Fonte } from '../api/tipos'
import { formatarDataHora } from '../formatar'

/** Procedência do dado, visível em toda página de detalhe. */
export function FonteDados({ fonte, paginaNaCamara }: { fonte: Fonte; paginaNaCamara?: string }) {
  const atualizado = formatarDataHora(fonte.atualizadoEm)
  return (
    <p className="fonte-dados pequeno texto-suave">
      Fonte: {fonte.origem}
      {atualizado && <> · sincronizado em {atualizado}</>}
      {paginaNaCamara && (
        <>
          {' · '}
          <a href={paginaNaCamara} target="_blank" rel="noreferrer">
            ver no site da Câmara
          </a>
        </>
      )}
      {fonte.uri && (
        <>
          {' · '}
          <a href={fonte.uri} target="_blank" rel="noreferrer">
            registro na API oficial
          </a>
        </>
      )}
    </p>
  )
}
