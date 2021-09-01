package com.zupacademy.henio.keymanager.shared.grpc

import com.zupacademy.henio.KeymanagerRegistraGrpcServiceGrpc
import io.grpc.ManagedChannel
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import javax.inject.Singleton


@Factory
class KeyManagerGrpcFactory(@GrpcChannel("keymanager") val channel: ManagedChannel) {

    @Singleton
    fun registraChave() = KeymanagerRegistraGrpcServiceGrpc.newBlockingStub(channel)

}