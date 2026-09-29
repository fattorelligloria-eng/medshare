// As formas que a API devolve. Mantidas em um arquivo so para que uma mudanca
// no back-end apareca como erro de compilacao aqui, e nao como tela quebrada.

export type Papel = 'DOADOR' | 'BENEFICIARIO' | 'FARMACEUTICO' | 'ADMIN'

export interface Sessao {
  tokenDeAcesso: string
  tokenDeRenovacao: string
  expiraEmSegundos: number
  usuarioId: number
  nome: string
  papeis: Papel[]
}

export interface Doacao {
  codigo: string
  medicamento: string
  principioAtivo: string
  lote: string
  validade: string
  status: string
  pontoDeColeta: string | null
  criadoEm: string
  atualizadoEm: string
}

export interface Evento {
  quando: string
  tipo: string
  descricao: string
  statusAnterior: string | null
  statusNovo: string | null
}

/** O agendamento atual traz o codigo que o doador mostra no balcao (UC03). */
export interface AgendamentoAtual {
  pontoDeColeta: string
  endereco: string
  dataHora: string
  codigoEntrega: string
}

export interface DoacaoDetalhada {
  doacao: Doacao
  agendamento: AgendamentoAtual | null
  /** UC02 A2 - cancelada e o reagendamento unico ainda nao foi usado. */
  podeReagendar: boolean
  historico: Evento[]
}

/** UC03 passo 2 - o que o balcao ve de uma doacao. */
export interface DoacaoNoBalcao {
  codigo: string
  medicamento: string
  principioAtivo: string
  apresentacao: string
  lote: string
  validade: string
  status: string
  fotoUrl: string | null
  doador: string
  codigoEntrega: string | null
  agendadaPara: string | null
  atualizadoEm: string
}

/** Um caso na fila da central: o que o doador declarou x o que a IA leu. */
export interface CasoDaCentral {
  codigo: string
  medicamento: string
  fotoUrl: string
  loteDeclarado: string
  validadeDeclarada: string
  eanEsperado: string | null
  loteLido: string | null
  validadeLida: string | null
  eanLido: string | null
  classeEmbalagem: string | null
  certeza: string | null
  motivo: string | null
  avaliador: string | null
  divergencias: string[]
  esperandoDesde: string
  lacreDeclarado: boolean
}

/** UC05/UC06 - caixa oferecida, com prazo para aceitar. */
export interface Oferta {
  id: number
  status: 'PENDENTE' | 'ACEITA' | 'RECUSADA' | 'EXPIRADA' | 'CANCELADA'
  medicamento: string
  apresentacao: string
  validade: string
  pontoDeColeta: string
  enderecoDoPonto: string
  horarioDoPonto: string
  expiraEm: string
}

export interface Reserva {
  codigoRetirada: string
  medicamento: string
  apresentacao: string
  pontoDeColeta: string | null
  enderecoDoPonto: string | null
  horarioDoPonto: string | null
  status: string
  expiraEm: string
}

/** RN03 — o que o balcão vê antes de entregar: o titular e a receita. */
export interface ConferenciaDaRetirada {
  codigoRetirada: string
  status: string
  expiraEm: string
  medicamento: string
  principioAtivo: string
  apresentacao: string
  validadeDaCaixa: string
  titular: string
  cpfDoTitular: string
  procuradores: { nome: string; cpf: string }[]
  receitaFotoUrl: string | null
  receitaCrm: string | null
  receitaEmissao: string | null
  receitaValidade: string | null
  receitaValida: boolean
}

export interface PontoDeColeta {
  id: number
  nome: string
  endereco: string
  bairro: string
  municipio: string
  horario: string
}

/** UC08 - a farmacia como o administrador ve. */
export interface PontoAdministrado {
  id: number
  nome: string
  cnpj: string
  endereco: string
  municipio: string
  horario: string
  dias: number[]
  abreAs: string
  fechaAs: string
  vagasPorHora: number
  ativo: boolean
  farmaceuticos: { nome: string; email: string; crf: string }[]
}

export interface Pagina<T> {
  content: T[]
  totalElements: number
  number: number
  totalPages: number
}

export interface Municipio {
  id: number
  nome: string
}

/** GET /enderecos/{cep} — preenche o cadastro e confere a RN09. */
export interface EnderecoDoCep {
  cep: string
  logradouro: string
  bairro: string
  municipio: string
  uf: string
  municipioId: number | null
  atendido: boolean
}

export interface Medicamento {
  id: number
  nomeComercial: string
  principioAtivo: string
  apresentacao: string
  laboratorio?: string | null
  pmc: number
  altoCusto: boolean
}

export interface Necessidade {
  id: number
  medicamento: string
  principioAtivo: string
  ativa: boolean
  temReceitaValida: boolean
  validadeDaReceita?: string | null
  /** UC07 A1 — a receita não bateu no balcão; precisa enviar outra. */
  emRevisao: boolean
  motivoRevisao?: string | null
  /** UC07 A2 — perdeu uma caixa por validade e está na frente da fila. */
  prioridade: boolean
  criadaEm: string
}

export interface Procurador {
  id: number
  nome: string
  cpf: string
}

export interface Notificacao {
  id: number
  titulo: string
  corpo: string
  tipo: string
  lida: boolean
  quando: string
}

export interface RespostaDoCadUnico {
  confirmado: boolean
  validoAte?: string | null
  fonte?: string | null
  observacao?: string | null
  precisaDeAnaliseHumana?: boolean
}

export interface FotoEnviada {
  nome: string
  url: string
}
