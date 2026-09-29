import { useEffect, useState, type ReactNode } from 'react'
import { ErroDaApi } from '../api/cliente'

/** Depois de alguns segundos, explica a demora: na hospedagem gratuita a API dorme quando ociosa. */
export function Carregando({ texto = 'Carregando…' }: { texto?: string }) {
  const [demorando, setDemorando] = useState(false)

  useEffect(() => {
    const timer = setTimeout(() => setDemorando(true), 4000)
    return () => clearTimeout(timer)
  }, [])

  return (
    <div className="estado" role="status" aria-live="polite">
      <span className="girando" aria-hidden="true" />
      {texto}
      {demorando && (
        <p className="pequeno" style={{ marginTop: 12 }}>
          A API pode estar “acordando” — em hospedagem gratuita ela desliga quando fica ociosa, e a primeira
          consulta depois disso leva de um a dois minutos. As seguintes são rápidas.
        </p>
      )}
    </div>
  )
}

export function MensagemErro({ erro, aoTentarDeNovo }: { erro: Error; aoTentarDeNovo?: () => void }) {
  const mensagem = erro instanceof ErroDaApi ? erro.message : 'Algo deu errado ao carregar os dados.'
  return (
    <div className="estado erro" role="alert">
      <p>{mensagem}</p>
      {aoTentarDeNovo && (
        <button type="button" className="botao secundario" onClick={aoTentarDeNovo}>
          Tentar de novo
        </button>
      )}
    </div>
  )
}

export function Vazio({ children }: { children: ReactNode }) {
  return <div className="estado">{children}</div>
}
