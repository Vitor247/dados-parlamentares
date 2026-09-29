import { Link, useParams } from 'react-router'
import { ErroDaApi } from '../api/cliente'
import type { Autor, Autores, Proposicao as ProposicaoDto } from '../api/tipos'
import { useApi } from '../api/useApi'
import { Dado } from '../componentes/Dado'
import { Carregando, MensagemErro } from '../componentes/Estados'
import { FonteDados } from '../componentes/FonteDados'
import { paginaDaProposicaoNaCamara } from '../constantes'
import { formatarData, formatarDataHora } from '../formatar'
import { NaoEncontrado } from './NaoEncontrado'

export function Proposicao() {
  const { id } = useParams()
  const proposicao = useApi<ProposicaoDto>(`/api/v1/proposicoes/${id}`)
  const autores = useApi<Autores>(`/api/v1/proposicoes/${id}/autores`)

  if (proposicao.erro instanceof ErroDaApi && (proposicao.erro.naoEncontrado || proposicao.erro.status === 400)) {
    return <NaoEncontrado mensagem="Não há proposição com esse identificador na base." />
  }
  if (proposicao.erro) return <MensagemErro erro={proposicao.erro} aoTentarDeNovo={proposicao.recarregar} />
  if (!proposicao.dados) return <Carregando />

  const p = proposicao.dados
  return (
    <>
      <p className="migalha pequeno">
        <Link to="/proposicoes">← Proposições</Link>
      </p>

      <header className="cabecalho-proposicao">
        <p className="texto-suave">{p.descricaoTipo ?? p.siglaTipo}</p>
        <h1>{p.identificacao}</h1>
        <p className="ementa">{p.ementa ?? 'Ementa não publicada pela fonte.'}</p>
      </header>

      {!p.detalheCarregado && (
        <p className="aviso">
          A situação, a data de apresentação, o inteiro teor e a autoria completa desta proposição ainda não foram
          carregados da Câmara. Eles são buscados automaticamente todos os dias — enquanto isso, veja a página oficial
          no link abaixo.
        </p>
      )}

      {p.detalheCarregado && (
        <section className="cartao">
          <h2>Situação</h2>
          {p.situacao?.descricao ? (
            <p className="situacao">
              <span className="etiqueta etiqueta-destaque">{p.situacao.descricao}</span>
              {p.situacao.orgaoSigla && <span className="texto-suave"> no órgão {p.situacao.orgaoSigla}</span>}
            </p>
          ) : (
            <p className="texto-suave">A Câmara não publica situação para esta proposição.</p>
          )}
          <dl className="dados">
            <Dado rotulo="Apresentada em" valor={formatarData(p.dataApresentacao)} />
            <Dado rotulo="Último evento de tramitação" valor={p.situacao?.ultimaTramitacao ?? null} />
            <Dado rotulo="Data do último evento" valor={formatarData(p.situacao?.data)} />
          </dl>
          {/* A situação é uma fotografia: pode ter mudado na Câmara depois da consulta. */}
          {p.situacao?.atualizadaEm && (
            <p className="pequeno texto-suave">
              Situação consultada na Câmara em {formatarDataHora(p.situacao.atualizadaEm)}. Ela é revalidada
              periodicamente e pode ter mudado desde então.
            </p>
          )}
          {p.urlInteiroTeor && (
            <p>
              <a href={p.urlInteiroTeor} target="_blank" rel="noreferrer" className="botao secundario">
                Ler o inteiro teor ↗
              </a>
            </p>
          )}
        </section>
      )}

      <section className="secao">
        <h2>Autoria</h2>
        {autores.erro ? (
          <MensagemErro erro={autores.erro} aoTentarDeNovo={autores.recarregar} />
        ) : !autores.dados ? (
          <Carregando texto="Carregando autores…" />
        ) : (
          <>
            {!autores.dados.detalheCarregado && (
              <p className="pequeno texto-suave">
                Lista parcial: mostra só os deputados da base que constam como autores. Coautores de fora da Câmara e a
                ordem de assinatura aparecem quando a autoria completa for carregada.
              </p>
            )}
            <ul className="lista-autores">
              {autores.dados.autores.map((autor, i) => (
                <ItemAutor key={`${autor.nome}-${i}`} autor={autor} />
              ))}
            </ul>
          </>
        )}
      </section>

      <FonteDados fonte={p._fonte} paginaNaCamara={paginaDaProposicaoNaCamara(p.id)} />
    </>
  )
}

function ItemAutor({ autor }: { autor: Autor }) {
  const d = autor.deputado
  return (
    <li className="item-autor">
      <div>
        {/* Só a ordem publicada pela fonte; sem ela, não se sugere ordem pela posição na lista. */}
        {autor.ordemAssinatura !== null && (
          <span className="ordem-assinatura">{autor.ordemAssinatura}º a assinar</span>
        )}
        {d ? <Link to={`/deputados/${d.id}`}>{autor.nome}</Link> : <strong>{autor.nome}</strong>}
        <span className="texto-suave pequeno">
          {d ? [d.siglaPartido, d.siglaUf].filter(Boolean).join(' · ') : autor.tipo}
        </span>
      </div>
      {autor.proponente && <span className="etiqueta">Proponente</span>}
    </li>
  )
}
