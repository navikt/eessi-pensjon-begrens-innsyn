package no.nav.eessi.pensjon.begrens.innsyn


import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.listener.ContainerProperties
import java.time.Duration

@EnableKafka
@Profile("integrationtest")
@Configuration
class IntegrationtestConfig(
    @param:Value("\${spring.embedded.kafka.brokers}") private val bootstrapServers: String) {

    @Bean
    fun aivenProducerFactory(): ProducerFactory<String, String> {
        val configMap: MutableMap<String, Any> = HashMap()
        populerCommonConfig(configMap)
        configMap[ProducerConfig.CLIENT_ID_CONFIG] = "eessi-pensjon-begrens-innsyn"
        configMap[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        configMap[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        configMap[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers

        return DefaultKafkaProducerFactory(configMap)
    }

    @Bean
    fun kafkaConsumerFactory(): ConsumerFactory<String, String> {
        val configMap: MutableMap<String, Any> = HashMap()
        populerCommonConfig(configMap)
        configMap[ConsumerConfig.CLIENT_ID_CONFIG] = "eessi-pensjon-begrens-innsyn"
        configMap[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configMap[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configMap[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        configMap[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false

        return DefaultKafkaConsumerFactory(configMap)
    }

    @Bean
    fun sedKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String>? {
        return ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            consumerFactory = kafkaConsumerFactory()
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
            containerProperties.setAuthExceptionRetryInterval(Duration.ofSeconds(4L))
        }
    }

    @Bean
    fun aivenKafkaTemplate(): KafkaTemplate<String, String> {
        return KafkaTemplate(aivenProducerFactory())
    }

    @Bean
    fun aivenKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String>? {
        return ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            consumerFactory = kafkaConsumerFactory()
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
            containerProperties.setAuthExceptionRetryInterval(Duration.ofSeconds(4L))
        }
    }

    private fun populerCommonConfig(configMap: MutableMap<String, Any>) {
        configMap[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "PLAINTEXT"
    }
}