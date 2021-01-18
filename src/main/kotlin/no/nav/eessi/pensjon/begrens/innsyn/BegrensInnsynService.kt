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

    fun begrensInnsyn(hendelse: String) {
        val sedHendelse = SedHendelseModel.fromJson(hendelse)
        if (sedHendelse.sektorKode == "P") {
            begrensInnsyn(sedHendelse)
        }
    }

    private fun begrensInnsyn(sedHendelse: SedHendelseModel) {

        val harAdressebeskyttelse = harAdressebeskyttelse(sedHendelse.rinaSakId, sedHendelse.rinaDokumentId)

        if (harAdressebeskyttelse) {
            euxService.settSensitivSak(sedHendelse.rinaSakId)
            return
        } else {
            //hvis null prøver vi samtlige SEDs på bucken
            val documentsIds = hentSedDocumentsIds(hentSedsIdfraRina(sedHendelse.rinaSakId))

            documentsIds.forEach { documentId ->
                if(harAdressebeskyttelse(sedHendelse.rinaSakId, documentId)) {
                    euxService.settSensitivSak(sedHendelse.rinaSakId)
                    return
                }
            }
        }
    }

    companion object {
        fun trimFnrString(fnrAsString: String) = fnrAsString.replace("[^0-9]".toRegex(), "")
    }


    private fun harAdressebeskyttelse(rinaNr: String, sedDokumentId: String): Boolean {
        logger.debug("Henter Sed dokument for å lete igjennom FNR for å sjekke adressebeskyttelse")
        val sed = euxService.getSed(rinaNr, sedDokumentId)

        return sedFnrSoek.finnAlleFnrDnrISed(sed!!)
            .map { trimFnrString(it)}
            .filter { it.isNotBlank() }
            .mapNotNull { fnr -> pernsonService.hentPerson(NorskIdent(fnr))}
            .mapNotNull { it.adressebeskyttelse }
            .flatten()
            .any{it == STRENGT_FORTROLIG || it == STRENGT_FORTROLIG_UTLAND }
    }

    fun hentSedsIdfraRina(rinaNr: String): String? {
        logger.debug("Prøver å Henter nødvendige Rina documentid fra rinasaknr: $rinaNr")
        return fagmodulService.hentAlleDokumenterFraRinaSak(rinaNr)
    }


    fun hentSedDocumentsIds(sedJson: String?): List<String> {
        val sedRootNode = mapper.readTree(sedJson)

        val resultater = BucHelper.filterUtGyldigSedId(sedRootNode)
        return resultater.map { it.first }

    }
}
