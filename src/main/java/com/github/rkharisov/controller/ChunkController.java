package com.github.rkharisov.controller;

import com.github.rkharisov.chunks.model.entity.ChunkEntity;
import com.github.rkharisov.chunks.model.response.ChunkDTO;
import com.github.rkharisov.chunks.model.response.Response;
import com.github.rkharisov.chunks.service.ChunkService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class ChunkController {

    @Autowired
    private ChunkService chunkService;


    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @Transactional(readOnly = true)
    @ApiOperation(value = "getActivitiesForToday", notes = "Получить список активных чанков, время повторения которых наступило")
    @ApiResponse(code = 200, message = "OK", response = ChunkDTO.class, responseContainer = "Map")
    public @ResponseBody
    Map<String, List<ChunkDTO>> getActivitiesForToday() {
        Map<String, List<ChunkDTO>> responseBody = new HashMap<>();
        try (Stream<ChunkEntity> chunksForToday = chunkService.getChunksForToday();
             Stream<ChunkEntity> mutatedChunks = chunkService.getMutated()) {
            responseBody.put("repeat",
                    chunksForToday.map(chunkService::mapToResponse)
                            .collect(Collectors.toList()));
            responseBody.put("mutated",
                    mutatedChunks.map(chunkService::mapToResponse)
                            .collect(Collectors.toList()));
        }
        return responseBody;
    }


    @GetMapping(value = "/mark/{id}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "markRepeated", notes = "Пометить чанк повторенным, сдвинув дату повторения на следующий этап")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = Response.class),
            @ApiResponse(code = 404, message = "NotFound"),
        }
    )
    public @ResponseBody
    Response markRepeated(@PathVariable @ApiParam(value = "UUID of chank to mark", required = true, readOnly = true) UUID id) {
        return chunkService.markRepeated(id);
    }


    @GetMapping(value = "/drop/{dropRequired}/{ids}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "dropOrUnmutate", notes = "Помечает чанк не измененным и в зависимости от переданного флага либо сбрасывает на первый этап повторения с началом сегодня, либо оставляет этап повторения не измененным")
    @ApiResponse(code = 200, message = "OK", response = ChunkDTO.class, responseContainer = "List")
    public @ResponseBody
    List<ChunkDTO> dropOrUnmutate(@PathVariable @ApiParam(value = "Flag for determining whether to reset the repeat phase or not", required = true, readOnly = true) Boolean dropRequired,
                                  @PathVariable @ApiParam(value = "UUID of chank to process", required = true, readOnly = true) String ids) {
        return chunkService.dropOrUnmutate(dropRequired, Arrays.asList(ids.split("[,|&]")))
                .stream()
                .map(chunkService::mapToResponse)
                .collect(Collectors.toList());
    }


}

