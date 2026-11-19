package app;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;

/** Esercizio "componi il codice": frammenti mescolati + distrattori, riordino con su/giù. */
final class ExerciseScreen extends BorderPane {
    private final App app;
    private final GameSession.Level level;
    private final Phase phase;
    private final boolean free;
    private final boolean scoring;
    private final List<GameSession.Fragment> pool = new ArrayList<>();
    private final List<GameSession.Fragment> chosen = new ArrayList<>();
    private final VBox poolBox = new VBox(6);
    private final VBox answerBox = new VBox(6);
    private final Label feedback = Ui.label("", "feedback");
    private final VBox solutionBox = new VBox(8);
    private final FlowPane actions = new FlowPane(10, 10);
    private int wrongAttempts;
    private String lastWrong;
    private boolean closed;

    ExerciseScreen(App app, GameSession.Level level, boolean free) {
        this.app = app;
        this.level = level;
        this.phase = level.phase();
        this.free = free;
        this.scoring = app.session().isScoring(phase, GameSession.Kind.ESERCIZIO, null);
        pool.addAll(level.fragments());

        VBox v = Ui.screen();
        v.getStyleClass().addAll("exercise", "palette-" + phase.paletteIndex());
        String sub = (free ? "Modalità libera · " : "Esercizio · ") + phase.category()
                + (scoring ? "" : " · già svolto: non conta per il punteggio");
        v.getChildren().add(Ui.header("Fase " + phase.id() + " · " + phase.title(), sub,
                (phase.help().isBlank() ? "" : phase.help() + "\n\n")
                        + "Clicca un frammento a sinistra per aggiungerlo in fondo al tuo codice. "
                        + "Usa ▲ ▼ per spostare una riga e ✕ per rimetterla fra i frammenti. "
                        + "Attenzione: alcuni frammenti sono distrattori e non vanno usati. "
                        + "Verifica: +10 se corretto, −5 per ogni tentativo errato."));

        VBox prompt = Ui.card("prompt-card");
        prompt.getChildren().addAll(Ui.label("Consegna", "card-title"), Ui.richText(phase.exercise().prompt()));
        v.getChildren().add(prompt);

        VBox left = Ui.card("pool-card");
        poolBox.getStyleClass().add("fragment-list");
        left.getChildren().addAll(Ui.label("Frammenti disponibili", "card-title"), poolBox);
        VBox right = Ui.card("answer-card");
        answerBox.getStyleClass().addAll("answer-list", "code-area", "palette-" + phase.paletteIndex());
        right.getChildren().addAll(Ui.label("Il tuo codice", "card-title"), answerBox);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(45);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(55);
        grid.getColumnConstraints().addAll(c1, c2);
        grid.add(left, 0, 0);
        grid.add(right, 1, 0);
        GridPane.setVgrow(left, Priority.ALWAYS);
        v.getChildren().add(grid);

        solutionBox.setVisible(false);
        solutionBox.setManaged(false);
        actions.setAlignment(Pos.CENTER_LEFT);
        v.getChildren().addAll(feedback, solutionBox, actions);
        render();
        setCenter(Ui.scroll(v));
    }

    private void render() {
        poolBox.getChildren().clear();
        for (GameSession.Fragment f : pool) {
            HBox row = new HBox(8, flow(f.text().strip()));
            row.getStyleClass().addAll("fragment", "code-area", "palette-" + phase.paletteIndex());
            row.setAlignment(Pos.CENTER_LEFT);
            if (!closed) {
                row.setCursor(Cursor.HAND);
                row.setOnMouseClicked(e -> {
                    pool.remove(f);
                    chosen.add(f);
                    clearFeedback();
                    render();
                });
            } else {
                row.getStyleClass().add("disabled");
            }
            poolBox.getChildren().add(row);
        }
        if (pool.isEmpty()) poolBox.getChildren().add(Ui.label("Hai usato tutti i frammenti.", "hint"));

        answerBox.getChildren().clear();
        for (int k = 0; k < chosen.size(); k++) {
            GameSession.Fragment f = chosen.get(k);
            final int idx = k;
            Label num = Ui.label(String.valueOf(k + 1), "line-number");
            num.setWrapText(false);
            num.setMinWidth(28);
            TextFlow code = flow(f.text());
            HBox.setHgrow(code, Priority.ALWAYS);
            Button up = Ui.btn("▲", "btn-icon");
            up.setTooltip(new Tooltip("Sposta su"));
            up.setDisable(closed || k == 0);
            up.setOnAction(e -> move(idx, -1));
            Button down = Ui.btn("▼", "btn-icon");
            down.setTooltip(new Tooltip("Sposta giù"));
            down.setDisable(closed || k == chosen.size() - 1);
            down.setOnAction(e -> move(idx, 1));
            Button rm = Ui.btn("✕", "btn-icon", "btn-remove");
            rm.setTooltip(new Tooltip("Rimuovi"));
            rm.setDisable(closed);
            rm.setOnAction(e -> {
                chosen.remove(idx);
                pool.add(f);
                clearFeedback();
                render();
            });
            HBox row = new HBox(8, num, code, up, down, rm);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("code-line");
            answerBox.getChildren().add(row);
        }
        if (chosen.isEmpty()) answerBox.getChildren().add(Ui.label("Clicca i frammenti a sinistra per comporre il codice.", "hint"));
        renderActions();
    }

    private TextFlow flow(String text) {
        TextFlow t = SyntaxHighlighter.highlight(text);
        t.setMaxWidth(Double.MAX_VALUE);
        return t;
    }

    private void move(int idx, int d) {
        int j = idx + d;
        if (j < 0 || j >= chosen.size()) return;
        GameSession.Fragment f = chosen.remove(idx);
        chosen.add(j, f);
        clearFeedback();
        render();
    }

    private void clearFeedback() {
        feedback.setText("");
        feedback.getStyleClass().removeAll("ok", "ko");
    }

    private void renderActions() {
        actions.getChildren().clear();
        if (!closed) {
            Button verify = Ui.btn("Verifica", "btn-primary", "btn-big");
            verify.setDisable(chosen.isEmpty());
            verify.setOnAction(e -> verify());
            Button reset = Ui.btn("Ricomincia");
            reset.setOnAction(e -> {
                pool.clear();
                chosen.clear();
                pool.addAll(level.fragments());
                clearFeedback();
                render();
            });
            Button give = Ui.btn("Mostra soluzione", "btn-danger");
            give.setOnAction(e -> giveUp());
            actions.getChildren().addAll(verify, reset, give);
        } else {
            if (free) {
                Button back = Ui.btn("Torna all'elenco", "btn-primary", "btn-big");
                back.setOnAction(e -> app.showList());
                Button game = Ui.btn("Torna al gioco");
                game.setOnAction(e -> app.play());
                actions.getChildren().addAll(back, game);
            } else {
                Button next = Ui.btn("Continua al quiz  →", "btn-primary", "btn-big");
                next.setOnAction(e -> app.goQuiz(level));
                actions.getChildren().add(next);
            }
        }
    }

    private void verify() {
        if (GameSession.isCorrect(phase, chosen)) {
            closed = true;
            app.session().recordExercise(phase, true, wrongAttempts, wrongAttempts > 0 ? lastWrong : GameSession.joinFragments(chosen), free, scoring);
            setFeedback(true, "Corretto! " + (scoring ? "+" + GameSession.EXERCISE_OK + " punti" : "(non conteggiato)")
                    + (wrongAttempts > 0 ? " · tentativi errati: " + wrongAttempts : ""));
        } else {
            wrongAttempts++;
            lastWrong = GameSession.joinFragments(chosen);
            int pts = app.session().exerciseWrongAttempt(scoring);
            List<String> exp = phase.exercise().lines();
            int right = 0;
            for (int k = 0; k < Math.min(exp.size(), chosen.size()); k++)
                if (GameSession.norm(exp.get(k)).equals(GameSession.norm(chosen.get(k).text()))) right++;
            String hint = chosen.size() != exp.size()
                    ? "La soluzione ha " + exp.size() + " righe, tu ne hai " + chosen.size() + "."
                    : right + " righe su " + exp.size() + " sono nella posizione giusta.";
            setFeedback(false, "Non ancora corretto" + (pts != 0 ? " (" + pts + " punti)" : "") + ". " + hint);
        }
        app.refreshTop();
        render();
    }

    private void giveUp() {
        closed = true;
        app.session().recordExercise(phase, false, wrongAttempts,
                wrongAttempts > 0 ? lastWrong : GameSession.joinFragments(chosen), free, scoring);
        setFeedback(false, "Ecco la soluzione corretta. L'esercizio è registrato come non risolto.");
        solutionBox.getChildren().setAll(Ui.label("Soluzione", "card-title"),
                Ui.codeBlock(phase.exercise().solution(), phase.paletteIndex()));
        solutionBox.getStyleClass().add("card");
        solutionBox.setVisible(true);
        solutionBox.setManaged(true);
        app.refreshTop();
        render();
    }

    private void setFeedback(boolean ok, String text) {
        feedback.getStyleClass().removeAll("ok", "ko");
        feedback.getStyleClass().add(ok ? "ok" : "ko");
        feedback.setText(text);
    }
}
