package com.zupacademy.henio.pix.carrega

import com.zupacademy.henio.pix.grpc.CarregaChavePixRequest
import com.zupacademy.henio.pix.grpc.KeymanagerCarregaGrpcServiceGrpc
import com.zupacademy.henio.pix.grpc.KeymanagerListaGrpcServiceGrpc
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import org.slf4j.LoggerFactory
import java.util.*

@Controller("/api/clientes/{clienteId}")
class CarregaChavePixController(
    val carregaChavePixClient: KeymanagerCarregaGrpcServiceGrpc.KeymanagerCarregaGrpcServiceBlockingStub,
) {

    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    @Get("/pix/{pixId}")
    fun carrega(clienteId: UUID,
                pixId: UUID): HttpResponse<Any>{

        LOGGER.info("[$clienteId] carrega chave pix por id: $pixId")

        val chaveResponse = carregaChavePixClient.carrega(CarregaChavePixRequest.newBuilder()
            .setPixId(CarregaChavePixRequest.FiltroPorPixId.newBuilder()
                .setClienteId(clienteId.toString())
                .setPixId(pixId.toString())
                .build())
            .build())

        return HttpResponse.ok(DetalheChavePixResponse(chaveResponse))
    }
}