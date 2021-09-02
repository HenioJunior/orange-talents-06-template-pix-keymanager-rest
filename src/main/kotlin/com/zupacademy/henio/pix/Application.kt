package com.zupacademy.henio.pix

import io.micronaut.runtime.Micronaut.*
fun main(args: Array<String>) {
	build()
	    .args(*args)
		.packages("com.zupacademy.henio.pix")
		.start()
}

