package com.github.rkharisov.chunks.repository;

import com.github.rkharisov.chunks.model.entity.ChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
public interface ChunkRepository extends JpaRepository<ChunkEntity, UUID> {

    Optional<ChunkEntity> findFirstByPathOrHash(String path, byte[] hash);

    Stream<ChunkEntity> findByPathStartsWith(String path_);

    Stream<ChunkEntity> findAllByActiveIsTrueAndNextRepeatDateIsLessThanEqual(LocalDate date);

    Stream<ChunkEntity> findAllByActiveIsTrueAndMutatedIsTrue();
}
