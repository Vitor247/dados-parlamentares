import { useEffect, useState } from 'react'

interface Props {
  rotulo: string
  valor: string
  placeholder?: string
  /** Aceita só dígitos (ex.: número da proposição); evita mandar texto para um filtro numérico. */
  somenteNumeros?: boolean
  largo?: boolean
  aoMudar: (valor: string) => void
}

/**
 * Campo de texto que busca enquanto a pessoa digita, mas só depois de uma pausa:
 * não dispara uma requisição por tecla. {aoMudar} deve ser estável (useCallback).
 */
export function CampoBusca({ rotulo, valor, placeholder, somenteNumeros = false, largo = true, aoMudar }: Props) {
  const [texto, setTexto] = useState(valor)
  const [valorAnterior, setValorAnterior] = useState(valor)

  // A URL pode mudar por fora (voltar do navegador, "Limpar filtros"): ajusta o texto durante o
  // render, sem efeito extra. Se a mudança veio da própria digitação, o texto já está certo.
  if (valor !== valorAnterior) {
    setValorAnterior(valor)
    if (valor !== texto.trim()) setTexto(valor)
  }

  useEffect(() => {
    if (texto.trim() === valor) return
    const timer = setTimeout(() => aoMudar(texto.trim()), 350)
    return () => clearTimeout(timer)
  }, [texto, valor, aoMudar])

  return (
    <label className={largo ? 'campo campo-largo' : 'campo'}>
      <span>{rotulo}</span>
      <input
        type="search"
        value={texto}
        placeholder={placeholder}
        inputMode={somenteNumeros ? 'numeric' : undefined}
        onChange={(e) => setTexto(somenteNumeros ? e.target.value.replace(/\D/g, '') : e.target.value)}
      />
    </label>
  )
}
