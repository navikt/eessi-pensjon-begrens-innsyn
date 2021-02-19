package no.nav.eessi.pensjon.begrens.innsyn

import no.nav.eessi.pensjon.begrens.innsyn.BegrensInnsynService.SedTypeUtils.ugyldigeTyper
import no.nav.eessi.pensjon.eux.EuxService
import no.nav.eessi.pensjon.eux.model.sed.SedType
import no.nav.eessi.pensjon.eux.model.sed.SedType.X001
import no.nav.eessi.pensjon.eux.model.sed.SedType.X002
import no.nav.eessi.pensjon.eux.model.sed.SedType.X003
import no.nav.eessi.pensjon.eux.model.sed.SedType.X004
import no.nav.eessi.pensjon.eux.model.sed.SedType.X005
import no.nav.eessi.pensjon.eux.model.sed.SedType.X006
import no.nav.eessi.pensjon.eux.model.sed.SedType.X007
import no.nav.eessi.pensjon.eux.model.sed.SedType.X008
import no.nav.eessi.pensjon.eux.model.sed.SedType.X009
import no.nav.eessi.pensjon.eux.model.sed.SedType.X010
import no.nav.eessi.pensjon.eux.model.sed.SedType.X011
import no.nav.eessi.pensjon.eux.model.sed.SedType.X012
import no.nav.eessi.pensjon.eux.model.sed.SedType.X013
import no.nav.eessi.pensjon.eux.model.sed.SedType.X050
import no.nav.eessi.pensjon.eux.model.sed.SedType.X100
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.personoppslag.pdl.model.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.eessi.pensjon.personoppslag.pdl.model.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BegrensInnsynService(
    private val euxService: EuxService,
    private val personService: PersonService
) {

    private val logger = LoggerFactory.getLogger(BegrensInnsynService::class.java)

    private val gradering = listOf(STRENGT_FORTROLIG, STRENGT_FORTROLIG_UTLAND)

    fun begrensInnsyn(hendelse: String) {
        val sedHendelse = SedHendelseModel.fromJson(hendelse)
        if (sedHendelse.sektorKode == "P") {
            begrensInnsyn(sedHendelse)
        }
    }

    private fun begrensInnsyn(sedHendelse: SedHendelseModel) {
        val rinaSakId = sedHendelse.rinaSakId
        val dokumentId = sedHendelse.rinaDokumentId

        if (harAdressebeskyttelse(rinaSakId, dokumentId)) {
            logger.info("Fant adressebeskyttet person (RinaSakId: $rinaSakId, DokumentId: $dokumentId)")
            euxService.settSensitivSak(rinaSakId)
        } else {
            // Hvis vi ikke finner adressebeskyttelse på hoved-SED, prøver vi på samtlige SED i BUC
            val documentIds = euxService.hentBucDokumenter(rinaSakId)
                .filter { it.harGyldigStatus() }
                .filterNot { it.type in ugyldigeTyper}
                .map { it.id }

            logger.info("Fant ${documentIds.size} dokumenter. IDer: $documentIds")

            val beskyttet = documentIds
                    .filterNot { it == dokumentId } // Denne er allerede sjekket over
                    .any { docId -> harAdressebeskyttelse(rinaSakId, docId) }

            if (beskyttet) {
                logger.info("BEGRENSER INNSYN! RinaSakID $rinaSakId inneholder SED med adressebeskyttet person.")
                euxService.settSensitivSak(rinaSakId)
            } else {
                logger.info("Fant ingen adressebeskyttet person i SEDer (RinaSakID: $rinaSakId)")
            }
        }
    }

    private fun harAdressebeskyttelse(rinaNr: String, sedDokumentId: String): Boolean {
        logger.info("Henter SED, finner alle fnr i dokumentet, og leter etter adressebeskyttelse i PDL")
        val sed = euxService.hentSedJson(rinaNr, sedDokumentId)

        val fnrListe = SedFnrSoek.finnAlleFnrDnrISed(sed!!)
                .map { trimFnrString(it) }
                .filter { it.isNotBlank() }
                .distinct()

        logger.info("Fant ${fnrListe.size} unike fnr i SED (rinaNr: $rinaNr, sedDokId: $sedDokumentId)")

        return personService.harAdressebeskyttelse(fnrListe, gradering)
    }

    private fun trimFnrString(fnrAsString: String) = fnrAsString.replace("[^0-9]".toRegex(), "")

    private object SedTypeUtils {
        /**
         * SED-typer som ikke skal sjekkes
         */
        val ugyldigeTyper: Set<SedType> = setOf(
            X001, X002, X003, X004, X005, X006, X007, X008, X009, X010, X011, X012, X013, X050, X100
        )
    }

}
