package br.com.medshare.app.dados

import android.content.Context
import br.com.medshare.app.ui.CapturaDeFoto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * Único ponto de conversa com a API.
 *
 * As telas nunca chamam a Retrofit direto: pedem aqui e recebem Result. Assim o
 * tratamento de erro acontece em um lugar só, e nenhuma tela precisa saber que
 * existe HTTP por baixo.
 */
class Repositorio(contexto: Context) {

    val guarda = GuardaDaSessao(contexto.applicationContext)

    private suspend fun api(): ApiMedShare {
        val endereco = guarda.servidor.first() ?: Rede.enderecoPadrao()
        Rede.definirToken(guarda.sessao.first()?.tokenDeAcesso)
        return Rede.api(endereco)
    }

    /**
     * Faz a chamada e, se o token de acesso venceu (401), troca pelo de
     * renovação e tenta mais uma vez. Se nem a renovação vale, a sessão é
     * apagada — e o app volta sozinho para o login, porque observa a sessão.
     */
    private suspend fun <T> chamar(bloco: suspend (ApiMedShare) -> T): Result<T> {
        val primeira = tentar(bloco)
        val erro = primeira.exceptionOrNull() as? ErroDaApi
        if (erro?.codigoHttp != 401 || guarda.sessao.first() == null) {
            return primeira
        }
        if (!renovarSessao()) {
            sair()
            return Result.failure(ErroDaApi(401, "Sua sessão expirou. Entre de novo."))
        }
        return tentar(bloco)
    }

    private suspend fun <T> tentar(bloco: suspend (ApiMedShare) -> T): Result<T> =
        try {
            Result.success(bloco(api()))
        } catch (cancelado: CancellationException) {
            // Tela fechada no meio da chamada: o cancelamento precisa seguir
            // adiante, e não virar uma mensagem de erro.
            throw cancelado
        } catch (erro: Throwable) {
            Result.failure(Rede.traduzir(erro))
        }

    private suspend fun renovarSessao(): Boolean {
        val tokenDeRenovacao = guarda.sessao.first()?.tokenDeRenovacao ?: return false
        return tentar { it.renovar(PedidoDeRenovacao(tokenDeRenovacao)) }
            .onSuccess { guarda.guardarSessao(it) }
            .isSuccess
    }

    // --- entrada ---

    suspend fun entrar(email: String, senha: String): Result<Sessao> =
        chamar { it.entrar(PedidoDeLogin(email.trim(), senha)) }
            .onSuccess { guarda.guardarSessao(it) }

    suspend fun cadastrar(pedido: PedidoDeCadastro): Result<Sessao> =
        chamar { it.cadastrar(pedido) }.onSuccess { guarda.guardarSessao(it) }

    suspend fun sair() {
        guarda.limparSessao()
        Rede.definirToken(null)
    }

    suspend fun municipios(): Result<List<Municipio>> = chamar { it.municipios() }

    // --- catálogo ---

    suspend fun buscarMedicamentos(termo: String): Result<List<Medicamento>> =
        chamar { it.buscarMedicamentos(termo).content }

    suspend fun pontosProximos(): Result<List<PontoDeColeta>> = chamar { it.pontosProximos() }

    // --- fotos ---

    suspend fun enviarFoto(arquivo: File): Result<FotoEnviada> = chamar { servico ->
        // Foto de câmera de celular pode passar dos 8 MB que o servidor aceita.
        withContext(Dispatchers.IO) { CapturaDeFoto.reduzirSeNecessario(arquivo) }
        val corpo = arquivo.asRequestBody("image/jpeg".toMediaType())
        val parte = MultipartBody.Part.createFormData("arquivo", arquivo.name, corpo)
        servico.enviarFoto(parte)
    }

    // --- doador ---

    suspend fun cadastrarDoacao(
        medicamentoId: Long,
        lote: String,
        validade: String,
        fotoUrl: String,
    ): Result<Doacao> =
        chamar { it.cadastrarDoacao(PedidoDeDoacao(medicamentoId, lote.trim(), validade, fotoUrl)) }

    suspend fun minhasDoacoes(): Result<List<Doacao>> = chamar { it.minhasDoacoes().content }

    suspend fun detalharDoacao(codigo: String): Result<DoacaoDetalhada> =
        chamar { it.detalharDoacao(codigo) }

    suspend fun agendar(codigo: String, pontoId: Long, dataHora: String): Result<Doacao> =
        chamar { it.agendar(codigo, PedidoDeAgendamento(pontoId, dataHora)) }

    // --- beneficiário ---

    suspend fun verificarCadUnico(nis: String): Result<RespostaDoCadUnico> =
        chamar { it.verificarCadUnico(PedidoDeVerificacaoCadUnico(nis.filter(Char::isDigit))) }

    suspend fun minhasNecessidades(): Result<List<Necessidade>> = chamar { it.minhasNecessidades() }

    suspend fun criarNecessidade(medicamentoId: Long): Result<Necessidade> =
        chamar { it.criarNecessidade(PedidoDeNecessidade(medicamentoId)) }

    suspend fun anexarReceita(
        necessidadeId: Long,
        fotoUrl: String,
        dataEmissao: String,
        validade: String,
        crm: String,
        uf: String,
    ): Result<Necessidade> = chamar {
        it.anexarReceita(
            necessidadeId,
            PedidoDeReceita(fotoUrl, dataEmissao, validade, crm.trim(), uf.trim().uppercase()),
        )
    }

    suspend fun reservar(necessidadeId: Long): Result<Reserva> =
        chamar { it.reservar(PedidoDeReserva(necessidadeId)) }

    suspend fun minhasReservas(): Result<List<Reserva>> = chamar { it.minhasReservas() }

    suspend fun cancelarReserva(codigo: String): Result<Reserva> =
        chamar { it.cancelarReserva(codigo) }
}
