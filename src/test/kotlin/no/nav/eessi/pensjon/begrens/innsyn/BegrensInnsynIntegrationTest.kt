package no.nav.eessi.pensjon.begrens.innsyn

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.eessi.pensjon.eux.EuxService
import no.nav.eessi.pensjon.eux.model.document.ForenkletSED
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockserver.integration.ClientAndServer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.KafkaMessageListenerContainer
import org.springframework.kafka.listener.MessageListener
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.retry.annotation.EnableRetry
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestTemplate
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.TimeUnit

private const val SED_SENDT_TOPIC = "eessi-basis-sedSendt-v1"
private const val SED_MOTTATT_TOPIC = "eessi-basis-sedMottatt-v1"

private lateinit var mockServer : ClientAndServer

@SpringBootTest(classes = [ BegrensInnsynIntegrationTest.TestConfig::class, EessiPensjonBegrensInnsynApplicationIntegrationtest::class, IntegrationtestConfig::class])
@ActiveProfiles("integrationtest")
@DirtiesContext
@EnableRetry
@EmbeddedKafka(count = 1, controlledShutdown = true, topics = [SED_SENDT_TOPIC, SED_MOTTATT_TOPIC], brokerProperties= ["log.dir=out/embedded-kafkainnsyn"])
class BegrensInnsynIntegrationTest {

    @Autowired
    lateinit var embeddedKafka: EmbeddedKafkaBroker

    @Autowired
    lateinit var sedListener: SedListener

    @Autowired
    lateinit var personService: PersonService

    @MockkBean
    lateinit var euxService: EuxService

    @Test
    fun `Gitt en sedSendt hendelse med KODE6 person når begrens innsyn blir sjekket så settes BUC til sensitiv sak `() {
        initMocks()

        // Vent til kafka er klar
        val container = settOppUtitlityConsumer()
        container.start()
        ContainerTestUtils.waitForAssignment(container, embeddedKafka.partitionsPerTopic)

        // Sett opp producer
        val sedSendtProducerTemplate = settOppProducerTemplate()

        // produserer sedSendt meldinger på kafka
        produserSedHendelser(sedSendtProducerTemplate)

        // Venter på at sedListener skal consumeSedSendt meldingene
        sedListener.getLatchSendt().await(15000, TimeUnit.MILLISECONDS)

        // Verifiserer alle kall
        verifiser()

        // Shutdown
        shutdown(container)
    }

    private fun initMocks() {

        // Mock person
        every { personService.harAdressebeskyttelse(any()) } returns false andThen true

        // Mocker EUX sensitiv sak
        every { euxService.settSensitivSak("147729") } returns true

        every { euxService.hentSedJson("147729", "4338515b6bed451798ba478c835409a3") }
            .answers { javaClass.getResource("/sed/P6000-NAV_uten_SPSF.json").readText() }

        every { euxService.hentSedJson("147729", "02249d3f5bdd4336999ccfbf7bb13c64") }
            .answers { javaClass.getResource("/sed/P2000-NAV_med_SPSF.json").readText() }

        val documentsJson = javaClass.getResource("/sed/allDocuments.json").readText()
        val documents = jacksonObjectMapper().readValue(documentsJson, object : TypeReference<List<ForenkletSED>>() {})

        every { euxService.hentBucDokumenter("147729") }
            .answers { documents }
    }

    private fun produserSedHendelser(sedSendtProducerTemplate: KafkaTemplate<Int, String>) {
        // Sender 1 Pensjon SED til Kafka
        sedSendtProducerTemplate.sendDefault(String(Files.readAllBytes(Paths.get("src/test/resources/sed/P_BUC_01.json"))))
    }

    private fun shutdown(container: KafkaMessageListenerContainer<String, String>) {
        mockServer.stop()
        container.stop()
    }

    private fun settOppProducerTemplate(): KafkaTemplate<Int, String> {
        return KafkaTemplate<Int, String>(DefaultKafkaProducerFactory(KafkaTestUtils.producerProps(embeddedKafka.brokersAsString))).apply {
            defaultTopic = SED_SENDT_TOPIC
        }
    }

    private fun settOppUtitlityConsumer(): KafkaMessageListenerContainer<String, String> {
        val consumerProperties = KafkaTestUtils.consumerProps("eessi-pensjon-group2",
                "false",
                embeddedKafka)
        consumerProperties["auto.offset.reset"] = "earliest"

        val consumerFactory = DefaultKafkaConsumerFactory<String, String>(consumerProperties)
        val containerProperties = ContainerProperties(SED_SENDT_TOPIC)
        val container = KafkaMessageListenerContainer(consumerFactory, containerProperties)
        val messageListener = MessageListener<String, String> { record -> println("Konsumerer melding:  $record") }
        container.setupMessageListener(messageListener)

        return container
    }

    companion object {

        init {
            // Start Mockserver in memory
            val port = randomFrom()
            mockServer = ClientAndServer.startClientAndServer(port)
            System.setProperty("mockServerport", port.toString())

        }

        private fun randomFrom(from: Int = 1024, to: Int = 65535): Int {
            val random = Random()
            return random.nextInt(to - from) + from
        }
    }

    private fun verifiser() {
        Assertions.assertEquals(0, sedListener.getLatchSendt().count, "Alle meldinger har ikke blitt konsumert")

        // Verifiserer at SED har blitt hentet
        verify(exactly = 1) { euxService.hentSedJson("147729", "4338515b6bed451798ba478c835409a3") }

        // Verifiserer at SED har blitt hentet
        verify(exactly = 1) { euxService.hentSedJson("147729", "02249d3f5bdd4336999ccfbf7bb13c64") }

        // Verifiserer at det har blitt forsøkt å sette en sak til sensitiv
        verify(exactly = 1) { euxService.settSensitivSak("147729") }

        // Verifisert at den er kjørt én gang pr. unike dokument. 
        verify(exactly = 2) { personService.harAdressebeskyttelse(any()) }
    }

    // Mocks the PersonService and EuxService
    @TestConfiguration
    class TestConfig{
        @Bean
        fun personService(): PersonService = mockk()

        @Bean
        fun downstreamClientCredentialsResourceRestTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate? {
            return restTemplateBuilder
                .rootUri("https://localhost:${mockServer.localPort}")
                .build()
        }
    }
}
