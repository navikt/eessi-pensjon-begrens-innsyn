package no.nav.eessi.pensjon.config

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler
import org.springframework.kafka.listener.CommonErrorHandler
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.stereotype.Component
import java.io.StringWriter

@Profile("prod")
@Component
class KafkaStoppingErrorHandler : CommonErrorHandler {
    private val logger = LoggerFactory.getLogger(KafkaStoppingErrorHandler::class.java)
    private val stopper = CommonContainerStoppingErrorHandler()

    override fun handleRecord(
        thrownException: Exception,
        record: ConsumerRecord<*, *>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer) {

        val stacktrace = StringWriter()

        logger.error("En feil oppstod under kafka konsumering av meldinger: \n ${hentMeldinger(record)} \n" +
                "Stopper containeren ! Restart er nødvendig for å fortsette konsumering, $stacktrace")
        stopper.handleRemaining(thrownException, listOf(record), consumer, container)

    }

    fun hentMeldinger(records: ConsumerRecord<*, *>) = "-" .repeat(20) +  vask11sifre(records.toString())

    // TODO Finn gjerne en bedre måte
    private fun vask11sifre(tekst: String) = tekst.replace(Regex("""\d{11}"""), "***")
}
