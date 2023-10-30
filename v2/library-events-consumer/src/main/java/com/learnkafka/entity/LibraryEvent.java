package com.learnkafka.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class LibraryEvent {

    @Id
    @GeneratedValue
    Integer libraryEventId;
    @Enumerated(EnumType.STRING)
    LibraryEventType libraryEventType;
    @OneToOne(mappedBy = "libraryEvent", cascade = CascadeType.ALL)
            @ToString.Exclude
    Book book;

}
