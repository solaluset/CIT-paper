package org.vinerdream;

import org.vinerdream.citPaper.config.MainConfig;
import org.vinerdream.citPaper.converter.ConversionHelper;

import java.io.File;
import java.nio.file.Path;
import java.util.logging.Logger;

public class CitCliMain {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: <config path>");
            System.exit(1);
            return;
        }
        final var result = ConversionHelper.runConversion(
                MainConfig.fromFile(new File(args[0])),
                Logger.getLogger("CIT-paper"),
                Path.of("renames"),
                Path.of("oraxen")
        );
        System.exit(result.getValue().isEmpty() ? 0 : 1);
    }
}
