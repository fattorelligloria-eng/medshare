/**
 * Como cada estado aparece para quem usa, e com que peso visual.
 *
 * O nome na tela não é o nome no banco. CADASTRADA, PRE_VALIDADA e VALIDADA
 * descrevem onde a caixa está na máquina de estados; quem abre o aplicativo
 * quer saber o que está acontecendo com a caixa dele.
 *
 * O par que mais importava separar é RECUSADA e REJEITADA: no banco são coisas
 * diferentes — não entrou na rede, e o farmacêutico reprovou no balcão — mas
 * em português as duas palavras dizem a mesma coisa, e quem lesse não saberia
 * qual das duas aconteceu.
 */
const TEXTO: Record<string, string> = {
  CADASTRADA: 'Em conferência',
  EM_ANALISE_CENTRAL: 'Em análise',
  PRE_VALIDADA: 'Liberada',
  AGENDADA: 'Agendada',
  RECEBIDA: 'Na farmácia',
  VALIDADA: 'Conferida',
  DISPONIVEL: 'Disponível',
  RESERVADA: 'Separada para alguém',
  ENTREGUE: 'Entregue',
  RECUSADA: 'Não aceita',
  CANCELADA: 'Cancelada',
  REJEITADA: 'Reprovada no balcão',
  DESCARTADA: 'Vencida',
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
    case 'DESCARTADA': return 'Passou da validade mínima e saiu do estoque.'
    default: return ''
  }
}
