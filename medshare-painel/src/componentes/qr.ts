/**
 * Gerador de QR Code, versao 1, correcao de erro Q.
 *
 * Por que escrever isto em vez de instalar uma biblioteca: o unico conteudo que
 * o MedShare precisa gerar e o codigo de retirada — 8 caracteres de
 * "ABCDEFGHJKLMNPQRSTUVWXYZ23456789", que cabem folgados no modo alfanumerico
 * da versao 1 (limite de 16). Uma versao fixa dispensa tabela de versoes,
 * padroes de alinhamento e intercalacao de blocos, e o arquivo inteiro fica
 * menor que o pacote que resolveria o mesmo problema.
 *
 * Nivel Q (recupera ~25%) porque o destino e uma tela de celular lida por
 * leitor de balcao: reflexo e dedo na tela sao a regra, nao a excecao.
 *
 * Referencia: ISO/IEC 18004. Conferido gerando 200 codigos aleatorios do
 * alfabeto real e decodificando todos com o zbar, o leitor usado na maioria
 * dos aplicativos de camera: 200 de 200.
 */

const ALFANUMERICO = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:'

/** Versao 1-Q: 26 codewords no total, 13 de dados e 13 de correcao. */
const CODEWORDS_DE_DADOS = 13
const CODEWORDS_DE_CORRECAO = 13
const LADO = 21

// ---------------------------------------------------------------- GF(256) --

/**
 * Aritmetica do corpo finito usado pelo Reed-Solomon.
 *
 * Multiplicar e caro; somar expoentes e barato. Por isso as duas tabelas:
 * uma leva expoente -> valor, a outra faz o caminho de volta.
 */
const EXP = new Uint8Array(512)
const LOG = new Uint8Array(256)

;(function montarTabelas() {
  let x = 1
  for (let i = 0; i < 255; i++) {
    EXP[i] = x
    LOG[x] = i
    x <<= 1
    if (x & 0x100) x ^= 0x11d // polinomio primitivo do QR
  }
  for (let i = 255; i < 512; i++) EXP[i] = EXP[i - 255]
})()

function multiplicar(a: number, b: number): number {
  if (a === 0 || b === 0) return 0
  return EXP[LOG[a] + LOG[b]]
}

/** Polinomio gerador de grau `grau`: (x - a^0)(x - a^1)...(x - a^(grau-1)). */
function polinomioGerador(grau: number): number[] {
  let g = [1]
  for (let i = 0; i < grau; i++) {
    const proximo = new Array<number>(g.length + 1).fill(0)
    for (let j = 0; j < g.length; j++) {
      proximo[j] ^= g[j]
      proximo[j + 1] ^= multiplicar(g[j], EXP[i])
    }
    g = proximo
  }
  return g
}

/** Divisao polinomial: o resto sao os codewords de correcao. */
function correcaoDeErro(dados: number[]): number[] {
  const gerador = polinomioGerador(CODEWORDS_DE_CORRECAO)
  const resto = new Array<number>(CODEWORDS_DE_CORRECAO).fill(0)

  for (const byte of dados) {
    const fator = byte ^ resto[0]
    resto.shift()
    resto.push(0)
    if (fator !== 0) {
      for (let i = 0; i < CODEWORDS_DE_CORRECAO; i++) {
        resto[i] ^= multiplicar(gerador[i + 1], fator)
      }
    }
  }
  return resto
}

// ------------------------------------------------------------ codificacao --

class Bits {
  private readonly bits: number[] = []

  push(valor: number, quantidade: number) {
    for (let i = quantidade - 1; i >= 0; i--) this.bits.push((valor >> i) & 1)
  }

  get tamanho() {
    return this.bits.length
  }

  /** Fecha o fluxo e devolve os 13 codewords de dados, ja com o enchimento. */
  paraCodewords(): number[] {
    const limite = CODEWORDS_DE_DADOS * 8

    // Terminador: ate 4 zeros, ou menos se o espaco acabar antes.
    this.push(0, Math.min(4, limite - this.bits.length))
    // Completa o ultimo byte.
    while (this.bits.length % 8 !== 0) this.bits.push(0)

    const bytes: number[] = []
    for (let i = 0; i < this.bits.length; i += 8) {
      let byte = 0
      for (let j = 0; j < 8; j++) byte = (byte << 1) | this.bits[i + j]
      bytes.push(byte)
    }

    // Enchimento alternado definido pela norma, a partir do primeiro byte livre.
    const ENCHIMENTO = [0xec, 0x11]
    for (let i = 0; bytes.length < CODEWORDS_DE_DADOS; i++) {
      bytes.push(ENCHIMENTO[i % 2])
    }
    return bytes
  }
}

function codificarAlfanumerico(texto: string): number[] {
  const bits = new Bits()
  bits.push(0b0010, 4) // modo alfanumerico
  bits.push(texto.length, 9) // contador, 9 bits na versao 1

  for (let i = 0; i < texto.length; i += 2) {
    const primeiro = ALFANUMERICO.indexOf(texto[i])
    if (primeiro < 0) throw new Error(`Caractere fora do modo alfanumerico: ${texto[i]}`)

    if (i + 1 < texto.length) {
      const segundo = ALFANUMERICO.indexOf(texto[i + 1])
      if (segundo < 0) throw new Error(`Caractere fora do modo alfanumerico: ${texto[i + 1]}`)
      bits.push(primeiro * 45 + segundo, 11)
    } else {
      bits.push(primeiro, 6)
    }
  }
  return bits.paraCodewords()
}

// ----------------------------------------------------------------- matriz --

type Matriz = (0 | 1 | null)[][]

function matrizVazia(): Matriz {
  return Array.from({ length: LADO }, () => new Array<0 | 1 | null>(LADO).fill(null))
}

function desenharLocalizador(m: Matriz, linha: number, coluna: number) {
  for (let i = -1; i <= 7; i++) {
    for (let j = -1; j <= 7; j++) {
      const y = linha + i
      const x = coluna + j
      if (y < 0 || y >= LADO || x < 0 || x >= LADO) continue

      // O anel de fora e o separador: sempre claro, e e ele que faz o leitor
      // distinguir o localizador do resto do desenho.
      if (i === -1 || i === 7 || j === -1 || j === 7) {
        m[y][x] = 0
        continue
      }

      const naBorda = i === 0 || i === 6 || j === 0 || j === 6
      const noCentro = i >= 2 && i <= 4 && j >= 2 && j <= 4
      m[y][x] = naBorda || noCentro ? 1 : 0
    }
  }
}

/** Padroes fixos: localizadores, separadores, temporizacao e o modulo escuro. */
function desenharPadroes(m: Matriz) {
  desenharLocalizador(m, 0, 0)
  desenharLocalizador(m, 0, LADO - 7)
  desenharLocalizador(m, LADO - 7, 0)

  for (let i = 8; i < LADO - 8; i++) {
    const valor: 0 | 1 = i % 2 === 0 ? 1 : 0
    m[6][i] = valor
    m[i][6] = valor
  }

  m[LADO - 8][8] = 1 // modulo escuro, sempre aceso

  // Reserva as casas da informacao de formato para nao receberem dados.
  for (let i = 0; i < 9; i++) {
    if (m[8][i] === null) m[8][i] = 0
    if (m[i][8] === null) m[i][8] = 0
  }
  for (let i = 0; i < 8; i++) {
    if (m[8][LADO - 1 - i] === null) m[8][LADO - 1 - i] = 0
    if (m[LADO - 1 - i][8] === null) m[LADO - 1 - i][8] = 0
  }
}

const MASCARAS: ((linha: number, coluna: number) => boolean)[] = [
  (l, c) => (l + c) % 2 === 0,
  (l) => l % 2 === 0,
  (_l, c) => c % 3 === 0,
  (l, c) => (l + c) % 3 === 0,
  (l, c) => (Math.floor(l / 2) + Math.floor(c / 3)) % 2 === 0,
  (l, c) => ((l * c) % 2) + ((l * c) % 3) === 0,
  (l, c) => (((l * c) % 2) + ((l * c) % 3)) % 2 === 0,
  (l, c) => (((l + c) % 2) + ((l * c) % 3)) % 2 === 0,
]

/**
 * Preenche a area de dados em ziguezague, de baixo para cima, dois modulos por
 * vez, pulando a coluna 6 (a de temporizacao) e as casas ja ocupadas.
 */
function preencherDados(m: Matriz, bits: number[], mascara: number) {
  const aplicar = MASCARAS[mascara]
  let posicao = 0
  let subindo = true

  // A coluna 6 e de temporizacao: ao chegar nela, o par desliza uma casa para a
  // esquerda e a contagem segue dali. Decrementar de dois em dois sem esse
  // ajuste repete uma coluna e deixa a coluna 0 de fora — foi assim que a
  // primeira versao deste arquivo corrompeu os dois ultimos codewords.
  for (let direita = LADO - 1; direita >= 1; direita -= 2) {
    if (direita === 6) direita = 5

    for (let passo = 0; passo < LADO; passo++) {
      const linha = subindo ? LADO - 1 - passo : passo

      for (const coluna of [direita, direita - 1]) {
        if (m[linha][coluna] !== null) continue
        const bit = posicao < bits.length ? bits[posicao] : 0
        posicao++
        m[linha][coluna] = (aplicar(linha, coluna) ? bit ^ 1 : bit) as 0 | 1
      }
    }
    subindo = !subindo
  }
}

/** BCH(15,5) da informacao de formato; nivel Q = 0b11. */
function informacaoDeFormato(mascara: number): number {
  const dados = (0b11 << 3) | mascara
  let resto = dados << 10
  for (let i = 14; i >= 10; i--) {
    if ((resto >> i) & 1) resto ^= 0b10100110111 << (i - 10)
  }
  return ((dados << 10) | resto) ^ 0b101010000010010
}

/**
 * Grava as duas copias da informacao de formato.
 *
 * Os 15 bits sao percorridos do mais significativo para o menos — n = 0 e o
 * bit 14. Escrever na ordem contraria produz um simbolo de aparencia perfeita
 * que nenhum leitor abre, porque o leitor confere o BCH antes dos dados.
 */
function gravarFormato(m: Matriz, mascara: number) {
  const formato = informacaoDeFormato(mascara)
  const bit = (n: number): 0 | 1 => ((formato >> (14 - n)) & 1) as 0 | 1

  // Copia 1: em volta do localizador superior esquerdo.
  for (let n = 0; n <= 5; n++) m[8][n] = bit(n)
  m[8][7] = bit(6)
  m[8][8] = bit(7)
  m[7][8] = bit(8)
  for (let n = 9; n <= 14; n++) m[14 - n][8] = bit(n)

  // Copia 2: sobe pela coluna 8 a partir da base, depois segue pela linha 8.
  for (let n = 0; n <= 6; n++) m[LADO - 1 - n][8] = bit(n)
  for (let n = 7; n <= 14; n++) m[8][6 + n] = bit(n)
}

// --------------------------------------------------------------- penalidade --

/** As quatro regras da norma para escolher a mascara menos ambigua. */
function penalidade(m: Matriz): number {
  const em = (l: number, c: number) => m[l][c] === 1
  let total = 0

  // Regra 1: sequencias de 5 ou mais modulos iguais.
  for (let i = 0; i < LADO; i++) {
    for (const porLinha of [true, false]) {
      let anterior: boolean | null = null
      let seguidos = 0
      for (let j = 0; j < LADO; j++) {
        const atual = porLinha ? em(i, j) : em(j, i)
        if (atual === anterior) {
          seguidos++
          if (seguidos === 5) total += 3
          else if (seguidos > 5) total += 1
        } else {
          anterior = atual
          seguidos = 1
        }
      }
    }
  }

  // Regra 2: blocos 2x2 de uma cor so.
  for (let l = 0; l < LADO - 1; l++) {
    for (let c = 0; c < LADO - 1; c++) {
      const a = em(l, c)
      if (a === em(l, c + 1) && a === em(l + 1, c) && a === em(l + 1, c + 1)) total += 3
    }
  }

  // Regra 3: o padrao 1:1:3:1:1 que imita um localizador.
  const ALVO = [true, false, true, true, true, false, true, false, false, false, false]
  const INVERSO = [...ALVO].reverse()
  for (let i = 0; i < LADO; i++) {
    for (let j = 0; j <= LADO - 11; j++) {
      for (const porLinha of [true, false]) {
        const trecho = Array.from({ length: 11 }, (_, k) =>
          porLinha ? em(i, j + k) : em(j + k, i))
        if (ALVO.every((v, k) => v === trecho[k])) total += 40
        if (INVERSO.every((v, k) => v === trecho[k])) total += 40
      }
    }
  }

  // Regra 4: desequilibrio entre claro e escuro.
  let escuros = 0
  for (let l = 0; l < LADO; l++) for (let c = 0; c < LADO; c++) if (em(l, c)) escuros++
  const proporcao = (escuros * 100) / (LADO * LADO)
  total += Math.floor(Math.abs(proporcao - 50) / 5) * 10

  return total
}

// --------------------------------------------------------------- fachada --

/**
 * Devolve a matriz do QR: `true` e modulo escuro.
 *
 * O texto precisa caber no modo alfanumerico (digitos, maiusculas e
 * ` $%*+-./:`) e ter no maximo 16 caracteres.
 */
export function gerarQr(texto: string): boolean[][] {
  const conteudo = texto.trim().toUpperCase()
  if (conteudo.length === 0) throw new Error('QR sem conteudo')
  if (conteudo.length > 16) throw new Error('A versao 1-Q cabe no maximo 16 caracteres')

  const dados = codificarAlfanumerico(conteudo)
  const completo = [...dados, ...correcaoDeErro(dados)]

  const bits: number[] = []
  for (const byte of completo) {
    for (let i = 7; i >= 0; i--) bits.push((byte >> i) & 1)
  }

  let melhor: Matriz | null = null
  let menorPenalidade = Infinity

  for (let mascara = 0; mascara < 8; mascara++) {
    const m = matrizVazia()
    desenharPadroes(m)
    preencherDados(m, bits, mascara)
    gravarFormato(m, mascara)

    const nota = penalidade(m)
    if (nota < menorPenalidade) {
      menorPenalidade = nota
      melhor = m
    }
  }

  return melhor!.map((linha) => linha.map((v) => v === 1))
}
