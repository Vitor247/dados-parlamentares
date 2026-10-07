/** Par rótulo/valor de uma lista de dados. Sem dado na fonte, diz isso: nunca some nem vira zero. */
export function Dado({ rotulo, valor }: { rotulo: string; valor: string | null }) {
  return (
    <div>
      <dt>{rotulo}</dt>
      <dd>{valor ?? <span className="texto-suave">não informado pela fonte</span>}</dd>
    </div>
  )
}
