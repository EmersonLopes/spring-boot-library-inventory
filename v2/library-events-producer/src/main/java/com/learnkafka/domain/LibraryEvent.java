package com.learnkafka.domain;

import jakarta.validation.Valid;

public record LibraryEvent(
        Integer libraryEventId,
        LibraryEventType libraryEventType,
        @Valid
        Book book
) {
}
