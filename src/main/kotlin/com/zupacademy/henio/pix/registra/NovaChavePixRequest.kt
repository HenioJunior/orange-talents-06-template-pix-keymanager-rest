package com.zupacademy.henio.pix.registra

import com.zupacademy.henio.pix.grpc.RegistraChavePixRequest
import com.zupacademy.henio.pix.grpc.TipoChave
import com.zupacademy.henio.pix.grpc.TipoConta
import com.zupacademy.henio.pix.validacoes.ValidaChavePix
import com.zupacademy.henio.pix.validacoes.ValidaUUID
import io.micronaut.core.annotation.Introspected
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator
import org.hibernate.validator.internal.constraintvalidators.hv.br.CPFValidator
import java.util.*
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@ValidaChavePix
@Introspected
data class NovaChavePixRequest(@field:NotNull val tipoDeChave: TipoDeChaveRequest?,
                               @field:NotNull val tipoDeConta: TipoDeContaRequest?,
                               @field:Size(max=77) val chave: String?) {

    fun paraModeloGrpc(@ValidaUUID clienteId: UUID): RegistraChavePixRequest {
        return RegistraChavePixRequest.newBuilder()
            .setClienteId(clienteId.toString())
            .setTipoConta(tipoDeConta?.atributoGrpc ?: TipoConta.UNKNOWN_TIPO_CONTA)
            .setTipoChave(tipoDeChave?.atributoGrpc ?: TipoChave.UNKNOWN_TIPO_CHAVE)
            .setChave(chave ?: "")
            .build()

    }

    override fun toString(): String {
        return "NovaChavePixRequest(tipoDeChave=$tipoDeChave, tipoDeConta=$tipoDeConta, chave=$chave)"
    }


}

enum class TipoDeChaveRequest(val atributoGrpc: TipoChave) {

    CPF(TipoChave.CPF) {
        override fun valida(chave: String?): Boolean {
            if (chave.isNullOrBlank()) {
                return false
            }
            return CPFValidator().run {
                initialize(null)
                isValid(chave, null)
            }
        }
    },

    PHONE(TipoChave.CELULAR) {
        override fun valida(chave: String?): Boolean {

            if (chave.isNullOrBlank()) {
                return false
            }

            return chave.matches("^\\+[1-9][0-9]\\d{11}\$".toRegex())
        }
    },

    EMAIL(TipoChave.EMAIL) {
        override fun valida(chave: String?): Boolean {

            if (chave.isNullOrBlank()) {
                return false
            }

            return EmailValidator().run {
                initialize(null)
                isValid(chave, null)
            }
        }
    },

    RANDOM(TipoChave.RANDOM) {
        override fun valida(chave: String?): Boolean = chave.isNullOrBlank()
    };

    abstract fun valida(chave: String?): Boolean

}

enum class TipoDeContaRequest(val atributoGrpc: TipoConta) {

        CONTA_CORRENTE(TipoConta.CONTA_CORRENTE),
        CONTA_POUPANCA(TipoConta.CONTA_POUPANCA)
}
