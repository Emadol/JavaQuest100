package app;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Stato di una partita: ordine randomico dei livelli, punteggio e registro delle risposte.
 * Nessuna persistenza: ogni avvio crea una nuova sessione con new Random().
 */
public final class GameSession {
    public static final int EXERCISE_OK = 10;
    public static final int EXERCISE_WRONG = -5;
    public static final int QUIZ_OK = 5;
    public static final int QUIZ_WRONG = -2;

    /** Frammento di codice mostrato nell'esercizio (uid distingue righe identiche). */
    public record Fragment(int uid, String text, boolean distractor) {
    }

    /** Livello preparato: fase + frammenti mescolati + quiz con domande/opzioni mescolate. */
    public record Level(Phase phase, List<Fragment> fragments, List<Phase.QuizQuestion> quiz) {
    }

    public enum Kind { ESERCIZIO, QUIZ }

    public enum Step { LESSON, EXERCISE, QUIZ }

    /** Una risposta registrata (una per esercizio / domanda di quiz). */
    public record AnswerRecord(int phaseId, String phaseTitle, String category, Kind kind, String question,
                               String userAnswer, String correctAnswer, String explain, boolean correct,
                               int points, int wrongAttempts, boolean freeMode, LocalDateTime time) {
    }

    private final Curriculum curriculum;
    private final Random rnd;
    private final List<Level> levels;
    private final List<AnswerRecord> records = new ArrayList<>();
    private final Set<String> scored = new HashSet<>();
    private final LocalDateTime startedAt = LocalDateTime.now();
    private int index;
    private int score;
    private boolean finished;
    private Step step = Step.LESSON;
    private int quizPos;

    public GameSession(Curriculum curriculum, Random rnd) {
        this.curriculum = curriculum;
        this.rnd = rnd;
        List<Phase> order = new ArrayList<>(curriculum.phases());
        Collections.shuffle(order, rnd);
        List<Level> l = new ArrayList<>();
        for (Phase p : order) l.add(prepare(p));
        this.levels = List.copyOf(l);
    }

    /** Prepara un livello con frammenti e quiz mescolati (usato anche in modalità libera). */
    public Level prepare(Phase p) {
        List<Fragment> frags = new ArrayList<>();
        int uid = 0;
        for (String s : p.exercise().lines()) frags.add(new Fragment(uid++, s, false));
        for (String s : p.exercise().distractors()) frags.add(new Fragment(uid++, s, true));
        Collections.shuffle(frags, rnd);

        List<Phase.QuizQuestion> quiz = new ArrayList<>();
        for (Phase.QuizQuestion q : p.quiz()) {
            List<Integer> idx = new ArrayList<>();
            for (int k = 0; k < q.options().size(); k++) idx.add(k);
            Collections.shuffle(idx, rnd);
            List<String> opts = new ArrayList<>();
            for (int k : idx) opts.add(q.options().get(k));
            quiz.add(new Phase.QuizQuestion(q.q(), List.copyOf(opts), Math.max(0, idx.indexOf(q.answer())), q.explain()));
        }
        Collections.shuffle(quiz, rnd);
        return new Level(p, List.copyOf(frags), List.copyOf(quiz));
    }

    // ---------- verifica ----------

    static String norm(String s) {
        return s.strip().replaceAll("\\s+", " ");
    }

    public static boolean isCorrect(Phase p, List<Fragment> answer) {
        List<String> exp = p.exercise().lines();
        if (answer.size() != exp.size()) return false;
        for (int k = 0; k < exp.size(); k++) {
            if (!norm(exp.get(k)).equals(norm(answer.get(k).text()))) return false;
        }
        return true;
    }

    public static String joinFragments(List<Fragment> answer) {
        List<String> l = new ArrayList<>();
        for (Fragment f : answer) l.add(f.text());
        return l.isEmpty() ? "(nessuna riga)" : String.join("\n", l);
    }

    // ---------- punteggio e registro ----------

    /** Un esercizio/domanda conta per il punteggio una sola volta per sessione. */
    public boolean isScoring(Phase p, Kind kind, String question) {
        return !scored.contains(key(p, kind, question));
    }

    private static String key(Phase p, Kind kind, String question) {
        return p.id() + "|" + kind + "|" + (kind == Kind.QUIZ ? question : "");
    }

    /** Tentativo errato all'esercizio: -5 subito (se conteggiato). */
    public int exerciseWrongAttempt(boolean scoring) {
        if (scoring) score += EXERCISE_WRONG;
        return scoring ? EXERCISE_WRONG : 0;
    }

    /** Chiude un esercizio (risolto o rinuncia) e lo registra. */
    public AnswerRecord recordExercise(Phase p, boolean solved, int wrongAttempts, String userAnswer,
                                       boolean freeMode, boolean scoring) {
        int pts = 0;
        if (scoring) {
            if (solved) score += EXERCISE_OK;
            pts = (solved ? EXERCISE_OK : 0) + wrongAttempts * EXERCISE_WRONG;
            scored.add(key(p, Kind.ESERCIZIO, null));
        }
        AnswerRecord r = new AnswerRecord(p.id(), p.title(), p.category(), Kind.ESERCIZIO, p.exercise().prompt(),
                userAnswer, p.exercise().solution(), "", solved && wrongAttempts == 0, pts, wrongAttempts,
                freeMode, LocalDateTime.now());
        records.add(r);
        return r;
    }

    public AnswerRecord recordQuiz(Phase p, Phase.QuizQuestion q, int chosen, boolean freeMode) {
        boolean ok = chosen == q.answer();
        boolean scoring = isScoring(p, Kind.QUIZ, q.q());
        int pts = 0;
        if (scoring) {
            pts = ok ? QUIZ_OK : QUIZ_WRONG;
            score += pts;
            scored.add(key(p, Kind.QUIZ, q.q()));
        }
        String user = chosen >= 0 && chosen < q.options().size() ? q.options().get(chosen) : "(nessuna)";
        AnswerRecord r = new AnswerRecord(p.id(), p.title(), p.category(), Kind.QUIZ, q.q(), user,
                q.correctOption(), q.explain(), ok, pts, ok ? 0 : 1, freeMode, LocalDateTime.now());
        records.add(r);
        return r;
    }

    public boolean hasExerciseRecord(int phaseId) {
        for (AnswerRecord r : records) if (r.phaseId() == phaseId && r.kind() == Kind.ESERCIZIO) return true;
        return false;
    }

    // ---------- avanzamento ----------

    public Level currentLevel() {
        return finished || index >= levels.size() ? null : levels.get(index);
    }

    /** Passa al livello successivo; true se la partita è terminata. */
    public boolean advance() {
        index++;
        step = Step.LESSON;
        quizPos = 0;
        if (index >= levels.size()) finished = true;
        return finished;
    }

    public void finish() {
        finished = true;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isStarted() {
        return index > 0 || step != Step.LESSON || !records.isEmpty();
    }

    public Step step() {
        return step;
    }

    public void setStep(Step s) {
        step = s;
    }

    public int quizPos() {
        return quizPos;
    }

    public void setQuizPos(int p) {
        quizPos = p;
    }

    /** Numero del livello corrente (1-based, limitato al totale). */
    public int levelNumber() {
        return Math.min(index + 1, levels.size());
    }

    public int completedLevels() {
        return Math.min(index, levels.size());
    }

    public int totalLevels() {
        return levels.size();
    }

    public int score() {
        return score;
    }

    public List<Level> levels() {
        return levels;
    }

    public List<AnswerRecord> records() {
        return Collections.unmodifiableList(records);
    }

    public Curriculum curriculum() {
        return curriculum;
    }

    public LocalDateTime startedAt() {
        return startedAt;
    }

    public long count(Kind kind, boolean correct) {
        return records.stream().filter(r -> r.kind() == kind && r.correct() == correct).count();
    }
}
