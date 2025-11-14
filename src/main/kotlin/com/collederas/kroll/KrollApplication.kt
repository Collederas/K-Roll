package com.collederas.kroll

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KrollApplication

fun main(args: Array<String>) {
	runApplication<KrollApplication>(*args)
}
