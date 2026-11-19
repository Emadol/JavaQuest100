package app;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Punto di ingresso. Non estende Application così funziona anche con JavaFX caricato
 * da classpath/runtime jlink. Con --selftest esegue le verifiche headless ed esce.
 */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> logError("Eccezione non gestita nel thread " + t.getName(), e));
        if (Arrays.asList(args).contains("--selftest")) {
            System.exit(SelfTest.run());
        }
        javafx.application.Application.launch(App.class, args);
    }

    /** Scrive l'errore su stderr e in %TEMP%\JavaQuest100\error.log. */
    public static void logError(String msg, Throwable e) {
        StringWriter sw = new StringWriter();
        if (e != null) e.printStackTrace(new PrintWriter(sw));
        String text = LocalDateTime.now() + " " + msg + System.lineSeparator() + sw + System.lineSeparator();
        System.err.print(text);
        try {
            Files.writeString(ReportBuilder.tempDir().resolve("error.log"), text, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }
}
