package br.com.medshare.app.dados

import kotlinx.serialization.Serializable

/**
 * O contrato com a API, em uma lista só.
 *
 * Datas ficam como texto no formato que a API devolve (AAAA-MM-DD para data,
 * ISO para data e hora). Converter aqui exigiria uma biblioteca a mais, e o
 * app só precisa exibir — então a formatação acontece na tela, não no modelo.
 */

@Serializable
data class Sessao(
    val tokenDeAcesso: String,
    val tokenDeRenovacao: String,
    val expiraEmSegundos: Long,
    val usuarioId: Long,
    val nome: String,
    val papeis: List<String>,
) {
    fun ehDoador() = papeis.contains("DOADOR")
    fun ehBeneficiario() = papeis.contains("BENEFICIARIO")
}

@Serializable
data class PedidoDeLogin(val email: String, val senha: String)

@Serializable
data class PedidoDeRenovacao(val tokenDeRenovacao: String)

@Serializable
data class PedidoDeCadastro(
    val nome: String,
    val cpf: String,
    val email: String,
    val senha: String,
    val telefone: String? = null,
    val cep: String,
    val logradouro: String,
    val numero: String,
    val complemento: String? = null,
    val bairro: String,
    val municipioId: Short,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val papeis: List<String>,
)

@Serializable
data class Municipio(val id: Short, val nome: String)

@Serializable
data class Medicamento(
    val id: Long,
    val nomeComercial: String,
    val principioAtivo: String,
    val apresentacao: String,
    val laboratorio: String? = null,
    val pmc: Double,
    val altoCusto: Boolean,
)

@Serializable
data class Doacao(
    val codigo: String,
    val medicamento: String,
    val principioAtivo: String,
    val lote: String,
    val validade: String,
    val status: String,
    val pontoDeColeta: String? = null,
    val criadoEm: String,
    val atualizadoEm: String,
)

@Serializable
data class Evento(
    val quando: String,
    val tipo: String,
    val descricao: String,
    val statusAnterior: String? = null,
    val statusNovo: String? = null,
)

/** O agendamento atual traz o código que o doador mostra no balcão (UC03). */
@Serializable
data class AgendamentoAtual(
    val pontoDeColeta: String,
    val endereco: String,
    val dataHora: String,
    val codigoEntrega: String,
)

@Serializable
data class DoacaoDetalhada(
    val doacao: Doacao,
    val historico: List<Evento>,
    val agendamento: AgendamentoAtual? = null,
    /** UC02 A2 - cancelada e o reagendamento único ainda não foi usado. */
    val podeReagendar: Boolean = false,
)

/** UC01 - [quantidade] caixas iguais; [lacreDeclarado] é a declaração da RN01. */
@Serializable
data class PedidoDeDoacao(
    val medicamentoId: Long,
    val lote: String,
    val validade: String,
    val fotoUrl: String,
    val quantidade: Int = 1,
    val lacreDeclarado: Boolean,
)

/** UC10 A2 - a foto nova que a central pediu. */
@Serializable
data class PedidoDeNovaFoto(val fotoUrl: String)

/** UC05/UC06 - caixa oferecida, com prazo para aceitar. */
@Serializable
data class Oferta(
    val id: Long,
    val status: String,
    val medicamento: String,
    val apresentacao: String,
    val validade: String,
    val pontoDeColeta: String,
    val enderecoDoPonto: String,
    val horarioDoPonto: String,
    val expiraEm: String,
)

@Serializable
data class Notificacao(
    val id: Long,
    val titulo: String,
    val corpo: String,
    val tipo: String,
    val lida: Boolean,
    val quando: String,
)

@Serializable
data class Contagem(val quantidade: Long = 0)

/** UC07 A3 - quem pode retirar no lugar do beneficiário. */
@Serializable
data class Procurador(val id: Long, val nome: String, val cpf: String)

@Serializable
data class PedidoDeProcurador(val nome: String, val cpf: String)

/** RN09 - endereço pelo CEP; [atendido] diz se fica na Grande São Paulo. */
@Serializable
data class EnderecoDoCep(
    val cep: String,
    val logradouro: String = "",
    val bairro: String = "",
    val municipio: String = "",
    val uf: String = "",
    val municipioId: Short? = null,
    val atendido: Boolean = false,
)

@Serializable
data class PedidoDeAgendamento(val pontoDeColetaId: Long, val dataHora: String)

@Serializable
data class PontoDeColeta(
    val id: Long,
    val nome: String,
    val endereco: String,
    val bairro: String,
    val municipio: String,
    val horario: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
data class Necessidade(
    val id: Long,
    val medicamento: String,
    val principioAtivo: String,
    val ativa: Boolean,
    val temReceitaValida: Boolean,
    val validadeDaReceita: String? = null,
    /** UC07 A1 - a receita não bateu no balcão; precisa enviar outra. */
    val emRevisao: Boolean = false,
    val motivoRevisao: String? = null,
    /** UC07 A2 - perdeu uma caixa por validade e está na frente da fila. */
    val prioridade: Boolean = false,
    val criadaEm: String,
)

@Serializable
data class PedidoDeNecessidade(val medicamentoId: Long)

@Serializable
data class PedidoDeReceita(
    val fotoUrl: String,
    val dataEmissao: String,
    val validade: String,
    val crmMedico: String,
    val ufCrm: String,
)

@Serializable
data class PedidoDeVerificacaoCadUnico(val nis: String)

@Serializable
data class RespostaDoCadUnico(
    val confirmado: Boolean,
    val validoAte: String? = null,
    val fonte: String? = null,
    /** NIS com formato certo, mas não encontrado: foi para conferência humana. */
    val precisaDeAnaliseHumana: Boolean = false,
    val observacao: String? = null,
)

@Serializable
data class Reserva(
    val codigoRetirada: String,
    val medicamento: String,
    val apresentacao: String,
    val pontoDeColeta: String? = null,
    val enderecoDoPonto: String? = null,
    val horarioDoPonto: String? = null,
    val status: String,
    val expiraEm: String,
)

@Serializable
data class FotoEnviada(val nome: String, val url: String)

/** O formato de página que o Spring Data devolve. */
@Serializable
data class Pagina<T>(
    val content: List<T> = emptyList(),
    val totalElements: Long = 0,
    val number: Int = 0,
    val totalPages: Int = 0,
)

@Serializable
data class CampoInvalido(val campo: String, val problema: String)

@Serializable
data class RespostaDeErro(
    val status: Int = 0,
    val erro: String? = null,
    val mensagem: String? = null,
    val regra: String? = null,
    val campos: List<CampoInvalido>? = null,
)

/**
 * Erro já traduzido para algo que dá para mostrar na tela.
 * Guarda a regra separada (RN02, RN07...) porque a interface mostra as duas
 * coisas de formas diferentes.
 */
class ErroDaApi(
    val codigoHttp: Int,
    mensagem: String,
    val regra: String? = null,
) : Exception(mensagem)
