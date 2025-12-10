package org.vinerdream;

import org.vinerdream.citPaper.config.MainConfig;
import org.vinerdream.citPaper.converter.ConversionHelper;

import java.io.File;
import java.nio.file.Path;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class CitCliMain {
    private static final Logger LOGGER = createLogger();

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: <config path>");
            System.exit(1);
            return;
        }

        final var result = ConversionHelper.runConversion(
                MainConfig.fromFile(new File(args[0])),
                LOGGER,
                Path.of("renames"),
                Path.of("oraxen")
        );
        System.exit(result.getValue().isEmpty() ? 0 : 1);
    }

    private static Logger createLogger() {
        if (LOGGER != null) {
            throw new IllegalStateException("Logger already present");
        }

        final var logger = Logger.getLogger("CIT-paper");
        final var handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter() {
            private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s%n";

            @Override
            public synchronized String format(LogRecord record) {
                return String.format(
                        format,
                        new Date(record.getMillis()),
                        record.getLevel().getLocalizedName(),
                        record.getMessage()
                );
            }
        });
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);

        return logger;
    }
}
