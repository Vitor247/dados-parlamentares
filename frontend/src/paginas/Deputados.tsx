import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router'
import type { DeputadoResumo, Pagina, PartidoResumo } from '../api/tipos'
import { useApi } from '../api/useApi'
import { Carregando, MensagemErro, Vazio } from '../componentes/Estados'
import { FotoDeputado } from '../componentes/FotoDeputado'
import { Paginacao } from '../componentes/Paginacao'
import { useFiltrosNaUrl } from '../componentes/useFiltrosNaUrl'
import { UFS } from '../constantes'

const CAMPOS = ['nome', 'uf', 'partido'] as const

export function Deputados() {
  const { filtros, pagina, paginaApi, alterarFiltro, irParaPagina, limpar } = useFiltrosNaUrl(CAMPOS)
  const deputados = useApi<Pagina<DeputadoResumo>>('/api/v1/deputados', {
    nome: filtros.nome,
    uf: filtros.uf,
    partido: filtros.partido,
    page: paginaApi,
    size: 24,
  })
  const partidos = useApi<Pagina<PartidoResumo>>('/api/v1/partidos', { size: 100 })
  const temFiltro = Boolean(filtros.nome || filtros.uf || filtros.partido)
  const alterarNome = useCallback((v: string) => alterarFiltro('nome', v, { replace: true }), [alterarFiltro])

  return (
    <>
      <h1>Deputados</h1>
      <p className="texto-suave">Deputados federais da legislatura 57, com o partido atual segundo a Câmara.</p>

      <form className="filtros" role="search" onSubmit={(e) => e.preventDefault()}>
        <CampoNome valor={filtros.nome} aoMudar={alterarNome} />
        <label className="campo">
          <span>UF</span>
          <select value={filtros.uf} onChange={(e) => alterarFiltro('uf', e.target.value)}>
            <option value="">Todas</option>
            {UFS.map((uf) => (
              <option key={uf}>{uf}</option>
            ))}
          </select>
        </label>
        <label className="campo">
          <span>Partido</span>
          <select value={filtros.partido} onChange={(e) => alterarFiltro('partido', e.target.value)}>
            <option value="">Todos</option>
            {partidos.dados?.conteudo.map((p) => (
              <option key={p.id} value={p.sigla}>
                {p.sigla}
              </option>
            ))}
          </select>
        </label>
        {temFiltro && (
          <button type="button" className="botao secundario" onClick={limpar}>
            Limpar filtros
          </button>
        )}
      </form>

      {deputados.erro ? (
        <MensagemErro erro={deputados.erro} aoTentarDeNovo={deputados.recarregar} />
      ) : !deputados.dados ? (
        <Carregando />
      ) : deputados.dados.totalElementos === 0 ? (
        <Vazio>Nenhum deputado encontrado com esses filtros.</Vazio>
      ) : (
        <div className={deputados.carregando ? 'atualizando' : undefined}>
          <ul className="grade-deputados">
            {deputados.dados.conteudo.map((d) => (
              <li key={d.id}>
                <Link to={`/deputados/${d.id}`} className="cartao cartao-deputado">
                  <FotoDeputado nome={d.nome} url={d.urlFoto} />
                  <span>
                    <strong>{d.nome}</strong>
                    <span className="texto-suave pequeno">
                      {[d.siglaPartido, d.siglaUf].filter(Boolean).join(' · ')}
                    </span>
                    {d.situacao && d.situacao !== 'Exercício' && <span className="etiqueta">{d.situacao}</span>}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
          <Paginacao
            pagina={pagina}
            totalPaginas={deputados.dados.totalPaginas}
            totalElementos={deputados.dados.totalElementos}
            aoMudar={irParaPagina}
          />
        </div>
      )}
    </>
  )
}

/** Busca enquanto digita, mas só depois de uma pausa: não dispara uma requisição por tecla. */
function CampoNome({ valor, aoMudar }: { valor: string; aoMudar: (v: string) => void }) {
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
    <label className="campo campo-largo">
      <span>Nome</span>
      <input type="search" value={texto} placeholder="Ex.: Silva" onChange={(e) => setTexto(e.target.value)} />
    </label>
  )
}
