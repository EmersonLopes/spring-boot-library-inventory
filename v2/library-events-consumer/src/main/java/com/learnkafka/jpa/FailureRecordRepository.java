package com.learnkafka.jpa;

import com.learnkafka.entity.FailureRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FailureRecordRepository extends JpaRepository<FailureRecord, Integer> {
}
