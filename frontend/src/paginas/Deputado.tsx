import { Link, useParams } from 'react-router'
import { ErroDaApi } from '../api/cliente'
import type { Deputado as DeputadoDto, Pagina, ProposicaoResumo } from '../api/tipos'
import { useApi } from '../api/useApi'
import { Dado } from '../componentes/Dado'
import { Carregando, MensagemErro, Vazio } from '../componentes/Estados'
import { FonteDados } from '../componentes/FonteDados'
import { FotoDeputado } from '../componentes/FotoDeputado'
import { ItemProposicao } from '../componentes/ItemProposicao'
import { Paginacao } from '../componentes/Paginacao'
import { useFiltrosNaUrl } from '../componentes/useFiltrosNaUrl'
import { ANOS, DESCRICAO_RECORTE, TIPOS_PROPOSICAO, paginaDoDeputadoNaCamara } from '../constantes'
import { formatarData, formatarNumero } from '../formatar'
import { NaoEncontrado } from './NaoEncontrado'

export function Deputado() {
  const { id } = useParams()
  const deputado = useApi<DeputadoDto>(`/api/v1/deputados/${id}`)

  if (deputado.erro instanceof ErroDaApi && (deputado.erro.naoEncontrado || deputado.erro.status === 400)) {
    return <NaoEncontrado mensagem="Não há deputado com esse identificador na base." />
  }
  if (deputado.erro) return <MensagemErro erro={deputado.erro} aoTentarDeNovo={deputado.recarregar} />
  if (!deputado.dados) return <Carregando />

  const d = deputado.dados
  return (
    <>
      <p className="migalha pequeno">
        <Link to="/deputados">← Deputados</Link>
      </p>

      <header className="cabecalho-perfil">
        <FotoDeputado nome={d.nome} url={d.urlFoto} tamanho={112} />
        <div>
          <h1>{d.nome}</h1>
          <p className="texto-suave">
            {d.partido ? (
              <Link to={`/deputados?partido=${encodeURIComponent(d.partido.sigla)}`}>{d.partido.nome}</Link>
            ) : (
              d.siglaPartido
            )}
            {d.siglaUf && <> · {d.siglaUf}</>}
          </p>
          <div className="etiquetas">
            {d.situacao && <span className="etiqueta">{d.situacao}</span>}
            {d.condicaoEleitoral && <span className="etiqueta">{d.condicaoEleitoral}</span>}
          </div>
        </div>
      </header>

      <section className="cartao">
        <h2>Dados publicados pela Câmara</h2>
        <dl className="dados">
          <Dado rotulo="Nome civil" valor={d.nomeCivil} />
          <Dado rotulo="Nascimento" valor={formatarData(d.dataNascimento)} />
          <Dado
            rotulo="Naturalidade"
            valor={[d.municipioNascimento, d.ufNascimento].filter(Boolean).join(' – ') || null}
          />
          <Dado rotulo="Escolaridade" valor={d.escolaridade} />
          <Dado rotulo="E-mail" valor={d.email} />
          <Dado rotulo="Legislatura" valor={d.idLegislatura?.toString() ?? null} />
        </dl>
        <FonteDados fonte={d._fonte} paginaNaCamara={paginaDoDeputadoNaCamara(d.id)} />
      </section>

      <ProposicoesDoDeputado id={d.id} total={d.totalProposicoes} />
    </>
  )
}

const CAMPOS = ['ano', 'tipo'] as const

function ProposicoesDoDeputado({ id, total }: { id: number; total: number }) {
  const { filtros, pagina, paginaApi, alterarFiltro, irParaPagina } = useFiltrosNaUrl(CAMPOS)
  const proposicoes = useApi<Pagina<ProposicaoResumo>>(`/api/v1/deputados/${id}/proposicoes`, {
    ano: filtros.ano,
    tipo: filtros.tipo,
    page: paginaApi,
    size: 10,
  })

  return (
    <section className="secao">
      <div className="secao-cabecalho">
        <h2>Proposições de autoria</h2>
        <p className="pequeno texto-suave">
          {formatarNumero(total)} {total === 1 ? 'proposição' : 'proposições'} na base, dentro do recorte importado
          ({DESCRICAO_RECORTE}). É uma contagem do que está disponível aqui, não uma medida de produtividade.
        </p>
      </div>

      <form className="filtros" onSubmit={(e) => e.preventDefault()}>
        <label className="campo">
          <span>Ano</span>
          <select value={filtros.ano} onChange={(e) => alterarFiltro('ano', e.target.value)}>
            <option value="">Todos</option>
            {ANOS.map((ano) => (
              <option key={ano}>{ano}</option>
            ))}
          </select>
        </label>
        <label className="campo">
          <span>Tipo</span>
          <select value={filtros.tipo} onChange={(e) => alterarFiltro('tipo', e.target.value)}>
            <option value="">Todos</option>
            {TIPOS_PROPOSICAO.map((t) => (
              <option key={t.sigla} value={t.sigla}>
                {t.sigla} — {t.nome}
              </option>
            ))}
          </select>
        </label>
      </form>

      {proposicoes.erro ? (
        <MensagemErro erro={proposicoes.erro} aoTentarDeNovo={proposicoes.recarregar} />
      ) : !proposicoes.dados ? (
        <Carregando />
      ) : proposicoes.dados.totalElementos === 0 ? (
        <Vazio>Nenhuma proposição na base com esses filtros.</Vazio>
      ) : (
        <div className={proposicoes.carregando ? 'atualizando' : undefined}>
          <ul className="lista-proposicoes">
            {proposicoes.dados.conteudo.map((p) => (
              <ItemProposicao key={p.id} proposicao={p} />
            ))}
          </ul>
          <Paginacao
            pagina={pagina}
            totalPaginas={proposicoes.dados.totalPaginas}
            totalElementos={proposicoes.dados.totalElementos}
            aoMudar={irParaPagina}
          />
        </div>
      )}
    </section>
  )
}
