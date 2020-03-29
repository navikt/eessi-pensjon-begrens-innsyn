package no.nav.eessi.pensjon.begrens.innsyn

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.eessi.pensjon.services.eux.EuxService
import no.nav.eessi.pensjon.services.fagmodul.FagmodulService
import no.nav.eessi.pensjon.services.personv3.Diskresjonskode
import no.nav.eessi.pensjon.services.personv3.PersonV3Service
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BegrensInnsynService(private val euxService: EuxService,
                           private val fagmodulService: FagmodulService,
                           private val personV3Service: PersonV3Service,
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

        val diskresjonskode = finnDiskresjonkode(sedHendelse.rinaSakId, sedHendelse.rinaDokumentId)

        if (diskresjonskode != null) {
            euxService.settSensitivSak(sedHendelse.rinaSakId)
            return
        } else {
            //hvis null prøver vi samtlige SEDs på bucken
            val documentsIds = hentSedDocumentsIds(hentSedsIdfraRina(sedHendelse.rinaSakId))

            documentsIds.forEach { documentId ->
                finnDiskresjonkode(sedHendelse.rinaSakId, documentId)?.let {
                    euxService.settSensitivSak(sedHendelse.rinaSakId)
                    return
                }
            }
        }
    }

    private fun finnDiskresjonkode(rinaNr: String, sedDokumentId: String): Diskresjonskode? {
        logger.debug("Henter Sed dokument for å lete igjennom FNR for diskresjonkode")
        val sed = euxService.getSed(rinaNr, sedDokumentId)

        val fnre = sedFnrSoek.finnAlleFnrDnrISed(sed!!)
        fnre.forEach { fnr ->
            val person = personV3Service.hentPerson(fnr)
            person?.diskresjonskode?.value?.let { kode ->
                logger.debug("Diskresjonskode: $kode")
                val diskresjonskode = Diskresjonskode.valueOf(kode)
                if (diskresjonskode == Diskresjonskode.SPSF) {
                    logger.debug("Personen har diskret adresse")
                    return diskresjonskode
                }
            }
        }
        return null
    }

    fun hentSedsIdfraRina(rinaNr: String): String? {
        logger.debug("Prøver å Henter nødvendige Rina documentid fra rinasaknr: $rinaNr")
        return fagmodulService.hentAlleDokumenterFraRinaSak(rinaNr)
    }


    fun hentSedDocumentsIds(sedJson: String?): List<String> {
        val sedRootNode = mapper.readTree(sedJson)
        return sedRootNode
                .filterNot { it.get("status").textValue() =="empty" }
                .map { it.get("id").textValue() }
                .toList()
    }
}
