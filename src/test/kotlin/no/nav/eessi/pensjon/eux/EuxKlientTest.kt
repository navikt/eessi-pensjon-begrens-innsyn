package no.nav.eessi.pensjon.eux
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.EnableRetry
import org.springframework.stereotype.Component
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate


@ActiveProfiles("retryConfigOverride")
@SpringJUnitConfig(classes = [
    EuxKlient::class,
    EuxKlientRetryLogger::class,
    TestEuxClientRetryConfig::class]
)
@EnableRetry
internal class EuxKlientTest{

    @MockkBean
    lateinit var restTemplate : RestTemplate

    @Autowired
    lateinit var euxKlient: EuxKlient

    @Test
    fun `Gitt at et restkall fra euxKlient returnerer en BAD_REQUEST så skal retry gjøre samme kallet 3 ganger før den avslutter`(){
        val euxGetPath = "/buc/111/sed/222"

        every {
            restTemplate.exchange(euxGetPath,HttpMethod.GET, any(), String::class.java)
        } throws HttpClientErrorException(HttpStatus.BAD_REQUEST)

        assertThrows<HttpClientErrorException> {
            euxKlient.hentSedJson("111", "222")
        }

        verify(exactly = 3) {
            restTemplate.exchange(euxGetPath,HttpMethod.GET, any(), String::class.java)
        }
    }

    @Test
    fun `Gitt at et restkall fra euxKlient returnerer en NotFound så skal ikke retry slå inn`(){
        val euxGetPath = "/buc/111/sed/222"

        every {
            restTemplate.exchange(euxGetPath,HttpMethod.GET, any(), String::class.java)
        } throws HttpClientErrorException(HttpStatus.NOT_FOUND)

        euxKlient.hentSedJson("111", "222")

        verify(exactly = 1) {
            restTemplate.exchange(euxGetPath,HttpMethod.GET, any(), String::class.java)
        }
    }
}

@Profile("retryConfigOverride")
@Component("euxKlientRetryConfig")
data class TestEuxClientRetryConfig(val initialRetryMillis: Long = 10L)


