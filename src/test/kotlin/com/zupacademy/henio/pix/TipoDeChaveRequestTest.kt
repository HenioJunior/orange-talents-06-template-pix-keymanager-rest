package com.zupacademy.henio.pix

import com.zupacademy.henio.pix.grpc.TipoChave
import com.zupacademy.henio.pix.registra.TipoDeChaveRequest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TipoDeChaveRequestTest {

    @Nested
    inner class ChaveAleatoriaTest {

        @Test
        fun `deve ser valido quando chave aleatoria for nula ou vazia`() {

            val tipoDeChave = TipoDeChaveRequest.RANDOM

            assertTrue(tipoDeChave.valida(null))
            assertTrue(tipoDeChave.valida(""))

        }

        @Test
        fun `deve rejeitar chave aleatoria quando possuir valor`() {
            with(TipoDeChaveRequest.RANDOM) {
                assertFalse(valida("valor passado pelo usuário"))
            }
        }
    }

    @Nested
    inner class CpfTest {

        @Test
        fun `deve validar chave de cpf nula ou vazia`() {

            val tipoDeChave = TipoDeChaveRequest.CPF

            with(tipoDeChave) {
                assertFalse(valida(null))
                assertFalse(valida(""))
            }
        }

        @Test
        fun `deve rejeitar chave de cpf com qualquer valor`() {

            val tipoDeChave = TipoDeChaveRequest.CPF

            with(tipoDeChave) {
                assertFalse(valida("valor passado pelo usuário"))
            }
        }
    }

    @Nested
    inner class CelularTest {

        @Test
        fun `deve aceitar chave de celular valido`() {

            with(TipoDeChaveRequest.PHONE) {
                assertTrue(valida("+5511990001234"))
            }
        }

        @Test
        fun `deve rejeitar chave de celular nulo ou vazio`() {

            with(TipoDeChaveRequest.PHONE) {
                assertFalse(valida(""))
                assertFalse(valida(null))
            }
        }

        @Test
        fun `deve rejeitar chave de celular invalido`() {
            with(TipoDeChaveRequest.PHONE) {
                assertFalse(valida("+05511990001234"))  //começando com 0
                assertFalse(valida("5511990001234"))    // sem o + no começo
                assertFalse(valida("+551199000123"))    // falta de caracteres
                assertFalse(valida("+55119900012345"))  // excesso de caracteres
            }
        }

    }

    @Nested
    inner class EmailTest {

        @Test
        fun `deve aceitar chave de email valido`() {
            with(TipoDeChaveRequest.EMAIL) {
                assertTrue(valida("rafael.ponte@email.com"))
            }
        }

        @Test
        fun `deve rejeitar chave de email nula ou vazia`() {
            with(TipoDeChaveRequest.EMAIL) {
                assertFalse(valida(""))
                assertFalse(valida(null))
            }
        }

        @Test
        fun `deve rejeitar chave de email invalido`() {
            with(TipoDeChaveRequest.EMAIL) {
                assertFalse(valida("rafael.ponte@"))
                assertFalse(valida("rafael.ponteemail.com"))
                assertFalse(valida("rafael.ponte.email.com"))
            }
        }
    }



}