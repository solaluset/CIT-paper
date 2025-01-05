package org.vinerdream.citPaper.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtils {
    public static void unzip(Path file, Path outputDirectory) throws IOException {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            zip.stream().forEach(entry -> {
                if (entry.isDirectory()) return;
                Path extractedPath = outputDirectory.resolve(entry.getName());
                extractedPath.getParent().toFile().mkdirs();
                try (FileOutputStream stream = new FileOutputStream(extractedPath.toFile())) {
                    zip.getInputStream(entry).transferTo(stream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public static void zip(Path directory, Path outputFile) throws IOException {
        outputFile.getParent().toFile().mkdirs();
        OutputStream stream = new FileOutputStream(outputFile.toFile());
        try (ZipOutputStream zip = new ZipOutputStream(stream)) {
            try (Stream<Path> files = Files.walk(directory)) {
                files.forEach(filePath -> {
                    File file = filePath.toFile();
                    if (file.isDirectory()) return;
                    try {
                        zip.putNextEntry(new ZipEntry(directory.relativize(filePath).toString()));
                        byte[] data = new byte[1024];
                        try (FileInputStream reader = new FileInputStream(file)) {
                            int read;
                            while ((read = reader.read(data, 0, 1024)) != -1) {
                                zip.write(data, 0, read);
                            }
                        }
                        zip.closeEntry();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        stream.close();
    }
}
