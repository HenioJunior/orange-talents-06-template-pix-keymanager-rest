package com.zupacademy.henio.pix
import com.zupacademy.henio.pix.chave.ChavePix
import com.zupacademy.henio.pix.chave.ChavePixRepository
import com.zupacademy.henio.pix.chave.TipoDeChave
import com.zupacademy.henio.pix.chave.TipoDeConta
import com.zupacademy.henio.pix.cliente.bcb.BancoCentralClient
import com.zupacademy.henio.pix.cliente.bcb.DeletePixKeyRequest
import com.zupacademy.henio.pix.cliente.bcb.DeletePixKeyResponse
import com.zupacademy.henio.pix.cliente.itau.ContaAssociada
import com.zupacademy.henio.pix.grpc.KeymanagerRemoveGrpcServiceGrpc
import com.zupacademy.henio.pix.grpc.RemoveChavePixRequest
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class RemoveChaveEndpointTest (
    val repository: ChavePixRepository,
    val grpcClient: KeymanagerRemoveGrpcServiceGrpc.KeymanagerRemoveGrpcServiceBlockingStub
){
    @Inject
    lateinit var bcbClient: BancoCentralClient

    lateinit var CHAVE_EXISTENTE: ChavePix


    @BeforeEach
    fun setup() {
        CHAVE_EXISTENTE = repository.save(chave(
            tipoDeChave = TipoDeChave.EMAIL,
            chave = "rponte@gmail.com",
            clienteId = UUID.randomUUID()
        ))
    }


    @AfterEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    fun `deve excluir chave pix existente`() {

        Mockito.`when`(bcbClient.excluiChaveNoBC("rponte@gmail.com", DeletePixKeyRequest("rponte@gmail.com")))
            .thenReturn(HttpResponse.ok(
                DeletePixKeyResponse("rponte@gmail.com",
                                                                participant = "60701190",
                                                                deletedAt = LocalDateTime.now() )
            ))

        val response = grpcClient.remove(
            RemoveChavePixRequest.newBuilder()
                .setPixId(CHAVE_EXISTENTE.id.toString())
                .setClienteId(CHAVE_EXISTENTE.clienteId.toString())
                .build())

            assertEquals(CHAVE_EXISTENTE.id.toString(), response.pixId)
            assertEquals(CHAVE_EXISTENTE.clienteId.toString(), response.clienteId)
    }

    @Test
    fun `nao deve remover chave pix quando ocorrer algum erro do BCB`() {
        Mockito.`when`(bcbClient.excluiChaveNoBC("rponte@gmail.com", DeletePixKeyRequest("rponte@gmail.com")))
            .thenReturn(HttpResponse.unprocessableEntity())

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.remove(RemoveChavePixRequest.newBuilder()
                .setPixId(CHAVE_EXISTENTE.id.toString())
                .setClienteId(CHAVE_EXISTENTE.clienteId.toString())
                .build())
        }

        with(excecao) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Erro ao remover chave Pix no Banco Central do Brasil (BCB)", status.description)
        }


    }

    @Test
    fun `nao deve excluir chave pix quando chave inexistente`() {

        val pixIdNaoExiste = UUID.randomUUID().toString()

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.remove(RemoveChavePixRequest.newBuilder()
                .setPixId(pixIdNaoExiste)
                .setClienteId(CHAVE_EXISTENTE.clienteId.toString())
                .build())
        }

        with(excecao) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada ou não pertence ao cliente", status.description)
        }
    }

    @Test
    fun `nao deve remover chave pix quando chave existente mas pertence a outro cliente` () {

        val outroClienteId = UUID.randomUUID().toString()

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.remove(RemoveChavePixRequest.newBuilder()
                .setPixId(CHAVE_EXISTENTE.id.toString())
                .setClienteId(outroClienteId)
                .build())
        }

        with(excecao) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada ou não pertence ao cliente", status.description)
        }
    }

    @MockBean(BancoCentralClient::class)
    fun bcbClient(): BancoCentralClient? {
        return Mockito.mock(BancoCentralClient::class.java)
    }
}

@Factory
class KeyDeleteClient {
    @Singleton
    fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel) = KeymanagerRemoveGrpcServiceGrpc.newBlockingStub(channel)

}


private fun chave(
    tipoDeChave: TipoDeChave,
    chave: String = UUID.randomUUID().toString(),
    clienteId: UUID = UUID.randomUUID(),
    tipoDeConta: TipoDeConta = TipoDeConta.CONTA_CORRENTE,
    conta: ContaAssociada = ContaAssociada(
        instituicao = "ITAÚ UNIBANCO S.A.",
        nomeDoTitular = "Rafael M C Ponte",
        cpfDoTitular = "02467781054",
        agencia = "0001",
        numero = "291900"
    )
): ChavePix {
    return ChavePix(
        clienteId = clienteId,
        tipoDeChave = tipoDeChave,
        chave = chave,
        tipoDeConta = tipoDeConta,
        conta = conta,
    )
}


