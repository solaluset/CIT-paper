package org.vinerdream.citPaper.utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import static org.vinerdream.citPaper.utils.CollectionUtils.iterateStream;

public class FileUtils {
    private static final Set<String> FILENAME_BLACKLIST = Set.of("desktop.ini");

    public static boolean isBlacklisted(Path path) {
        return FILENAME_BLACKLIST.contains(path.getFileName().toString().toLowerCase(Locale.ROOT));
    }

    public static void copyDirectory(Path source, Path destination) throws IOException {
        try (Stream<Path> contents = Files.walk(source)) {
            for (Path file : iterateStream(contents)) {
                if (!file.toFile().isFile() || isBlacklisted(file)) continue;
                final Path destinationFilePath = destination.resolve(source.relativize(file));
                destinationFilePath.getParent().toFile().mkdirs();
                Files.copy(file, destinationFilePath);
            }
        }
    }

    public static void removeDirectory(Path directory) throws IOException {
        if (!directory.toFile().exists()) return;
        Files.walkFileTree(directory, new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                Files.delete(path);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
                throw e;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
                if (e == null) {
                    Files.delete(path);
                    return FileVisitResult.CONTINUE;
                } else {
                    throw e;
                }
            }
        });
    }
}
