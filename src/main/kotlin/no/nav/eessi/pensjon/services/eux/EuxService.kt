package no.nav.eessi.pensjon.services.eux

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.eessi.pensjon.metrics.MetricsHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import javax.annotation.PostConstruct

/**
 * @param metricsHelper Usually injected by Spring Boot, can be set manually in tests - no way to read metrics if not set.
 */
@Service
class EuxService(
        private val euxOidcRestTemplate: RestTemplate,
        @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper(SimpleMeterRegistry())
) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(EuxService::class.java) }

    private lateinit var hentSed: MetricsHelper.Metric
    private lateinit var settSensitiv: MetricsHelper.Metric

    @PostConstruct
    fun initMetrics() {
        hentSed = metricsHelper.init("hentSed")
        settSensitiv = metricsHelper.init("settSensitiv")
    }

    @Retryable(include = [HttpServerErrorException::class, HttpClientErrorException.Unauthorized::class])
    fun getSed(rinaSakId: String, rinaDokumentId: String) : String? {
        return hentSed.measure {
            val path = "/buc/$rinaSakId/sed/$rinaDokumentId"
            val uriParams = mapOf("RinaSakId" to rinaSakId, "DokumentId" to rinaDokumentId)
            val builder = UriComponentsBuilder.fromUriString(path).buildAndExpand(uriParams)

            logger.info("Henter SED fra EUX /${builder.toUriString()}")
            try {
                euxOidcRestTemplate.exchange(builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        String::class.java).body
            } catch (ex: Exception) {
                logger.warn("Feil ved henting av SED fra EUX /${builder.toUriString()}", ex) // warn because retry
                throw ex
            }
        }
    }

    fun settSensitivSak(rinaSakId: String) : String? {
        return settSensitiv.measure {
            val path = "/buc/$rinaSakId/sensitivsak"
            val uriParams = mapOf("RinaSakId" to rinaSakId)
            val builder = UriComponentsBuilder.fromUriString(path).buildAndExpand(uriParams)

            try {
                logger.info("Setter BUC som sensitiv /${builder.toUriString()}")
                euxOidcRestTemplate.exchange(builder.toUriString(),
                        HttpMethod.PUT,
                        null,
                        String::class.java).body
            } catch(ex: HttpStatusCodeException) {
                logger.error("En feil oppstod under markering av sensitiv sak ex: $ex body: ${ex.responseBodyAsString}")
                throw RuntimeException("En feil oppstod under markering av sensitiv sak ex: ${ex.message} body: ${ex.responseBodyAsString}")
            } catch(ex: Exception) {
                logger.error("En feil oppstod under markering av sensitiv sak ex: $ex")
                throw RuntimeException("En feil oppstod under markering av sensitiv sak ex: ${ex.message}")
            }
        }
    }
}
