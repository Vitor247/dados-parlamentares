import { Link } from 'react-router'
import type { ProposicaoResumo } from '../api/tipos'
import { formatarData } from '../formatar'

export function ItemProposicao({ proposicao }: { proposicao: ProposicaoResumo }) {
  return (
    <li className="item-proposicao">
      <Link to={`/proposicoes/${proposicao.id}`} className="item-proposicao-link">
        <span className="item-proposicao-titulo">{proposicao.identificacao}</span>
        <span className="item-proposicao-ementa">{proposicao.ementa ?? 'Ementa não publicada pela fonte.'}</span>
      </Link>
      <div className="item-proposicao-meta pequeno">
        {proposicao.detalheCarregado ? (
          <>
            {proposicao.situacao && <span className="etiqueta">{proposicao.situacao}</span>}
            {proposicao.dataApresentacao && (
              <span className="texto-suave">Apresentada em {formatarData(proposicao.dataApresentacao)}</span>
            )}
          </>
        ) : (
          <span className="texto-suave" title="A situação e a data ainda não foram carregadas da fonte">
            Situação ainda não carregada
          </span>
        )}
      </div>
    </li>
  )
}
