package br.com.medshare.app.dados

import br.com.medshare.app.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Monta o cliente HTTP.
 *
 * A Retrofit fixa a URL base na construção, e o endereço do servidor pode mudar
 * dentro do app — então guardamos a instância junto do endereço que a gerou e
 * refazemos quando ele muda. Sem isso, trocar o servidor na tela de login não
 * teria efeito nenhum até reiniciar o app.
 */
object Rede {

    private val json = Json {
        ignoreUnknownKeys = true      // campo novo na API não quebra o app
        encodeDefaults = true
        explicitNulls = false
    }

    private var enderecoAtual: String? = null
    private var apiAtual: ApiMedShare? = null

    @Volatile
    private var token: String? = null

    fun definirToken(novo: String?) {
        token = novo
    }

    fun api(endereco: String): ApiMedShare {
        val guardada = apiAtual
        if (guardada != null && endereco == enderecoAtual) {
            return guardada
        }
        val nova = construir(endereco)
        enderecoAtual = endereco
        apiAtual = nova
        return nova
    }

    fun enderecoPadrao(): String = BuildConfig.API_PADRAO.removeSuffix("/") + "/"

    private fun construir(endereco: String): ApiMedShare {
        val cliente = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)   // o envio de foto é o mais demorado
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(autenticacao())
            .build()

        return Retrofit.Builder()
            .baseUrl(endereco)
            .client(cliente)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiMedShare::class.java)
    }

    private fun autenticacao() = Interceptor { corrente ->
        val requisicao = corrente.request()
        val comToken = token?.let {
            requisicao.newBuilder().addHeader("Authorization", "Bearer $it").build()
        } ?: requisicao
        corrente.proceed(comToken)
    }

    /**
     * Traduz qualquer falha para ErroDaApi.
     *
     * Sem isso, a tela receberia um HttpException com o corpo cru do JSON, e
     * cada tela teria que saber desmontar esse corpo. O erro chega pronto: a
     * mensagem que a API escreveu e a regra que foi violada.
     */
    fun traduzir(erro: Throwable): ErroDaApi = when (erro) {
        is ErroDaApi -> erro

        is HttpException -> {
            val corpo = runCatching { erro.response()?.errorBody()?.string() }.getOrNull()
            val detalhe = corpo?.let {
                runCatching { json.decodeFromString(RespostaDeErro.serializer(), it) }.getOrNull()
            }
            val mensagem = detalhe?.mensagem
                ?: detalhe?.campos?.joinToString("; ") { "${it.campo}: ${it.problema}" }
                ?: quandoNaoHaMensagem(erro.code())
            ErroDaApi(erro.code(), mensagem, detalhe?.regra)
        }

        is IOException -> ErroDaApi(0,
            "Não consegui falar com o servidor. Confira sua conexão e o endereço do servidor.")

        else -> ErroDaApi(0, erro.message ?: "Algo deu errado. Tente de novo.")
    }

    private fun quandoNaoHaMensagem(codigo: Int) = when (codigo) {
        401 -> "Sua sessão expirou. Entre de novo."
        403 -> "Seu perfil não tem permissão para isso."
        404 -> "Não encontramos o que você procurou."
        in 500..599 -> "O servidor teve um problema. Tente de novo em instantes."
        else -> "Não foi possível completar a operação."
    }
}
