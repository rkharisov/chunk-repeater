package com.github.rkharisov.chunks.service;

import com.github.rkharisov.Utils;
import com.github.rkharisov.chunks.model.entity.ChunkEntity;
import com.github.rkharisov.chunks.model.enums.RepeatInterval;
import com.github.rkharisov.chunks.model.response.ChunkDTO;
import com.github.rkharisov.chunks.model.response.Response;
import com.github.rkharisov.chunks.model.response.UnaryResponse;
import com.github.rkharisov.chunks.repository.ChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.rkharisov.chunks.model.enums.RepeatInterval.DAY;

@Service
public class ChunkService {

    private static final Logger log = LoggerFactory.getLogger(ChunkService.class);

    @Autowired
    private ChunkRepository chunkRepository;

    public void createOrUpdate(Path path) throws IOException, NoSuchAlgorithmException {
        byte[] digest = Utils.digest(Files.readAllBytes(path));
        String path_ = path.toString();
        Optional<ChunkEntity> maybeChunk = chunkRepository.findFirstByPathOrHash(path_, digest);
        if (maybeChunk.isPresent()) {
            ChunkEntity chunk = maybeChunk.get();
            if (Arrays.equals(chunk.getHash(), digest)) {
                //здесь если файл был переименован
                chunk.setPath(path_);
                chunk.setActive(true);
            } else if (chunk.getPath().equals(path_)) {
                //здесь если файл был изменен
                chunk.setHash(digest);
                chunk.setMutated(true);
            }
            chunkRepository.save(chunk);
        } else {
            //здесь если новый чанк
            createNewChunk(path_, digest);
        }
    }

    private void createNewChunk(String path, byte[] hash) {
        ChunkEntity chunk = new ChunkEntity();
        chunk.setPath(path);
        chunk.setHash(hash);
        chunk.setCurrentRepetitionInterval(DAY);
        chunk.setCreationDate(LocalDate.now());
        chunk.setNextRepeatDate(chunk.getCreationDate().plus(chunk.getCurrentRepetitionInterval().getPeriod()));
        chunk.setActive(true);
        chunk.setMutated(false);
        chunkRepository.save(chunk);
    }

    @Transactional
    public void markInactive(Path path) {
        String path_ = path.toString();
        try (Stream<ChunkEntity> chunkStream = chunkRepository.findByPathStartsWith(path_)) {
            List<ChunkEntity> chunks = chunkStream
                    .peek(ce -> ce.setActive(false))
                    .collect(Collectors.toList());
            chunkRepository.saveAll(chunks);
        }
    }

    @Transactional(readOnly = true)
    public Stream<ChunkEntity> getChunksForToday() {
        return getChunksForDay(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public Stream<ChunkEntity> getChunksForDay(LocalDate day) {
        return chunkRepository.findAllByActiveIsTrueAndNextRepeatDateIsLessThanEqual(day);
    }

    @Transactional(readOnly = true)
    public Stream<ChunkEntity> getMutated() {
        return chunkRepository.findAllByActiveIsTrueAndMutatedIsTrue();
    }

    public ChunkDTO mapToResponse(ChunkEntity entity) {
        String name = new File(entity.getPath()).getName();
        return new ChunkDTO(
                name.substring(0, name.indexOf(MapDirWatcher.MAP_SUFFIX)),
                entity.getId(),
                entity.getCurrentRepetitionInterval(),
                entity.getPath()
        );
    }

    public Response markRepeated(UUID id) {
        Optional<ChunkEntity> maybeEntity = chunkRepository.findById(id);
        ChunkEntity chunkEntity;
        if (maybeEntity.isPresent() && !(chunkEntity = maybeEntity.get()).getNextRepeatDate().isAfter(LocalDate.now())) {
            RepeatInterval cri = chunkEntity.getCurrentRepetitionInterval();
            if (cri == RepeatInterval.YEAR) {
                chunkEntity.setNextRepeatDate(chunkEntity.getNextRepeatDate().plus(cri.getPeriod()));
                chunkEntity.setCurrentRepetitionInterval(RepeatInterval.YEAR);
            } else {
                chunkEntity.setCurrentRepetitionInterval(chunkEntity.getCurrentRepetitionInterval().next());
                chunkEntity.setNextRepeatDate(chunkEntity.getNextRepeatDate().plus(chunkEntity.getCurrentRepetitionInterval().getPeriod()));
            }
            chunkEntity.setMutated(false);
            return mapToResponse(chunkRepository.save(chunkEntity));
        } else {
            String message = String.join("", "Не удалось пометить чанк повторенным. Сущность найдена: ",
                    Boolean.toString(maybeEntity.isPresent()),
                    ", дата повторения: ",
                    maybeEntity.map(ce -> ce.getNextRepeatDate().toString()).orElse("не известно"));
            log.warn(message);
            return new UnaryResponse(message);
        }
    }


   /**
     * Установить дату следующего повторения на завтра и фазу на DAY
     *
     * @param uuids
     */
    public List<ChunkEntity> dropRepetitionDay(List<UUID> uuids) {
        return chunkRepository.findAllById(uuids).stream()
                .map(ce -> {
                    ce.setNextRepeatDate(LocalDate.now().plusDays(1));
                    ce.setCurrentRepetitionInterval(DAY);
                    ce.setMutated(false);
                    return chunkRepository.save(ce);
                }).collect(Collectors.toList());
    }

    /**
     * Отметить чанк как не нужный для обновления
     * @param uuids
     * @return
     */
    public List<ChunkEntity> unmutate(List<UUID> uuids) {
        return chunkRepository.findAllById(uuids).stream()
                .map(ce -> {
                    ce.setMutated(false);
                    return chunkRepository.save(ce);
                }).collect(Collectors.toList());
    }

    /**
     *
     * @param dropRequired
     * @param ids
     * @return
     */
    public List<ChunkEntity> dropOrUnmutate(Boolean dropRequired, String ... ids) {
        List<UUID> uuids = Arrays.stream(ids)
                .map(UUID::fromString).collect(Collectors.toList());
        return dropRequired ? dropRepetitionDay(uuids) : unmutate(uuids);
    }


}
