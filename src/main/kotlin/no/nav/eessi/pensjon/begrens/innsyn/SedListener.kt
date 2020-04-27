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
    private val latch = CountDownLatch(1)
    private lateinit var consumeOutgoingSed: MetricsHelper.Metric
    private lateinit var consumeIncomingSed: MetricsHelper.Metric

    fun getLatch(): CountDownLatch {
        return latch
    }

    @PostConstruct
    fun initMetrics() {
        consumeOutgoingSed = metricsHelper.init("consumeOutgoingSed")
        consumeIncomingSed = metricsHelper.init("consumeIncomingSed")
    }


    @KafkaListener(topics = ["\${kafka.sedSendt.topic}"], groupId = "\${kafka.sedSendt.groupid}")
    fun consumeSedSendt(hendelse: String, cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        MDC.putCloseable("x_request_id", UUID.randomUUID().toString()).use {
            consumeOutgoingSed.measure {
                logger.info("Innkommet sedSendt hendelse i partisjon: ${cr.partition()}, med offset: ${cr.offset()}")
                logger.debug(vask11sifre(hendelse))
                try {
                    begrensInnsynService.begrensInnsyn(hendelse)
                    acknowledgment.acknowledge()
                    logger.info("Acket sedSendt melding med offset: ${cr.offset()} i partisjon ${cr.partition()}")
                    latch.countDown()
                } catch (ex: Exception) {
                    logger.error("Noe gikk galt under behandling av sedSendt hendelse:\n ${vask11sifre(hendelse)} \n ${ex.message}", ex)
                    throw RuntimeException(ex.message)
                }
            }
        }
    }

    @KafkaListener(topics = ["\${kafka.sedMottatt.topic}"], groupId = "\${kafka.sedMottatt.groupid}")
    fun consumeSedMottatt(hendelse: String, cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        MDC.putCloseable("x_request_id", UUID.randomUUID().toString()).use {
            consumeIncomingSed.measure {
                logger.info("Innkommet sedMottatt hendelse i partisjon: ${cr.partition()}, med offset: ${cr.offset()}")
                logger.debug(vask11sifre(hendelse))
                try {
                    begrensInnsynService.begrensInnsyn(hendelse)
                    acknowledgment.acknowledge()
                    logger.info("Acket sedMottatt melding med offset: ${cr.offset()} i partisjon ${cr.partition()}")
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
