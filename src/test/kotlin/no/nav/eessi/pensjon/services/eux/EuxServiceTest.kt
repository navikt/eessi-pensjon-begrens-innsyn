package no.nav.eessi.pensjon.services.eux

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.retry.annotation.EnableRetry
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate

@ExtendWith(MockKExtension::class, SpringExtension::class)
@ContextConfiguration
internal class EuxServiceTest {

    @Configuration
    @EnableRetry // To make @Retryable work
    @ComponentScan("no.nav.eessi.pensjon.services.eux")
    class Config {
        @Bean fun restTemplate() = mockk<RestTemplate>()
    }

    @Autowired
    lateinit var euxService: EuxService

    @Autowired
    lateinit var restTemplate: RestTemplate

    private val rinaSakId = "42"
    private val rinaDokumentId = "666"
    private val sedAsJsonString = "SED as a JSON-string"

    @Test
    fun `getSed lykkes - returnerer verdi`() {
        every {
            restTemplate.exchange("/buc/$rinaSakId/sed/$rinaDokumentId", HttpMethod.GET,null, String::class.java)
        } returns ResponseEntity(sedAsJsonString, HttpStatus.OK)

        assertEquals(sedAsJsonString, euxService.getSed(rinaSakId = rinaSakId, rinaDokumentId = rinaDokumentId))
    }

    @Test
    fun `getSed feiler første gang med HttpServerError, men lykkes andre gang - returnerer verdi`() {
        every {
            restTemplate.exchange("/buc/$rinaSakId/sed/$rinaDokumentId", HttpMethod.GET,null, String::class.java)
        }.throws(HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
                .andThen(ResponseEntity(sedAsJsonString, HttpStatus.OK))

        assertEquals(sedAsJsonString, euxService.getSed(rinaSakId = rinaSakId, rinaDokumentId = rinaDokumentId))
    }

    @Test
    fun `getSed feiler med Unautorized første gang, men lykkes andre gang - returnerer verdi`() {
        every {
            restTemplate.exchange("/buc/$rinaSakId/sed/$rinaDokumentId", HttpMethod.GET,null, String::class.java)
        }.throws(UnauthorizedException())
                .andThen(ResponseEntity(sedAsJsonString, HttpStatus.OK))

        assertEquals(sedAsJsonString, euxService.getSed(rinaSakId = rinaSakId, rinaDokumentId = rinaDokumentId))
    }

    private fun UnauthorizedException() =
            HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED.name, HttpHeaders.EMPTY, "{}".toByteArray(), null)
}

