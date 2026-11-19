package app;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Schermata Lezione: testo + esempio di codice colorato. */
final class LessonScreen extends BorderPane {

    LessonScreen(App app, GameSession.Level level) {
        Phase p = level.phase();
        VBox v = Ui.screen();
        v.getStyleClass().addAll("lesson", "palette-" + p.paletteIndex());
        v.getChildren().add(Ui.header("Fase " + p.id() + " · " + p.title(), "Lezione · " + p.category(),
                (p.help().isBlank() ? "" : p.help() + "\n\n")
                        + "Leggi la lezione e studia l'esempio: nel prossimo passo dovrai comporre un codice simile."));

        VBox lesson = Ui.card("lesson-card");
        lesson.getChildren().addAll(Ui.label("Lezione", "card-title"), Ui.richText(p.lesson()));
        v.getChildren().add(lesson);

        if (!p.example().isBlank()) {
            VBox ex = Ui.card("example-card");
            ex.getChildren().addAll(Ui.label("Esempio", "card-title"), Ui.codeBlock(p.example(), p.paletteIndex()));
            v.getChildren().add(ex);
        }

        Button next = Ui.btn("Vai all'esercizio  →", "btn-primary", "btn-big");
        next.setOnAction(e -> app.goExercise(level));
        HBox actions = new HBox(12, next);
        actions.setAlignment(Pos.CENTER_RIGHT);
        v.getChildren().add(actions);
        setCenter(Ui.scroll(v));
    }
}
