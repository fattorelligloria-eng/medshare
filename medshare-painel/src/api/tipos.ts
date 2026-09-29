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

export interface DoacaoDetalhada {
  doacao: Doacao
  historico: Evento[]
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
  titular: string
  cpfDoTitular: string
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
  criadaEm: string
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
