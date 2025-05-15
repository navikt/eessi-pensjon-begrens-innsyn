package no.nav.eessi.pensjon.begrens.innsyn

import no.nav.eessi.pensjon.begrens.innsyn.BegrensInnsynService.SedTypeUtils.ugyldigeTyper
import no.nav.eessi.pensjon.eux.EuxService
import no.nav.eessi.pensjon.eux.model.BucType.*
import no.nav.eessi.pensjon.eux.model.SedHendelse
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.SedType.*
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.time.measureTimedValue

@Service
class BegrensInnsynService(
    private val euxService: EuxService,
    private val personService: PersonService
) {

    private val logger = LoggerFactory.getLogger(BegrensInnsynService::class.java)

    private val gyldigSektorKoder = listOf("P", "R", "H")
    private val gyldigeBucTyper = listOf(H_BUC_07, R_BUC_02, M_BUC_02, M_BUC_03a, M_BUC_03b)

    fun begrensInnsyn(sedHendelse: SedHendelse) {
        if (sedHendelse.sedType in ugyldigeTyper) return
        if (sedHendelse.sektorKode in gyldigSektorKoder || sedHendelse.bucType in gyldigeBucTyper) {
            sjekkAdresseBeskyttelse(sedHendelse)
        }
    }

    private fun sjekkAdresseBeskyttelse(sedHendelse: SedHendelse) {
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

            val beskyttet = measureTimedValue {
                documentIds
                    .filterNot { it == dokumentId } // Denne er allerede sjekket over
                    .any { docId -> harAdressebeskyttelse(rinaSakId, docId) }
            }.also {
                logger.info("hentSed for rinasak:$rinaSakId tid: ${it.duration.inWholeSeconds}")
            }.value

            if (beskyttet) {
                logger.info("BEGRENSER INNSYN! RinaSakID $rinaSakId inneholder SED med adressebeskyttet person.")
                euxService.settSensitivSak(rinaSakId)
            } else {
                logger.info("Fant ingen adressebeskyttet person i SEDer (RinaSakID: $rinaSakId)")
            }
        }
    }

    private fun harAdressebeskyttelse(rinaNr: String, sedDokumentId: String): Boolean {
        logger.info("Henter SED, finner alle fnr i dokumentet med rinanr: $rinaNr og dukumentId:$sedDokumentId, og leter etter adressebeskyttelse i PDL")
        val sed = euxService.hentSedJson(rinaNr, sedDokumentId)

        val fnrListe = SedFnrSoek.finnAlleFnrDnrISed(sed!!)
                .map { trimFnrString(it) }
                .filter { it.isNotBlank() }
                .distinct()

        logger.info("Fant ${fnrListe.size} unike fnr i SED (rinaNr: $rinaNr, sedDokId: $sedDokumentId)")

        return personService.harAdressebeskyttelse(fnrListe)
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
