//package no.nav.eessi.pensjon.services.eux
//
//import com.fasterxml.jackson.databind.DeserializationFeature
//import com.fasterxml.jackson.databind.JsonNode
//import com.fasterxml.jackson.databind.ObjectMapper
//import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
//import io.micrometer.core.instrument.simple.SimpleMeterRegistry
//import no.nav.eessi.pensjon.metrics.MetricsHelper
//import org.slf4j.Logger
//import org.slf4j.LoggerFactory
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.http.HttpEntity
//import org.springframework.http.HttpMethod
//import org.springframework.stereotype.Service
//import org.springframework.web.client.RestTemplate
//import java.lang.RuntimeException
//
///**
// * @param metricsHelper Usually injected by Spring Boot, can be set manually in tests - no way to read metrics if not set.
// */
//@Service
//class EuxService(
//        private val euxOidcRestTemplate: RestTemplate,
//        @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper(SimpleMeterRegistry())
//) {
//
//    private val logger: Logger by lazy { LoggerFactory.getLogger(EuxService::class.java) }
//    private val mapper: ObjectMapper = jacksonObjectMapper().configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
//
//}
