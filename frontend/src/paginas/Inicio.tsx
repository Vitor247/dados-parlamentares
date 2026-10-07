import { Link } from 'react-router'
import type { DeputadoResumo, Pagina, PartidoResumo, ProposicaoResumo } from '../api/tipos'
import { useApi } from '../api/useApi'
import { Carregando, MensagemErro } from '../componentes/Estados'
import { DESCRICAO_RECORTE } from '../constantes'
import { formatarNumero } from '../formatar'

export function Inicio() {
  // size=1: só interessa o totalElementos de cada recurso.
  const deputados = useApi<Pagina<DeputadoResumo>>('/api/v1/deputados', { size: 1 })
  const partidos = useApi<Pagina<PartidoResumo>>('/api/v1/partidos', { size: 1 })
  const proposicoes = useApi<Pagina<ProposicaoResumo>>('/api/v1/proposicoes', { size: 1 })

  const erro = deputados.erro ?? partidos.erro ?? proposicoes.erro
  const carregando = deputados.carregando || partidos.carregando || proposicoes.carregando

  return (
    <div className="inicio">
      <section className="apresentacao">
        <h1>Quem são os deputados federais e o que eles propõem</h1>
        <p className="texto-suave">
          Consulte deputados, partidos e proposições legislativas com dados oficiais da Câmara dos Deputados.
          Cada informação mostra de onde veio e quando foi atualizada.
        </p>
        <div className="acoes">
          <Link to="/deputados" className="botao">
            Buscar deputados
          </Link>
          <Link to="/proposicoes" className="botao secundario">
            Buscar proposições
          </Link>
        </div>
      </section>

      <section aria-label="Conteúdo da base">
        {erro ? (
          <MensagemErro
            erro={erro}
            aoTentarDeNovo={() => {
              deputados.recarregar()
              partidos.recarregar()
              proposicoes.recarregar()
            }}
          />
        ) : carregando ? (
          <Carregando texto="Conectando à API…" />
        ) : (
          <div className="numeros">
            <Numero valor={deputados.dados?.totalElementos} rotulo="deputados" para="/deputados" />
            <Numero valor={partidos.dados?.totalElementos} rotulo="partidos" />
            <Numero valor={proposicoes.dados?.totalElementos} rotulo="proposições" para="/proposicoes" />
          </div>
        )}
        <p className="pequeno texto-suave">
          Na base: deputados da legislatura 57 e {DESCRICAO_RECORTE}.
        </p>
      </section>
    </div>
  )
}

function Numero({ valor, rotulo, para }: { valor: number | undefined; rotulo: string; para?: string }) {
  const conteudo = (
    <>
      <strong>{valor === undefined ? '…' : formatarNumero(valor)}</strong>
      <span>{rotulo} na base</span>
    </>
  )
  return para ? (
    <Link to={para} className="cartao numero">
      {conteudo}
    </Link>
  ) : (
    <div className="cartao numero">{conteudo}</div>
  )
}
