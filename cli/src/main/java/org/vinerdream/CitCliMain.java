package org.vinerdream;

import org.vinerdream.citPaper.converter.ResourcePackConverter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

public class CitCliMain {
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.out.println("Usage: <resource pack path> <output path> <config output path>");
            return;
        }
        ResourcePackConverter converter = new ResourcePackConverter(
                Path.of(args[0]),
                Path.of(args[1]),
                Path.of(System.getProperty("java.io.tmpdir"), "cit-paper"),
                true,
                Logger.getLogger("CIT-paper")
        );
        converter.convertResourcePack();
        converter.saveConfiguration(Path.of(args[2]));
    }
}