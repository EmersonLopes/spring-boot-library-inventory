package com.learnkafka.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Book {


    @Id

    Integer bookId;

    String bookName;

    String bookAuthor;

    @OneToOne
    @JoinColumn(name = "libraryEventId")
    private LibraryEvent libraryEvent;
}
