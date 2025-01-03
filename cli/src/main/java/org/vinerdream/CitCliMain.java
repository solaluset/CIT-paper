package org.vinerdream;

import org.vinerdream.citPaper.converter.ResourcePackConverter;

import java.io.IOException;
import java.nio.file.Paths;

public class CitCliMain {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: <resource pack path> <output path>");
            return;
        }
        ResourcePackConverter converter = new ResourcePackConverter(System.out::println);
        converter.convertResourcePack(args[0], args[1]);
        converter.saveConfiguration(Paths.get("items.yml"));
    }
}