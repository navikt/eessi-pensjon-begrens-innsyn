package no.nav.eessi.pensjon.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class MetricsHelper(val registry: MeterRegistry, @Autowired(required = false) val configuration: Configuration = Configuration()) {

    @PostConstruct
    fun initCounters() {
        listOf("consumeOutgoingSed",
                "hentperson",
                "hentSed",
                "settSensitiv",
                "disoverSTS",
                "getSystemOidcToken",
                "hentSeds").forEach {counterName ->
            Counter.builder(counterName)
                    .tag(configuration.typeTag, configuration.successTypeTagValue)
                    .register(registry)

            Counter.builder(counterName)
                    .tag(configuration.typeTag, configuration.failureTypeTagValue)
                    .register(registry)
        }
    }

    fun <R> measure(
            method: String,
            failure: String = configuration.failureTypeTagValue,
            success: String = configuration.successTypeTagValue,
            meterName: String = configuration.measureMeterName,
            eventType: String = configuration.callEventTypeTagValue,
            block: () -> R): R {

        var typeTag = success

        try {
            return Timer.builder("$meterName.${configuration.measureTimerSuffix}")
                    .tag(configuration.methodTag, method)
                    .register(registry)
                    .recordCallable {
                        block.invoke()
                    }
        } catch (throwable: Throwable) {
            typeTag = failure
            throw throwable
        } finally {
            try {
                Counter.builder(meterName)
                        .tag(configuration.methodTag, method)
                        .tag(configuration.typeTag, typeTag)
                        .register(registry)
                        .increment()
            } catch (e: Exception) {
                // ignoring on purpose
            }
        }
    }

    class Configuration(
            val incrementMeterName: String = "event",
            val measureMeterName: String = "method",
            val measureTimerSuffix: String = "timer",

            val eventTag: String = "event",
            val methodTag: String = "method",
            val typeTag: String = "type",

            val successTypeTagValue: String = "successful",
            val failureTypeTagValue: String = "failed",

            val callEventTypeTagValue: String = "called"
    )
}
