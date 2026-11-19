package app;

import java.util.List;

/** Una fase del curriculum (= un livello del gioco). */
public record Phase(int id, String category, int paletteIndex, String title, String lesson,
                    String example, String help, Exercise exercise, List<QuizQuestion> quiz) {

    /** Esercizio "componi il codice": righe corrette in ordine + distrattori. */
    public record Exercise(String prompt, List<String> lines, List<String> distractors) {
        public String solution() {
            return String.join("\n", lines);
        }
    }

    /** Domanda a scelta multipla; answer = indice dell'opzione corretta. */
    public record QuizQuestion(String q, List<String> options, int answer, String explain) {
        public String correctOption() {
            return options.get(answer);
        }
    }
}
