package com.github.rkharisov.chunks.service;

import com.github.rkharisov.chunks.model.entity.ChunkEntity;
import com.github.rkharisov.chunks.model.enums.RepeatInterval;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AbstractTest {

    public static final String XMIND_SUFFIX = ".xmind";

    protected Random random;

    private Set<Integer> usedNums;

    protected static LinkedList<Path> tempFiles;

    public AbstractTest() {
        this.random = new Random();
        usedNums = new HashSet<>();
        tempFiles = new LinkedList<>();
    }

    public int randInt() {
        return getUnique(random::nextInt, Function.identity());
    }

    public int randInt(int bound) {
        return getUnique(() -> random.nextInt(bound), Function.identity());
    }

    public byte[] rantByteArr(int size) {
        byte[] buf = new byte[size];
        random.nextBytes(buf);
        return buf;
    }

    public String randStr() {
        return getUnique(random::nextInt, Object::toString);
    }

    public UUID uuid() {
        return UUID.randomUUID();
    }

    public Path createTmpFile(String suffix) throws IOException {
        Path tempFile = Files.createTempFile("", suffix);
        Files.write(tempFile, new byte[0]);
        tempFiles.add(tempFile);
        return tempFile;
    }

    public Path createTmpFile() throws IOException {
        return createTmpFile("");
    }

    private <T> T getUnique(IntSupplier numSupplier, Function<Integer, T> converter) {
        int randInt;
        while (true) {
            randInt = numSupplier.getAsInt();
            if (!usedNums.contains(randInt)) {
                return converter.apply(randInt);
            }
        }
    }

    public ChunkEntity chunkEntity() {
        return new ChunkEntity()
                .setPath(randStr() + XMIND_SUFFIX)
                .setNextRepeatDate(LocalDate.now())
                .setCurrentRepetitionInterval(RepeatInterval.DAY)
                .setCreationDate(LocalDate.now().minusDays(3))
                .setHash(rantByteArr(3))
                .setId(uuid())
                .setActive(true)
                .setMutated(true);
    }

    public List<ChunkEntity> chunkEntities(int count, UnaryOperator<ChunkEntity> fieldMapper) {
        return Stream.generate(this::chunkEntity)
                .limit(count)
                .map(fieldMapper)
                .collect(Collectors.toList());
    }

    public List<UUID> chunkEntityUuids(List<ChunkEntity> chunkEntities) {
        return chunkEntities.stream()
                .map(ChunkEntity::getId)
                .collect(Collectors.toList());
    }


    static class Pair {
        String name;
        Object value;

        private Pair(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        public static Pair of(String name, Object value) {
            return new Pair(name, value);
        }
    }
}
