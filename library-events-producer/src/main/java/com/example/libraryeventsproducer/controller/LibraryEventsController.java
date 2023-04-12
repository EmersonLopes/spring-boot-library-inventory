package com.example.libraryeventsproducer.controller;

import com.example.libraryeventsproducer.domain.LibraryEvent;
import com.example.libraryeventsproducer.producer.LibraryEventProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

@Log4j2
@RestController
public class LibraryEventsController {

    @Autowired
    LibraryEventProducer libraryEventProducer;

    @PostMapping("/v1/libraryevent")
    public ResponseEntity<LibraryEvent> postLibraryEvent(@RequestBody LibraryEvent libraryEvent) throws JsonProcessingException, ExecutionException, InterruptedException {

        log.info("before sendLibraryEvent");
        // invoke kafka producer
//        libraryEventProducer.sendLibraryEvent(libraryEvent);
        SendResult<Integer, String> sendResult = libraryEventProducer.sendLibraryEventSynchronous(libraryEvent);
        log.info("SendResult is {}", sendResult.toString());
        log.info("after sendLibraryEvent");

        return ResponseEntity.status(HttpStatus.CREATED).body(libraryEvent);

    }
}
