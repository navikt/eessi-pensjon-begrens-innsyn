package no.nav.eessi.pensjon.services.fagmodul

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.eessi.pensjon.metrics.MetricsHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import java.lang.RuntimeException
import javax.annotation.PostConstruct


/**
 * @param metricsHelper Usually injected by Spring Boot, can be set manually in tests - no way to read metrics if not set.
 */
@Service
class FagmodulService(
        private val fagmodulOidcRestTemplate: RestTemplate,
        @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper(SimpleMeterRegistry())) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(FagmodulService::class.java) }
    private lateinit var hentSeds: MetricsHelper.Metric

    @PostConstruct
    fun initMetrics() {
        hentSeds = metricsHelper.init("hentSeds")
    }

    fun hentAlleDokumenterFraRinaSak(rinaNr: String): String? {
        return hentSeds.measure {
            val path = "/buc/$rinaNr/allDocuments"
            try {
                logger.info("Henter jsondata for alle sed for rinaNr: $rinaNr")
                fagmodulOidcRestTemplate.exchange(path,
                        HttpMethod.GET,
                        null,
                        String::class.java).body
            } catch(ex: HttpStatusCodeException) {
                logger.error("En feil oppstod under henting av seds fra fagmodulen ex: $ex body: ${ex.responseBodyAsString}")
                throw RuntimeException("En feil oppstod under henting av seds fra fagmodulen ex: ${ex.message} body: ${ex.responseBodyAsString}")
            } catch(ex: Exception) {
                logger.error("En feil oppstod under henting av seds fra fagmodulen ex: $ex")
                throw RuntimeException("En feil oppstod under henting av seds fra fagmodulen ex: ${ex.message}")
            }
        }
    }
}
