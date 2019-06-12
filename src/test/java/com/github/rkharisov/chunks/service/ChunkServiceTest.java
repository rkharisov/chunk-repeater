package com.github.rkharisov.chunks.service;


import com.github.rkharisov.Utils;
import com.github.rkharisov.chunks.model.entity.ChunkEntity;
import com.github.rkharisov.chunks.model.response.ChunkDTO;
import com.github.rkharisov.chunks.model.response.Response;
import com.github.rkharisov.chunks.model.response.UnaryResponse;
import com.github.rkharisov.chunks.repository.ChunkRepository;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.rkharisov.chunks.model.enums.RepeatInterval.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class ChunkServiceTest extends AbstractTest {

    @Spy
    @InjectMocks
    private ChunkService subj;

    @Mock
    private ChunkRepository chunkRepository;

    @Test
    public void createOrUpdate_shouldCreateChunkIfNew() throws IOException, NoSuchAlgorithmException {
        doReturn(Optional.empty()).when(chunkRepository).findFirstByPathOrHash(anyString(), any());
        Path testPath = createTmpFile();

        subj.createOrUpdate(testPath);

        verify(subj).createNewChunk(eq(testPath.toString()), any());
        verify(chunkRepository).save(any());
    }

    @Test
    public void createOrUpdate_shouldRenameIfChunksNameChanged() throws IOException, NoSuchAlgorithmException {
        Path testPath = createTmpFile();
        byte[] digest = Utils.digest(Files.readAllBytes(testPath));
        ChunkEntity existedChunk = chunkEntity().setHash(digest).setPath(randStr());

        doReturn(Optional.of(existedChunk)).when(chunkRepository).findFirstByPathOrHash(testPath.toString(), digest);

        subj.createOrUpdate(testPath);

        verify(subj, never()).createNewChunk(eq(testPath.toString()), any());
        verify(chunkRepository).save(existedChunk);

        assertEquals(testPath.toString(), existedChunk.getPath());
    }

    @Test
    public void createOrUpdate_shouldUpdateIfChunkContentWasChanged() throws IOException, NoSuchAlgorithmException {
        Path testPath = createTmpFile();
        byte[] digest = Utils.digest(Files.readAllBytes(testPath));
        ChunkEntity existedChunk = chunkEntity().setHash(new byte[]{0, 1, 2, 3}).setPath(testPath.toString());

        doReturn(Optional.of(existedChunk)).when(chunkRepository).findFirstByPathOrHash(anyString(), any());

        subj.createOrUpdate(testPath);

        verify(subj, never()).createNewChunk(eq(testPath.toString()), any());
        verify(chunkRepository).save(existedChunk);

        assertArrayEquals(digest, existedChunk.getHash());
    }

    @Test
    public void markInactive() throws IOException {
        Path testPath = createTmpFile();
        ChunkEntity existedChunk = chunkEntity().setActive(true);

        doReturn(Stream.of(existedChunk)).when(chunkRepository).findByPathStartsWith(testPath.toString());

        subj.markInactive(testPath);

        verify(chunkRepository).saveAll(Collections.singletonList(existedChunk));
        assertFalse(existedChunk.getActive());
    }

    @Test
    public void getChunksForToday() throws IOException {
        Path testPath1 = createTmpFile();
        ChunkEntity testChunk1 = chunkEntity()
                .setPath(testPath1.toString())
                .setNextRepeatDate(LocalDate.now())
                .setCurrentRepetitionInterval(DAY)
                .setActive(true);

        Path testPath2 = createTmpFile();
        ChunkEntity testChunk2 = chunkEntity()
                .setPath(testPath2.toString())
                .setNextRepeatDate(LocalDate.now())
                .setCurrentRepetitionInterval(MONTH)
                .setActive(true);

        doReturn(Stream.of(testChunk1, testChunk2)).when(chunkRepository).findAllByActiveIsTrueAndNextRepeatDateIsLessThanEqual(LocalDate.now());

        List<ChunkEntity> chunksForToday = subj.getChunksForToday().collect(Collectors.toList());

        assertEquals(2, chunksForToday.size());
        assertEquals(testChunk1, chunksForToday.get(0));
        assertEquals(testChunk2, chunksForToday.get(1));
    }

    @Test
    public void getChunksForDay() throws IOException {
        LocalDate testDate = LocalDate.now().minusDays(randInt(365));

        Path testPath1 = createTmpFile();
        ChunkEntity testChunk1 = chunkEntity()
                .setPath(testPath1.toString())
                .setNextRepeatDate(testDate)
                .setCurrentRepetitionInterval(DAY)
                .setActive(true);

        Path testPath2 = createTmpFile();
        ChunkEntity testChunk2 = chunkEntity()
                .setPath(testPath2.toString())
                .setNextRepeatDate(testDate)
                .setCurrentRepetitionInterval(DAY)
                .setActive(true);

        doReturn(Stream.of(testChunk1, testChunk2)).when(chunkRepository).findAllByActiveIsTrueAndNextRepeatDateIsLessThanEqual(testDate);

        List<ChunkEntity> chunksForToday = subj.getChunksForDay(testDate).collect(Collectors.toList());

        assertEquals(2, chunksForToday.size());
        assertEquals(testChunk1, chunksForToday.get(0));
        assertEquals(testChunk2, chunksForToday.get(1));
    }

    @Test
    public void getMutated() throws IOException {
        Path testPath1 = createTmpFile();

        ChunkEntity testChunk1 = chunkEntity()
                .setPath(testPath1.toString())
                .setCurrentRepetitionInterval(DAY)
                .setActive(true)
                .setMutated(true);

        Path testPath2 = createTmpFile();
        ChunkEntity testChunk2 = chunkEntity()
                .setPath(testPath2.toString())
                .setCurrentRepetitionInterval(MONTH)
                .setActive(true)
                .setMutated(true);

        doReturn(Stream.of(testChunk1, testChunk2)).when(chunkRepository).findAllByActiveIsTrueAndMutatedIsTrue();

        List<ChunkEntity> chunksForToday = subj.getMutated().collect(Collectors.toList());

        assertEquals(2, chunksForToday.size());
        assertEquals(testChunk1, chunksForToday.get(0));
        assertEquals(testChunk2, chunksForToday.get(1));
    }

    @Test
    public void mapToResponse() {
        String fileName = randStr();
        String fileNameWithSuffix = fileName + XMIND_SUFFIX;
        ChunkEntity testChunk = chunkEntity()
                .setPath(fileNameWithSuffix)
                .setCurrentRepetitionInterval(DAY)
                .setId(uuid());

        ChunkDTO chunkDTO = subj.mapToResponse(testChunk);

        assertEquals(testChunk.getId(), chunkDTO.getId());
        assertEquals(testChunk.getCurrentRepetitionInterval(), chunkDTO.getCurrentRepetitionInterval());
        assertEquals(fileName, chunkDTO.getName());
        assertEquals(testChunk.getPath(), chunkDTO.getPath());
    }

    @Test
    public void markRepeated_shouldReturnMessageIfNotFound() {
        doReturn(Optional.empty()).when(chunkRepository).findById(any(UUID.class));

        Response genericResponse = subj.markRepeated(uuid());

        assertTrue(genericResponse instanceof UnaryResponse);
        UnaryResponse response = (UnaryResponse) genericResponse;
        assertNotNull(response.getValue());
        assertThat(response.getValue(), containsString(Boolean.FALSE.toString()));
    }

    @Test
    public void markRepeated_shouldReturnMessageIfRepetitionDayInTheFuture() {
        UUID uuid = uuid();
        LocalDate dateInTheFuture = LocalDate.now().plusDays(3);

        ChunkEntity testChunk = chunkEntity()
                .setId(uuid)
                .setNextRepeatDate(dateInTheFuture);

        doReturn(Optional.of(testChunk)).when(chunkRepository).findById(uuid);

        Response genericResponse = subj.markRepeated(uuid);

        assertTrue(genericResponse instanceof UnaryResponse);
        UnaryResponse response = (UnaryResponse) genericResponse;
        assertNotNull(response.getValue());
        assertThat(response.getValue(), containsString(Boolean.TRUE.toString()));
        assertThat(response.getValue(), containsString(dateInTheFuture.toString()));
    }

    @Test
    public void markRepeated_shouldSetNextIntervalWeekIfCurrentRepetitionIntervalDay() {
        LocalDate localDate = LocalDate.now().minusDays(3);
        ChunkEntity testChunk = chunkEntity()
                .setId(uuid())
                .setNextRepeatDate(localDate)
                .setCurrentRepetitionInterval(DAY);

        doReturn(Optional.of(testChunk)).when(chunkRepository).findById(any(UUID.class));
        doReturn(testChunk).when(chunkRepository).save(testChunk);

        Response genericResponse = subj.markRepeated(uuid());

        assertTrue(genericResponse instanceof ChunkDTO);
        verify(chunkRepository).save(testChunk);
        assertFalse(testChunk.getMutated());
        assertEquals(WEEK, testChunk.getCurrentRepetitionInterval());
        assertEquals(localDate.plus(WEEK.getPeriod()), testChunk.getNextRepeatDate());
    }

    @Test
    public void markRepeated_shouldSetNextIntervalYearIfCurrentRepetitionIntervalYear() {
        LocalDate localDate = LocalDate.now().minusDays(3);
        ChunkEntity testChunk = chunkEntity()
                .setId(uuid())
                .setNextRepeatDate(localDate)
                .setCurrentRepetitionInterval(YEAR);

        doReturn(Optional.of(testChunk)).when(chunkRepository).findById(any(UUID.class));
        doReturn(testChunk).when(chunkRepository).save(testChunk);

        Response genericResponse = subj.markRepeated(uuid());

        assertTrue(genericResponse instanceof ChunkDTO);

        verify(chunkRepository).save(testChunk);
        assertFalse(testChunk.getMutated());
        assertEquals(YEAR, testChunk.getCurrentRepetitionInterval());
        assertEquals(localDate.plus(YEAR.getPeriod()), testChunk.getNextRepeatDate());
    }

    @Test
    public void dropRepetitionDay() {
        List<ChunkEntity> chunkEntities = chunkEntities(
                3,
                ce -> ce.setMutated(true)
                        .setCurrentRepetitionInterval(YEAR)
                        .setNextRepeatDate(LocalDate.now().minusDays(10))
        );

        List<UUID> uuids = chunkEntityUuids(chunkEntities);

        doReturn(chunkEntities).when(chunkRepository).findAllById(uuids);
        doReturn(chunkEntities).when(chunkRepository).saveAll(chunkEntities);

        subj.dropRepetitionDay(uuids);

        assertTrue(chunkEntities.stream().noneMatch(ChunkEntity::getMutated));
        assertTrue(chunkEntities.stream().allMatch(ce -> ce.getCurrentRepetitionInterval() == DAY));
        assertTrue(chunkEntities.stream().allMatch(ce -> ce.getNextRepeatDate().equals(LocalDate.now().plusDays(1))));
    }

    @Test
    public void unmutate() {
        List<ChunkEntity> chunkEntities = chunkEntities(3, ce -> ce.setMutated(true));

        List<UUID> uuids = chunkEntityUuids(chunkEntities);

        doReturn(chunkEntities).when(chunkRepository).findAllById(uuids);
        doReturn(chunkEntities).when(chunkRepository).saveAll(chunkEntities);

        subj.unmutate(uuids);

        verify(chunkRepository).saveAll(chunkEntities);
        assertTrue(chunkEntities.stream().noneMatch(ChunkEntity::getMutated));
    }

    @Test
    public void dropOrUnmutate_shouldDropIfFlagTrue() {
        List<ChunkEntity> chunkEntities = chunkEntities(
                3,
                UnaryOperator.identity()
        );

        List<UUID> uuids = chunkEntityUuids(chunkEntities);

        List<String> rawUuids = uuids.stream()
                .map(UUID::toString)
                .collect(Collectors.toList());

        subj.dropOrUnmutate(true, rawUuids);

        verify(subj).dropRepetitionDay(uuids);
        verify(subj, never()).unmutate(uuids);
    }

    @Test
    public void dropOrUnmutate_shouldUnmutateIfFlagFalse() {
        List<ChunkEntity> chunkEntities = chunkEntities(
                3,
                UnaryOperator.identity()
        );

        List<UUID> uuids = chunkEntityUuids(chunkEntities);

        List<String> rawUuids = uuids.stream()
                .map(UUID::toString)
                .collect(Collectors.toList());

        subj.dropOrUnmutate(false, rawUuids);

        verify(subj).unmutate(uuids);
        verify(subj, never()).dropRepetitionDay(uuids);
    }


    @AfterClass
    public static void removeTempFiles() throws IOException {
        for (Path tempPath : tempFiles) {
            Files.deleteIfExists(tempPath);
        }
    }
}