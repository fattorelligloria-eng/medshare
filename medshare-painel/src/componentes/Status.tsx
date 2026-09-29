/** Como cada status aparece para quem usa, e com que peso visual. */
const TEXTO: Record<string, string> = {
  CADASTRADA: 'Cadastrada',
  EM_ANALISE_CENTRAL: 'Em análise',
  PRE_VALIDADA: 'Pré-validada',
  AGENDADA: 'Agendada',
  RECEBIDA: 'Recebida',
  VALIDADA: 'Validada',
  DISPONIVEL: 'Disponível',
  RESERVADA: 'Reservada',
  ENTREGUE: 'Entregue',
  RECUSADA: 'Recusada',
  CANCELADA: 'Cancelada',
  REJEITADA: 'Rejeitada',
  DESCARTADA: 'Descartada',
  ATIVA: 'Ativa',
  CONCLUIDA: 'Concluída',
  EXPIRADA: 'Expirada',
}

const PRECISA_DE_ATENCAO = ['EM_ANALISE_CENTRAL', 'RECUSADA', 'CANCELADA', 'REJEITADA', 'DESCARTADA', 'EXPIRADA']
const JA_ACABOU = ['ENTREGUE', 'CONCLUIDA']

export function Status({ status }: { status: string }) {
  const tom = PRECISA_DE_ATENCAO.includes(status)
    ? 'atencao'
    : JA_ACABOU.includes(status)
      ? 'neutro'
      : 'andamento'

  return (
    <div className={`status ${tom}`}>
      <span className="ponto" />
      <span className="texto">{TEXTO[status] ?? status}</span>
    </div>
  )
}

/** O que a pessoa precisa fazer, ou esperar, em cada estado. */
export function explicar(status: string): string {
  switch (status) {
    case 'CADASTRADA': return 'Conferindo a foto da embalagem.'
    case 'EM_ANALISE_CENTRAL': return 'Nossa equipe está conferindo a foto. Resposta em até 48 horas.'
    case 'PRE_VALIDADA': return 'Tudo certo. Escolha a farmácia e o horário da entrega.'
    case 'AGENDADA': return 'Leve a caixa à farmácia no horário marcado.'
    case 'RECEBIDA': return 'A farmácia recebeu. O farmacêutico vai conferir o lacre.'
    case 'VALIDADA': return 'Conferida pelo farmacêutico.'
    case 'DISPONIVEL': return 'Na prateleira, esperando quem precisa.'
    case 'RESERVADA': return 'Alguém reservou e vai retirar em breve.'
    case 'ENTREGUE': return 'Entregue a quem precisava.'
    case 'RECUSADA': return 'Não pôde entrar na rede.'
    case 'CANCELADA': return 'O agendamento foi cancelado.'
    case 'REJEITADA': return 'Não passou na conferência do farmacêutico.'
    case 'DESCARTADA': return 'Saiu do estoque por causa da validade.'
    default: return ''
  }
}
