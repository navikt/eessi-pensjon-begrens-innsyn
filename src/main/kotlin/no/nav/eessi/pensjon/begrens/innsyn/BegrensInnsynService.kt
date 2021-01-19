package no.nav.eessi.pensjon.begrens.innsyn

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.personoppslag.pdl.model.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.eessi.pensjon.personoppslag.pdl.model.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import no.nav.eessi.pensjon.personoppslag.pdl.model.NorskIdent
import no.nav.eessi.pensjon.services.eux.EuxService
import no.nav.eessi.pensjon.services.fagmodul.FagmodulService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BegrensInnsynService(private val euxService: EuxService,
                           private val fagmodulService: FagmodulService,
                           private val pernsonService: PersonService,
                           private val sedFnrSoek: SedFnrSoek)  {

    private val logger = LoggerFactory.getLogger(BegrensInnsynService::class.java)

    private val mapper = jacksonObjectMapper()

    private val gradering = listOf(STRENGT_FORTROLIG, STRENGT_FORTROLIG_UTLAND)

    fun begrensInnsyn(hendelse: String) {
        val sedHendelse = SedHendelseModel.fromJson(hendelse)
        if (sedHendelse.sektorKode == "P") {
            begrensInnsyn(sedHendelse)
        }
    }

    private fun begrensInnsyn(sedHendelse: SedHendelseModel) {
        val rinaSakId = sedHendelse.rinaSakId

        if (harAdressebeskyttelse(rinaSakId, sedHendelse.rinaDokumentId)) {
            euxService.settSensitivSak(rinaSakId)
        } else {
            //hvis null prøver vi samtlige SEDs på bucken
            hentSedDocumentsIds(hentSedsIdfraRina(rinaSakId))
                    .firstOrNull { docId -> harAdressebeskyttelse(rinaSakId, docId) }
                    ?.run { euxService.settSensitivSak(rinaSakId) }
        }
    }

    private fun harAdressebeskyttelse(rinaNr: String, sedDokumentId: String): Boolean {
        logger.debug("Henter SED, finner alle fnr i dokumentet, og leter etter adressebeskyttelse i PDL")
        val sed = euxService.getSed(rinaNr, sedDokumentId)

        val fnrListe = sedFnrSoek.finnAlleFnrDnrISed(sed!!)
                .map { trimFnrString(it) }
                .filter { it.isBlank() }

        return pernsonService.harAdressebeskyttelse(fnrListe, gradering)
    }

    private fun trimFnrString(fnrAsString: String) = fnrAsString.replace("[^0-9]".toRegex(), "")

    private fun hentSedsIdfraRina(rinaNr: String): String? {
        logger.debug("Prøver å Henter nødvendige Rina documentid fra rinasaknr: $rinaNr")
        return fagmodulService.hentAlleDokumenterFraRinaSak(rinaNr)
    }

    private fun hentSedDocumentsIds(sedJson: String?): List<String> {
        val sedRootNode = mapper.readTree(sedJson)

        return BucHelper.filterUtGyldigSedId(sedRootNode)
                .map { (id, _) -> id }
    }
}
