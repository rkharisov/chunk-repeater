package com.github.rkharisov.chunks.model.response;

import com.github.rkharisov.chunks.model.enums.RepeatInterval;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ChunkDTO implements Response {

    public String name;
    public UUID id;
    public RepeatInterval currentRepetitionInterval;
    public String path;

}
