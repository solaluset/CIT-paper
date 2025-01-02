package org.vinerdream;

import org.vinerdream.citPaper.converter.ResourcePackConverter;

public class CitCliMain {
    public static void main(String[] args) {
        new ResourcePackConverter(System.out::println).convertResourcePack("test", "out");
    }
}