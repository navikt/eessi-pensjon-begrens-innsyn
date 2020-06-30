package no.nav.eessi.pensjon.personoppslag

import no.nav.tjeneste.virksomhet.person.v3.informasjon.*

object PersonMock {
    internal fun createWith(fnr: String? = null,
                            landkoder: Boolean = true,
                            fornavn: String = "Test",
                            etternavn: String = "Testesen",
                            diskresjonskode: Diskresjonskoder? = null):
            Person? = Person()
            .withAktoer(PersonIdent().withIdent(NorskIdent().withIdent(fnr)))
            .withPersonnavn(Personnavn()
                            .withEtternavn(etternavn)
                            .withFornavn(fornavn)
                            .withSammensattNavn("$fornavn $etternavn"))
                    .withKjoenn(Kjoenn().withKjoenn(Kjoennstyper().withValue("M")))
                    .withStatsborgerskap(Statsborgerskap().withLand(Landkoder().withValue("NOR")))
                    .withBostedsadresse(Bostedsadresse()
                            .withStrukturertAdresse(Gateadresse()
                                            .withGatenavn("Oppoverbakken")
                                            .withHusnummer(66)
                                            .withPoststed(Postnummer().withValue("1920"))
                                            .withLandkode(when(landkoder){
                                                        true -> Landkoder().withValue("NOR")
                                                        else -> null
                                            })))
            .withDiskresjonskode(diskresjonskode)
}
