package no.nav.eessi.pensjon.begrens.innsyn

import com.nhaarman.mockitokotlin2.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.kafka.support.Acknowledgment
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Paths

@ExtendWith(MockitoExtension::class)
class SedListenerTest {

    @Mock
    lateinit var acknowledgment: Acknowledgment

    @Mock
    lateinit var cr: ConsumerRecord<String, String>

    @Mock
    lateinit var begrensInnsynService: BegrensInnsynService

    lateinit var sedListener: SedListener

    @BeforeEach
    fun setup() {
        sedListener = SedListener(begrensInnsynService)
    }

    @Test
    fun `gitt en gyldig sedHendelse når sedSendt hendelse konsumeres så ack melding`() {
        sedListener.consumeSedSendt(String(Files.readAllBytes(Paths.get("src/test/resources/sed/P_BUC_01.json"))),cr,  acknowledgment)
        verify(acknowledgment).acknowledge()
    }


    @Test
    fun `gitt en gyldig sedHendelse når sedMottatt hendelse konsumeres så så ack melding`() {
        sedListener.consumeSedMottatt(String(Files.readAllBytes(Paths.get("src/test/resources/sed/P_BUC_01.json"))),cr, acknowledgment)
        verify(acknowledgment).acknowledge()
    }

    @Test
    fun `gitt en exception ved sedSendt så kastes RunTimeException og meldig blir IKKE ack'et`() {
        `when`(begrensInnsynService.begrensInnsyn(anyString())).thenThrow(UncheckedIOException(IOException("JSON-issue")))

        assertThrows<RuntimeException> {
            sedListener.consumeSedSendt("SomeSEDAsString", cr, acknowledgment)
        }
        verify(acknowledgment, times(0)).acknowledge()
    }

    @Test
    fun `gitt en exception ved sedMottatt så kastes RunTimeException og meldig blir IKKE ack'et`() {
        `when`(begrensInnsynService.begrensInnsyn(anyString())).thenThrow(UncheckedIOException(IOException("JSON-issue")))

        assertThrows<RuntimeException> {
            sedListener.consumeSedMottatt("SomeSEDAsString", cr, acknowledgment)
        }
        verify(acknowledgment, times(0)).acknowledge()
    }
}