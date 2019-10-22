package no.nav.eessi.pensjon.begrens.innsyn

import com.nhaarman.mockitokotlin2.*
import no.nav.eessi.pensjon.sed.SedFnrSøk
import no.nav.eessi.pensjon.services.eux.EuxService
import no.nav.eessi.pensjon.services.personv3.PersonV3Service
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Diskresjonskoder
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.nio.file.Files
import java.nio.file.Paths


@ExtendWith(MockitoExtension::class)
internal class BegrensInnsynServiceTest {

    @Mock
    lateinit var mockEuxService: EuxService

    @Mock
    lateinit var mockPersonV3Service: PersonV3Service

    @Mock
    lateinit var mockSedFnrSøk: SedFnrSøk
    lateinit var begrensInnsyn: BegrensInnsynService

    @BeforeEach
    fun setup() {

    begrensInnsyn = BegrensInnsynService(
            mockEuxService,
            mockPersonV3Service,
            mockSedFnrSøk )

    }

    @Test
    fun `gitt at vi mottar en R004 så skal EP behandle den`() {
        val sedHendelse = String(Files.readAllBytes(Paths.get("src/test/resources/sed/R_BUC_02.json")))
        doReturn(sedHendelse).whenever(mockEuxService).getSed(any(), any())

        val fnr = setOf("12050412345")
        doReturn(fnr).whenever(mockSedFnrSøk).finnAlleFnrDnrISed(sedHendelse)

        val person = Person().withDiskresjonskode(Diskresjonskoder().withValue("SPSF"))

        doReturn(person).whenever(mockPersonV3Service).hentPerson("12050412345")

        val result = begrensInnsyn.begrensInnsyn(sedHendelse)

        verify(mockEuxService, times(1)).getSed("147729", "b12e06dda2c7474b9998c7139c841646")
        verify(mockSedFnrSøk, times(1)).finnAlleFnrDnrISed(sedHendelse)
        verify(mockPersonV3Service, times(1)).hentPerson("12050412345")
        verify(mockEuxService, times(1)).settSensitivSak("147729")
    }

    @Test
    fun `gitt at vi mottar en R008 så skal ikke EP behandle den`() {
        val sedHendelse = String(Files.readAllBytes(Paths.get("src/test/resources/sed/R_BUC_02_ikkeBehandle.json")))

        verify(mockEuxService, times(0)).getSed("147729", "b12e06dda2c7474b9998c7139c841646")
        verify(mockSedFnrSøk, times(0)).finnAlleFnrDnrISed(sedHendelse)
        verify(mockPersonV3Service, times(0)).hentPerson("12050412345")
        verify(mockEuxService, times(0)).settSensitivSak("147729")
    }
}