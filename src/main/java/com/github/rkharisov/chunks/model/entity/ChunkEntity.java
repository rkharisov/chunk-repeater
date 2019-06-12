package com.github.rkharisov.chunks.model.entity;

import com.github.rkharisov.chunks.model.enums.RepeatInterval;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Data
@Accessors(chain = true)
public class ChunkEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(unique = true)
    private String path;

    @Column(unique = true)
    private byte[] hash;

    private RepeatInterval currentRepetitionInterval;

    @Column(updatable = false)
    private LocalDate creationDate;
    private LocalDate nextRepeatDate;

    private Boolean active;
    private Boolean mutated;

}
