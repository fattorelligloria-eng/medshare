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

    @GET("api/enderecos/{cep}")
    suspend fun enderecoPorCep(@Path("cep") cep: String): EnderecoDoCep

    // --- catálogo ---

    @GET("api/medicamentos")
    suspend fun buscarMedicamentos(
        @Query("termo") termo: String,
        @Query("size") quantidade: Int = 20,
    ): Pagina<Medicamento>

    @GET("api/pontos-de-coleta/proximos")
    suspend fun pontosProximos(): List<PontoDeColeta>

    /** UC02 - horários com vaga nas próximas duas semanas. */
    @GET("api/pontos-de-coleta/{id}/horarios")
    suspend fun horarios(@Path("id") pontoId: Long): List<String>

    // --- fotos ---

    @Multipart
    @POST("api/fotos")
    suspend fun enviarFoto(@Part arquivo: MultipartBody.Part): FotoEnviada

    // --- doador ---

    /** Uma doação por caixa: devolve todas as criadas. */
    @POST("api/doacoes")
    suspend fun cadastrarDoacao(@Body pedido: PedidoDeDoacao): List<Doacao>

    @GET("api/doacoes/minhas")
    suspend fun minhasDoacoes(@Query("size") quantidade: Int = 50): Pagina<Doacao>

    @GET("api/doacoes/{codigo}")
    suspend fun detalharDoacao(@Path("codigo") codigo: String): DoacaoDetalhada

    @POST("api/doacoes/{codigo}/agendamento")
    suspend fun agendar(
        @Path("codigo") codigo: String,
        @Body pedido: PedidoDeAgendamento,
    ): Doacao

    @DELETE("api/doacoes/{codigo}/agendamento")
    suspend fun cancelarAgendamento(@Path("codigo") codigo: String): Doacao

    @POST("api/doacoes/{codigo}/foto")
    suspend fun trocarFoto(
        @Path("codigo") codigo: String,
        @Body pedido: PedidoDeNovaFoto,
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

    @GET("api/necessidades/procuradores")
    suspend fun procuradores(): List<Procurador>

    @POST("api/necessidades/procuradores")
    suspend fun cadastrarProcurador(@Body pedido: PedidoDeProcurador): Procurador

    @DELETE("api/necessidades/procuradores/{id}")
    suspend fun removerProcurador(@Path("id") id: Long): retrofit2.Response<Unit>

    /** UC05/UC06 - ofertas recebidas; aceitar vira reserva. */
    @GET("api/ofertas/minhas")
    suspend fun minhasOfertas(): List<Oferta>

    @POST("api/ofertas/{id}/aceite")
    suspend fun aceitarOferta(@Path("id") id: Long): Reserva

    @POST("api/ofertas/{id}/recusa")
    suspend fun recusarOferta(@Path("id") id: Long): Oferta

    @GET("api/reservas/minhas")
    suspend fun minhasReservas(): List<Reserva>

    @DELETE("api/reservas/{codigo}")
    suspend fun cancelarReserva(@Path("codigo") codigo: String): Reserva

    // --- notificações ---

    @GET("api/notificacoes")
    suspend fun notificacoes(@Query("size") quantidade: Int = 50): Pagina<Notificacao>

    @GET("api/notificacoes/nao-lidas")
    suspend fun naoLidas(): Contagem

    @POST("api/notificacoes/{id}/leitura")
    suspend fun marcarComoLida(@Path("id") id: Long): retrofit2.Response<Unit>
}
