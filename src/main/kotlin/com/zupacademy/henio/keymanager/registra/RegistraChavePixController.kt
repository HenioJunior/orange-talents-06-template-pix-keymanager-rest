package com.zupacademy.henio.keymanager.registra

import com.zupacademy.henio.KeymanagerRegistraGrpcServiceGrpc
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpResponse.created
import io.micronaut.http.HttpResponse.uri
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.validation.Validated
import org.slf4j.LoggerFactory
import java.util.*
import javax.validation.Valid

@Validated
@Controller("/api/clientes/{clienteId}")
class RegistraChavePixController(
    private val registraChavePixClient: KeymanagerRegistraGrpcServiceGrpc.KeymanagerRegistraGrpcServiceBlockingStub) {

    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    @Post("/pix")
    fun create(clienteId: UUID,
    @Valid @Body request: NovaChavePixRequest
    ): HttpResponse<Any> {

        LOGGER.info("[$clienteId] criando uma nova chave pix com $request")

        val grpcResponse = registraChavePixClient.registra(request.paraModeloGrpc(clienteId))

        return created(location(clienteId, grpcResponse.pixId))

    }

    private fun location(clienteId: UUID, pixId: String) = HttpResponse.uri("/api/clientes/$clienteId/pix/$pixId")

}