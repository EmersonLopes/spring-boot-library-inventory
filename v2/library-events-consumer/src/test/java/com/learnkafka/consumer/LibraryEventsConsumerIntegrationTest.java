package com.learnkafka.consumer;

import com.learnkafka.entity.LibraryEvent;
import com.learnkafka.jpa.LibraryEventRepository;
import com.learnkafka.service.LibraryEventService;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;

@SpringBootTest
@EmbeddedKafka(topics = "library-events")
@TestPropertySource(properties = {"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.admin.properties.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}"})
public class LibraryEventsConsumerIntegrationTest {

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    KafkaTemplate<Integer, String> kafkaTemplate;

    @Autowired
    KafkaListenerEndpointRegistry endpointRegistry;

    @SpyBean
    LibraryEventsConsumer libraryEventsConsumerSpy;

    @SpyBean
    LibraryEventService libraryEventServiceSpy;

    @Autowired
    LibraryEventRepository libraryEventRepository;

    @BeforeEach
    void setup(){
        for(MessageListenerContainer messageListenerContainer :endpointRegistry.getListenerContainers()){
            ContainerTestUtils.waitForAssignment(messageListenerContainer, embeddedKafkaBroker.getPartitionsPerTopic());
        }
    }

    @AfterEach
    void tearDown(){
        libraryEventRepository.deleteAll();
    }

    @SneakyThrows
    @Test
    void publishNewLibraryEvent() throws ExecutionException, InterruptedException {
        // given
        String json = "{\n" +
                "    \"libraryEventId\" : null,\n" +
                "    \"libraryEventType\": \"NEW\",\n" +
                "    \"book\" : {\n" +
                "       \"bookId\" : 456,\n" +
                "       \"bookName\" : \"Kafka Using Spring Boot 2\",\n" +
                "       \"bookAuthor\" : \"Dilip\" \n" +
                "    }\n" +
                "}";
        kafkaTemplate.sendDefault(json).get();

        // when
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(3, TimeUnit.SECONDS);

        // then
        Mockito.verify(libraryEventsConsumerSpy, Mockito.times(1)).onMessage(isA(ConsumerRecord.class));
        Mockito.verify(libraryEventServiceSpy, Mockito.times(1)).processLibraryEvent(isA(ConsumerRecord.class));

        List<LibraryEvent> libraryEventList = (List<LibraryEvent>) libraryEventRepository.findAll();
        assert libraryEventList.size() == 1;
        libraryEventList.forEach(libraryEvent -> {
            assert libraryEvent.getLibraryEventId() != null;
            assertEquals(456, libraryEvent.getBook().getBookId());
        });
    }
}
