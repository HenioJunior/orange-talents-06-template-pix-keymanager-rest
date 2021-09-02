package com.zupacademy.henio.pix.remove

import com.zupacademy.henio.pix.grpc.KeymanagerRemoveGrpcServiceGrpc
import com.zupacademy.henio.pix.grpc.RemoveChavePixRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import org.slf4j.LoggerFactory
import java.util.*

@Controller("/api/clientes/{clienteId}")
class RemoveChaveController(private val removerChavePixClient: KeymanagerRemoveGrpcServiceGrpc
                                                    .KeymanagerRemoveGrpcServiceBlockingStub){

    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    @Delete("/pix/{pixId}")
    fun removerChave (clienteId: UUID, pixId: UUID): HttpResponse<Any> {

        LOGGER.info("Cliente $clienteId removendo chave $pixId")

        val grpcRequest = RemoveChavePixRequest.newBuilder()
            .setClienteId(clienteId.toString())
            .setPixId(pixId.toString())
            .build()

        removerChavePixClient.remove(grpcRequest)

        return HttpResponse.ok()
    }
 }