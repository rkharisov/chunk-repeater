package com.github.rkharisov.chunks.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Ослеживает создание, изменение, удаление чанков в целевой директории
 *
 * - При создании файла приходят события о создании и изменении
 * - При создании каталога приходит событие только о создании
 * - При переименовании каталога/файла приходят события о удалении  и создании
 * - При удалении каталога/файла приходит событие об удалении, понять файл или каталог это был или файл нет возможности,
 * поскольку объекта уже нет.
 * - При удалении каталога со вложенными файлами/директориями, то нет уведомления об их удалении
 * - События изменения приходят только для файлов. Для каталогов нет
 */
@Service
public class MapDirWatcher {

    private final static Logger log = LoggerFactory.getLogger(MapDirWatcher.class);
    public static final String MAP_SUFFIX = ".xmind";


    @Value("${webdav.mapDir}")
    private Path mapDir;

    @Autowired
    private ChunkService chunkService;

    private HashMap<WatchKey, Path> keys;
    private WatchService watchService;

    @PostConstruct
    private void runWatcher() {
        keys = new HashMap<>();
        new Thread(initWatcher()).start();
    }

    private Runnable initWatcher() {
        return () -> {
            try {
                watchService = FileSystems.getDefault().newWatchService();
                addExistedFiles(mapDir);
                subscribe(mapDir);
            } catch (IOException | InterruptedException e) {
                log.error(e.getMessage());
            }
        };
    }

    /**
     * Рекурсивно добавить все файлы в папке с картами
     *
     * @param path
     * @throws IOException
     */
    private void addExistedFiles(Path path) throws IOException {
        Files.walk(path, FileVisitOption.FOLLOW_LINKS)
                .filter(this::isValidEntry)
                .forEach(this::onEntryCreate);
    }


    /**
     * Подписка на события изменения состояния папки
     *
     * @param path
     * @throws IOException
     * @throws InterruptedException
     */
    private void subscribe(Path path) throws IOException, InterruptedException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
        WatchKey key;
        while ((key = watchService.take()) != null) {
            try {
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind kind = event.kind();
                    Path eventEntry = keys.get(key).resolve((Path) event.context());
                    process(kind, eventEntry);
                }
                key.reset();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        log.info("Завершаю наблюдение за директорией {}", mapDir);
    }

    /**
     * Зарегистрировать наблюдателя
     * @param path
     * @return
     * @throws IOException
     */
    private WatchKey register(Path path) throws IOException {
        WatchKey key = path.register(watchService, ENTRY_CREATE, ENTRY_DELETE);
        keys.put(key, path);
        return key;
    }


    private void process(WatchEvent.Kind kind, Path eventEntry) {
        if (kind.equals(ENTRY_CREATE)) {
            if (!isValidEntry(eventEntry)) return;
            onEntryCreate(eventEntry);
        } else if (kind.equals(ENTRY_DELETE)) {
            onEntryDelete(eventEntry);
        } else if (kind.equals(OVERFLOW)) {
            onOverflow(eventEntry);
        }
    }

    private boolean isValidEntry(Path eventEntry) {
        File file = eventEntry.toFile();
        return file.isDirectory() || file.getName().endsWith(MAP_SUFFIX);
    }

    /**
     * Реакция на создание объекта в папке
     * Здесь если новый файл, если переименовали старый файл или если изменили файл
     * @param eventEntry
     */
    private void onEntryCreate(Path eventEntry) {
        try {
            if (eventEntry.toFile().isDirectory()) {
                register(eventEntry);
                File[] files = eventEntry.toFile().listFiles();
                for (File file : files) {
                    onEntryCreate(file.toPath());
                }
                log.debug("Отслеживаю каталог {}", eventEntry);
            } else {
                chunkService.createOrUpdate(eventEntry);
                log.debug("Добавлен файл {}", eventEntry);
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Реакция на удаление объекта в папке
     * Здесь если объект удален или переименован
     * @param eventEntry
     */
    private void onEntryDelete(Path eventEntry) {
        ArrayList<WatchKey> toRemove = new ArrayList<>();
        keys.entrySet().stream()
                .filter(e -> e.getValue().startsWith(eventEntry))
                .forEach(e -> {
                    e.getKey().cancel();
                    toRemove.add(e.getKey());
                    log.debug("Отменяю отслеживание каталога {}", e.getValue());
                });
        toRemove.forEach(keys::remove);
        chunkService.markInactive(eventEntry);
        log.debug("Удален файл или каталог{}", eventEntry);
    }

    /**
     * Реакция на переполнение буфера событий(события постаявляются быстрее чем приложение успевает их обработать)
     * @param path
     */
    private void onOverflow(Path path) {
        log.error("Получено событие переполнения {}", path);
    }

}
