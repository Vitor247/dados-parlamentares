import { Link, NavLink, Outlet, ScrollRestoration } from 'react-router'

export function Layout() {
  return (
    <div className="pagina">
      <header className="cabecalho">
        <div className="conteiner">
          <Link to="/" className="marca">
            <img src="/favicon.svg" alt="" />
            Dados Parlamentares
          </Link>
          <nav className="navegacao" aria-label="Principal">
            <NavLink to="/deputados">Deputados</NavLink>
            <NavLink to="/proposicoes">Proposições</NavLink>
          </nav>
        </div>
      </header>

      <main>
        <div className="conteiner">
          <Outlet />
        </div>
      </main>

      <footer className="rodape">
        <div className="conteiner">
          <p>
            Dados oficiais da{' '}
            <a href="https://dadosabertos.camara.leg.br" target="_blank" rel="noreferrer">
              API de Dados Abertos da Câmara dos Deputados
            </a>
            , importados e servidos sem alteração.
          </p>
          <p>Este site mostra dados; não avalia, classifica nem compara parlamentares.</p>
        </div>
      </footer>
      <ScrollRestoration />
    </div>
  )
}
