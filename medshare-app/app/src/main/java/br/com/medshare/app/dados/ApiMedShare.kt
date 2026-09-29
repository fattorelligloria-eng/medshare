package br.com.medshare.app.dados

import okhttp3.MultipartBody
import retrofit2.http.*

interface ApiMedShare {

    // --- entrada ---

    @POST("api/autenticacao/login")
    suspend fun entrar(@Body pedido: PedidoDeLogin): Sessao

    @POST("api/autenticacao/renovacao")
    suspend fun renovar(@Body pedido: PedidoDeRenovacao): Sessao

    @POST("api/autenticacao/cadastro")
    suspend fun cadastrar(@Body pedido: PedidoDeCadastro): Sessao

    @GET("api/municipios")
    suspend fun municipios(): List<Municipio>

    // --- catálogo ---

    @GET("api/medicamentos")
    suspend fun buscarMedicamentos(
        @Query("termo") termo: String,
        @Query("size") quantidade: Int = 20,
    ): Pagina<Medicamento>

    @GET("api/pontos-de-coleta/proximos")
    suspend fun pontosProximos(): List<PontoDeColeta>

    // --- fotos ---

    @Multipart
    @POST("api/fotos")
    suspend fun enviarFoto(@Part arquivo: MultipartBody.Part): FotoEnviada

    // --- doador ---

    @POST("api/doacoes")
    suspend fun cadastrarDoacao(@Body pedido: PedidoDeDoacao): Doacao

    @GET("api/doacoes/minhas")
    suspend fun minhasDoacoes(@Query("size") quantidade: Int = 50): Pagina<Doacao>

    @GET("api/doacoes/{codigo}")
    suspend fun detalharDoacao(@Path("codigo") codigo: String): DoacaoDetalhada

    @POST("api/doacoes/{codigo}/agendamento")
    suspend fun agendar(
        @Path("codigo") codigo: String,
        @Body pedido: PedidoDeAgendamento,
    ): Doacao

    // --- beneficiário ---

    @POST("api/necessidades/cadunico")
    suspend fun verificarCadUnico(
        @Body pedido: PedidoDeVerificacaoCadUnico,
    ): RespostaDoCadUnico

    @GET("api/necessidades")
    suspend fun minhasNecessidades(): List<Necessidade>

    @POST("api/necessidades")
    suspend fun criarNecessidade(@Body pedido: PedidoDeNecessidade): Necessidade

    @POST("api/necessidades/{id}/receita")
    suspend fun anexarReceita(
        @Path("id") id: Long,
        @Body pedido: PedidoDeReceita,
    ): Necessidade

    @POST("api/reservas")
    suspend fun reservar(@Body pedido: PedidoDeReserva): Reserva

    @GET("api/reservas/minhas")
    suspend fun minhasReservas(): List<Reserva>

    @DELETE("api/reservas/{codigo}")
    suspend fun cancelarReserva(@Path("codigo") codigo: String): Reserva
}
