package com.zupacademy.henio.pix

import com.zupacademy.henio.pix.chave.ChavePix
import com.zupacademy.henio.pix.chave.ChavePixRepository
import com.zupacademy.henio.pix.chave.TipoDeChave
import com.zupacademy.henio.pix.chave.TipoDeConta
import com.zupacademy.henio.pix.cliente.bcb.BancoCentralClient
import com.zupacademy.henio.pix.cliente.itau.ContaAssociada
import com.zupacademy.henio.pix.grpc.KeymanagerListaGrpcServiceGrpc
import com.zupacademy.henio.pix.grpc.ListaChavePixRequest
import com.zupacademy.henio.pix.grpc.TipoChave
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.util.*

@MicronautTest(transactional = false)
internal class ListaChaveEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: KeymanagerListaGrpcServiceGrpc.KeymanagerListaGrpcServiceBlockingStub
) {

    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }


    @BeforeEach
    fun setup() {
        repository.save(chave(tipo = TipoDeChave.EMAIL, chave = "rafael.ponte@zup.com.br", clienteId = CLIENTE_ID))
        repository.save(chave(tipo = TipoDeChave.RANDOM, chave = "randomkey-2", clienteId = CLIENTE_ID))
        repository.save(chave(tipo = TipoDeChave.CPF, chave = "02467781054", clienteId = CLIENTE_ID))

    }

    @AfterEach
    fun cleanup() {
        repository.deleteAll()
    }

    @Test
    fun `deve listar todas as chaves do cliente`() {

        val clienteId = CLIENTE_ID.toString()

        val response = grpcClient.lista(ListaChavePixRequest.newBuilder()
            .setClienteId(clienteId)
            .build())

        with(response.chavesList) {
            assertThat(this, hasSize(3))
            assertThat(
                this.map { Pair(it.tipoChave, it.chave) }.toList(),
                Matchers.containsInAnyOrder(
                    Pair(TipoChave.RANDOM, "randomkey-2"),
                    Pair(TipoChave.EMAIL, "rafael.ponte@zup.com.br"),
                    Pair(TipoChave.CPF, "02467781054"),
                )
            )
        }
    }

    @Test
    fun `nao deve listar as chaves do cliente quando cliente nao possuir chaves`() {

        val clienteSemchaves = UUID.randomUUID().toString()

        val response = grpcClient.lista(ListaChavePixRequest.newBuilder().setClienteId(clienteSemchaves).build())

        assertEquals(0,response.chavesCount)
    }

    @Test
    fun `nao deve listar todas as chaves do cliente quando clienteid for invalido`() {

        val clienteInvalido = ""

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.lista(
                ListaChavePixRequest.newBuilder()
                    .setClienteId(clienteInvalido)
                    .build()
            )
        }

        with(excecao) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Cliente ID não pode ser nulo ou vazio", status.description)
        }
   }



    @MockBean(BancoCentralClient::class)
    fun bcbClient(): BancoCentralClient? {
        return Mockito.mock(BancoCentralClient::class.java)
    }

    @Factory
    class KeyGetClient {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel):
                KeymanagerListaGrpcServiceGrpc.KeymanagerListaGrpcServiceBlockingStub{
            return KeymanagerListaGrpcServiceGrpc.newBlockingStub(channel)
        }
    }
}

private fun chave(
    tipo: TipoDeChave,
    chave: String,
    clienteId: UUID = UUID.randomUUID()
): ChavePix {
    return ChavePix(
        clienteId = clienteId,
        tipoDeChave = tipo,
        chave = chave,
        tipoDeConta = TipoDeConta.CONTA_CORRENTE,
        conta = ContaAssociada(
            instituicao = "ITAÚ UNIBANCO S.A.",
            nomeDoTitular = "Rafael M C Ponte",
            cpfDoTitular = "02467781054",
            agencia = "0001",
            numero = "291900",
        )
    )
}

