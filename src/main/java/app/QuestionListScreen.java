package app;

import java.util.List;
import java.util.Map;

import javafx.geometry.Pos;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/** Elenco di tutte le domande raggruppate per categoria (accordion); clic = modalità libera. */
final class QuestionListScreen extends BorderPane {

    QuestionListScreen(App app) {
        VBox v = Ui.screen();
        v.getStyleClass().add("question-list");
        v.getChildren().add(Ui.header("Elenco domande",
                app.curriculum().size() + " esercizi in " + app.curriculum().categories().size() + " categorie",
                "Apri una categoria e clicca un esercizio per svolgerlo in modalità libera. "
                        + "Il risultato viene registrato e finisce nel PDF; ogni esercizio conta per il punteggio una sola volta. "
                        + "✓ indica gli esercizi già svolti in questa partita."));

        Accordion acc = new Accordion();
        acc.getStyleClass().add("category-accordion");
        Map<String, List<Phase>> groups = app.curriculum().byCategory();
        for (Map.Entry<String, List<Phase>> g : groups.entrySet()) {
            VBox items = new VBox(6);
            items.getStyleClass().add("category-items");
            int done = 0;
            for (Phase p : g.getValue()) {
                boolean has = app.session().hasExerciseRecord(p.id());
                if (has) done++;
                Button b = Ui.btn((has ? "✓  " : "") + "Fase " + p.id() + " · " + p.title(), "list-item",
                        "palette-" + p.paletteIndex());
                if (has) b.getStyleClass().add("done");
                b.setMaxWidth(Double.MAX_VALUE);
                b.setAlignment(Pos.CENTER_LEFT);
                b.setOnAction(e -> app.openFree(p));
                items.getChildren().add(b);
            }
            TitledPane tp = new TitledPane(g.getKey() + "   (" + done + "/" + g.getValue().size() + ")", items);
            tp.getStyleClass().addAll("category-pane", "palette-" + g.getValue().get(0).paletteIndex());
            acc.getPanes().add(tp);
        }
        if (!acc.getPanes().isEmpty()) acc.setExpandedPane(acc.getPanes().get(0));
        v.getChildren().add(acc);
        setCenter(Ui.scroll(v));
    }
}
