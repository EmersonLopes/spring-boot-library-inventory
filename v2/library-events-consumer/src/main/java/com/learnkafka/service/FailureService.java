package com.learnkafka.service;


import com.learnkafka.entity.FailureRecord;
import com.learnkafka.jpa.FailureRecordRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class FailureService {

    private FailureRecordRepository failureRecordRepository;


    public FailureService(FailureRecordRepository failureRecordRepository) {
        this.failureRecordRepository = failureRecordRepository;
    }

    public void saveFailedRecord(ConsumerRecord<Integer, String> record, Exception exception, String retry) {
        FailureRecord failureRecord =
                new FailureRecord(null, record.topic(), record.key(), record.value(), record.partition(), record.offset(), exception.getCause().getMessage(), retry);

        failureRecordRepository.save(failureRecord);
    }
}
