package no.nav.eessi.pensjon.begrens.innsyn

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.eessi.pensjon.eux.model.buc.BucType

@JsonIgnoreProperties(ignoreUnknown = true)
class SedHendelseModel(
    val sektorKode: String,
    val rinaSakId: String,
    val rinaDokumentId: String,
    val bucType: BucType? = null
) {
    companion object {
        private val sedMapper: ObjectMapper = jacksonObjectMapper().configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)

        fun fromJson(json: String): SedHendelseModel = sedMapper.readValue(json, SedHendelseModel::class.java)
    }
}




