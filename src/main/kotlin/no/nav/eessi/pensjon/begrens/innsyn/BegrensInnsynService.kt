package no.nav.eessi.pensjon.begrens.innsyn

import no.nav.eessi.pensjon.sed.SedFnrSøk
import no.nav.eessi.pensjon.services.eux.EuxService
import no.nav.eessi.pensjon.services.personv3.Diskresjonskode
import no.nav.eessi.pensjon.services.personv3.PersonV3Service
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BegrensInnsynService(private val euxService: EuxService,
                           private val personV3Service: PersonV3Service,
                           private val sedFnrSøk: SedFnrSøk)  {

    private val logger = LoggerFactory.getLogger(BegrensInnsynService::class.java)

    fun begrensInnsyn(hendelse: String) {

        val sedHendelse = SedHendelseModel.fromJson(hendelse)

        if (sedHendelse.sektorKode != "P") {
            // Vi ignorerer alle hendelser som ikke har vår sektorkode
            return
        }

        val sed = euxService.getSed(sedHendelse.rinaSakId, sedHendelse.rinaDokumentId)
        val fnre = sedFnrSøk.finnAlleFnrDnrISed(sed!!)

        fnre.forEach { fnr ->
            val person = personV3Service.hentPerson(fnr)
            person.diskresjonskode?.kodeverksRef.let {
                val diskresjonskode = Diskresjonskode.valueOf(person.diskresjonskode.kodeverksRef)
                if(diskresjonskode == Diskresjonskode.SPFO || diskresjonskode == Diskresjonskode.SPSF) {
                    logger.debug("personen har diskret adresse")
                    euxService.settSensitivSak(sedHendelse.rinaSakId)
                }
            }
        }
    }
}