package no.nav.eessi.pensjon.config

import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.SeekToCurrentErrorHandler
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer
import org.springframework.context.annotation.Bean


@Configuration
class KafkaConfig  {

    /**
     * Denne konfigurasjonen forsøker å behandle en melding en gang og stopper videre behandling av nye meldinger dersom det feiler
     */
    @Bean
    fun kafkaListenerContainerFactory(
            configurer: ConcurrentKafkaListenerContainerFactoryConfigurer,
            kafkaConsumerFactory: ConsumerFactory<Any, Any>): ConcurrentKafkaListenerContainerFactory<*, *> {
        val factory = ConcurrentKafkaListenerContainerFactory<Any, Any>()
        configurer.configure(factory, kafkaConsumerFactory)
        factory.setErrorHandler(SeekToCurrentErrorHandler(1))
        return factory
    }
}