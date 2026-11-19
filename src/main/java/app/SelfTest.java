package app;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Verifiche headless (flag --selftest): curriculum da 100 fasi valido, shuffle diverso fra
 * due sessioni, tokenizer coerente, generazione PDF valida. Esito su stdout e in
 * %TEMP%\JavaQuest100\selftest.log; exit code 0 = tutto ok.
 */
public final class SelfTest {
    private final List<String> log = new ArrayList<>();
    private int failures;

    private SelfTest() {
    }

    public static int run() {
        SelfTest t = new SelfTest();
        try {
            t.execute();
        } catch (Throwable e) {
            t.check("eccezione inattesa: " + e, false);
            Main.logError("selftest", e);
        }
        t.log.add(t.failures == 0 ? "SELFTEST OK" : "SELFTEST FALLITO (" + t.failures + " errori)");
        String text = String.join(System.lineSeparator(), t.log) + System.lineSeparator();
        System.out.print(text);
        try {
            Files.writeString(ReportBuilder.tempDir().resolve("selftest.log"), text, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
        return t.failures == 0 ? 0 : 1;
    }

    private void check(String what, boolean ok) {
        log.add((ok ? "[OK]   " : "[FAIL] ") + what);
        if (!ok) failures++;
    }

    private void execute() throws Exception {
        log.add("Java Quest 100 selftest - Java " + System.getProperty("java.version"));
        Curriculum c = CurriculumLoader.loadDefault();
        check("fasi caricate: " + c.size() + " (attese 100)", c.size() == 100);
        boolean idsOk = true;
        for (int k = 0; k < c.size(); k++) idsOk &= c.phases().get(k).id() == k + 1;
        check("id consecutivi 1.." + c.size(), idsOk);
        List<String> bad = new ArrayList<>();
        for (Phase p : c.phases()) {
            if (p.title().isBlank() || p.lesson().isBlank() || p.exercise().lines().isEmpty()
                    || p.quiz().size() != 3 || p.quiz().stream().anyMatch(q -> q.options().size() != 4))
                bad.add(String.valueOf(p.id()));
        }
        check("struttura fasi completa (titolo, lezione, righe, 3 quiz x 4 opzioni)" + (bad.isEmpty() ? "" : " - errate: " + bad), bad.isEmpty());
        check("categorie: " + c.categories().size(), !c.categories().isEmpty());

        GameSession a = new GameSession(c, new Random());
        GameSession b = new GameSession(c, new Random());
        List<Integer> oa = a.levels().stream().map(l -> l.phase().id()).toList();
        List<Integer> ob = b.levels().stream().map(l -> l.phase().id()).toList();
        boolean fragsDiffer = false;
        for (int k = 0; k < a.levels().size() && !fragsDiffer; k++) {
            GameSession.Level la = a.levels().get(k);
            GameSession.Level lb = b.prepare(la.phase());
            fragsDiffer = !la.fragments().equals(lb.fragments()) || !la.quiz().equals(lb.quiz());
        }
        check("ordine livelli diverso fra due istanze", !oa.equals(ob));
        check("frammenti/quiz mescolati diversamente", fragsDiffer);
        check("ogni sessione contiene tutte le fasi", oa.stream().sorted().toList().equals(ob.stream().sorted().toList()));

        // le opzioni mescolate conservano la risposta corretta
        boolean answersKept = true;
        for (GameSession.Level l : a.levels()) {
            for (Phase.QuizQuestion q : l.quiz()) {
                Phase.QuizQuestion orig = l.phase().quiz().stream().filter(x -> x.q().equals(q.q())).findFirst().orElseThrow();
                answersKept &= q.correctOption().equals(orig.correctOption());
            }
        }
        check("risposta corretta preservata dopo lo shuffle delle opzioni", answersKept);

        // tokenizer: la concatenazione dei token ricostruisce il sorgente
        boolean tokOk = true;
        for (Phase p : c.phases()) {
            StringBuilder sb = new StringBuilder();
            for (SyntaxHighlighter.Token t : SyntaxHighlighter.tokenize(p.example())) sb.append(t.text());
            tokOk &= sb.toString().equals(p.example());
        }
        check("evidenziatore: token coerenti con gli esempi", tokOk);

        // simulazione partita: esercizio giusto, esercizio con errori, quiz
        GameSession.Level l1 = a.currentLevel();
        Phase p1 = l1.phase();
        List<GameSession.Fragment> right = new ArrayList<>();
        for (String s : p1.exercise().lines()) right.add(new GameSession.Fragment(-1, s, false));
        check("verifica esercizio corretto", GameSession.isCorrect(p1, right));
        if (right.size() > 1) {
            List<GameSession.Fragment> rev = new ArrayList<>(right);
            java.util.Collections.reverse(rev);
            boolean same = p1.exercise().lines().equals(p1.exercise().lines().reversed());
            check("verifica esercizio in ordine errato", same || !GameSession.isCorrect(p1, rev));
        }
        a.recordExercise(p1, true, 0, GameSession.joinFragments(right), false, true);
        for (Phase.QuizQuestion q : l1.quiz()) a.recordQuiz(p1, q, q.answer(), false);
        a.advance();
        GameSession.Level l2 = a.currentLevel();
        if (l2 != null) {
            boolean sc = a.isScoring(l2.phase(), GameSession.Kind.ESERCIZIO, null);
            a.exerciseWrongAttempt(sc);
            a.recordExercise(l2.phase(), true, 1, "riga sbagliata (\\ escape)", false, sc);
            for (Phase.QuizQuestion q : l2.quiz()) a.recordQuiz(l2.phase(), q, (q.answer() + 1) % 4, false);
        }
        int expected = 10 + 15 + (l2 != null ? 10 - 5 - 6 : 0);
        check("punteggio calcolato: " + a.score() + " (atteso " + expected + ")", a.score() == expected);

        Path pdf = ReportBuilder.writeToTemp(a);
        byte[] bytes = Files.readAllBytes(pdf);
        String err = PdfWriter.validate(bytes);
        check("PDF generato e valido (" + bytes.length + " byte): " + pdf + (err == null ? "" : " - " + err), err == null);

        PdfWriter big = new PdfWriter("Test paginazione àèéìòù € “citazioni” (parentesi) \\ backslash");
        for (int k = 0; k < 200; k++) big.paragraph("Riga " + k + " con caratteri speciali: àèìòù € (x) \\ — fine.", PdfWriter.Font.REGULAR, 11);
        String err2 = PdfWriter.validate(big.toBytes());
        check("PDF multipagina (" + big.pageCount() + " pagine) con escape/WinAnsi", err2 == null && big.pageCount() > 1);
    }
}
