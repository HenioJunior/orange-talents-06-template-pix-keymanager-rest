package com.zupacademy.henio.pix
import com.zupacademy.henio.pix.chave.ChavePix
import com.zupacademy.henio.pix.chave.ChavePixRepository
import com.zupacademy.henio.pix.chave.TipoDeChave
import com.zupacademy.henio.pix.chave.TipoDeConta
import com.zupacademy.henio.pix.cliente.bcb.*
import com.zupacademy.henio.pix.cliente.itau.BancoItauClient
import com.zupacademy.henio.pix.cliente.itau.ContaAssociada
import com.zupacademy.henio.pix.grpc.*
import com.zupacademy.henio.pix.registra.DadosDaContaResponse
import com.zupacademy.henio.pix.registra.InstituicaoResponse
import com.zupacademy.henio.pix.registra.TitularResponse
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.AbstractBlockingStub
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

@MicronautTest(transactional = false)
internal class RegistraChaveEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: KeymanagerRegistraGrpcServiceGrpc.KeymanagerRegistraGrpcServiceBlockingStub
) {

    @Inject
    lateinit var itauClient: BancoItauClient

    @Inject
    lateinit var bcbClient: BancoCentralClient

    companion object {
        val CLIENTE_ID = "c56dfef4-7901-44fb-84e2-a2cefb157890"
        val CLIENTE_INEXISTENTE = UUID.randomUUID()
        val contaAssociada = ContaAssociada(
            instituicao = "ITAÚ UNIBANCO S.A.",
            nomeDoTitular = "Rafael M C Ponte",
            cpfDoTitular = "02467781054",
            agencia = "0001",
            numero = "291900")
    }

    @BeforeEach
    fun setup() {
        repository.deleteAll()
    }

    @Test
    fun `deve registrar nova chave pix`() {

        Mockito.`when`(itauClient()!!.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), TipoDeConta.CONTA_CORRENTE.toString()))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        Mockito.`when`(bcbClient()!!.cadastraChaveNoBC(createPixKeyRequest()))
            .thenReturn(HttpResponse.created(createPixKeyResponse()))

        val response = grpcClient.registra(
            RegistraChavePixRequest.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .setTipoChave(TipoChave.CPF)
                .setChave("02467781054")
                .setTipoConta(TipoConta.CONTA_CORRENTE)
                .build())

        with(response) {
            assertEquals(CLIENTE_ID.toString(), clienteId)
            assertNotNull(pixId)
        }
    }

    @Test
    fun `nao deve registrar chave pix quando nap for possivel registrar chave no BCB`() {

        Mockito.`when`(itauClient()!!.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), TipoDeConta.CONTA_CORRENTE.toString()))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        Mockito.`when`(bcbClient()!!.cadastraChaveNoBC(createPixKeyRequest()))
            .thenReturn(HttpResponse.badRequest())

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.registra(
                RegistraChavePixRequest.newBuilder()
                    .setClienteId(CLIENTE_ID.toString())
                    .setTipoChave(TipoChave.CPF)
                    .setChave("02467781054")
                    .setTipoConta(TipoConta.CONTA_CORRENTE)
                    .build())
            }

        with(excecao) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Erro ao registrar chave Pix no Banco Central do Brasil (BCB)", status.description)
        }
    }

    @Test
    fun `nao deve registrar chave pix quando chave existente`() {

        Mockito.`when`(itauClient()!!.buscaContaPorTipo(CLIENTE_ID, TipoDeConta.CONTA_CORRENTE.toString()))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        repository.save(
            ChavePix(
                clienteId = UUID.fromString(CLIENTE_ID),
                tipoDeChave = TipoDeChave.EMAIL,
                chave = "rponte@gmail.com",
                tipoDeConta = TipoDeConta.CONTA_CORRENTE,
                conta = contaAssociada
            )
        )

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.registra(RegistraChavePixRequest.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .setTipoChave(TipoChave.CPF)
                .setChave("02467781054")
                .setTipoConta(TipoConta.CONTA_CORRENTE)
                .build())
        }
        with(excecao) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("Chave Pix rponte@gmail.com existente", status.description)
        }

    }

    @Test
    fun `nao deve registrar chave pix quando nao encontrar dados da conta cliente` () {

        `when`(itauClient()!!.buscaContaPorTipo(CLIENTE_INEXISTENTE.toString(), TipoDeConta.CONTA_CORRENTE.toString()))
            .thenReturn(HttpResponse.notFound())

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.registra(RegistraChavePixRequest.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .setTipoChave(TipoChave.CPF)
                .setChave("02467781054")
                .setTipoConta(TipoConta.CONTA_CORRENTE)
                .build())
        }

        with(excecao) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Cliente não encontrado no Itau", status.description)
        }
    }

    @Test
    fun `nao deve registrar chave pix quando parametros forem invalidos` () {
        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.registra(RegistraChavePixRequest.newBuilder().build())
        }

        with(excecao) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("dados de entrada invalidos", status.description)
        }
    }
}

@MockBean(BancoItauClient::class)
fun itauClient(): BancoItauClient? {
    return Mockito.mock(BancoItauClient::class.java)
}

@MockBean(BancoCentralClient::class)
fun bcbClient(): BancoCentralClient? {
    return Mockito.mock(BancoCentralClient::class.java)
}

@Factory
class KeyRegisterClient {
    @Bean
    fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel):
            KeymanagerRegistraGrpcServiceGrpc.KeymanagerRegistraGrpcServiceBlockingStub{
        return KeymanagerRegistraGrpcServiceGrpc.newBlockingStub(channel)
    }
}

private fun dadosDaContaResponse(): DadosDaContaResponse {

    return DadosDaContaResponse(
        tipo = "CONTA_CORRENTE",
        instituicao = InstituicaoResponse(
            nome = "ITAÚ UNIBANCO S.A.",
            ispb = "60701190"
        ),
        agencia = "0001",
        numero = "291900",
        titular = TitularResponse(
            nome = "Rafael M C Ponte",
            cpf = "02467781054"
        )
    )
}

private fun createPixKeyRequest(): CreatePixKeyRequest {

    return CreatePixKeyRequest(
        keyType = PixKeyType.CPF,
        key = "02467781054",
        bankAccount = BankAccount (
            participant =  "60701190",
            branch = "0001",
            accountNumber = "291900",
            accountType =  BankAccount.AccountType.CACC,
        ),
        owner = Owner(
            type = Owner.OwnerType.NATURAL_PERSON,
            name = "Rafael M C Ponte",
            taxIdNumber = "02467781054"
        )
    )
}



private fun createPixKeyResponse(): CreatePixKeyResponse {

    return CreatePixKeyResponse(
        keyType = TipoChave.CPF.toString(),
        key = "02467781054",
        bankAccount = BankAccount(
            participant = "60701190",
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



