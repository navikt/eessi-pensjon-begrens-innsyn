package no.nav.eessi.pensjon.begrens.innsyn

import no.nav.eessi.pensjon.eux.model.SedHendelse
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.shared.person.Fodselsnummer
import no.nav.eessi.pensjon.utils.mapJsonToAny
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.CountDownLatch

@Service
class SedListener(private val begrensInnsynService: BegrensInnsynService,
                  @Value("\${SPRING_PROFILES_ACTIVE:}") private val profile: String,
                  @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper.ForTest()
) {

    private val logger = LoggerFactory.getLogger(SedListener::class.java)
    private val secureLog = LoggerFactory.getLogger("secureLog")

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

    init {
        consumeOutgoingSed = metricsHelper.init("consumeOutgoingSed")
        consumeIncomingSed = metricsHelper.init("consumeIncomingSed")
    }

    @KafkaListener(
        containerFactory = "sedKafkaListenerContainerFactory",
        topics = ["\${kafka.sedSendt.topic}"],
        groupId = "\${kafka.sedSendt.groupid}",
    )
    fun consumeSedSendt(hendelse: String, cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        MDC.putCloseable("x_request_id", UUID.randomUUID().toString()).use {
            consumeOutgoingSed.measure {
                logger.info("Innkommet sedSendt hendelse i partisjon: ${cr.partition()}, med offset: ${cr.offset()}")
                secureLog.debug("Hendelse sendt:\n${Fodselsnummer.vaskFnr(hendelse)}")
                val sedHendelse = mapJsonToAny<SedHendelse>(hendelse)
                if (testMeldingIProdLogError(sedHendelse, acknowledgment)) return@measure

                val offsetToSkip = listOf(70196L, 70197L, 70768L, 176379L)
                if (cr.offset() in offsetToSkip) {
                    logger.warn("Hopper over offset: ${cr.offset()} grunnet feil.")
                    return@measure
                }
                try {
                    begrensInnsynService.begrensInnsyn(sedHendelse)
                    acknowledgment.acknowledge()
                    logger.info("Acket sedSendt melding med offset: ${cr.offset()} i partisjon ${cr.partition()}")
                    latchSendt.countDown()
                } catch (ex: Exception) {
                    logger.error("Noe gikk galt under behandling av sedSendt hendelse:\n ${Fodselsnummer.vaskFnr(hendelse)} \n ${ex.message}", ex)
                    throw RuntimeException(ex.message)
                }
            }
        }
    }

    @KafkaListener(
        containerFactory = "sedKafkaListenerContainerFactory",
        topics = ["\${kafka.sedMottatt.topic}"],
        groupId = "\${kafka.sedMottatt.groupid}",
    )
    fun consumeSedMottatt(hendelse: String, cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        MDC.putCloseable("x_request_id", UUID.randomUUID().toString()).use {
            consumeIncomingSed.measure {
                logger.info("Innkommet sedMottatt hendelse i partisjon: ${cr.partition()}, med offset: ${cr.offset()}")
                secureLog.debug("Hendelse mottatt:\n${Fodselsnummer.vaskFnr(hendelse)}")
                val sedHendelse = mapJsonToAny<SedHendelse>(hendelse)

                if (testMeldingIProdLogError(sedHendelse, acknowledgment)) return@measure

                val offsetToSkip = listOf(814980L)
                if (cr.offset() in offsetToSkip) {
                    logger.warn("Hopper over offset: ${cr.offset()} grunnet feil.")
                    return@measure
                }

                if (profile == "prod" && sedHendelse.avsenderId in listOf("NO:NAVAT05", "NO:NAVAT07")) {
                    logger.error("Avsender id er ${sedHendelse.avsenderId}. Dette er testdata i produksjon!!!\n$sedHendelse")
                    acknowledgment.acknowledge()
                    return@measure
                }

                try {
                    begrensInnsynService.begrensInnsyn(sedHendelse)
                    acknowledgment.acknowledge()
                    logger.info("Acket sedMottatt melding med offset: ${cr.offset()} i partisjon ${cr.partition()}")
                    latchMottatt.countDown()
                } catch (ex: Exception) {
                    logger.error("Noe gikk galt under behandling av sedMottatt hendelse:\n ${Fodselsnummer.vaskFnr(hendelse)} \n ${ex.message}", ex)
                    throw RuntimeException(ex.message)
                }
            }
        }
    }

    private fun testMeldingIProdLogError(
        sedHendelseRina: SedHendelse,
        acknowledgment: Acknowledgment
    ): Boolean {
        if (profile == "prod" && sedHendelseRina.avsenderId in listOf("NO:NAVAT05", "NO:NAVAT07") || profile == "prod" && sedHendelseRina.mottakerId in listOf("NO:NAVAT05", "NO:NAVAT07")) {
            logger.error("Avsender id er ${sedHendelseRina.avsenderId}. Dette er testdata i produksjon!!!\n$sedHendelseRina")
            acknowledgment.acknowledge()
            return true
        }
        return false
    }

}
