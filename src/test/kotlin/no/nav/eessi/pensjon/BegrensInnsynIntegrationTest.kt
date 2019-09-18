package no.nav.eessi.pensjon

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import no.nav.eessi.pensjon.listeners.SedListener
import no.nav.eessi.pensjon.services.personv3.PersonMock
import no.nav.eessi.pensjon.services.personv3.PersonV3Service
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.Header
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.HttpStatusCode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
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
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.TimeUnit
import javax.ws.rs.HttpMethod

private const val SED_SENDT_TOPIC = "eessi-basis-sedSendt-v1"

private lateinit var mockServer : ClientAndServer

@SpringBootTest(classes = [ BegrensInnsynIntegrationTest.TestConfig::class])
@ActiveProfiles("integrationtest")
@DirtiesContext
@EmbeddedKafka(count = 1, controlledShutdown = true, topics = [SED_SENDT_TOPIC])
class BegrensInnsynIntegrationTest {

    @Autowired
    lateinit var embeddedKafka: EmbeddedKafkaBroker

    @Autowired
    lateinit var sedListener: SedListener

    @Autowired
    lateinit var  personV3Service: PersonV3Service

    @Disabled
    @Test
    fun `Når en sedSendt hendelse blir konsumert skal det opprettes journalføringsoppgave for pensjon SEDer`() {

        // Mock personV3
        capturePersonMock()

        // Vent til kafka er klar
        val container = settOppUtitlityConsumer(SED_SENDT_TOPIC)
        container.start()
        ContainerTestUtils.waitForAssignment(container, embeddedKafka.partitionsPerTopic)

        // Sett opp producer
        val sedSendtProducerTemplate = settOppProducerTemplate(SED_SENDT_TOPIC)

        // produserer sedSendt meldinger på kafka
        produserSedHendelser(sedSendtProducerTemplate)

        // Venter på at sedListener skal consumeSedSendt meldingene
        sedListener.getLatch().await(15000, TimeUnit.MILLISECONDS)

        // Verifiserer alle kall
        //TODO verifiser()

        // Shutdown
        shutdown(container)
    }

    private fun produserSedHendelser(sedSendtProducerTemplate: KafkaTemplate<Int, String>) {
        // Sender 1 Foreldre SED til Kafka
        println("Produserer FB_BUC_01 melding")
        sedSendtProducerTemplate.sendDefault(String(Files.readAllBytes(Paths.get("src/test/resources/sed/FB_BUC_01.json"))))

        // Sender 3 Pensjon SED til Kafka
        println("Produserer P_BUC_01 melding")
        sedSendtProducerTemplate.sendDefault(String(Files.readAllBytes(Paths.get("src/test/resources/sed/P_BUC_01.json"))))
        println("Produserer P_BUC_03 melding")
        sedSendtProducerTemplate.sendDefault(String(Files.readAllBytes(Paths.get("src/test/resources/sed/P_BUC_03.json"))))
        println("Produserer P_BUC_05 melding")
        sedSendtProducerTemplate.sendDefault(String(Files.readAllBytes(Paths.get("src/test/resources/sed/P_BUC_05.json"))))
    }

    private fun shutdown(container: KafkaMessageListenerContainer<String, String>) {
        mockServer.stop()
        container.stop()
        embeddedKafka.kafkaServers.forEach { it.shutdown() }
    }

    private fun settOppProducerTemplate(topicNavn: String): KafkaTemplate<Int, String> {
        val senderProps = KafkaTestUtils.senderProps(embeddedKafka.brokersAsString)
        val pf = DefaultKafkaProducerFactory<Int, String>(senderProps)
        val template = KafkaTemplate(pf)
        template.defaultTopic = topicNavn
        return template
    }

    private fun settOppUtitlityConsumer(topicNavn: String): KafkaMessageListenerContainer<String, String> {
        val consumerProperties = KafkaTestUtils.consumerProps("eessi-pensjon-group2",
                "false",
                embeddedKafka)
        consumerProperties["auto.offset.reset"] = "earliest"

        val consumerFactory = DefaultKafkaConsumerFactory<String, String>(consumerProperties)
        val containerProperties = ContainerProperties(topicNavn)
        val container = KafkaMessageListenerContainer<String, String>(consumerFactory, containerProperties)
        val messageListener = MessageListener<String, String> { record -> println("Konsumerer melding:  $record") }
        container.setupMessageListener(messageListener)

        return container
    }

    private fun capturePersonMock() {
        val slot = slot<String>()
        every { personV3Service.hentPerson(fnr = capture(slot)) } answers { PersonMock.createWith()!! }
    }

    companion object {

        init {
            // Start Mockserver in memory
            val port = randomFrom()
            mockServer = ClientAndServer.startClientAndServer(port)
            System.setProperty("mockServerport", port.toString())

            // Mocker STS
            mockServer.`when`(
                    HttpRequest.request()
                            .withMethod(HttpMethod.GET)
                            .withQueryStringParameter("grant_type", "client_credentials"))
                    .respond(HttpResponse.response()
                            .withHeader(Header("Content-Type", "application/json; charset=utf-8"))
                            .withStatusCode(HttpStatusCode.OK_200.code())
                            .withBody(String(Files.readAllBytes(Paths.get("src/test/resources/sed/STStoken.json"))))
                    )
            // Mocker STS service discovery
            mockServer.`when`(
                    HttpRequest.request()
                            .withMethod(HttpMethod.GET)
                            .withPath("/.well-known/openid-configuration"))
                    .respond(HttpResponse.response()
                            .withHeader(Header("Content-Type", "application/json; charset=utf-8"))
                            .withStatusCode(HttpStatusCode.OK_200.code())
                            .withBody(
                                    "{\n" +
                                            "  \"issuer\": \"http://localhost:${port}\",\n" +
                                            "  \"token_endpoint\": \"http://localhost:${port}/rest/v1/sts/token\",\n" +
                                            "  \"exchange_token_endpoint\": \"http://localhost:${port}/rest/v1/sts/token/exchange\",\n" +
                                            "  \"jwks_uri\": \"http://localhost:${port}/rest/v1/sts/jwks\",\n" +
                                            "  \"subject_types_supported\": [\"public\"]\n" +
                                            "}"
                            )
                    )
        }

        private fun randomFrom(from: Int = 1024, to: Int = 65535): Int {
            val random = Random()
            return random.nextInt(to - from) + from
        }
    }

/*

    private fun verifiser() {
        assertEquals(0, sedListener.getLatch().count, "Alle meldinger har ikke blitt konsumert")

        // Verifiser at det har blitt forsøkt å hente person fra tps
        verify(exactly = 1) { personV3Service.hentPerson(any()) }
    }

*/
    // Mocks the PersonV3 Service so we don't have to deal with SOAP
    @TestConfiguration
    class TestConfig{
        @Bean
        @Primary
        fun personV3(): PersonV3 = mockk()

        @Bean
        fun personV3Service(personV3: PersonV3): PersonV3Service {
            return spyk(PersonV3Service(personV3))
        }
    }
}
