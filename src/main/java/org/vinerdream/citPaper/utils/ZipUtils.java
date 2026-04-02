package org.vinerdream.citPaper.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.vinerdream.citPaper.utils.CollectionUtils.*;
import static org.vinerdream.citPaper.utils.FileUtils.isBlacklisted;

public class ZipUtils {

    public static void unzip(Path file, Path outputDirectory) throws IOException {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            for (ZipEntry entry : iterateStream(zip.stream())) {
                if (entry.isDirectory()) continue;
                Path extractedPath = outputDirectory.resolve(stringToPath(entry.getName()));
                if (isBlacklisted(extractedPath)) {
                    continue;
                }
                Files.createDirectories(extractedPath.getParent());
                try (FileOutputStream stream = new FileOutputStream(extractedPath.toFile())) {
                    zip.getInputStream(entry).transferTo(stream);
                }
            }
        }
    }

    public static void zip(Path directory, Path outputFile) throws IOException {
        if (outputFile.getParent() != null) {
            Files.createDirectories(outputFile.getParent());
        }
        try (OutputStream stream = new FileOutputStream(outputFile.toFile())) {
            try (ZipOutputStream zip = new ZipOutputStream(stream)) {
                try (Stream<Path> files = Files.walk(directory)) {
                    for (Path filePath : iterateStream(files)) {
                        File file = filePath.toFile();
                        if (file.isDirectory() || isBlacklisted(filePath)) {
                            continue;
                        }
                        zip.putNextEntry(new ZipEntry(pathToString(directory.relativize(filePath))));
                        byte[] data = new byte[1024];
                        try (FileInputStream reader = new FileInputStream(file)) {
                            int read;
                            while ((read = reader.read(data, 0, 1024)) != -1) {
                                zip.write(data, 0, read);
                            }
                        }
                        zip.closeEntry();
                    }
                }
            }
        }
    }
}
