//package no.nav.eessi.pensjon.listeners
//
//import com.nhaarman.mockitokotlin2.times
//import com.nhaarman.mockitokotlin2.verify
//import no.nav.eessi.pensjon.begrens.innsyn.BegrensInnsynService
//import org.apache.kafka.clients.consumer.ConsumerRecord
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Disabled
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.assertThrows
//import org.junit.jupiter.api.extension.ExtendWith
//import org.mockito.Mock
//import org.mockito.junit.jupiter.MockitoExtension
//import org.springframework.kafka.support.Acknowledgment
//import java.nio.file.Files
//import java.nio.file.Paths
//
//@ExtendWith(MockitoExtension::class)
//class SedListenerTest {
//
//    @Mock
//    lateinit var acknowledgment: Acknowledgment
//
//    @Mock
//    lateinit var cr: ConsumerRecord<String, String>
//
//    @Mock
//    lateinit var begrensInnsynService: BegrensInnsynService
//
//    lateinit var sedListener: SedListener
//
//    @BeforeEach
//    fun setup() {
//        sedListener = SedListener(begrensInnsynService)
//    }
//
//    @Test
//    fun `gitt en gyldig sedHendelse når sedSendt hendelse konsumeres så ack melding`() {
//        sedListener.consumeSedSendt(String(Files.readAllBytes(Paths.get("src/test/resources/sed/P_BUC_01.json"))),cr,  acknowledgment)
//        verify(acknowledgment).acknowledge()
//    }
//
//    @Test
//    fun `gitt en gyldig sedHendelse når sedMottatt hendelse konsumeres så så ack melding`() {
//        sedListener.consumeSedMottatt(String(Files.readAllBytes(Paths.get("src/test/resources/sed/P_BUC_01.json"))),cr, acknowledgment)
//        verify(acknowledgment).acknowledge()
//    }
//
//    @Disabled //TODO
//    @Test
//    fun `gitt en exception ved sedSendt så kastes RunTimeException og meldig blir IKKE ack'et`() {
//       //doThrow(MockitoException("Boom!")).`when`(jouralforingService).journalfor(eq("Explode!"), any())
//
//        assertThrows<RuntimeException> {
//            sedListener.consumeSedSendt("Explode!",cr, acknowledgment)
//        }
//        verify(acknowledgment, times(0)).acknowledge()
//    }
//
//    @Disabled //TODO
//    @Test
//    fun `gitt en exception ved sedMottatt så kastes RunTimeException og meldig blir IKKE ack'et`() {
//        //doThrow(MockitoException("Boom!")).`when`(jouralforingService).journalfor(eq("Explode!"), any())
//
//        assertThrows<RuntimeException> {
//            sedListener.consumeSedMottatt("Explode!",cr, acknowledgment)
//        }
//        verify(acknowledgment, times(0)).acknowledge()
//    }
//}
