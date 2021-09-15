package com.zupacademy.henio.pix

import com.google.protobuf.Timestamp
import com.zupacademy.henio.pix.exceptions.GlobalExceptionHandler
import com.zupacademy.henio.pix.grpc.*
import com.zupacademy.henio.pix.shared.grpc.KeyManagerGrpcFactory
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Replaces
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.hateoas.JsonError
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest
internal class CarregaChaveControllerTest {

    @field:Inject
    lateinit var carregaChaveStub: KeymanagerCarregaGrpcServiceGrpc.KeymanagerCarregaGrpcServiceBlockingStub

    @field:Inject
    lateinit var listaChaveStub: KeymanagerListaGrpcServiceGrpc.KeymanagerListaGrpcServiceBlockingStub

    @field:Inject
    @field:Client("/")
    lateinit var httpClient: HttpClient

    val clienteId = UUID.randomUUID().toString()
    val pixId = UUID.randomUUID().toString()

    val CHAVE_EMAIL = "teste@teste.com.br"
    val CHAVE_CELULAR = "+5511912345678"
    val CONTA_CORRENTE = TipoConta.CONTA_CORRENTE
    val TIPO_DE_CHAVE_EMAIL = TipoChave.EMAIL
    val TIPO_DE_CHAVE_CELULAR = TipoChave.CELULAR
    val INSTITUICAO = "Itau"
    val TITULAR = "Woody"
    val CPF_DO_TITULAR = "34597563067"
    val AGENCIA = "0001"
    val NUMERO_DA_CONTA = "1010-1"
    val CHAVE_CRIADA_EM = LocalDateTime.now()

    @Test
    fun `deve carregar uma chave pix existente`() {

        given(carregaChaveStub.carrega(Mockito.any())).willReturn(carregaChavePixResponse(clienteId, pixId))

        val request = HttpRequest.GET<Any>("/api/clientes/$clienteId/pix/$pixId")
        val response = httpClient.toBlocking().exchange(request, Any::class.java)

        Assertions.assertEquals(HttpStatus.OK, response.status)
        Assertions.assertNotNull(response.body())
    }

    val requestGenerica = HttpRequest.GET<Any>("/")

    @Test
    fun `deve retornar 404 quando statusException for not found`() {
        val mensagem = "não encontrado"
        val notFoundException = StatusRuntimeException(Status.NOT_FOUND.withDescription(mensagem))

        val resposta = GlobalExceptionHandler().handle(requestGenerica, notFoundException)

        assertEquals(HttpStatus.NOT_FOUND, resposta.status)
        assertNotNull(resposta.body())
        assertEquals(mensagem, (resposta.body() as JsonError).message)
    }

    @Test
    fun `deve retornar 422 quando statusException for already exists`() {
        val mensagem = "chave já existente"
        val alreadyExistsException = StatusRuntimeException(Status.ALREADY_EXISTS.withDescription(mensagem))

        val resposta = GlobalExceptionHandler().handle(requestGenerica, alreadyExistsException)

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resposta.status)
        assertNotNull(resposta.body())
        assertEquals(mensagem, (resposta.body() as JsonError).message)
    }

    @Test
    fun `deve retornar 400 quando statusException for invalidArgument`() {
        val mensagem = "Dados inválidos"
        val invalidArgumentException = StatusRuntimeException(Status.INVALID_ARGUMENT)

        val resposta = GlobalExceptionHandler().handle(requestGenerica, invalidArgumentException)

        assertEquals(HttpStatus.BAD_REQUEST, resposta.status)
        assertNotNull(resposta.body())
        assertEquals(mensagem, (resposta.body() as JsonError).message)
    }

    @Test
    fun `deve retornar 500 quando qualquer outro erro for lançado`() {

        val internalException = StatusRuntimeException(Status.INTERNAL)

        val resposta = GlobalExceptionHandler().handle(requestGenerica, internalException)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resposta.status)
        assertNotNull(resposta.body())
        assertTrue((resposta.body() as JsonError).message.contains("Não foi possível completar a requisição"))
    }

    @Test
    internal fun `deve listar todas as chaves pix existente`() {

       val respostaGrpc = listaChavePixResponse(clienteId)

        given(listaChaveStub.lista(Mockito.any())).willReturn(respostaGrpc)

        val request = HttpRequest.GET<Any>("/api/clientes/$clienteId/pix/")
        val response = httpClient.toBlocking().exchange(request, List::class.java)

        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.body())
        assertEquals(response.body().size, 2)
    }

    @Factory
    @Replaces(factory = KeyManagerGrpcFactory::class)
    internal class MockitoStubFactory {

        @Singleton
        fun stubCarregaMock() = Mockito.mock(KeymanagerCarregaGrpcServiceGrpc.KeymanagerCarregaGrpcServiceBlockingStub::class.java)

        @Singleton
        fun stubListaMock() = Mockito.mock(KeymanagerListaGrpcServiceGrpc.KeymanagerListaGrpcServiceBlockingStub::class.java)
    }

    private fun carregaChavePixResponse(clienteId: String, pixId: String) =

        CarregaChavePixResponse.newBuilder()
            .setClienteId(clienteId)
            .setPixId(pixId)
            .setChave(
                CarregaChavePixResponse.ChavePix.newBuilder()
                    .setTipoChave(TIPO_DE_CHAVE_EMAIL)
                    .setChave("02467781054")
                    .setConta(CarregaChavePixResponse.ChavePix.ContaInfo.newBuilder()
                        .setTipoConta(TipoConta.CONTA_CORRENTE)
                        .setInstituicao(INSTITUICAO)
                        .setNomeDoTitular(TITULAR)
                        .setCpfDoTitular(CPF_DO_TITULAR)
                        .setAgencia(AGENCIA)
                        .setNumeroDaConta(NUMERO_DA_CONTA)
                        .build()
                   )
                    .setCriadaEm(CHAVE_CRIADA_EM.let {it ->
                        val createdAt = it.atZone(ZoneId.of("UTC")).toInstant()
                        Timestamp.newBuilder()
                            .setSeconds(createdAt.epochSecond)
                            .setNanos(createdAt.nano)
                            .build()
                    })).build()

    private fun listaChavePixResponse(clienteId: String): ListaChavePixResponse {

       val chaveEmail =  ListaChavePixResponse.ChavePix.newBuilder()
            .setPixId(UUID.randomUUID().toString())
            .setTipoChave(TIPO_DE_CHAVE_EMAIL)
            .setChave(CHAVE_EMAIL)
            .setTipoConta(CONTA_CORRENTE)
            .setCriadaEm(CHAVE_CRIADA_EM.let {it ->
                val createdAt = it.atZone(ZoneId.of("UTC")).toInstant()
                Timestamp.newBuilder()
                    .setSeconds(createdAt.epochSecond)
                    .setNanos(createdAt.nano)
                    .build()
            })
            .build()

        val chaveCelular =  ListaChavePixResponse.ChavePix.newBuilder()
            .setPixId(UUID.randomUUID().toString())
            .setTipoChave(TIPO_DE_CHAVE_CELULAR)
            .setChave(CHAVE_CELULAR)
            .setTipoConta(CONTA_CORRENTE)
            .setCriadaEm(CHAVE_CRIADA_EM.let {it ->
                val createdAt = it.atZone(ZoneId.of("UTC")).toInstant()
                Timestamp.newBuilder()
                    .setSeconds(createdAt.epochSecond)
                    .setNanos(createdAt.nano)
                    .build()
            })
            .build()

        return ListaChavePixResponse.newBuilder()
            .setClienteId(clienteId)
            .addAllChaves(listOf(chaveEmail, chaveCelular))
            .build()
    }
}


