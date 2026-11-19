package app;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Insieme ordinato delle fasi, con le categorie nell'ordine in cui compaiono. */
public record Curriculum(List<Phase> phases, List<String> categories) {

    public Map<String, List<Phase>> byCategory() {
        Map<String, List<Phase>> m = new LinkedHashMap<>();
        for (String c : categories) m.put(c, new ArrayList<>());
        for (Phase p : phases) m.computeIfAbsent(p.category(), k -> new ArrayList<>()).add(p);
        return m;
    }

    public Phase byId(int id) {
        for (Phase p : phases) if (p.id() == id) return p;
        return null;
    }

    public int size() {
        return phases.size();
    }
}
