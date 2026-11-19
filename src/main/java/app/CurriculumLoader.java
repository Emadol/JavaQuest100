package app;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Carica /data/curriculum.json (array di fasi) e lo converte nel modello. */
public final class CurriculumLoader {
    public static final String RESOURCE = "/data/curriculum.json";

    private CurriculumLoader() {
    }

    public static Curriculum loadDefault() {
        try (InputStream in = CurriculumLoader.class.getResourceAsStream(RESOURCE)) {
            if (in == null) throw new IllegalStateException("Risorsa mancante: " + RESOURCE);
            return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Impossibile leggere " + RESOURCE, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Curriculum parse(String json) {
        Object root = Json.parse(json);
        if (root instanceof Map<?, ?> m && m.get("phases") instanceof List<?> l) root = l;
        if (!(root instanceof List<?> arr)) throw new IllegalArgumentException("Il curriculum deve essere un array di fasi");

        List<Map<String, Object>> raw = new ArrayList<>();
        for (Object o : arr) {
            if (o instanceof Map<?, ?> mm) raw.add((Map<String, Object>) mm);
        }
        raw.sort(Comparator.comparingInt(m -> intOf(m.get("id"), 0)));

        List<String> categories = new ArrayList<>();
        List<Phase> phases = new ArrayList<>();
        for (Map<String, Object> m : raw) {
            String cat = text(m.get("category"));
            if (cat.isBlank()) cat = "Generale";
            if (!categories.contains(cat)) categories.add(cat);
            int palette = categories.indexOf(cat) % 10;

            Map<String, Object> ex = m.get("exercise") instanceof Map<?, ?> e ? (Map<String, Object>) e : Map.of();
            Phase.Exercise exercise = new Phase.Exercise(text(ex.get("prompt")),
                    strings(ex.get("lines")), strings(ex.get("distractors")));

            List<Phase.QuizQuestion> quiz = new ArrayList<>();
            if (m.get("quiz") instanceof List<?> ql) {
                for (Object qo : ql) {
                    if (!(qo instanceof Map<?, ?> q)) continue;
                    List<String> options = strings(q.get("options"));
                    quiz.add(new Phase.QuizQuestion(text(q.get("q")), options,
                            answerIndex(q.get("answer"), options), text(q.get("explain"))));
                }
            }
            phases.add(new Phase(intOf(m.get("id"), phases.size() + 1), cat, palette, text(m.get("title")),
                    text(m.get("lesson")), text(m.get("example")), text(m.get("help")), exercise, List.copyOf(quiz)));
        }
        return new Curriculum(List.copyOf(phases), List.copyOf(categories));
    }

    /** Accetta indice numerico, lettera (A-D) o testo dell'opzione. */
    static int answerIndex(Object a, List<String> options) {
        int idx = -1;
        if (a instanceof Number n) idx = n.intValue();
        else if (a instanceof String s) {
            String t = s.trim();
            idx = options.indexOf(t);
            if (idx < 0 && t.length() == 1 && Character.isLetter(t.charAt(0))) idx = Character.toUpperCase(t.charAt(0)) - 'A';
            if (idx < 0 && t.matches("\\d+")) idx = Integer.parseInt(t);
        }
        if (idx < 0 || idx >= options.size()) idx = 0;
        return idx;
    }

    static int intOf(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    /** Stringa o array di stringhe (unite con a capo). */
    static String text(Object o) {
        if (o == null) return "";
        if (o instanceof List<?> l) {
            List<String> parts = new ArrayList<>();
            for (Object x : l) parts.add(String.valueOf(x));
            return String.join("\n", parts);
        }
        if (o instanceof Double d && d == Math.rint(d)) return String.valueOf(d.longValue());
        return String.valueOf(o);
    }

    static List<String> strings(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List<?> l) for (Object x : l) out.add(text(x));
        else if (o instanceof String s && !s.isEmpty()) out.addAll(List.of(s.split("\n")));
        return List.copyOf(out);
    }
}
