package com.zupacademy.henio.pix

import com.zupacademy.henio.pix.exceptions.GlobalExceptionHandler
import com.zupacademy.henio.pix.grpc.KeymanagerRemoveGrpcServiceGrpc
import com.zupacademy.henio.pix.grpc.RemoveChavePixResponse
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest
internal class RemoveChaveControllerTest {

    @field:Inject
    lateinit var removeStub: KeymanagerRemoveGrpcServiceGrpc.KeymanagerRemoveGrpcServiceBlockingStub

    @field:Inject
    @field:Client("/")
    lateinit var httpClient: HttpClient

    val clienteId = UUID.randomUUID().toString()
    val pixId = UUID.randomUUID().toString()

    @Test
    fun `deve remover uma chave pix existente`() {


        val respostaGrpc = RemoveChavePixResponse.newBuilder()
                                                    .setClienteId(clienteId)
                                                    .setPixId(pixId)
                                                    .build()
        given(removeStub.remove(any())).willReturn(respostaGrpc)

        val request = HttpRequest.DELETE<Any>("/api/clientes/$clienteId/pix/$pixId")
        val response = httpClient.toBlocking().exchange(request, Any::class.java)

        Assertions.assertEquals(HttpStatus.OK, response.status)
    }

    @Test
    fun `deve retornar 404 quando a chave nao existe`() {

        val mensagem = "Chave não encontrada"
        val excecao = StatusRuntimeException(
            Status.NOT_FOUND
            .withDescription(mensagem))

        val response = GlobalExceptionHandler().handle(HttpRequest.DELETE<Any>("/"), excecao)

        with(response) {
            assertEquals(HttpStatus.NOT_FOUND, status)
            assertNotNull(body())
            assertEquals(mensagem, (body() as JsonError).message)
        }
    }

    @Test
    fun `deve retornar 403 quando o cliente nao eh dono da chave`() {

        val mensagem = "Cliente não é dono da chave"
        val excecao = StatusRuntimeException(
            Status.PERMISSION_DENIED
            .withDescription(mensagem))

        val response = GlobalExceptionHandler().handle(HttpRequest.DELETE<Any>("/"), excecao)

        with(response) {
            assertEquals(HttpStatus.FORBIDDEN, status)
            assertNotNull(body())
            assertEquals(mensagem, (body() as JsonError).message)
        }
    }

    @Factory
    @Replaces(factory = KeyManagerGrpcFactory::class)
    internal class RemoveStubFactory {

        @Singleton
        fun deletaChave() = Mockito.mock(KeymanagerRemoveGrpcServiceGrpc.KeymanagerRemoveGrpcServiceBlockingStub::class.java)
    }
}