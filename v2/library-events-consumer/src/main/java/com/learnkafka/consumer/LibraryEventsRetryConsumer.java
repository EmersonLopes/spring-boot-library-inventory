package com.learnkafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.learnkafka.service.LibraryEventService;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class LibraryEventsRetryConsumer {

    @Autowired
    private LibraryEventService libraryEventService;

    @KafkaListener(topics = {"${topics.retry}"}, groupId = "retry-listener-group", autoStartup = "${retryListener.startup:false}")
    public void onMessage(ConsumerRecord<Integer, String> consumerRecord) throws JsonProcessingException {
        log.info("ConsumerRecord in Retry Consumer: {}", consumerRecord);

//        consumerRecord.headers().forEach(header -> log.info("Key: {}, value; {}", header.key(), new String(header.value())));

        libraryEventService.processLibraryEvent(consumerRecord);
    }
}
