package com.zupacademy.henio.pix.carrega

import com.zupacademy.henio.pix.grpc.CarregaChavePixRequest
import com.zupacademy.henio.pix.grpc.KeymanagerCarregaGrpcServiceGrpc
import com.zupacademy.henio.pix.grpc.KeymanagerListaGrpcServiceGrpc
import com.zupacademy.henio.pix.grpc.ListaChavePixRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import org.slf4j.LoggerFactory
import java.util.*

@Controller("/api/clientes/{clienteId}")
class CarregaChavePixController( val carregaChavesPixClient: KeymanagerCarregaGrpcServiceGrpc.KeymanagerCarregaGrpcServiceBlockingStub,
                                 val listaChavesPixClient: KeymanagerListaGrpcServiceGrpc.KeymanagerListaGrpcServiceBlockingStub
) {

    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    @Get("/pix/{pixId}")
    fun carrega(clienteId: UUID,
                pixId: UUID): HttpResponse<Any>{

        LOGGER.info("[$clienteId] carrega chave pix por id: $pixId")

        val chaveResponse = carregaChavesPixClient.carrega(CarregaChavePixRequest.newBuilder()
            .setPixId(CarregaChavePixRequest.FiltroPorPixId.newBuilder()
                .setClienteId(clienteId.toString())
                .setPixId(pixId.toString())
                .build())
            .build())

        return HttpResponse.ok(DetalheChavePixResponse(chaveResponse))
    }

    @Get("/pix")
    fun lista(clienteId: UUID): HttpResponse<Any> {

        LOGGER.info("Listando chaves do cliente $clienteId")

        val pix = listaChavesPixClient.lista(
            ListaChavePixRequest.newBuilder()
            .setClienteId(clienteId.toString())
            .build())

        val chaves = pix.chavesList.map { ChavePixResponse(it) }

        return HttpResponse.ok(chaves)
    }
}