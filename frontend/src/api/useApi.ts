import { useCallback, useEffect, useState } from 'react'
import { buscar, type Parametros } from './cliente'

interface Estado<T> {
  dados: T | undefined
  erro: Error | undefined
  carregando: boolean
}

/**
 * Carrega um recurso da API e recarrega quando o caminho ou os parâmetros mudam.
 * Os dados anteriores continuam visíveis durante o recarregamento (sem "piscar" ao
 * trocar de página), e a requisição antiga é cancelada se o usuário mudar de ideia.
 * Caminho null = não carregar ainda.
 */
export function useApi<T>(caminho: string | null, parametros?: Parametros) {
  const [estado, setEstado] = useState<Estado<T>>({ dados: undefined, erro: undefined, carregando: caminho !== null })
  const [tentativa, setTentativa] = useState(0)
  const chave = caminho === null ? null : caminho + JSON.stringify(parametros ?? {})

  useEffect(() => {
    if (caminho === null) return
    const controle = new AbortController()
    setEstado((anterior) => ({ ...anterior, erro: undefined, carregando: true }))
    buscar<T>(caminho, parametros, controle.signal)
      .then((dados) => setEstado({ dados, erro: undefined, carregando: false }))
      .catch((erro: unknown) => {
        if (erro instanceof DOMException && erro.name === 'AbortError') return
        setEstado((anterior) => ({ ...anterior, erro: erro as Error, carregando: false }))
      })
    return () => controle.abort()
    // A chave já codifica caminho e parâmetros; comparar objetos por referência recarregaria a cada render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [chave, tentativa])

  const recarregar = useCallback(() => setTentativa((t) => t + 1), [])

  return { ...estado, recarregar }
}
