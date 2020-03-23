package no.nav.eessi.pensjon

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.retry.annotation.EnableRetry

@SpringBootApplication
@EnableRetry
class EessiPensjonBegrensInnsynApplication

fun main(args: Array<String>) {
	runApplication<EessiPensjonBegrensInnsynApplication>(*args)
}
