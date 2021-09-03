package com.zupacademy.henio.pix.shared.grpc


import com.zupacademy.henio.pix.grpc.KeymanagerCarregaGrpcServiceGrpc
import com.zupacademy.henio.pix.grpc.KeymanagerListaGrpcServiceGrpc
import com.zupacademy.henio.pix.grpc.KeymanagerRegistraGrpcServiceGrpc
import com.zupacademy.henio.pix.grpc.KeymanagerRemoveGrpcServiceGrpc
import io.grpc.ManagedChannel
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import javax.inject.Singleton


@Factory
class KeyManagerGrpcFactory(@GrpcChannel("keyManager") val channel: ManagedChannel) {

    @Singleton
    fun registraChave() = KeymanagerRegistraGrpcServiceGrpc.newBlockingStub(channel)

    @Singleton
    fun removeChave() = KeymanagerRemoveGrpcServiceGrpc.newBlockingStub(channel)

    @Singleton
    fun carregaChave() = KeymanagerCarregaGrpcServiceGrpc.newBlockingStub(channel)

    @Singleton
    fun listaChave() = KeymanagerListaGrpcServiceGrpc.newBlockingStub(channel)
}