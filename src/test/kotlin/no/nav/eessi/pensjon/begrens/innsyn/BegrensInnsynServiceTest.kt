package no.nav.eessi.pensjon.begrens.innsyn

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.eessi.pensjon.eux.EuxService
import no.nav.eessi.pensjon.eux.model.SedHendelse
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.document.ForenkletSED
import no.nav.eessi.pensjon.eux.model.document.SedStatus
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.toJson
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class BegrensInnsynServiceTest {

    private val euxService = mockk<EuxService>(relaxed = true)
    private val personService = mockk<PersonService>()
    private val begrensInnsynService = BegrensInnsynService(euxService, personService)

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        value = [
            "P_BUC_01.json, P2000-NAV_med_SPSF.json, true, 147729, 147729",
            "R_BUC_02.json, R_BUC_02-R005-AP.json, true, 147710, 147710"
        ],
        nullValues = ["null"]
    )
    fun `Gitt at vi mottar en sed med skjermede personer, så skal vi begrense innsyn i hele bucen`(hendelse:String, sedJson:String, harbeskyttelse:Boolean, rinaNr: String, beskyttetRinaNr:String) {
        val hendelse = javaClass.getResource("/sed/$hendelse")!!.readText()
        val sedJson = javaClass.getResource("/sed/$sedJson")!!.readText()

        every { euxService.hentSedJson(any(), any()) } returns sedJson
        every { personService.harAdressebeskyttelse(any()) }.returns(harbeskyttelse)

        begrensInnsynService.begrensInnsyn(mapJsonToAny(hendelse))

        verify(exactly = 1) { euxService.hentSedJson(rinaNr, "4338515b6bed451798ba478c835409a3") }
        verify(exactly = 1) { personService.harAdressebeskyttelse(any()) }
        verify(exactly = 1) { euxService.settSensitivSak(beskyttetRinaNr) }
        verify(exactly = 0) { euxService.hentBucDokumenter(rinaNr) }

    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        value = [
            "for P_BUC og P2000, P_BUC_01.json, P2000-NAV_med_SPSF.json, 147729",
            "for R_BUC og R_005, R_BUC_02.json, R_BUC_02-R005-AP.json, 147710"],
        nullValues = ["null"]
    )    fun `Gitt at vi mottar en R_BUC uten skjermede personer, så skal vi returnere en liste over alle seder som skal sjekkes`(beskrivelse: String, hendelse:String, sedJson:String, rinaNr: String) {

        val hendelse = javaClass.getResource("/sed/$hendelse")!!.readText()
        val sedJson = javaClass.getResource("/sed/$sedJson")!!.readText()

        every { euxService.hentSedJson(rinaNr, any()) } returns sedJson
        every { personService.harAdressebeskyttelse(any()) }.returns(false)
        every { euxService.hentBucDokumenter(rinaNr) } returns opprettDokumenter()

        begrensInnsynService.begrensInnsyn(mapJsonToAny(hendelse))

        verify(exactly = 1) { euxService.hentSedJson(rinaNr, "4338515b6bed451798ba478c835409a3") }
        verify(exactly = 2) { personService.harAdressebeskyttelse(any()) }
        verify(exactly = 1) { euxService.hentBucDokumenter(rinaNr) }
        verify(exactly = 0) { euxService.settSensitivSak(any()) }
    }

    @Test
    fun `Gitt at vi får inn en X050 så skal vi returnere uten å gjøre noe `() {

        val hendelse = javaClass.getResource("/sed/P_BUC_06_X050.json")!!.readText()

        every { personService.harAdressebeskyttelse(any()) } returns false
        begrensInnsynService.begrensInnsyn(mapJsonToAny(hendelse))

        verify(exactly = 0) { euxService.hentSedJson(any(), any()) }
        verify(exactly = 0) { personService.harAdressebeskyttelse(any()) }
        verify(exactly = 0) { euxService.settSensitivSak(any()) }
    }

    @Test
    fun `Sjekk at ugyldige SEDer blir ignorert`() {
        val rinaSakId = "123"
        val rinaDokumentId = "456"

        val p8000 = SED(type = SedType.P8000).toJson()

        val docs = listOf(
            ForenkletSED(rinaDokumentId, type = SedType.P8000, status = SedStatus.SENT),
            ForenkletSED("2", type = SedType.X001, status = SedStatus.RECEIVED),
            ForenkletSED("3", type = SedType.X002, status = SedStatus.RECEIVED),
            ForenkletSED("4", type = SedType.X003, status = SedStatus.RECEIVED),
            ForenkletSED("5", type = SedType.X004, status = SedStatus.RECEIVED),
            ForenkletSED("6", type = SedType.X005, status = SedStatus.RECEIVED),
            ForenkletSED("7", type = SedType.X006, status = SedStatus.RECEIVED),
            ForenkletSED("8", type = SedType.X007, status = SedStatus.RECEIVED),
            ForenkletSED("9", type = SedType.X008, status = SedStatus.RECEIVED),
            ForenkletSED("10", type = SedType.X009, status = SedStatus.RECEIVED),
            ForenkletSED("11", type = SedType.X010, status = SedStatus.RECEIVED),
            ForenkletSED("12", type = SedType.X011, status = SedStatus.RECEIVED),
            ForenkletSED("13", type = SedType.X012, status = SedStatus.RECEIVED),
            ForenkletSED("14", type = SedType.X013, status = SedStatus.RECEIVED),
            ForenkletSED("15", type = SedType.X050, status = SedStatus.RECEIVED),
            ForenkletSED("16", type = SedType.X100, status = SedStatus.RECEIVED)
        )

        every { euxService.hentSedJson(any(), any()) } returns p8000
        every { euxService.hentBucDokumenter(any()) } returns docs
        every { personService.harAdressebeskyttelse(any()) } returns false

        val hendelse = SedHendelse(sektorKode = "P", rinaSakId = rinaSakId, rinaDokumentId = rinaDokumentId, rinaDokumentVersjon = "4.25" ).toJson()
        begrensInnsynService.begrensInnsyn(mapJsonToAny(hendelse))

        verify(exactly = 1) { euxService.hentBucDokumenter(rinaSakId) }
        verify(exactly = 1) { euxService.hentSedJson(rinaSakId, rinaDokumentId) }
        verify(exactly = 0) { euxService.hentSedJson(rinaSakId, not(rinaDokumentId)) }
        verify(exactly = 0) { euxService.settSensitivSak(any()) }
    }

    private fun opprettDokumenter(): List<ForenkletSED> {
        val json = javaClass.getResource("/sed/allDocuments.json")!!.readText()
        return jacksonObjectMapper().readValue(json, object : TypeReference<List<ForenkletSED>>() {})
    }
}