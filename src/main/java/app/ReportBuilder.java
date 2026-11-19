package app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Costruisce il PDF di riepilogo della partita. */
public final class ReportBuilder {
    private static final float[] GREEN = {0.0f, 0.55f, 0.25f};
    private static final float[] RED = {0.75f, 0.1f, 0.1f};
    private static final float[] GREY = {0.35f, 0.35f, 0.35f};

    private ReportBuilder() {
    }

    /** Cartella temporanea dell'applicazione. */
    public static Path tempDir() throws IOException {
        Path dir = Path.of(System.getProperty("java.io.tmpdir"), "JavaQuest100");
        Files.createDirectories(dir);
        return dir;
    }

    /** Genera il PDF nella cartella temporanea e ne restituisce il percorso. */
    public static Path writeToTemp(GameSession s) throws IOException {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path file = tempDir().resolve("JavaQuest100_risultati_" + stamp + ".pdf");
        build(s).save(file);
        return file;
    }

    public static PdfWriter build(GameSession s) {
        PdfWriter pdf = new PdfWriter("Java Quest 100 - Riepilogo");
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        pdf.paragraph("Java Quest 100", PdfWriter.Font.BOLD, 24, 0, GREEN);
        pdf.paragraph("Riepilogo della partita", PdfWriter.Font.REGULAR, 14, 0, GREY);
        pdf.gap(6);
        pdf.paragraph("Inizio: " + s.startedAt().format(df) + "    Fine: " + LocalDateTime.now().format(df),
                PdfWriter.Font.REGULAR, 10, 0, GREY);
        pdf.rule(GREEN);

        long exOk = s.count(GameSession.Kind.ESERCIZIO, true), exKo = s.count(GameSession.Kind.ESERCIZIO, false);
        long qOk = s.count(GameSession.Kind.QUIZ, true), qKo = s.count(GameSession.Kind.QUIZ, false);
        int wrongAttempts = s.records().stream().filter(r -> r.kind() == GameSession.Kind.ESERCIZIO)
                .mapToInt(GameSession.AnswerRecord::wrongAttempts).sum();
        pdf.paragraph("Punteggio finale: " + s.score(), PdfWriter.Font.BOLD, 16);
        pdf.gap(4);
        pdf.paragraph("Livelli completati: " + s.completedLevels() + " / " + s.totalLevels(), PdfWriter.Font.REGULAR, 11);
        pdf.paragraph("Esercizi giusti al primo tentativo: " + exOk + "    con errori o non risolti: " + exKo
                + "    tentativi errati totali: " + wrongAttempts, PdfWriter.Font.REGULAR, 11);
        pdf.paragraph("Domande quiz giuste: " + qOk + "    sbagliate: " + qKo, PdfWriter.Font.REGULAR, 11);
        pdf.paragraph("Regole: esercizio +" + GameSession.EXERCISE_OK + " (" + GameSession.EXERCISE_WRONG
                + " per ogni tentativo errato), quiz +" + GameSession.QUIZ_OK + " / " + GameSession.QUIZ_WRONG
                + " per domanda.", PdfWriter.Font.ITALIC, 9, 0, GREY);

        List<GameSession.AnswerRecord> all = new ArrayList<>(s.records());
        all.sort(Comparator.comparingInt(GameSession.AnswerRecord::phaseId)
                .thenComparing(GameSession.AnswerRecord::kind));
        List<GameSession.AnswerRecord> ok = all.stream().filter(GameSession.AnswerRecord::correct).toList();
        List<GameSession.AnswerRecord> ko = all.stream().filter(r -> !r.correct()).toList();

        pdf.gap(10);
        pdf.paragraph("Risposte sbagliate (" + ko.size() + ")", PdfWriter.Font.BOLD, 15, 0, RED);
        pdf.rule(RED);
        if (ko.isEmpty()) pdf.paragraph("Nessuna risposta sbagliata.", PdfWriter.Font.ITALIC, 10);
        for (GameSession.AnswerRecord r : ko) entry(pdf, r, false);

        pdf.gap(10);
        pdf.paragraph("Risposte giuste (" + ok.size() + ")", PdfWriter.Font.BOLD, 15, 0, GREEN);
        pdf.rule(GREEN);
        if (ok.isEmpty()) pdf.paragraph("Nessuna risposta giusta registrata.", PdfWriter.Font.ITALIC, 10);
        for (GameSession.AnswerRecord r : ok) entry(pdf, r, true);
        return pdf;
    }

    private static void entry(PdfWriter pdf, GameSession.AnswerRecord r, boolean correct) {
        boolean ex = r.kind() == GameSession.Kind.ESERCIZIO;
        String head = "Fase " + r.phaseId() + " - " + r.phaseTitle() + "  [" + (ex ? "Esercizio" : "Quiz")
                + (r.freeMode() ? ", modalità libera" : "") + "]  punti: " + (r.points() > 0 ? "+" : "") + r.points();
        pdf.gap(4);
        pdf.paragraph(head, PdfWriter.Font.BOLD, 10.5f, 0, correct ? GREEN : RED);
        pdf.paragraph(r.category(), PdfWriter.Font.ITALIC, 8.5f, 0, GREY);
        pdf.paragraph((ex ? "Consegna: " : "Domanda: ") + r.question().replace("`", ""), PdfWriter.Font.REGULAR, 10, 8, null);
        if (ex) {
            if (correct) {
                pdf.paragraph("Codice composto:", PdfWriter.Font.REGULAR, 9.5f, 8, GREY);
                pdf.code(r.correctAnswer(), 8.5f, 8);
            } else {
                String status = r.wrongAttempts() > 0 ? "Tentativi errati: " + r.wrongAttempts() : "Non risolto";
                pdf.paragraph(status + ". Tua risposta (ultimo tentativo errato):", PdfWriter.Font.REGULAR, 9.5f, 8, RED);
                pdf.code(r.userAnswer(), 8.5f, 8);
                pdf.paragraph("Risposta corretta:", PdfWriter.Font.REGULAR, 9.5f, 8, GREEN);
                pdf.code(r.correctAnswer(), 8.5f, 8);
            }
        } else {
            if (correct) {
                pdf.paragraph("Risposta: " + r.correctAnswer(), PdfWriter.Font.REGULAR, 10, 8, GREEN);
            } else {
                pdf.paragraph("Tua risposta: " + r.userAnswer(), PdfWriter.Font.REGULAR, 10, 8, RED);
                pdf.paragraph("Risposta corretta: " + r.correctAnswer(), PdfWriter.Font.BOLD, 10, 8, GREEN);
            }
            if (!r.explain().isBlank()) pdf.paragraph(r.explain(), PdfWriter.Font.ITALIC, 9, 8, GREY);
        }
    }
}
