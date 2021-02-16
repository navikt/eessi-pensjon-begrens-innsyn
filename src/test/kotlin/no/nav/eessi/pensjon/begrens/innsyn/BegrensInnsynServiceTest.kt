package no.nav.eessi.pensjon.begrens.innsyn

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.eessi.pensjon.eux.EuxService
import no.nav.eessi.pensjon.eux.model.document.ForenkletSED
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

    private fun opprettDokumenter(): List<ForenkletSED> {
        val json = javaClass.getResource("/sed/allDocuments.json").readText()
        return jacksonObjectMapper().readValue(json, object : TypeReference<List<ForenkletSED>>() {})
    }

}