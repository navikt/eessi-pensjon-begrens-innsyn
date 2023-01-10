package no.nav.eessi.pensjon.begrens.innsyn

import no.nav.eessi.pensjon.eux.model.SedHendelse
import no.nav.eessi.pensjon.utils.mapJsonToAny
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import org.junit.jupiter.api.Assertions.assertEquals

class SedHendelseModelTest {

    @Test
    fun `Gitt en gyldig SEDSendt json naar mapping saa skal nødvendige felter mappes`() {
        val sedSendtJson = javaClass.getResource("/sed/P_BUC_01.json").readText()
        val sedHendelse = mapJsonToAny<SedHendelse>(sedSendtJson)
        assertEquals(sedHendelse.sektorKode, "P")
        assertEquals(sedHendelse.rinaSakId, "147729")
        assertEquals(sedHendelse.rinaDokumentId, "4338515b6bed451798ba478c835409a3")
    }
}
