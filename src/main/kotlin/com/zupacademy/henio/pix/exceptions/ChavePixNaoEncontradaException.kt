package com.zupacademy.henio.pix.exceptions

import java.lang.Exception

class ChavePixNaoEncontradaException(
 override val message: String?
) : Exception()