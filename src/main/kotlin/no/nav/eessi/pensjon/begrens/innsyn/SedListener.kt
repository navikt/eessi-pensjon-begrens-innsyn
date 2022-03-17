package no.nav.eessi.pensjon.begrens.innsyn

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.eessi.pensjon.metrics.MetricsHelper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.CountDownLatch
import javax.annotation.PostConstruct

@Service
class SedListener(private val begrensInnsynService: BegrensInnsynService,
        @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper(SimpleMeterRegistry())
) {

    private val logger = LoggerFactory.getLogger(SedListener::class.java)
    private val latchSendt = CountDownLatch(1)
    private val latchMottatt = CountDownLatch(1)

    private lateinit var consumeOutgoingSed: MetricsHelper.Metric
    private lateinit var consumeIncomingSed: MetricsHelper.Metric

    fun getLatchSendt(): CountDownLatch {
        return latchSendt
    }
    fun getLatchMottatt(): CountDownLatch {
        return latchMottatt
    }

    @PostConstruct
    fun initMetrics() {
        consumeOutgoingSed = metricsHelper.init("consumeOutgoingSed")
        consumeIncomingSed = metricsHelper.init("consumeIncomingSed")
    }


    @KafkaListener(id="sedSendtListener",
            idIsGroup = false,
            topics = ["\${kafka.sedSendt.topic}"],
            groupId = "\${kafka.sedSendt.groupid}",
            autoStartup = "false")
    fun consumeSedSendt(hendelse: String, cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        MDC.putCloseable("x_request_id", UUID.randomUUID().toString()).use {
            consumeOutgoingSed.measure {
                logger.info("Innkommet sedSendt hendelse i partisjon: ${cr.partition()}, med offset: ${cr.offset()}")
                logger.debug(vask11sifre(hendelse))

                val offsetToSkip = listOf(183769L)
                val offset = cr.offset()
                if (offset in offsetToSkip) {
                    logger.warn("Hopper over offset: $offset grunnet feil.")
                    return@measure
                }
                try {
                    begrensInnsynService.begrensInnsyn(hendelse)
                    acknowledgment.acknowledge()
                    logger.info("Acket sedSendt melding med offset: ${cr.offset()} i partisjon ${cr.partition()}")
                    latchSendt.countDown()
                } catch (ex: Exception) {
                    logger.error("Noe gikk galt under behandling av sedSendt hendelse:\n ${vask11sifre(hendelse)} \n ${ex.message}", ex)
                    throw RuntimeException(ex.message)
                }
            }
        }
    }

    @KafkaListener(id="sedMottattListener",
            idIsGroup = false,
            topics = ["\${kafka.sedMottatt.topic}"],
            groupId = "\${kafka.sedMottatt.groupid}",
            autoStartup = "false")
    fun consumeSedMottatt(hendelse: String, cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        MDC.putCloseable("x_request_id", UUID.randomUUID().toString()).use {
            consumeIncomingSed.measure {
                logger.info("Innkommet sedMottatt hendelse i partisjon: ${cr.partition()}, med offset: ${cr.offset()}")
                logger.debug(vask11sifre(hendelse))

                val offsetToSkip = listOf(325513L, 325514L, 325515L, 376151L, 427538L, 427785L, 427842L)
                val offset = cr.offset()
                if (offset in offsetToSkip) {
                    logger.warn("Hopper over offset: $offset grunnet feil.")
                    return@measure
                }
                try {
                    begrensInnsynService.begrensInnsyn(hendelse)
                    acknowledgment.acknowledge()
                    logger.info("Acket sedMottatt melding med offset: ${cr.offset()} i partisjon ${cr.partition()}")
                    latchMottatt.countDown()
                } catch (ex: Exception) {
                    logger.error("Noe gikk galt under behandling av sedMottatt hendelse:\n ${vask11sifre(hendelse)} \n ${ex.message}", ex)
                    throw RuntimeException(ex.message)
                }
            }
        }
    }

    // TODO Finn gjerne en bedre måte
    private fun vask11sifre(tekst: String) = tekst.replace(Regex("""\d{11}"""), "***")
}
