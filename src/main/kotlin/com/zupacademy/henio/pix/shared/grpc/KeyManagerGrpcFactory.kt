package com.zupacademy.henio.pix.shared.grpc


import com.zupacademy.henio.pix.grpc.KeymanagerRegistraGrpcServiceGrpc
import io.grpc.ManagedChannel
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import javax.inject.Singleton


@Factory
class KeyManagerGrpcFactory(@GrpcChannel("keyManager") val channel: ManagedChannel) {

    @Singleton
    fun registraChave() = KeymanagerRegistraGrpcServiceGrpc.newBlockingStub(channel)

}