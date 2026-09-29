import { formatarNumero } from '../formatar'

interface Props {
  /** Página atual, começando em 1. */
  pagina: number
  totalPaginas: number
  totalElementos: number
  aoMudar: (pagina: number) => void
}

export function Paginacao({ pagina, totalPaginas, totalElementos, aoMudar }: Props) {
  if (totalElementos === 0) return null
  return (
    <nav className="paginacao" aria-label="Paginação">
      <span className="texto-suave pequeno">
        {formatarNumero(totalElementos)} {totalElementos === 1 ? 'resultado' : 'resultados'}
        {totalPaginas > 1 && ` · página ${pagina} de ${formatarNumero(totalPaginas)}`}
      </span>
      {totalPaginas > 1 && (
        <div className="paginacao-botoes">
          <button type="button" className="botao secundario" disabled={pagina <= 1} onClick={() => aoMudar(pagina - 1)}>
            ← Anterior
          </button>
          <button
            type="button"
            className="botao secundario"
            disabled={pagina >= totalPaginas}
            onClick={() => aoMudar(pagina + 1)}
          >
            Próxima →
          </button>
        </div>
      )}
    </nav>
  )
}
