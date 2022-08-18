package no.nav.eessi.pensjon.begrens.innsyn

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.kafka.support.Acknowledgment
import java.io.IOException
import java.io.UncheckedIOException

internal class SedListenerTest {

    private val acknowledgment = mockk<Acknowledgment>(relaxUnitFun = true)
    private val cr = mockk<ConsumerRecord<String, String>>(relaxed = true)
    private val begrensInnsynService = mockk<BegrensInnsynService>(relaxed = true)

    private val sedListener = SedListener(begrensInnsynService,"test")

    private val sedHendelse = javaClass.getResource("/sed/P_BUC_01.json").readText()

    @BeforeEach
    fun setup() {
        sedListener.initMetrics()
    }

    @Test
    fun `gitt en gyldig sedHendelse når sedSendt hendelse konsumeres så ack melding`() {
        sedListener.consumeSedSendt(sedHendelse, cr, acknowledgment)
        verify(exactly = 1) { acknowledgment.acknowledge() }
    }


    @Test
    fun `gitt en gyldig sedHendelse når sedMottatt hendelse konsumeres så så ack melding`() {
        sedListener.consumeSedMottatt(sedHendelse, cr, acknowledgment)
        verify(exactly = 1) { acknowledgment.acknowledge() }
    }

    @Test
    fun `gitt en exception ved sedSendt så kastes RunTimeException og meldig blir IKKE ack'et`() {
        every { begrensInnsynService.begrensInnsyn(any()) } throws UncheckedIOException(IOException("JSON-issue"))

        assertThrows<RuntimeException> { sedListener.consumeSedSendt("SomeSEDAsString", cr, acknowledgment) }

        verify(exactly = 0) { acknowledgment.acknowledge() }
    }

    @Test
    fun `gitt en exception ved sedMottatt så kastes RunTimeException og meldig blir IKKE ack'et`() {
        every { begrensInnsynService.begrensInnsyn(any()) } throws  UncheckedIOException(IOException("JSON issue"))

        assertThrows<RuntimeException> { sedListener.consumeSedMottatt(sedHendelse, cr, acknowledgment) }

        verify(exactly = 0) { acknowledgment.acknowledge() }

    }
}
