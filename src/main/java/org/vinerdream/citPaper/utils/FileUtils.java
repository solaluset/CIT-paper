package org.vinerdream.citPaper.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class FileUtils {
    public static void copyDirectory(Path source, Path destination) throws IOException {
        try (Stream<Path> contents = Files.walk(source)) {
            contents.forEach(file -> {
                if (!file.toFile().isFile()) return;
                final Path destinationFilePath = destination.resolve(source.relativize(file));
                destinationFilePath.getParent().toFile().mkdirs();
                try {
                    Files.copy(file, destinationFilePath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public static void removeDirectory(Path directory) throws IOException {
        try (Stream<Path> contents = Files.walk(directory)) {
            contents.toList().reversed().forEach(file -> file.toFile().delete());
        }
    }
}
