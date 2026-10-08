/**
 * O nome de cada regra em português, para aparecer na tela.
 *
 * O servidor devolve o erro com a regra separada da mensagem, e a sigla é a
 * nossa: "RN02" é como o documento de modelagem chama a validade mínima.
 * Serve para a equipe conversar; não serve para quem usa o aplicativo. Quem
 * está cadastrando uma caixa lê "RN02" e não aprende nada.
 *
 * Então o código continua vindo do servidor — é ele que liga o erro à regra
 * no documento, e isso tem valor para quem mantém — mas a tela mostra a
 * palavra. Um mapa só, para a palavra ser a mesma em todo lugar.
 */
const EM_PORTUGUES: Record<string, string> = {
  // As regras de negócio do documento de modelagem.
  RN01: 'Lacre',
  RN02: 'Validade',
  RN03: 'Receita',
  RN04: 'Reserva',
  RN05: 'Privacidade',
  RN06: 'Histórico',
  RN07: 'Preço',
  RN08: 'CadÚnico',
  RN09: 'Região',
  RN10: 'Conferência',

  // Os demais já são palavras; só faltava acento e caixa.
  AGENDAMENTO: 'Agendamento',
  AUTENTICACAO: 'Acesso',
  BUSCA: 'Busca',
  CADASTRO: 'Cadastro',
  CICLO: 'Etapa',
  CRF: 'CRF',
  FOTO: 'Foto',
  OFERTA: 'Oferta',
  PERMISSAO: 'Permissão',
  PONTO: 'Farmácia',
  PROCURADOR: 'Procurador',
  QUANTIDADE: 'Quantidade',
  RESERVA: 'Reserva',
  SENHA: 'Senha',
}

/**
 * Devolve o rótulo da regra, ou nulo quando não houver tradução.
 *
 * Nulo de propósito: um código novo que o servidor passe a devolver e que
 * ninguém traduziu aqui some da tela em vez de aparecer cru. A mensagem ao
 * lado sempre explica o problema sozinha — a etiqueta é um reforço, não a
 * informação.
 */
export function rotuloDaRegra(codigo?: string | null): string | null {
  if (!codigo) return null
  return EM_PORTUGUES[codigo] ?? null
}
