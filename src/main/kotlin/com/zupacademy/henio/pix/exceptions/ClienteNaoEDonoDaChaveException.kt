package com.zupacademy.henio.pix.exceptions

import java.lang.Exception

class ClienteNaoEDonoDaChaveException(
    override val message: String?
) : Exception()