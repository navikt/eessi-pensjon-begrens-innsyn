package no.nav.eessi.pensjon.listeners

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.eessi.pensjon.begrens.innsyn.BegrensInnsynService
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

@Service
class SedListener(private val begrensInnsynService: BegrensInnsynService,
        @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper(SimpleMeterRegistry())
) {

    private val logger = LoggerFactory.getLogger(SedListener::class.java)
    private val latch = CountDownLatch(1)

    fun getLatch(): CountDownLatch {
        return latch
    }

    @KafkaListener(topics = ["\${kafka.sedSendt.topic}"], groupId = "\${kafka.sedSendt.groupid}")
    fun consumeSedSendt(hendelse: String, cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        MDC.putCloseable("x_request_id", UUID.randomUUID().toString()).use {
            metricsHelper.measure("consumeOutgoingSed") {
                logger.info("Innkommet sedSendt hendelse i partisjon: ${cr.partition()}, med offset: ${cr.offset()}")
                logger.debug(hendelse)
                try {
                    begrensInnsynService.begrensInnsyn(hendelse)
                    acknowledgment.acknowledge()
                    logger.info("Acket sedSendt melding med offset: ${cr.offset()} i partisjon ${cr.partition()}")
                    latch.countDown()
                } catch (ex: Exception) {
                    logger.error("Noe gikk galt under behandling av sedSendt hendelse:\n $hendelse \n ${ex.message}", ex)
                    throw RuntimeException(ex.message)
                }
            }
        }
    }

    @KafkaListener(topics = ["\${kafka.sedMottatt.topic}"], groupId = "\${kafka.sedMottatt.groupid}")
    fun consumeSedMottatt(hendelse: String, cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        MDC.putCloseable("x_request_id", UUID.randomUUID().toString()).use {
            metricsHelper.measure("consumeIncomingSed") {
                logger.info("Innkommet sedMottatt hendelse i partisjon: ${cr.partition()}, med offset: ${cr.offset()}\")")
                logger.debug(hendelse)
                try {
                    begrensInnsynService.begrensInnsyn(hendelse)
                    acknowledgment.acknowledge()
                    logger.info("Acket sedMottatt melding med offset: ${cr.offset()} i partisjon ${cr.partition()}")
                } catch (ex: Exception) {
                    logger.error("Noe gikk galt under behandling av sedMottatt hendelse:\n $hendelse \n ${ex.message}", ex)
                    throw RuntimeException(ex.message)
                }
            }
        }
    }
}
