package br.com.medshare.app

import br.com.medshare.app.dados.*
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import kotlin.test.*

/**
 * Prova que o app e a API falam a mesma língua.
 *
 * Roda na JVM, sem emulador, mas usa exatamente a interface Retrofit e os
 * modelos que o app usa no aparelho. É o que pega o erro mais caro desse tipo
 * de projeto: um campo renomeado no back-end que só apareceria como tela em
 * branco no celular de alguém.
 *
 * Só executa quando existe uma API de verdade no endereço informado:
 *     ./gradlew test -Dmedshare.api=http://localhost:8080
 * Sem isso os testes são ignorados, para não quebrar o build de quem clonou o
 * projeto e ainda não subiu o servidor.
 */
class ContratoDaApiTest {

    private val endereco: String? =
        System.getProperty("medshare.api")?.takeIf { it.isNotBlank() }?.removeSuffix("/")?.plus("/")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private var token: String? = null

    private fun api(): ApiMedShare {
        val cliente = OkHttpClient.Builder()
            .addInterceptor(Interceptor { corrente ->
                val requisicao = token?.let {
                    corrente.request().newBuilder().addHeader("Authorization", "Bearer $it").build()
                } ?: corrente.request()
                corrente.proceed(requisicao)
            })
            .build()

        return Retrofit.Builder()
            .baseUrl(endereco!!)
            .client(cliente)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiMedShare::class.java)
    }

    private fun exigirServidor(): ApiMedShare {
        assertNotNull(endereco, "sem -Dmedshare.api, o teste de contrato não roda")
        return api()
    }

    @Test
    fun `login devolve a sessao no formato que o app espera`() = runTest {
        if (endereco == null) return@runTest
        val servico = exigirServidor()

        val sessao = servico.entrar(PedidoDeLogin("ana@medshare.test", "medshare123"))

        assertTrue(sessao.tokenDeAcesso.isNotBlank(), "token de acesso veio vazio")
        assertTrue(sessao.tokenDeRenovacao.isNotBlank(), "token de renovação veio vazio")
        assertTrue(sessao.expiraEmSegundos > 0)
        assertTrue(sessao.ehDoador(), "Ana deveria ter o papel DOADOR")
        assertFalse(sessao.ehBeneficiario())
    }

    @Test
    fun `lista de municipios traz os 39 da Grande Sao Paulo`() = runTest {
        if (endereco == null) return@runTest
        val municipios = exigirServidor().municipios()

        assertEquals(39, municipios.size, "a RN09 fala em 39 municípios")
        assertTrue(municipios.any { it.nome == "São Paulo" })
        assertTrue(municipios.all { it.nome.isNotBlank() })
    }

    @Test
    fun `busca de medicamento devolve so os de alto custo`() = runTest {
        if (endereco == null) return@runTest
        val servico = exigirServidor()
        token = servico.entrar(PedidoDeLogin("ana@medshare.test", "medshare123")).tokenDeAcesso

        val resultados = servico.buscarMedicamentos("a").content

        assertTrue(resultados.isNotEmpty(), "a carga de demonstração deveria ter medicamentos")
        assertTrue(resultados.all { it.altoCusto }, "RN07: só alto custo aparece na busca")
        assertTrue(resultados.all { it.pmc >= 150.0 })
    }

    @Test
    fun `pontos de coleta chegam com endereco e horario preenchidos`() = runTest {
        if (endereco == null) return@runTest
        val servico = exigirServidor()
        token = servico.entrar(PedidoDeLogin("ana@medshare.test", "medshare123")).tokenDeAcesso

        val pontos = servico.pontosProximos()

        assertTrue(pontos.isNotEmpty())
        pontos.forEach {
            assertTrue(it.nome.isNotBlank())
            assertTrue(it.endereco.isNotBlank())
            assertTrue(it.horario.isNotBlank())
        }
    }

    @Test
    fun `minhas doacoes vem paginado e com os campos que a tela usa`() = runTest {
        if (endereco == null) return@runTest
        val servico = exigirServidor()
        token = servico.entrar(PedidoDeLogin("ana@medshare.test", "medshare123")).tokenDeAcesso

        val pagina = servico.minhasDoacoes()

        assertTrue(pagina.content.isNotEmpty(), "a carga de demonstração cria 3 doações da Ana")
        val doacao = pagina.content.first()
        assertTrue(doacao.codigo.startsWith("MS-"))
        assertTrue(doacao.medicamento.isNotBlank())
        assertTrue(doacao.status.isNotBlank())
    }

    @Test
    fun `detalhe da doacao traz o historico completo`() = runTest {
        if (endereco == null) return@runTest
        val servico = exigirServidor()
        token = servico.entrar(PedidoDeLogin("ana@medshare.test", "medshare123")).tokenDeAcesso

        val codigo = servico.minhasDoacoes().content.first().codigo
        val detalhe = servico.detalharDoacao(codigo)

        assertEquals(codigo, detalhe.doacao.codigo)
        assertTrue(detalhe.historico.isNotEmpty(), "RN06: toda doação nasce com evento de cadastro")
        assertEquals("CADASTRO", detalhe.historico.first().tipo)
    }

    @Test
    fun `beneficiario sem CadUnico recebe a RN08 explicada`() = runTest {
        if (endereco == null) return@runTest
        val servico = exigirServidor()
        token = servico.entrar(PedidoDeLogin("bruno@medshare.test", "medshare123")).tokenDeAcesso

        val necessidade = servico.criarNecessidade(PedidoDeNecessidade(1))
        val erro = assertFails { servico.reservar(PedidoDeReserva(necessidade.id)) }

        // O app precisa conseguir extrair a regra e a mensagem — é o que a tela mostra.
        val traduzido = Rede.traduzir(erro)
        assertEquals("RN08", traduzido.regra, "a resposta deveria citar a RN08")
        assertTrue(
            traduzido.message!!.contains("CadÚnico"),
            "a mensagem deveria explicar o que falta: ${traduzido.message}",
        )
    }

    @Test
    fun `erro de regra chega ao app com regra e mensagem separadas`() = runTest {
        if (endereco == null) return@runTest
        val servico = exigirServidor()
        token = servico.entrar(PedidoDeLogin("ana@medshare.test", "medshare123")).tokenDeAcesso

        val medicamento = servico.buscarMedicamentos("a").content.first()
        val venceEmDezDias = java.time.LocalDate.now().plusDays(10).toString()

        val erro = assertFails {
            servico.cadastrarDoacao(
                PedidoDeDoacao(medicamento.id, "LOTE-TESTE", venceEmDezDias, "http://x/f.jpg"),
            )
        }

        // É esta separação que a tela usa: a regra vira etiqueta, a mensagem vira texto.
        val traduzido = Rede.traduzir(erro)
        assertEquals("RN02", traduzido.regra, "validade curta deveria citar a RN02")
        assertTrue(
            traduzido.message!!.contains("30"),
            "a mensagem deveria dizer quantos dias faltam: ${traduzido.message}",
        )
    }
}
