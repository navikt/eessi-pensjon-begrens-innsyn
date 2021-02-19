package no.nav.eessi.pensjon.begrens.innsyn

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.eessi.pensjon.eux.EuxService
import no.nav.eessi.pensjon.eux.model.document.ForenkletSED
import no.nav.eessi.pensjon.eux.model.document.SedStatus
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.eux.model.sed.SedType
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import org.junit.jupiter.api.Test

internal class BegrensInnsynServiceTest {

    private val euxService = mockk<EuxService>(relaxed = true)
    private val personService = mockk<PersonService>()
    private val begrensInnsynService = BegrensInnsynService(euxService, personService)

    @Test
    fun `Gitt at vi mottar en sed med skjermede personer, så skal vi begrense innsyn i hele bucen`() {

        val hendelse = javaClass.getResource("/sed/P_BUC_01.json").readText()
        val sedJson = javaClass.getResource("/sed/P2000-NAV_med_SPSF.json").readText()

        every { euxService.hentSedJson(any(), any()) } returns sedJson
        every { personService.harAdressebeskyttelse(any(), any()) }.returns(true)

        begrensInnsynService.begrensInnsyn(hendelse)

        verify(exactly = 1) { euxService.hentSedJson("147729", "4338515b6bed451798ba478c835409a3") }
        verify(exactly = 1) { personService.harAdressebeskyttelse(any(), any()) }
        verify(exactly = 1) { euxService.settSensitivSak("147729") }
        verify(exactly = 0) { euxService.hentBucDokumenter("147729") }
    }

    @Test
    fun `Gitt at vi mottar en sed uten skjermede personer, så skal vi få tilbake en liste over alle seder som skal sjekkes på bucen`() {

        val hendelse = javaClass.getResource("/sed/P_BUC_01.json").readText()
        val sedJson = javaClass.getResource("/sed/P2000-NAV_med_SPSF.json").readText()

        every { euxService.hentSedJson(any(), any()) } returns sedJson
        every { personService.harAdressebeskyttelse(any(), any()) } returns false andThen true
        every { euxService.hentBucDokumenter(any()) } returns opprettDokumenter()

        begrensInnsynService.begrensInnsyn(hendelse)

        verify(exactly = 1) { euxService.hentSedJson(any(), "4338515b6bed451798ba478c835409a3") }
        verify(exactly = 1) { euxService.hentSedJson(any(), "02249d3f5bdd4336999ccfbf7bb13c64") }
        verify(exactly = 2) { personService.harAdressebeskyttelse(any(), any()) }
        verify(exactly = 1) { euxService.hentBucDokumenter("147729") }
        verify(exactly = 1) { euxService.settSensitivSak("147729") }
    }

    @Test
    fun `Git at vi ikke har skjermede personer i sedene, så skal vi ikke låse bucen`() {

        val hendelse = javaClass.getResource("/sed/P_BUC_01.json").readText()
        val sedJson = javaClass.getResource("/sed/P2000-NAV_med_SPSF.json").readText()

        every { euxService.hentSedJson(any(), any()) } returns sedJson
        every { personService.harAdressebeskyttelse(any(), any()) } returns false
        every { euxService.hentBucDokumenter(any()) } returns opprettDokumenter()

        begrensInnsynService.begrensInnsyn((hendelse))

        verify(exactly = 2) { euxService.hentSedJson(any(), any()) }
        verify(exactly = 2) { personService.harAdressebeskyttelse(any(), any()) }
        verify(exactly = 1) { euxService.hentBucDokumenter(any()) }
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
        every { personService.harAdressebeskyttelse(any(), any()) } returns false

        val hendelse = SedHendelseModel("P", rinaSakId, rinaDokumentId).toJson()
        begrensInnsynService.begrensInnsyn(hendelse)

        verify(exactly = 1) { euxService.hentBucDokumenter(rinaSakId) }
        verify(exactly = 1) { euxService.hentSedJson(rinaSakId, rinaDokumentId) }
        verify(exactly = 0) { euxService.hentSedJson(rinaSakId, not(rinaDokumentId)) }
        verify(exactly = 0) { euxService.settSensitivSak(any()) }
    }

    private fun opprettDokumenter(): List<ForenkletSED> {
        val json = javaClass.getResource("/sed/allDocuments.json").readText()
        return jacksonObjectMapper().readValue(json, object : TypeReference<List<ForenkletSED>>() {})
    }

    private fun Any.toJson(): String = jacksonObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .writeValueAsString(this)

}