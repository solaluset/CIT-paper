package org.vinerdream;

import org.vinerdream.citPaper.converter.ResourcePackConverter;

import java.io.IOException;
import java.nio.file.Paths;

public class CitCliMain {
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.out.println("Usage: <resource pack path> <output path> <config output path>");
            return;
        }
        ResourcePackConverter converter = new ResourcePackConverter(
                System.out::println,
                Paths.get(System.getProperty("java.io.tmpdir"), "cit-paper")
        );
        converter.convertResourcePack(Paths.get(args[0]), Paths.get(args[1]));
        converter.saveConfiguration(Paths.get(args[2]));
    }
}