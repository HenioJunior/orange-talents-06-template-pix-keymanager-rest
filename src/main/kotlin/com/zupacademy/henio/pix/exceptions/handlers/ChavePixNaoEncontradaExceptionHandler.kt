package com.zupacademy.henio.pix.exceptions.handlers

import com.zupacademy.henio.pix.exceptions.ChavePixNaoEncontradaException
import com.zupacademy.henio.pix.exceptions.handlers.ExceptionHandler.*
import io.grpc.Status
import javax.inject.Singleton

@Singleton
class ChavePixNaoEncontradaExceptionHandler: ExceptionHandler<ChavePixNaoEncontradaException> {

    override fun handle(e: ChavePixNaoEncontradaException): StatusWithDetails {
        return StatusWithDetails(
            Status.NOT_FOUND
                .withDescription(e.message)
                .withCause(e)
        )
    }

    override fun supports(e: Exception): Boolean {
        return e is ChavePixNaoEncontradaException
    }

}