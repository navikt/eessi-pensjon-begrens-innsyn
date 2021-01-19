package no.nav.eessi.pensjon.personoppslag

import no.nav.eessi.pensjon.personoppslag.pdl.model.AdressebeskyttelseGradering
import no.nav.eessi.pensjon.personoppslag.pdl.model.Foedsel
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentGruppe
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentInformasjon
import no.nav.eessi.pensjon.personoppslag.pdl.model.Navn
import no.nav.eessi.pensjon.personoppslag.pdl.model.Person
import no.nav.eessi.pensjon.personoppslag.pdl.model.UtenlandskAdresse
import no.nav.eessi.pensjon.personoppslag.pdl.model.Vegadresse
import java.time.LocalDateTime


object PersonMock {
    internal fun createWith(
        fnr: String? = null,
        landkoder: Boolean = true,
        fornavn: String = "Test",
        etternavn: String = "Testesen",
        harAdressebeskyttelse: Boolean = false
    ): Person {

//        val foedselsdato = fnr?.let { Fodselsnummer.fra(it)?.getBirthDate() }
        val foedselsdato = null
        val utenlandskadresse = if (landkoder) null else UtenlandskAdresse(landkode = "SWE")

        val identer = listOfNotNull(
            fnr?.let { IdentInformasjon(ident = it, gruppe = IdentGruppe.FOLKEREGISTERIDENT) }
        )

        val adressebeskyttelse = listOfNotNull(
            AdressebeskyttelseGradering.STRENGT_FORTROLIG.takeIf { harAdressebeskyttelse })

        return Person(
            identer = identer,
            navn = Navn(fornavn, null, etternavn),
            adressebeskyttelse = adressebeskyttelse,
            bostedsadresse = no.nav.eessi.pensjon.personoppslag.pdl.model.Bostedsadresse(
                gyldigFraOgMed = LocalDateTime.now(),
                gyldigTilOgMed = LocalDateTime.now(),
                vegadresse = Vegadresse("Oppoverbakken", "66", null, "1920"),
                utenlandskAdresse = utenlandskadresse
            ),
            oppholdsadresse = null,
            statsborgerskap = emptyList(),
            foedsel = Foedsel(foedselsdato, null),
            geografiskTilknytning = null,
            kjoenn = null,
            doedsfall = null,
            sivilstand = emptyList(),
            familierelasjoner = emptyList()
        )
    }
}
