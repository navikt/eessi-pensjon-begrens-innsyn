package no.nav.eessi.pensjon.services.personv3

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.mockk.*
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.xml.soap.SOAPFault
import javax.xml.ws.soap.SOAPFaultException

class PersonV3ServiceTest {

    private lateinit var personV3 : PersonV3

    private lateinit var personV3Service : PersonV3Service

    private val subject = "23037329381"
    private val ikkeFunnetSubject = "33037329381"
    private val sikkerhetsbegrensingSubject = "43037329382"
    private val ugyldigIdSubject = "121212 4545"
    private val annenSoapIssueSubject = "annen soap issue"

    @BeforeEach
    fun setup() {
        personV3 = mockk()
        personV3Service = spyk(PersonV3Service(personV3))

        every { personV3Service.konfigurerSamlToken() } just Runs

        every { personV3Service.hentPerson(subject) } returns PersonMock.createWith(subject)

        every { personV3Service.hentPerson(ikkeFunnetSubject) } returns null

        every { personV3Service.hentPerson(sikkerhetsbegrensingSubject) } throws
                PersonV3SikkerhetsbegrensningException("$sikkerhetsbegrensingSubject har sikkerhetsbegrensning")


        val soapFaultF002001F = mock<SOAPFault>()
        whenever(soapFaultF002001F.faultString).thenReturn("PersonV3: faultString: TPS svarte med FEIL, folgende status: F002001F og folgende melding: UGYLDIG VERDI I INPUT FNR")
        every { personV3.hentPerson(requestBuilder(ugyldigIdSubject, listOf(Informasjonsbehov.ADRESSE))) } throws
                SOAPFaultException(soapFaultF002001F)

        val soapFaultOther = mock<SOAPFault>()
        whenever(soapFaultOther.faultString).thenReturn("other")
        every { personV3.hentPerson(requestBuilder(annenSoapIssueSubject, listOf(Informasjonsbehov.ADRESSE))) } throws
                SOAPFaultException(soapFaultOther)
    }

    @Test
    fun `Kaller hentPerson med gyldig subject`(){
        try {
            val person = personV3Service.hentPerson(subject)
            assertEquals("23037329381", (person!!.aktoer as PersonIdent).ident.ident)
        }catch(ex: Exception){
            assert(false)
        }
    }

    @Test
    fun `Kaller hentPerson med subject som ikke finnes`(){
        assertNull(personV3Service.hentPerson(ikkeFunnetSubject))
    }

    @Test
    fun `Kaller hentPerson med subject med sikkerhetsbegrensing`(){
        try {
            personV3Service.hentPerson(sikkerhetsbegrensingSubject)
            assert(false)
        }catch(ex: Exception){
            assert(ex is PersonV3SikkerhetsbegrensningException)
            assertEquals(ex.message, "$sikkerhetsbegrensingSubject har sikkerhetsbegrensning")
        }
    }

    @Test
    fun `Kaller hentPerson med ugyldig input (i følge TPS) - vi oppfører oss som om vi ikke finner svar`() {
        assertNull(personV3Service.hentPerson(ugyldigIdSubject))
    }

    @Test
    fun `Kaller hentPerson med annen soap-feil`() {
        assertThrows(SOAPFaultException::class.java) {
            personV3Service.hentPerson(annenSoapIssueSubject)
        }
    }


    fun requestBuilder(norskIdent: String, informasjonsbehov: List<Informasjonsbehov>): HentPersonRequest {
        return HentPersonRequest().apply {
            withAktoer(PersonIdent().withIdent(NorskIdent().withIdent(norskIdent)))
            withInformasjonsbehov(informasjonsbehov)
        }
    }

}
