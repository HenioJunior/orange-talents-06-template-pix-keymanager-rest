package com.zupacademy.henio.pix.exceptions.handlers
import io.grpc.Status
import java.lang.IllegalStateException
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ExceptionHandlerResolver(@Inject val handlers: List<ExceptionHandler<Exception>>) {

    private var defaultHandler: ExceptionHandler<Exception> = DefaultExceptionHandler()

    constructor(handlers: List<ExceptionHandler<Exception>>, defaultHandler: ExceptionHandler<Exception>)
            : this(handlers) {
        this.defaultHandler = defaultHandler
    }

    fun resolve(e: Exception): ExceptionHandler<Exception> {

        val foundHandlers = handlers.filter { h -> h.supports(e) }
        if(foundHandlers.size > 1) {
            throw IllegalStateException(
                "Too many handlers supporting ${e.javaClass.name}: $foundHandlers")
        }

        return foundHandlers.firstOrNull() ?: defaultHandler
    }
}

class DefaultExceptionHandler : ExceptionHandler<Exception> {

    override fun handle(e: Exception): ExceptionHandler.StatusWithDetails {
        val status = when (e) {
            is IllegalArgumentException -> Status.INVALID_ARGUMENT.withDescription(e.message)
            is IllegalStateException -> Status.FAILED_PRECONDITION.withDescription(e.message)
            else -> Status.UNKNOWN
        }
        return ExceptionHandler.StatusWithDetails(status.withCause(e))
    }

    override fun supports(e: Exception): Boolean {
        return true
    }

}