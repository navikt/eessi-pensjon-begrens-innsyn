package no.nav.eessi.pensjon.services.eux

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.eessi.pensjon.metrics.MetricsHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

/**
 * @param metricsHelper Usually injected by Spring Boot, can be set manually in tests - no way to read metrics if not set.
 */
@Service
class EuxService(
        private val euxOidcRestTemplate: RestTemplate,
        @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper(SimpleMeterRegistry())
) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(EuxService::class.java) }
    private val mapper: ObjectMapper = jacksonObjectMapper().configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)

    fun getSed(rinaSakId: String, rinaDokumentId: String) : String? {
        return metricsHelper.measure("hentSed") {
            val path = "/buc/{RinaSakId}/sed/{DokumentId}"
            val uriParams = mapOf("RinaSakId" to rinaSakId, "DokumentId" to rinaDokumentId)
            val builder = UriComponentsBuilder.fromUriString(path).buildAndExpand(uriParams)

            try {
                logger.info("Henter SED fra EUX /${builder.toUriString()}")
                euxOidcRestTemplate.exchange(builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        String::class.java).body
            } catch( ex: Exception) {
                logger.error("En feil oppstod under henting av SED fra EUX", ex)
                throw ex
            }
        }
    }

    fun settSensitivSak(rinaSakId: String) : String? {
        return metricsHelper.measure("settSensitiv") {
            val path = "/buc/{RinaSakId}/sensitivsak"
            val uriParams = mapOf("RinaSakId" to rinaSakId)
            val builder = UriComponentsBuilder.fromUriString(path).buildAndExpand(uriParams)

            try {
                logger.info("Setter BUC som sensitiv /${builder.toUriString()}")
                val resp = euxOidcRestTemplate.exchange(builder.toUriString(),
                        HttpMethod.PUT,
                        null,
                        String::class.java)
                if(resp.statusCode.isError) {
                    logger.error("En feil oppstod under setting av sensitiv sak: status: ${resp.statusCode} + ${resp.statusCodeValue} body: ${resp.body}")
                }

                resp.body
            } catch( ex: Exception) {
                logger.error("En feil oppstod under henting av SED fra EUX", ex)
                throw ex
            }
        }
    }
}
