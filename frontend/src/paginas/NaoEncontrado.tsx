import { Link } from 'react-router'

export function NaoEncontrado({ mensagem = 'Esta página não existe.' }: { mensagem?: string }) {
  return (
    <div className="estado">
      <h1>Não encontrado</h1>
      <p>{mensagem}</p>
      <Link to="/" className="botao secundario">
        Voltar ao início
      </Link>
    </div>
  )
}
