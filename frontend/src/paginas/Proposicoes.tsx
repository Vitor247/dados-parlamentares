import { useCallback } from 'react'
import type { Pagina, ProposicaoResumo } from '../api/tipos'
import { useApi } from '../api/useApi'
import { CampoBusca } from '../componentes/CampoBusca'
import { Carregando, MensagemErro, Vazio } from '../componentes/Estados'
import { ItemProposicao } from '../componentes/ItemProposicao'
import { Paginacao } from '../componentes/Paginacao'
import { SeletorSituacao } from '../componentes/SeletorSituacao'
import { useFiltrosNaUrl } from '../componentes/useFiltrosNaUrl'
import { ANOS, DESCRICAO_RECORTE, TIPOS_PROPOSICAO } from '../constantes'

const CAMPOS = ['tipo', 'numero', 'ano', 'situacao', 'ementa'] as const

export function Proposicoes() {
  const { filtros, pagina, paginaApi, alterarFiltro, irParaPagina, limpar } = useFiltrosNaUrl(CAMPOS)
  const proposicoes = useApi<Pagina<ProposicaoResumo>>('/api/v1/proposicoes', {
    tipo: filtros.tipo,
    numero: filtros.numero,
    ano: filtros.ano,
    situacao: filtros.situacao,
    ementa: filtros.ementa,
    page: paginaApi,
    size: 20,
  })
  const temFiltro = Object.values(filtros).some(Boolean)
  const alterarEmenta = useCallback((v: string) => alterarFiltro('ementa', v, { replace: true }), [alterarFiltro])
  const alterarNumero = useCallback((v: string) => alterarFiltro('numero', v, { replace: true }), [alterarFiltro])

  return (
    <>
      <h1>Proposições</h1>
      <p className="texto-suave">
        Projetos de lei, propostas de emenda à Constituição, projetos de lei complementar e de decreto legislativo
        apresentados desde fevereiro de 2023, início da legislatura 57. Para achar uma proposição específica, como
        “PL 1234/2025”, use tipo, número e ano.
      </p>

      <form className="filtros" role="search" onSubmit={(e) => e.preventDefault()}>
        <label className="campo">
          <span>Tipo</span>
          <select value={filtros.tipo} onChange={(e) => alterarFiltro('tipo', e.target.value)}>
            <option value="">Todos</option>
            {TIPOS_PROPOSICAO.map((t) => (
              <option key={t.sigla} value={t.sigla} title={t.nome}>
                {t.sigla}
              </option>
            ))}
          </select>
        </label>
        <CampoBusca
          rotulo="Número"
          valor={filtros.numero}
          placeholder="Ex.: 1234"
          somenteNumeros
          largo={false}
          aoMudar={alterarNumero}
        />
        <label className="campo">
          <span>Ano</span>
          <select value={filtros.ano} onChange={(e) => alterarFiltro('ano', e.target.value)}>
            <option value="">Todos</option>
            {ANOS.map((ano) => (
              <option key={ano}>{ano}</option>
            ))}
          </select>
        </label>
        <SeletorSituacao valor={filtros.situacao} aoMudar={(v) => alterarFiltro('situacao', v)} mostrarTotais />
        <CampoBusca rotulo="Palavra na ementa" valor={filtros.ementa} placeholder="Ex.: saúde" aoMudar={alterarEmenta} />
        {temFiltro && (
          <button type="button" className="botao secundario" onClick={limpar}>
            Limpar filtros
          </button>
        )}
      </form>

      {proposicoes.erro ? (
        <MensagemErro erro={proposicoes.erro} aoTentarDeNovo={proposicoes.recarregar} />
      ) : !proposicoes.dados ? (
        <Carregando />
      ) : proposicoes.dados.totalElementos === 0 ? (
        <Vazio>
          Nenhuma proposição na base com esses filtros. Lembre que só estão aqui {DESCRICAO_RECORTE}.
        </Vazio>
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
    </>
  )
}
