package no.nav.eessi.pensjon.begrens.innsyn

import no.nav.eessi.pensjon.models.BucType
import no.nav.eessi.pensjon.models.SedType
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import org.junit.jupiter.api.Assertions.assertEquals

class SedHendelseModelTest {

    @Test
    fun `Gitt en gyldig SEDSendt json naar mapping saa skal alle felter mappes`() {
        val sedSendtJson = String(Files.readAllBytes(Paths.get("src/test/resources/sed/P_BUC_01.json")))
        val sedHendelse = SedHendelseModel.fromJson(sedSendtJson)
        assertEquals(sedHendelse.id, 1869L)
        assertEquals(sedHendelse.sedId, "P6000_4338515b6bed451798ba478c835409a3_2")
        assertEquals(sedHendelse.sektorKode, "P")
        assertEquals(sedHendelse.bucType, BucType.P_BUC_01)
        assertEquals(sedHendelse.rinaSakId, "147729")
        assertEquals(sedHendelse.avsenderId, "NO:NAVT003")
        assertEquals(sedHendelse.avsenderNavn, "NAVT003")
        assertEquals(sedHendelse.mottakerNavn, "NAV Test 07")
        assertEquals(sedHendelse.rinaDokumentId, "4338515b6bed451798ba478c835409a3")
        assertEquals(sedHendelse.rinaDokumentVersjon, "2")
        assertEquals(sedHendelse.sedType, SedType.P6000)
        assertEquals(sedHendelse.navBruker, "12378945601")
    }

    @Test
    fun `AvsenderId, avsnederNavn, mottakerNavn og mottakerId skal kunne vaere null i mapping av SEDSendt`() {
        val sedSendtJson = String(Files.readAllBytes(Paths.get("src/test/resources/sed/P_BUC_01_avsenderId_null.json")))
        val sedHendelse = SedHendelseModel.fromJson(sedSendtJson)
        assertEquals(sedHendelse.id, 1869L)
        assertEquals(sedHendelse.sedId, "P2000_b12e06dda2c7474b9998c7139c841646_2")
        assertEquals(sedHendelse.sektorKode, "P")
        assertEquals(sedHendelse.bucType, BucType.P_BUC_01)
        assertEquals(sedHendelse.rinaSakId, "147729")
        assertEquals(sedHendelse.avsenderId, null)
        assertEquals(sedHendelse.avsenderNavn, null)
        assertEquals(sedHendelse.mottakerNavn, null)
        assertEquals(sedHendelse.mottakerId, null)
        assertEquals(sedHendelse.rinaDokumentId, "b12e06dda2c7474b9998c7139c841646")
        assertEquals(sedHendelse.rinaDokumentVersjon, "2")
        assertEquals(sedHendelse.sedType, SedType.P2000)
        assertEquals(sedHendelse.navBruker, "12378945601")
    }
}
