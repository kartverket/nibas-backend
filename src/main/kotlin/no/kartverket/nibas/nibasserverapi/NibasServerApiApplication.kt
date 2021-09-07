package no.kartverket.nibas.nibasserverapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class NibasServerApiApplication

fun main(args: Array<String>) {
	runApplication<NibasServerApiApplication>(*args)
}
