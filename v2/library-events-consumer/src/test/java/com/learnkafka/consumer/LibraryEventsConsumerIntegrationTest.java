package com.learnkafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkafka.entity.Book;
import com.learnkafka.entity.LibraryEvent;
import com.learnkafka.entity.LibraryEventType;
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

    @Autowired
    ObjectMapper objectMapper;

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

    @Test
    public void publishUpdateLibraryEvent() throws JsonProcessingException, InterruptedException {
        // given
        String json = "{\n" +
                "    \"libraryEventId\" : null,\n" +
                "    \"libraryEventType\": \"NEW\",\n" +
                "    \"book\" : {\n" +
                "       \"bookId\" : 456,\n" +
                "       \"bookName\" : \"Kafka Using Spring Boot\",\n" +
                "       \"bookAuthor\" : \"Dilip\" \n" +
                "    }\n" +
                "}";

        LibraryEvent libraryEvent = objectMapper.readValue(json, LibraryEvent.class);
        libraryEvent.getBook().setLibraryEvent(libraryEvent);
        libraryEventRepository.save(libraryEvent);

        // publish the updated LibraryEvent
        Book updatedBook = Book.builder()
                .bookId(456)
                .bookName("Kafka Using Spring Boot 2.x")
                .bookAuthor("Dilip")
                .build();
        libraryEvent.setLibraryEventType(LibraryEventType.UPDATE);
        libraryEvent.setBook(updatedBook);
        String updatedJson = objectMapper.writeValueAsString(libraryEvent);
        kafkaTemplate.sendDefault(updatedJson);

        // when
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(3, TimeUnit.SECONDS);

        // then
        LibraryEvent persistedLibraryEvent = libraryEventRepository.findById(libraryEvent.getLibraryEventId()).get();
        assertEquals("Kafka Using Spring Boot 2.x", persistedLibraryEvent.getBook().getBookName());

    }

    @Test
    public void publishUpdateLibraryEvent_null_LibraryEvent() throws JsonProcessingException, InterruptedException {
        // given
        String json = "{\n" +
                "    \"libraryEventId\" : null,\n" +
                "    \"libraryEventType\": \"UPDATE\",\n" +
                "    \"book\" : {\n" +
                "       \"bookId\" : 456,\n" +
                "       \"bookName\" : \"Kafka Using Spring Boot\",\n" +
                "       \"bookAuthor\" : \"Dilip\" \n" +
                "    }\n" +
                "}";


        kafkaTemplate.sendDefault(json);

        // when
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(3, TimeUnit.SECONDS);

        // then
        Mockito.verify(libraryEventsConsumerSpy, Mockito.times(3)).onMessage(isA(ConsumerRecord.class));
        Mockito.verify(libraryEventServiceSpy, Mockito.times(3)).processLibraryEvent(isA(ConsumerRecord.class));

    }
}
