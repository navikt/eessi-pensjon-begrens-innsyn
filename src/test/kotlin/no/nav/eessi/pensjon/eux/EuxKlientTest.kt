package no.nav.eessi.pensjon.eux

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.eessi.pensjon.eux.klient.EuxKlient
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.EnableRetry
import org.springframework.stereotype.Component
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.web.client.HttpClientErrorException


@ActiveProfiles("retryConfigOverride")
@SpringJUnitConfig(classes = [
    EuxService::class,
    EuxKlientRetryLogger::class,
    TestEuxClientRetryConfig::class]
)
@EnableRetry
internal class EuxKlientTest{

    @MockkBean
    lateinit var euxKlient: EuxKlient

    @Autowired
    lateinit var euxService: EuxService

    @Test
    fun `Gitt at et restkall fra euxKlient returnerer en BAD_REQUEST så skal retry gjøre samme kallet 3 ganger før den avslutter`(){
        //val euxGetPath = "/buc/111/sed/222"

        every {
            euxKlient.hentSedJson(eq("111"), eq("222"))
        } throws HttpClientErrorException(HttpStatus.BAD_REQUEST)

        assertThrows<HttpClientErrorException> {
            euxService.hentSedJson("111", "222")
        }

        verify(exactly = 3) {
            euxKlient.hentSedJson(eq("111"), eq("222"))
        }
    }

    @Test
    @Disabled //TODO: fungerer ikke etter flytting fra klient til service
    fun `Gitt at et restkall fra euxKlient returnerer en NotFound så skal ikke retry slå inn`(){
        every {
            euxKlient.hentSedJson(eq("111"), eq("222"))

        } throws HttpClientErrorException(HttpStatus.NOT_FOUND)

        euxService.hentSedJson("111", "222")

        verify(exactly = 3) {
            euxKlient.hentSedJson(eq("111"), eq("222"))
        }
    }
}

@Profile("retryConfigOverride")
@Component("euxKlientRetryConfig")
data class TestEuxClientRetryConfig(val initialRetryMillis: Long = 10L)


