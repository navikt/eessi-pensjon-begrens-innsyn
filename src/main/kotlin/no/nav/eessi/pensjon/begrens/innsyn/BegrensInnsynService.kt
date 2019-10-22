package no.nav.eessi.pensjon.begrens.innsyn

import com.sun.org.apache.xpath.internal.operations.Bool
import no.nav.eessi.pensjon.models.BucType
import no.nav.eessi.pensjon.models.SedType
import no.nav.eessi.pensjon.sed.SedFnrSøk
import no.nav.eessi.pensjon.services.eux.EuxService
import no.nav.eessi.pensjon.services.personv3.Diskresjonskode
import no.nav.eessi.pensjon.services.personv3.PersonV3Service
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BegrensInnsynService(private val euxService: EuxService,
                           private val personV3Service: PersonV3Service,
                           private val sedFnrSøk: SedFnrSøk) {

    private val logger = LoggerFactory.getLogger(BegrensInnsynService::class.java)

    fun begrensInnsyn(hendelse: String) {
        val sedHendelse = SedHendelseModel.fromJson(hendelse)
        if (!behandlesAvEP(sedHendelse)) {
            // Vi ignorerer alle hendelser som ikke har vår sektorkode
            return
        }

        val sed = euxService.getSed(sedHendelse.rinaSakId, sedHendelse.rinaDokumentId)
        val fnre = sedFnrSøk.finnAlleFnrDnrISed(sed!!)

        fnre.forEach { fnr ->
            val person = personV3Service.hentPerson(fnr)
            person.diskresjonskode?.value?.let { kode ->
                logger.debug("Diskresjonskode: $kode")
                val diskresjonskode = Diskresjonskode.valueOf(kode)
                if (diskresjonskode == Diskresjonskode.SPFO || diskresjonskode == Diskresjonskode.SPSF) {
                    logger.debug("Personen har diskret adresse")
                    euxService.settSensitivSak(sedHendelse.rinaSakId)
                    return
                }
            }
        }
    }

    private fun behandlesAvEP(sedHendelse: SedHendelseModel): Boolean {
        return sedHendelse.sektorKode == "P" || sjekkPaaRBucOgSed(sedHendelse)

    }

    private fun sjekkPaaRBucOgSed(sedHendelse: SedHendelseModel): Boolean {

        if (sedHendelse.bucType == BucType.R_BUC_02) {
            val liste = listOf(SedType.R004,SedType.R005,SedType.R006)
                if (liste.contains(sedHendelse.sedType)){
                    return true
            }
            return false
        }
        return false
    }
}