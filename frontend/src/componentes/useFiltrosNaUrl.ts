import { useCallback } from 'react'
import { useSearchParams } from 'react-router'

/**
 * Filtros e página guardados na URL (?nome=silva&uf=MG&pagina=2): a busca vira um link
 * compartilhável e o botão "voltar" do navegador funciona.
 * A página na URL começa em 1 (para humanos); a API começa em 0.
 */
export function useFiltrosNaUrl<C extends string>(campos: readonly C[]) {
  const [params, setParams] = useSearchParams()

  const filtros = Object.fromEntries(campos.map((c) => [c, params.get(c) ?? ''])) as Record<C, string>
  const pagina = Math.max(1, Number.parseInt(params.get('pagina') ?? '1', 10) || 1)

  /** Mudar um filtro volta para a primeira página. {replace} evita um histórico por tecla digitada. */
  const alterarFiltro = useCallback(
    (campo: C, valor: string, opcoes?: { replace?: boolean }) => {
      setParams(
        (atual) => {
          const novo = new URLSearchParams(atual)
          if (valor) novo.set(campo, valor)
          else novo.delete(campo)
          novo.delete('pagina')
          return novo
        },
        { replace: opcoes?.replace },
      )
    },
    [setParams],
  )

  const irParaPagina = useCallback(
    (numero: number) => {
      setParams((atual) => {
        const novo = new URLSearchParams(atual)
        if (numero > 1) novo.set('pagina', String(numero))
        else novo.delete('pagina')
        return novo
      })
    },
    [setParams],
  )

  const limpar = useCallback(() => setParams(new URLSearchParams()), [setParams])

  return { filtros, pagina, paginaApi: pagina - 1, alterarFiltro, irParaPagina, limpar }
}
