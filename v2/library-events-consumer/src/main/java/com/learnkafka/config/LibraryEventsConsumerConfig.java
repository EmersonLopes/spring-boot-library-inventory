package com.learnkafka.config;

import com.learnkafka.service.FailureService;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.FixedBackOff;

import java.util.List;

@Log4j2
@Configuration
@EnableKafka
public class LibraryEventsConsumerConfig {

    public static final String RETRY = "RETRY";
    public static final String DEAD = "DEAD";
    public static final String SUCCESS = "SUCCESS";

    @Autowired
    KafkaTemplate kafkaTemplate;

    @Autowired
    private FailureService failureService;

    @Value("${topics.retry}")
    private String retryTopic;

    @Value("${topics.dlt}")
    private String deadLetterTopic;



    public DeadLetterPublishingRecoverer publishingRecoverer() {


        return new DeadLetterPublishingRecoverer(kafkaTemplate,

                (consumerRecord, e) -> {
                    log.error("Exception in publishingRecoverer : {} ", e.getMessage(), e);
                    if (e.getCause() instanceof RecoverableDataAccessException) {
                        return new TopicPartition(retryTopic, consumerRecord.partition());
                    } else {
                        return new TopicPartition(deadLetterTopic, consumerRecord.partition());
                    }
                });


    }

    ConsumerRecordRecoverer consumerRecordRecoverer = (record, exception) -> {
        log.error("Exception is : {} Failed Record : {} ", exception, record);
        if (exception.getCause() instanceof RecoverableDataAccessException) {
            log.info("Inside the recoverable logic");
            //Add any Recovery Code here.
            failureService.saveFailedRecord((ConsumerRecord<Integer, String>) record, exception, RETRY);

        } else {
            log.info("Inside the non recoverable logic and skipping the record : {}", record);
            failureService.saveFailedRecord((ConsumerRecord<Integer, String>) record, exception, DEAD);

        }
    };

    public DefaultErrorHandler errorHandler() {

        var exceptionsToIgnoreList = List.of(IllegalArgumentException.class);
        var exceptionsToRetryList = List.of(RecoverableDataAccessException.class);

        var fixedBackOff = new FixedBackOff(5000L, 2);
        var expBackOff = new ExponentialBackOffWithMaxRetries(2);
        expBackOff.setInitialInterval(1_000L);
        expBackOff.setMultiplier(2.0);
        expBackOff.setMaxInterval(2_000L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
//                publishingRecoverer(),
                consumerRecordRecoverer,
                fixedBackOff
//                expBackOff
        );

        exceptionsToIgnoreList.forEach(errorHandler::addNotRetryableExceptions);
//        exceptionsToRetryList.forEach(errorHandler::addRetryableExceptions);
        errorHandler.setRetryListeners((consumerRecord, e, i) -> {
            log.info("Failed Record in Retry Listener, Exception : {}, deliveryAttempty : {}", e.getMessage(), i);
        });

        return errorHandler;
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory();
        configurer.configure(factory, kafkaConsumerFactory);
//        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
//        factory.setConcurrency(3);
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }
}
