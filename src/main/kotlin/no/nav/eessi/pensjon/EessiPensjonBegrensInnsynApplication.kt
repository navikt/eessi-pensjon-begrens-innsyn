package no.nav.eessi.pensjon

import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Profile
import org.springframework.retry.annotation.EnableRetry

@Profile("prod", "test")
@EnableJwtTokenValidation
@EnableOAuth2Client(cacheEnabled = false)
@SpringBootApplication
@EnableRetry
class EessiPensjonBegrensInnsynApplication

fun main(args: Array<String>) {
	runApplication<EessiPensjonBegrensInnsynApplication>(*args)
}
