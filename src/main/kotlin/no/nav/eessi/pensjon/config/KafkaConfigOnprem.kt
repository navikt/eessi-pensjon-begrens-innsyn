package no.nav.eessi.pensjon.config

import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import java.time.Duration

@Profile("prod", "integrationtest")
@Configuration
class KafkaConfigOnprem {

    @Bean
    fun sedSendtAuthRetry(registry: KafkaListenerEndpointRegistry): ApplicationRunner? {
        return ApplicationRunner {
            val sedSendtListenerContainer = registry.getListenerContainer("sedSendtListener")
            sedSendtListenerContainer!!.containerProperties.setAuthExceptionRetryInterval(Duration.ofSeconds(4L))
            sedSendtListenerContainer.start()

            val sedMottattListenerContainer = registry.getListenerContainer("sedMottattListener")
            sedMottattListenerContainer!!.containerProperties.setAuthExceptionRetryInterval(Duration.ofSeconds(4L))
            sedMottattListenerContainer.start()
        }
    }
}