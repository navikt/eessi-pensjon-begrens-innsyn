package no.nav.eessi.pensjon.begrens.innsyn

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.services.eux.EuxService
import no.nav.eessi.pensjon.services.fagmodul.FagmodulService
import org.junit.jupiter.api.Test

internal class BegrensInnsynServiceTest {

    private val euxService = mockk<EuxService>(relaxed = true)
    private val fagmodulService = mockk<FagmodulService>()
    private val personService = mockk<PersonService>()
    private val begrensInnsynService = BegrensInnsynService(euxService, fagmodulService, personService, SedFnrSoek())


    @Test
    fun `Gitt at vi mottar en sed med skjermede personer, så skal vi begrense innsyn i hele bucen`() {

        val hendelse = javaClass.getResource("/sed/P_BUC_01.json").readText()
        val sedJson = javaClass.getResource("/sed/P2000-NAV_med_SPSF.json").readText()

        every { euxService.getSed(any(), any()) } returns sedJson
        every { personService.harAdressebeskyttelse(any(), any()) }.returns(true)

        begrensInnsynService.begrensInnsyn(hendelse)

        verify(exactly = 1) { euxService.getSed("147729", "4338515b6bed451798ba478c835409a3") }
        verify(exactly = 1) { personService.harAdressebeskyttelse(any(), any()) }
        verify(exactly = 1) { euxService.settSensitivSak("147729") }
        verify(exactly = 0) { fagmodulService.hentAlleDokumenterFraRinaSak("147729") }

    }

    @Test
    fun `Gitt at vi mottar en sed uten skjermede personer, så skal vi få tilbake en liste over alle seder som skal sjekkes på bucen`() {

        val hendelse = javaClass.getResource("/sed/P_BUC_01.json").readText()
        val sedJson = javaClass.getResource("/sed/P2000-NAV_med_SPSF.json").readText()
        val allDocuments = javaClass.getResource("/sed/allDocuments.json").readText()

        every { euxService.getSed(any(), any()) } returns sedJson
        every { personService.harAdressebeskyttelse(any(), any()) } returns false andThen true
        every { fagmodulService.hentAlleDokumenterFraRinaSak(any()) } returns allDocuments

        begrensInnsynService.begrensInnsyn(hendelse)

        verify(exactly = 1) { euxService.getSed(any(), "4338515b6bed451798ba478c835409a3") }
        verify(exactly = 1) { euxService.getSed(any(), "02249d3f5bdd4336999ccfbf7bb13c64") }
        verify(exactly = 2) { personService.harAdressebeskyttelse(any(), any()) }
        verify(exactly = 1) { fagmodulService.hentAlleDokumenterFraRinaSak("147729") }
        verify(exactly = 1) { euxService.settSensitivSak("147729") }
    }

    @Test
    fun `Git at vi ikke har skjermede personer i sedene, så skal vi ikke låse bucen`() {

        val hendelse = javaClass.getResource("/sed/P_BUC_01.json").readText()
        val sedJson = javaClass.getResource("/sed/P2000-NAV_med_SPSF.json").readText()
        val alleDokumenter = javaClass.getResource("/sed/allDocuments.json").readText()

        every { euxService.getSed(any(), any()) } returns sedJson
        every { personService.harAdressebeskyttelse(any(), any()) } returns false
        every { fagmodulService.hentAlleDokumenterFraRinaSak(any()) } returns alleDokumenter

        begrensInnsynService.begrensInnsyn((hendelse))

        verify(exactly = 2) { euxService.getSed(any(), any()) }
        verify(exactly = 2) { personService.harAdressebeskyttelse(any(), any()) }
        verify(exactly = 1) { fagmodulService.hentAlleDokumenterFraRinaSak(any()) }
        verify(exactly = 0) { euxService.settSensitivSak(any()) }

    }

}