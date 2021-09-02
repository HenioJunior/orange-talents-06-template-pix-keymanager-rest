package com.zupacademy.henio.pix.exceptions

import io.grpc.Status.*
import io.grpc.StatusRuntimeException
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.hateoas.JsonError
import io.micronaut.http.server.exceptions.ExceptionHandler
import org.slf4j.LoggerFactory
import javax.inject.Singleton

@Singleton
class GlobalExceptionHandler : ExceptionHandler<StatusRuntimeException, HttpResponse<Any>> {

    val LOGGER = LoggerFactory.getLogger(this::class.java)

    override fun handle(request: HttpRequest<*>, exception: StatusRuntimeException): HttpResponse<Any> {

        val statusCode = exception.status.code
        val statusDescription = exception.status.description ?: ""
        val (httpStatus, message) = when(statusCode) {
            NOT_FOUND.code -> Pair(HttpStatus.NOT_FOUND, statusDescription)
            INVALID_ARGUMENT.code -> Pair(HttpStatus.BAD_REQUEST, "Dados inválidos")
            ALREADY_EXISTS.code -> Pair(HttpStatus.UNPROCESSABLE_ENTITY, statusDescription)
            PERMISSION_DENIED.code -> Pair(HttpStatus.FORBIDDEN, statusDescription)
            UNAVAILABLE.code -> Pair(HttpStatus.UNPROCESSABLE_ENTITY, statusDescription)
            else -> {
                LOGGER.error("Erro inesperado '${exception.javaClass.name}' ao processar a requisição", exception)
                Pair(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível completar a requisição")
            }
        }

        return HttpResponse.status<JsonError>(httpStatus).body(JsonError(message))
    }
}