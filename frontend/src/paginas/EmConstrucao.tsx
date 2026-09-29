/** Provisório: as telas de busca e detalhe entram nos próximos commits. */
export function EmConstrucao({ titulo }: { titulo: string }) {
  return (
    <div className="estado">
      <h1>{titulo}</h1>
      <p>Em construção.</p>
    </div>
  )
}
