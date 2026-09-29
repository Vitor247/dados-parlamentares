import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createBrowserRouter } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { Layout } from './componentes/Layout'
import { EmConstrucao } from './paginas/EmConstrucao'
import { Inicio } from './paginas/Inicio'
import { NaoEncontrado } from './paginas/NaoEncontrado'
import './styles.css'

const router = createBrowserRouter([
  {
    element: <Layout />,
    children: [
      { index: true, element: <Inicio /> },
      { path: 'deputados', element: <EmConstrucao titulo="Deputados" /> },
      { path: 'deputados/:id', element: <EmConstrucao titulo="Deputado" /> },
      { path: 'proposicoes', element: <EmConstrucao titulo="Proposições" /> },
      { path: 'proposicoes/:id', element: <EmConstrucao titulo="Proposição" /> },
      { path: '*', element: <NaoEncontrado /> },
    ],
  },
])

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
)
