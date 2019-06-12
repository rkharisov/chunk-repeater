package com.github.rkharisov.chunks.model.response;

import com.github.rkharisov.chunks.model.enums.RepeatInterval;

import java.util.UUID;

public class ChunkDTO implements Response {

    public String name;
    public UUID id;
    public RepeatInterval currentRepeatInterval;
    public String path;

    public ChunkDTO() {
    }

    public ChunkDTO(String name, UUID id, RepeatInterval currentRepeatInterval, String path) {
        this.name = name;
        this.id = id;
        this.currentRepeatInterval = currentRepeatInterval;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public UUID getId() {
        return id;
    }

    public RepeatInterval getCurrentRepeatInterval() {
        return currentRepeatInterval;
    }

    public String getPath() {
        return path;
    }
}
