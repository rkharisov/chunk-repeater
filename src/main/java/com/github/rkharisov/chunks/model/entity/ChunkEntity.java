package com.github.rkharisov.chunks.model.entity;

import com.github.rkharisov.chunks.model.enums.RepeatInterval;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.LocalDate;
import java.util.UUID;

@Entity
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

    public ChunkEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public byte[] getHash() {
        return hash;
    }

    public void setHash(byte[] hash) {
        this.hash = hash;
    }

    public RepeatInterval getCurrentRepetitionInterval() {
        return currentRepetitionInterval;
    }

    public void setCurrentRepetitionInterval(RepeatInterval currentRepetitionInterval) {
        this.currentRepetitionInterval = currentRepetitionInterval;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDate getNextRepeatDate() {
        return nextRepeatDate;
    }

    public void setNextRepeatDate(LocalDate nextRepeatDate) {
        this.nextRepeatDate = nextRepeatDate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getMutated() {
        return mutated;
    }

    public void setMutated(Boolean mutated) {
        this.mutated = mutated;
    }
}
