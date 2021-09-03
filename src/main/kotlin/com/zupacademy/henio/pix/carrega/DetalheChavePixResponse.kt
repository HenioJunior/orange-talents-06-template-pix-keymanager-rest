package com.zupacademy.henio.pix.carrega

import com.zupacademy.henio.pix.grpc.CarregaChavePixResponse
import com.zupacademy.henio.pix.grpc.TipoConta
import io.micronaut.core.annotation.Introspected
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Introspected
class DetalheChavePixResponse(chaveResponse: CarregaChavePixResponse) {

    val pixId = chaveResponse.pixId
    val tipoChave = chaveResponse.chave.tipoChave
    val chave = chaveResponse.chave.chave

    val criadaEm = chaveResponse.chave.criadaEm.let {it ->
        LocalDateTime.ofInstant(Instant.ofEpochSecond(it.seconds, it.nanos.toLong()), ZoneOffset.UTC)
    }

    val tipoConta = when (chaveResponse.chave.conta.tipoConta) {
        TipoConta.CONTA_CORRENTE -> "CONTA_CORRENTE"
        TipoConta.CONTA_POUPANCA -> "CONTA_POUPANCA"
        else -> "NAO_RECONHECIDA"
    }

    val conta = mapOf(Pair("tipoConta", tipoConta),
    Pair("instituicao", chaveResponse.chave.conta.instituicao),
    Pair("nomeDoTitular", chaveResponse.chave.conta.nomeDoTitular),
    Pair("cpfDoTitular", chaveResponse.chave.conta.cpfDoTitular),
    Pair("agencia", chaveResponse.chave.conta.agencia),
    Pair("numeroDaConta", chaveResponse.chave.conta.numeroDaConta))
}




