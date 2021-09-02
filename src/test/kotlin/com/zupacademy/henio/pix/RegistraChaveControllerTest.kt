package com.zupacademy.henio.pix

import com.zupacademy.henio.pix.exceptions.GlobalExceptionHandler
import com.zupacademy.henio.pix.grpc.KeymanagerRegistraGrpcServiceGrpc
import com.zupacademy.henio.pix.grpc.RegistraChavePixResponse
import com.zupacademy.henio.pix.registra.NovaChavePixRequest
import com.zupacademy.henio.pix.registra.TipoDeChaveRequest
import com.zupacademy.henio.pix.registra.TipoDeContaRequest
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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest
internal class RegistraChaveControllerTest {

    @field:Inject
    lateinit var registraStub: KeymanagerRegistraGrpcServiceGrpc.KeymanagerRegistraGrpcServiceBlockingStub

    @field:Inject
    @field:Client("/")
    lateinit var httpClient: HttpClient

    val clienteId = UUID.randomUUID().toString()
    val pixId = UUID.randomUUID().toString()

    @Test
    fun `deve registrar nova chave`() {

        val respostaGrpc = RegistraChavePixResponse.newBuilder()
            .setClienteId(clienteId)
            .setPixId(pixId)
            .build()

        given(registraStub.registra(Mockito.any())).willReturn(respostaGrpc)

        val novaChavePix = NovaChavePixRequest(tipoDeConta = TipoDeContaRequest.CONTA_CORRENTE,
        chave = "teste@teste.com.br",
            tipoDeChave = TipoDeChaveRequest.EMAIL)

        val request = HttpRequest.POST("/api/clientes/$clienteId/pix", novaChavePix)
        val response = httpClient.toBlocking().exchange(request, NovaChavePixRequest::class.java)

        assertEquals(HttpStatus.CREATED, response.status)
        assertTrue(response.headers.contains("Location"))
        assertTrue(response.header("Location")!!.contains(pixId))

    }

    @Test
    fun `deve retornar 422 quando a chave ja existe`() {

        val mensagem = "Chave já existe"
        val excecao = StatusRuntimeException(Status.ALREADY_EXISTS
            .withDescription(mensagem))

        val response = GlobalExceptionHandler().handle(HttpRequest.POST("/", Any::class.java), excecao)

        with(response) {
            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, status)
            assertNotNull(body())
            assertEquals(mensagem, (body() as JsonError).message)
        }
    }

    @Test
    fun `deve retornar 400 por dados invalidos`() {

        val mensagem = "Dados inválidos"
        val excecao = StatusRuntimeException(Status.INVALID_ARGUMENT
            .withDescription(mensagem))

        val response = GlobalExceptionHandler().handle(HttpRequest.POST("/", Any::class.java), excecao)

        with(response) {
            assertEquals(HttpStatus.BAD_REQUEST, status)
            assertNotNull(body())
            assertEquals(mensagem, (body() as JsonError).message)
        }
    }

    @Test
    fun `deve retornar 500 por outros erros`() {

        val excecao = StatusRuntimeException(Status.DATA_LOSS)

        val response = GlobalExceptionHandler().handle(HttpRequest.POST("/", Any::class.java), excecao)

        with(response) {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, status)
            assertNotNull(body())
            assertEquals("Não foi possível completar a requisição", (body() as JsonError).message)
        }
    }

    @Factory
    @Replaces(factory = KeyManagerGrpcFactory::class)
    internal class MockitoStubFactory {

        @Singleton
        fun stubMock() = Mockito.mock(KeymanagerRegistraGrpcServiceGrpc.KeymanagerRegistraGrpcServiceBlockingStub::class.java)
    }
}