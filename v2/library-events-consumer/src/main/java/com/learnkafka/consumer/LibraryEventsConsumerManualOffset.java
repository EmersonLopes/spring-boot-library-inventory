package com.learnkafka.consumer;

import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AcknowledgingConsumerAwareMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class LibraryEventsConsumerManualOffset implements AcknowledgingConsumerAwareMessageListener<Integer, String> {

    @Override
    @KafkaListener(topics = {"library-events"})
    public void onMessage(ConsumerRecord<Integer, String> consumerRecord, Acknowledgment acknowledgment, Consumer<?, ?> consumer) {
        log.info("ConsumerRecord: {}", consumerRecord);

        acknowledgment.acknowledge();
    }
}
