package org.vinerdream.citPaper.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipFile;

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
}
