package com.zupacademy.henio

import io.micronaut.runtime.Micronaut.*
fun main(args: Array<String>) {
	build()
	    .args(*args)
		.packages("com.zupacademy.henio")
		.start()
}

