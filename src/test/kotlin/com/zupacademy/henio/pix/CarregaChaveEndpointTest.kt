package com.zupacademy.henio.pix

import com.zupacademy.henio.pix.chave.ChavePix
import com.zupacademy.henio.pix.chave.ChavePixRepository
import com.zupacademy.henio.pix.chave.TipoDeChave
import com.zupacademy.henio.pix.chave.TipoDeConta
import com.zupacademy.henio.pix.cliente.bcb.BancoCentralClient
import com.zupacademy.henio.pix.cliente.bcb.BankAccount
import com.zupacademy.henio.pix.cliente.bcb.Owner
import com.zupacademy.henio.pix.cliente.bcb.PixKeyDetailsResponse
import com.zupacademy.henio.pix.cliente.itau.ContaAssociada
import com.zupacademy.henio.pix.grpc.CarregaChavePixRequest
import com.zupacademy.henio.pix.grpc.KeymanagerCarregaGrpcServiceGrpc
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Bean
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
import org.mockito.Mockito
import java.util.*
import javax.inject.Inject
import org.junit.jupiter.api.*
import java.time.LocalDateTime

@MicronautTest(transactional = false)
internal class CarregaChaveEndpointTest(
    @Inject val repository: ChavePixRepository,
    @Inject val grpcClient: KeymanagerCarregaGrpcServiceGrpc.KeymanagerCarregaGrpcServiceBlockingStub
) {

    @Inject
    lateinit var bcbClient: BancoCentralClient

    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

    @BeforeEach
    fun setup() {
        repository.save(chave(tipo = TipoDeChave.EMAIL, chave = "rafael.ponte@zup.com.br", clienteId = ListaChaveEndpointTest.CLIENTE_ID))
        repository.save(chave(tipo = TipoDeChave.RANDOM, chave = "randomkey-2", clienteId = ListaChaveEndpointTest.CLIENTE_ID))
        repository.save(chave(tipo = TipoDeChave.CPF, chave = "02467781054", clienteId = ListaChaveEndpointTest.CLIENTE_ID))

    }

    @AfterEach
    fun cleanup() {
        repository.deleteAll()
    }

    @Test
    fun `deve carregar chave por pixId e clienteId`() {

        val chaveExistente = repository.findByChave("02467781054").get()

        val response = grpcClient.carrega(CarregaChavePixRequest.newBuilder()
            .setPixId(CarregaChavePixRequest.FiltroPorPixId.newBuilder()
                .setPixId(chaveExistente.id.toString())
                .setClienteId(chaveExistente.clienteId.toString())
                .build()
            ).build())

        with(response) {
            assertEquals(chaveExistente.id.toString(), this.pixId)
            assertEquals(chaveExistente.clienteId.toString(), this.clienteId)
            assertEquals(chaveExistente.tipoDeChave.toString(), this.chave.tipoChave.name)
            assertEquals(chaveExistente.chave, this.chave.chave)
        }
    }

    @Test
    fun `nao deve carregar chave por pixId e clienteId quando filtro for invalido`() {
        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.carrega(CarregaChavePixRequest.newBuilder()
                    .build())
        }
            with(excecao) {
                assertEquals(Status.INVALID_ARGUMENT.code, status.code)
                assertEquals("Chave Pix inválida ou não informada", status.description)
                //TODO: extrair e validar os detalhes do erro...
            }
}

    @Test
    fun `nao deve carregar chave por pixId e clienteId quando registro nao existir`() {

        val pixIdNaoExistente = UUID.randomUUID().toString()
        val clienteIdNaoExistente = UUID.randomUUID().toString()

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.carrega(CarregaChavePixRequest.newBuilder()
                .setPixId(CarregaChavePixRequest.FiltroPorPixId.newBuilder()
                    .setPixId(pixIdNaoExistente)
                    .setClienteId(clienteIdNaoExistente)
                    .build()
                ).build())
        }
        with(excecao) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada", status.description)
        }
    }

    @Test
    fun `deve carregar chave por valor da chave quando registro existir localmente`() {
        val chaveExistente = repository.findByChave("02467781054").get()

        val response = grpcClient.carrega(CarregaChavePixRequest.newBuilder()
            .setChave("02467781054")
            .build())

        with(response) {
            assertEquals(chaveExistente.id.toString(), this.pixId)
            assertEquals(chaveExistente.clienteId.toString(), this.clienteId)
            assertEquals(chaveExistente.tipoDeChave.toString(), this.chave.tipoChave.name)
            assertEquals(chaveExistente.chave, this.chave.chave)
        }
    }

    @Test
    fun `deve carregar chave por valor da chave quando registro nao existir localmente mas existir no BCB` () {

        val bcbResponse = pixKeyDetailsResponse()
        Mockito.`when`(bcbClient.consultaPorChave("otheruser@other.com"))
            .thenReturn(HttpResponse.ok(pixKeyDetailsResponse()))

        val response = grpcClient.carrega(CarregaChavePixRequest.newBuilder()
            .setChave("otheruser@other.com")
            .build())

        with(response) {
            assertEquals("otheruser@other.com", this.chave.chave)
            assertEquals(bcbResponse.keyType.name, this.chave.tipoChave.name)
        }
    }

    @Test
    fun `nao deve carregar chave por valor da chave filtro invalido` () {

        val chaveInvalido = ""

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.carrega(
                CarregaChavePixRequest.newBuilder()
                    .setChave(chaveInvalido).build())
        }

        with(excecao) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
        }
    }

    @Test
    fun `nao deve carregar chave quando filtro invalido`() {
        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.carrega(CarregaChavePixRequest.newBuilder().build())
        }

        with(excecao) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Chave Pix inválida ou não informada", status.description)
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
                KeymanagerCarregaGrpcServiceGrpc.KeymanagerCarregaGrpcServiceBlockingStub{
            return KeymanagerCarregaGrpcServiceGrpc.newBlockingStub(channel)
        }
    }



    private fun pixKeyDetailsResponse(): PixKeyDetailsResponse {

        return PixKeyDetailsResponse(
            keyType = TipoDeChave.CPF,
            key = "02467781054",
            bankAccount = BankAccount(
                participant = "ITAÚ UNIBANCO S.A.",
                branch = "0001",
                accountNumber = "291900",
                accountType = BankAccount.AccountType.CACC
            ),
            owner = Owner(
                type = Owner.OwnerType.NATURAL_PERSON,
                name = "Rafael M C Ponte",
                taxIdNumber = "02467781054"
            ),
            createdAt = LocalDateTime.now()
        )
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
}

