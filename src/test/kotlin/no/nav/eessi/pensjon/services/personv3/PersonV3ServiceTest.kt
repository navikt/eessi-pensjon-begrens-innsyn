package no.nav.eessi.pensjon.services.personv3

import io.mockk.*
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.feil.PersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.feil.Sikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.remoting.soap.SoapFaultException


@Disabled
class PersonV3ServiceTest {

    private lateinit var personV3 : PersonV3

    lateinit var personV3Service : PersonV3Service

    private val subject = "23037329381"
    private val ikkeFunnetSubject = "33037329381"
    private val sikkerhetsbegrensingSubject = "43037329381"
    private val ugyldigIdSubject = "121212 4545"
    private val annenSoapIssueSubject = "annen soap issue"

    @BeforeEach
    fun setup() {
        personV3 = mockk()
        personV3Service = spyk(PersonV3Service(personV3))

        every { personV3Service.konfigurerSamlToken() } just Runs

        every { personV3.hentPerson(requestBuilder(subject, listOf(Informasjonsbehov.ADRESSE))) } returns
                HentPersonResponse().withPerson(PersonMock.createWith())

        every { personV3.hentPerson(requestBuilder(ikkeFunnetSubject, listOf(Informasjonsbehov.ADRESSE))) } throws
                HentPersonPersonIkkeFunnet("$ikkeFunnetSubject ikke funnet", PersonIkkeFunnet())

        every { personV3.hentPerson(requestBuilder(ugyldigIdSubject, listOf(Informasjonsbehov.ADRESSE))) } throws
                DummySoapFaultException("F002001F")

        every { personV3.hentPerson(requestBuilder(annenSoapIssueSubject, listOf(Informasjonsbehov.ADRESSE))) } throws
                DummySoapFaultException("some other code")

        every { personV3.hentPerson(requestBuilder(sikkerhetsbegrensingSubject, listOf(Informasjonsbehov.ADRESSE))) } throws
                HentPersonSikkerhetsbegrensning("$sikkerhetsbegrensingSubject har sikkerhetsbegrensning", Sikkerhetsbegrensning())
    }

    @Test
    fun `Kaller hentPerson med gyldig subject`(){
        try {
            assertEquals(personV3Service.hentPerson(subject), PersonMock.createWith())
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
        assertThrows(SoapFaultException::class.java) {
            personV3Service.hentPerson(annenSoapIssueSubject)
        }
    }


    fun requestBuilder(norskIdent: String, informasjonsbehov: List<Informasjonsbehov>): HentPersonRequest{
        return HentPersonRequest().apply {
            withAktoer(PersonIdent().withIdent(NorskIdent().withIdent(norskIdent)))
            withInformasjonsbehov(informasjonsbehov)
        }
    }

    class DummySoapFaultException(val code: String) : SoapFaultException(null, null) {
        override fun getFaultActor() = "FaultActor"
        override fun getFaultCodeAsQName() = null
        override fun getFaultString() = "FaultString"
        override fun getFaultCode() = code
    }

}
