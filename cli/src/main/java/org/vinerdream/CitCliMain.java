package org.vinerdream;

import org.vinerdream.citPaper.converter.ResourcePackConverter;

public class CitCliMain {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: <resource pack path> <output path>");
            return;
        }
        new ResourcePackConverter(System.out::println).convertResourcePack(args[0], args[1]);
    }
}