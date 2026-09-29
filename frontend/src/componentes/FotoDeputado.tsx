import { useState } from 'react'

/** Foto oficial; sem foto (ou se ela falhar ao carregar), mostra as iniciais. */
export function FotoDeputado({ nome, url, tamanho = 64 }: { nome: string; url: string | null; tamanho?: number }) {
  const [falhou, setFalhou] = useState(false)
  const estilo = { width: tamanho, height: tamanho }

  if (!url || falhou) {
    const iniciais = nome
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((parte) => parte[0]?.toUpperCase())
      .join('')
    return (
      <span className="foto foto-iniciais" style={{ ...estilo, fontSize: tamanho * 0.36 }} aria-hidden="true">
        {iniciais}
      </span>
    )
  }
  return (
    <img
      className="foto"
      src={url}
      alt={`Foto de ${nome}`}
      style={estilo}
      loading="lazy"
      onError={() => setFalhou(true)}
    />
  )
}
