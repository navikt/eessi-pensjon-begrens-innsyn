package no.nav.eessi.pensjon.eux

import no.nav.eessi.pensjon.eux.klient.EuxKlientLib
import no.nav.eessi.pensjon.eux.model.buc.Buc
import no.nav.eessi.pensjon.eux.model.document.ForenkletSED
import no.nav.eessi.pensjon.eux.model.document.SedStatus
import no.nav.eessi.pensjon.metrics.MetricsHelper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.retry.RetryListener
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Service
class EuxService(
    private val euxKlient: EuxKlientLib,
    @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper.ForTest()
) {

    private lateinit var hentSed: MetricsHelper.Metric
    private lateinit var hentBuc: MetricsHelper.Metric
    private lateinit var settSensitiv: MetricsHelper.Metric

    init {
        hentSed = metricsHelper.init("hentSed", alert = MetricsHelper.Toggle.OFF)
        hentBuc = metricsHelper.init("hentBuc", alert = MetricsHelper.Toggle.OFF)
        settSensitiv = metricsHelper.init("settSensitiv", alert = MetricsHelper.Toggle.OFF)
    }

    /**
     * Henter SED som JSON fra Rina EUX API.
     *
     * @param rinaSakId: Hvilken Rina-sak SED skal hentes fra.
     * @param dokumentId: Hvilket SED-dokument som skal hentes fra spesifisert sak.
     *
     * @return [String] SED JSON
     */
    @Retryable(
        backoff = Backoff(delayExpression = "@euxKlientRetryConfig.initialRetryMillis", maxDelay = 200000L, multiplier = 3.0),
        listeners  = ["euxKlientRetryLogger"]
    )
    fun hentSedJson(rinaSakId: String, dokumentId: String): String? {
        return hentSed.measure {
            euxKlient.hentSedJson(rinaSakId, dokumentId)
        }
    }

    /**
     * Henter Buc fra Rina.
     *
     * @param rinaSakId: Hvilken Rina-sak (buc) som skal hentes.
     *
     * @return [Buc]
     */
    @Retryable(
        backoff = Backoff(delayExpression = "@euxKlientRetryConfig.initialRetryMillis", maxDelay = 200000L, multiplier = 3.0),
        listeners  = ["euxKlientBucRetryLogger"]
    )
    fun hentBuc(rinaSakId: String): Buc? {
        return hentBuc.measure {
            euxKlient.hentBuc(rinaSakId)
        }
    }

    /**
     * Henter alle dokumenter (SEDer) i en Buc.
     *
     * @param rinaSakId: Hvilken Rina-sak (buc) dokumentene skal hentes fra.
     *
     * @return Liste med [ForenkletSED]
     */
    fun hentBucDokumenter(rinaSakId: String): List<ForenkletSED> {
        val documents = hentBuc(rinaSakId)?.documents ?: return emptyList()

        return documents
            .filter { it.id != null }
            .map { ForenkletSED(it.id!!, it.type, SedStatus.fra(it.status)) }
    }

    /**
     * Markerer en Rina-sak som sensitiv.
     *
     * @param rinaSakId: Hvilken Rina-sak som skal markeres som sensitiv.
     *
     * @return [Boolean] true hvis respons fra EUX API er HttpStatus.OK
     */
    fun settSensitivSak(rinaSakId: String): Boolean {
        return settSensitiv.measure {
            euxKlient.settSensitivSak(rinaSakId)
        }
    }
}
@Profile("!retryConfigOverride")
@Component
data class EuxKlientRetryConfig(val initialRetryMillis: Long = 20000L)

@Component
class EuxKlientRetryLogger : RetryListener {
    private val logger = LoggerFactory.getLogger(EuxKlientRetryLogger::class.java)
    override fun <T : Any?, E : Throwable?> onError(context: RetryContext?, callback: RetryCallback<T, E>?, throwable: Throwable?) {
        logger.warn("Feil under henting fra Sed - try #${context?.retryCount } - ${throwable?.toString()}", throwable)
    }
}

@Component
class EuxKlientBucRetryLogger : RetryListener {
    private val logger = LoggerFactory.getLogger(EuxKlientBucRetryLogger::class.java)
    override fun <T : Any?, E : Throwable?> onError(context: RetryContext?, callback: RetryCallback<T, E>?, throwable: Throwable?) {
        logger.warn("Feil under henting fra BUC - try #${context?.retryCount } - ${throwable?.toString()}", throwable)
    }
}