package no.nav.eessi.pensjon.begrens.innsyn

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Profile
import org.springframework.retry.annotation.EnableRetry

@Profile("integrationtest")
@SpringBootApplication
@EnableRetry
class EessiPensjonBegrensInnsynApplicationIntegrationtest

fun main(args: Array<String>) {
	runApplication<EessiPensjonBegrensInnsynApplicationIntegrationtest>(*args)
}
