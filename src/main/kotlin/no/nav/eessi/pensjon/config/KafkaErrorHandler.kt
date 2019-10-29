package no.nav.eessi.pensjon.config

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.listener.*
import org.springframework.stereotype.Component
import java.lang.Exception

@Component
class KafkaErrorHandler : ContainerAwareErrorHandler {

    private val stopper = ContainerStoppingErrorHandler()

    override fun handle(thrownException: Exception?, records: MutableList<ConsumerRecord<*, *>>?, consumer: Consumer<*, *>?, container: MessageListenerContainer?) {
        stopper.handle(thrownException, records, consumer, container)
    }
 }
