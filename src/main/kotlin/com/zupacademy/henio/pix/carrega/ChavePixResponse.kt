package com.zupacademy.henio.pix.carrega

import com.zupacademy.henio.pix.grpc.ListaChavePixResponse
import io.micronaut.core.annotation.Introspected
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Introspected
class ChavePixResponse(chavePix: ListaChavePixResponse.ChavePix) {

    val id = chavePix.pixId
    val chave = chavePix.chave
    val tipoChave = chavePix.tipoChave
    val tipoConta = chavePix.tipoConta
    val criadaEm = chavePix.criadaEm.let {
        LocalDateTime.ofInstant(Instant.ofEpochSecond(it.seconds, it.nanos.toLong()), ZoneOffset.UTC)
    }
}
